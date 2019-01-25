package de.lgohlke.pebuild.graph

import de.lgohlke.pebuild.JobTrigger
import de.lgohlke.pebuild.StepExecutor
import lombok.NonNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import reactor.core.CoreSubscriber
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toFlux
import reactor.test.StepVerifier
import reactor.test.test
import java.time.Duration
import java.util.ArrayList
import java.util.LinkedHashSet
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.LongAdder
import java.util.logging.Level
import kotlin.collections.LinkedHashMap
import kotlin.collections.set

class ExecutionGraph2Test {
    private val root = Node("root", setOf(), true)
    private val nodes = ArrayList<Node>()

    @BeforeEach
    internal fun setUp() {

        val a = Node("A", root)
        val c = Node("C", a)
        val e = Node("E", a, c)
        val d = Node("D", e)
        val f = Node("F", e)
        val b = Node("B", e, f)

        nodes.addAll(setOf(a, b, c, d, e, f))

        val endNodes = createSuccessorMap()
                .filter { (_, successors) -> successors.isEmpty() }
                .map { (node, _) -> node }
        val end = Node("end", endNodes.toSet())

        nodes.add(root)
        nodes.add(end)
    }

    @Test
    fun `should create successor map`() {
        val successorMap = createSuccessorMap().toString()

        assertThat(successorMap).isEqualTo("{root*=[A], A=[C, E], B=[], C=[E], D=[], E=[B, D, F], F=[B]}")
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
        val jobs = convertNodesToStepExecutor(nodes)
        val sortedJobs = TopologicalSorter.sort(jobs)
        val sortedNodes = convertSortedJobsToNodeList(sortedJobs)
//        println(sortedJobs)
        println(sortedNodes)

        val end = nodes.last()

        println()
        val fluxTree = creatFluxTree(end).log(null, Level.WARNING)
//                .subscribeOn(elastic())

        StepVerifier
                .create(fluxTree)
                .expectNext("root", "a")
                .expectNext("root", "a", "c", "e")
                .expectNext("root", "a")
                .expectNext("root", "a", "c", "e", "f", "b")
                .expectNext("root", "a")
                .expectNext("root", "a", "c", "e", "d")
                .expectNext("end")
                .expectComplete()
                .verifyThenAssertThat(Duration.ofMillis(600))
    }

    private fun creatFluxTree(node: Node): Flux<String> {
        return if (node.predecessors.isEmpty()) {
            println("last node: $node")
            node.cache().toFlux()
        } else {
            println("predecessors $node -> ${node.predecessors}")

            val preFluxes = node.predecessors.map { p -> creatFluxTree(p) }
            val merge = Flux.merge(Flux.fromIterable(preFluxes))
            Flux.concat(merge, node)
        }
    }

    private fun convertSortedJobsToNodeList(sortedJobs: Collection<StepExecutor>): Collection<Node> {
        return sortedJobs.map { j ->
            nodes.first { n -> n.name === j.name }
        }
    }

    private fun convertNodesToStepExecutor(nodes: ArrayList<Node>): Collection<StepExecutor> {
        val jobs = hashMapOf<String, StepExecutor>()
        for (n in nodes) {
            jobs[n.name] = DummyStepExecutor(n.name)
        }

        for (n in nodes) {
            for (p in n.predecessors) {

                val stepExecutor = jobs[p.name]
                if (stepExecutor != null) {
                    jobs[n.name]?.waitFor(stepExecutor)
                }
            }
        }


        return jobs.values
    }

    class DummyStepExecutor(name: String,
                            command: @NonNull String = "",
                            timeout: @NonNull Duration = Duration.ZERO,
                            jobTrigger: @NonNull JobTrigger = JobTrigger("")) : StepExecutor(name,
            command,
            timeout,
            jobTrigger)


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

    class Node(internal val name: String,
               val predecessors: Set<Node> = setOf(),
               private var terminated: Boolean = false) : Mono<String>() {

        constructor(name: String, vararg predecessors: Node) : this(name, predecessors.toSet())

        private val executed = AtomicBoolean(false)

        companion object {
            private val log = LoggerFactory.getLogger(this::class.java)
        }

        override fun subscribe(actual: CoreSubscriber<in String>) {
            if (executed.compareAndSet(false, true)) {
                execute()
                actual.onNext(name.toLowerCase())
                actual.onComplete()
            } else {
                log.warn("tried executing $name")
            }
        }

        private fun execute() {
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