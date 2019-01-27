package de.lgohlke.pebuild.graph

import de.lgohlke.pebuild.StepExecutor
import de.lgohlke.pebuild.graph.validators.CycleValidator
import de.lgohlke.pebuild.graph.validators.ReferencedJobMissingValidator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.util.LinkedHashSet
import java.util.logging.Level
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class ExecutionGraph2(private val steps: Collection<StepExecutor>,
                      private val finalStep: StepExecutor,
                      private val timeout: Duration) {
    fun execute() {
        val cachedSteps = steps.map { step ->
            step to Mono.fromRunnable<StepExecutor>(step::execute)
                    .cache()
                    .doOnCancel { step.cancel() }
        }
                .toMap()

        val graph = createExecutionGraph(finalStep, cachedSteps).log(null, Level.WARNING)

        try {
            val subscribedGraph = graph.subscribeOn(Schedulers.parallel())
            if (timeout.isZero) {
                subscribedGraph.blockLast()
            } else {
                subscribedGraph.blockLast(timeout)
            }
        } catch (e: java.lang.IllegalStateException) {
            System.err.println("execution timeout: " + e.message)
        }
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
        private var timeout = Duration.ZERO

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
            val theFinalStep = object : StepExecutor("end", "", Duration.ZERO) {}
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

        fun build(): ExecutionGraph2 {
            validate()
            val finalStep = createFinalStep(steps)
            steps.add(finalStep)
            return ExecutionGraph2(steps, finalStep, timeout)
        }

        class DuplicateJobException(message: String) : RuntimeException(message)
    }
}