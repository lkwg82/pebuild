package de.lgohlke.pebuild.graph

import de.lgohlke.pebuild.ExecutionResult
import de.lgohlke.pebuild.JobTrigger
import de.lgohlke.pebuild.StepExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.scheduler.Schedulers.parallel
import java.time.Duration
import java.time.Duration.ZERO
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.LongAdder
import java.util.logging.Level
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap
import kotlin.collections.set

class ExecutionGraph2Test {

    @RepeatedTest(100)
    fun `should execute in correct orders`() {
        val executions = Collections.synchronizedList(ArrayList<String>())

        val a = DummyExecutor("a", executions)
        val c = DummyExecutor("c", executions)
        val e = DummyExecutor("e", executions)
        val d = DummyExecutor("d", executions)
        val f = DummyExecutor("f", executions)
        val b = DummyExecutor("b", executions)

        c.waitFor(a)
        e.waitFor(a, c)
        d.waitFor(e)
        f.waitFor(e)
        b.waitFor(e, f)

        ExecutionGraph2.Builder()
                .addJobs(a, b, c, d, e, f)
                .build()
                .execute()

        assertThat(executions).startsWith("a", "c", "e")
        try {
            assertThat(executions).endsWith("d", "f", "b")
        } catch (e: AssertionError) {
            assertThat(executions).endsWith("f", "b", "d")
        }
    }

    @Test
    fun `graph execution should timeout`() {
        val step = object : DummyExecutor("step") {
            override fun runCommand(): ExecutionResult {
                TimeUnit.MILLISECONDS.sleep(5000)
                return ExecutionResult(0)
            }
        }

        val graph = ExecutionGraph2.Builder()
                .addJob(step)
                .timeout(Duration.ofMillis(1000))
                .build()

        val start = System.currentTimeMillis()
        graph.execute()
        val end = System.currentTimeMillis()

        assertThat(end - start).isLessThan(4500)
    }

    @Test
    fun `graph execution timeout should cancel the step`() {
        val cancelCounter = LongAdder()
        val step = object : DummyExecutor("step") {
            override fun runCommand(): ExecutionResult {
                TimeUnit.MILLISECONDS.sleep(5000)
                return ExecutionResult(0)
            }

            override fun cancel() {
                cancelCounter.increment()
            }
        }

        ExecutionGraph2.Builder()
                .addJob(step)
                .timeout(Duration.ofMillis(1000))
                .build()
                .execute()

        assertThat(cancelCounter.sum()).isEqualTo(1L)
    }

    //@Test
    // TODO
    fun printGraph() {
        val a = DummyExecutor("A")
        val b = DummyExecutor("B")
        b.waitFor(a)

        val graph = ExecutionGraph2.Builder()
                .addJobs(a, b)
                .build()

        assertThat(graph.toString()).isEqualTo("(A [])->(B [A])")
    }

    @Test
    fun `should fail adding the same step twice`() {
        val a = DummyExecutor("A")

        val builder = ExecutionGraph2.Builder()
                .addJob(a)

        assertThrows<ExecutionGraph2.Builder.DuplicateJobException> { builder.addJob(a) }
    }

    open class DummyExecutor(private val name2: String,
                             private val executions: MutableList<String> = ArrayList()) :
            StepExecutor(name2, "command $name2", ZERO, JobTrigger(name2)) {

        override fun runCommand(): ExecutionResult {
            executions.add(name2)
            return ExecutionResult(0)
        }
    }
}

class ExecutionGraph2(private val steps: Collection<StepExecutor>,
                      private val finalStep: StepExecutor,
                      private val timeout: Duration) {
    fun execute() {
        val cachedSteps = steps.map { step ->
            step to Mono.just(step)
                    .doOnSubscribe { step.execute() }
                    .cache()
                    .doOnCancel { step.cancel() }
        }
                .toMap()

        val graph = createExecutionGraph(finalStep, cachedSteps).log(null, Level.WARNING)

        try {
            val subscribedGraph = graph.subscribeOn(parallel())
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

        private val executors = HashSet<StepExecutor>()
        private var timeout = ZERO

        fun addJob(executor: StepExecutor): Builder {
            if (!executors.add(executor)) {
                throw DuplicateJobException("$executor is already added")
            }
            return this
        }

        private fun createFinalStep(executors: Collection<StepExecutor>): StepExecutor {
            val finalSteps = createSuccessorMap(executors)
                    .filter { (_, successors) -> successors.isEmpty() }
                    .map { (node, _) -> node }
            val theFinalStep = object : StepExecutor("end", "", ZERO, JobTrigger("end")) {}
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

        fun build(): ExecutionGraph2 {
            val finalStep = createFinalStep(executors)
            executors.add(finalStep)
            return ExecutionGraph2(executors, finalStep, timeout)
        }

        class DuplicateJobException(message: String) : RuntimeException(message)
    }
}
