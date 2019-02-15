package de.lgohlke.pebuild.graph

import de.lgohlke.pebuild.StepExecutor
import de.lgohlke.pebuild.graph.validators.CycleValidator
import de.lgohlke.pebuild.graph.validators.ReferencedJobMissingValidator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.scheduler.Schedulers.elastic
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class ExecutionGraph(val steps: Collection<StepExecutor>,
                     private val finalStep: StepExecutor,
                     val timeout: Duration) {

    fun execute() {
        val timingContext = TimingContext()
        timingContext.time("__global__") { innerExecute(timingContext) }
        timingContext.save()
    }

    private fun innerExecute(timingContext: TimingContext) {
        val cachedSteps = steps.map { step -> step to createCachedMono(step, timingContext) }.toMap()
        val graph = createExecutionGraph(finalStep, cachedSteps).log()

        graph.timeout(timeout)
            // fail fast
            // stop the whole graph in case of timeout or another error
            .onErrorResume { Mono.empty() }
            .blockLast()
    }

    class TimingContext {
        data class Timing(val start: Long,
                          val end: Long)

        private val timings = ConcurrentHashMap<String, Long>()
        private val finishedTimings = ConcurrentLinkedQueue<Timing>()

        private fun start(name: String) {
            timings[name] = System.currentTimeMillis()
        }

        private fun stop(name: String) {
            val start = timings[name] ?: throw IllegalStateException("could not stop a not started task: $name")
            val end = System.currentTimeMillis()

            finishedTimings.add(Timing(start, end))
            timings.remove(name)
        }

        fun save() {
//            finishedTimings.toArray()
            // TODO
        }

        fun time(name: String,
                 execution: () -> Unit) {
            start(name)
            try {
                execution()
            } finally {
                stop(name)
            }
        }
    }

    // TODO
    class StepError(val step: StepExecutor,
                    private val error: Throwable) : java.lang.RuntimeException(error)

    private fun createCachedMono(step: StepExecutor,
                                 timingContext: TimingContext): Mono<StepExecutor> {
        val runnable = Runnable {
            timingContext.time(step.name) { step.execute() }
        }
        return Mono.fromRunnable<StepExecutor>(runnable)
            .name(step.name)
            .subscribeOn(elastic())
            .timeout(step.timeout)
            .onErrorMap { error -> StepError(step, error) }
            .doOnError { step.cancel() }
            .cache()
            .doOnCancel { step.cancel() }
    }

    private fun createExecutionGraph(step: StepExecutor,
                                     cachedNodes: Map<StepExecutor, Mono<StepExecutor>>): Flux<StepExecutor> {
        val thisNode = cachedNodes[step]?.toFlux() ?: throw IllegalStateException("missing step in cached map: $step")
        val preFluxes = step.waitForJobs.map { predecessor -> createExecutionGraph(predecessor, cachedNodes) }
        val merge = Flux.merge(Flux.fromIterable(preFluxes))
        return Flux.concat(merge, thisNode)
    }

    class Builder {

        private val steps = HashSet<StepExecutor>()
        private var timeout = Duration.ofDays(999)

        fun addJob(executor: StepExecutor): Builder {
            if (!steps.add(executor)) {
                throw DuplicateJobException("$executor is already added")
            }
            return this
        }

        private fun createFinalStep(executors: Collection<StepExecutor>): StepExecutor {
            val finalSteps = createSuccessorMap(executors)
                .filter { (_, successors) -> successors.isEmpty() }
                .map { (node, _) -> node }
            val theFinalStep = object : StepExecutor("end", "") {}
            finalSteps.forEach { step -> theFinalStep.waitFor(step) }
            return theFinalStep
        }

        private fun createSuccessorMap(steps: Collection<StepExecutor>): Map<StepExecutor, Set<StepExecutor>> {
            val successorMap = LinkedHashMap<StepExecutor, MutableSet<StepExecutor>>()

            steps.forEach { step ->
                successorMap[step] = LinkedHashSet()
            }

            steps.forEach { step ->
                step.waitForJobs.forEach { s ->
                    successorMap[s]?.add(step)
                }
            }
            return successorMap
        }

        fun timeout(timeout: Duration): Builder {
            this.timeout = timeout
            return this
        }

        fun addJobs(vararg jobs: StepExecutor): Builder {
            jobs.forEach { addJob(it) }
            return this
        }

        private fun validate() {
            ReferencedJobMissingValidator.validate(steps)
            CycleValidator.validate(steps)
        }

        fun build(): ExecutionGraph {
            validate()
            val finalStep = createFinalStep(steps)
            steps.add(finalStep)
            return ExecutionGraph(steps, finalStep, timeout)
        }

        class DuplicateJobException(message: String) : RuntimeException(message)
    }
}