package de.lgohlke.pebuild.graph

import de.lgohlke.pebuild.ExecutionResult
import de.lgohlke.pebuild.JobTrigger
import de.lgohlke.pebuild.StepExecutor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import reactor.core.CoreSubscriber
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.core.scheduler.Schedulers.parallel
import reactor.test.test
import java.time.Duration
import java.time.Duration.ZERO
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.LongAdder
import java.util.logging.Level
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap
import kotlin.collections.set

class ExecutionGraph2Test {
    private val root = Node("root", setOf(), true)
    private val nodes = ArrayList<Node>()

    private val a = Node("A", root)
    private val c = Node("C", a)
    private val e = Node("E", a, c)
    private val d = Node("D", e)
    private val f = Node("F", e)
    private val b = Node("B", e, f)
    private var end = Node("end")

    @BeforeEach
    internal fun setUp() {
        nodes.addAll(setOf(root, a, b, c, d, e, f))

        val endNodes = createSuccessorMap()
                .filter { (_, successors) -> successors.isEmpty() }
                .map { (node, _) -> node }
        end = Node("end", endNodes.toSet())

        nodes.add(end)
    }

    @Test
    fun cachedMono() {
        val counter = LongAdder()
        val mono = object : Mono<String>() {
            override fun subscribe(p0: CoreSubscriber<in String>) {
                counter.increment()
                p0.onNext("executed " + counter.toInt())
                p0.onComplete()
            }
        }

        val cache = mono.cache()

        cache.test()
                .expectNext("executed 1")
                .expectComplete()
                .verifyThenAssertThat(Duration.ofMillis(300))
        cache.test()
                .expectNext("executed 1")
                .expectComplete()
                .verifyThenAssertThat(Duration.ofMillis(300))
    }

    @Test
    fun `should execute in correct order`() {
        val executions = Collections.synchronizedList(ArrayList<String>())

        val a = DummyExecutor("a", executions)
        val c = DummyExecutor("c", executions)
        val e = DummyExecutor("e", executions)
        val d = DummyExecutor("d", executions)
        val f = DummyExecutor("f", executions)
        val b = DummyExecutor("b", executions)

        c.waitFor(a)
        e.waitFor(a)
        e.waitFor(c)
        d.waitFor(e)
        f.waitFor(e)
        b.waitFor(e)
        b.waitFor(f)

        ExecutionGraph2.Builder()
                .addJobs(a, b, c, d, e, f)
                .build()
                .execute()

        assertThat(executions).containsExactly("a", "c", "e", "f", "b", "d")
    }

    @Test
    fun `should run with stepExecutors`() {

        val executions = Collections.synchronizedList(ArrayList<String>())

        val s1 = DummyExecutor("s1", executions)
        val s2 = DummyExecutor("s2", executions)

        s2.waitFor(s1)

        ExecutionGraph2.Builder()
                .addJob(s1)
                .addJob(s2)
                .build()
                .execute()

        assertThat(executions).containsExactly("s1", "s2")
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

        assertThat(end - start).isLessThan(2500)
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

    open class DummyExecutor(private val name2: String,
                             private val executions: MutableList<String> = ArrayList()) :
            StepExecutor(name2, "command $name2", ZERO, JobTrigger(name2)) {

        override fun runCommand(): ExecutionResult {
            executions.add(name2)
            return ExecutionResult(0)
        }
    }

    private fun createExecutionGraph(node: Node,
                                     cachedNodes: Map<Node, Mono<Node>>): Flux<Node> {
        val thisNode = cachedNodes[node]?.toFlux() ?: throw IllegalStateException("missing node in cached map: $node")
        val preFluxes = node.predecessors.map { predecessor -> createExecutionGraph(predecessor, cachedNodes) }
        val merge = Flux.merge(Flux.fromIterable(preFluxes))
        return Flux.concat(merge, thisNode)
    }

    private fun createSuccessorMap(): Map<Node, Set<Node>> {
        val successorMap = LinkedHashMap<Node, MutableSet<Node>>()

        nodes.forEach { node ->
            successorMap[node] = LinkedHashSet()
        }

        nodes.forEach { node ->
            node.predecessors.forEach { n ->
                successorMap[n]?.add(node)
            }
        }
        return successorMap
    }

    class Node(private val name: String,
               val predecessors: Set<Node> = setOf(),
               private var terminated: Boolean = false) {

        constructor(name: String,
                    vararg predecessors: Node) : this(name, predecessors.toSet())

        companion object {
            private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        }

        fun execute() {
            val t = Thread.currentThread()
                    .name
            log.warn("execute {} on {}", name, t)
        }

        override fun toString(): String {
            var str = name
            if (terminated) {
                str += "*"
            }
            return str
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

        private val executors = ArrayList<StepExecutor>()
        private var timeout = ZERO

        fun addJob(executor: StepExecutor): Builder {
            executors.add(executor)
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
    }
}
