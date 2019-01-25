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
import reactor.core.scheduler.Schedulers.elastic
import reactor.core.scheduler.Schedulers.parallel
import reactor.test.test
import java.lang.IllegalStateException
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
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

    @BeforeEach
    internal fun setUp() {
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
        val executedNodes = Collections.synchronizedList(ArrayList<Node>())
        val cachedNodes = nodes.map { n ->
            n to Mono.just(n).doOnSubscribe {
                n.execute()
                executedNodes.add(n)
            }.cache()
        }.toMap()
        val jobs = convertNodesToStepExecutor(nodes)
        val sortedJobs = TopologicalSorter.sort(jobs)
        val sortedNodes = convertSortedJobsToNodeList(sortedJobs)
//        println(sortedJobs)
        // println(sortedNodes)

        val end = nodes.last()

        val fluxTree = createFluxTree(end, cachedNodes)//.log(null, Level.WARNING)
                .subscribeOn(parallel())

        fluxTree.subscribe()

        TimeUnit.MILLISECONDS.sleep(500)

        assertThat(executedNodes).startsWith(root, a, c, e, f, b, d)
    }

    private fun createFluxTree(node: Node, cachedNodes: Map<Node, Mono<Node>>): Flux<Node> {
        val thisNode = cachedNodes[node]?.toFlux() ?: throw IllegalStateException("missing node in cached map: $node")
        return if (node.predecessors.isEmpty()) {
            //println("last node: $node")
            thisNode
        } else {
            //println("predecessors $node -> ${node.predecessors}")

            val preFluxes = node.predecessors.map { p -> createFluxTree(p, cachedNodes) }
            val merge = Flux.merge(Flux.fromIterable(preFluxes))
            Flux.concat(merge, thisNode)
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
               private var terminated: Boolean = false) {

        constructor(name: String, vararg predecessors: Node) : this(name, predecessors.toSet())

        companion object {
            private val log = LoggerFactory.getLogger(this::class.java)
        }

        fun execute() {
            val t = Thread.currentThread()
                    .name
            log.warn("execute {} on {}", name, t)
            TimeUnit.MILLISECONDS.sleep(70)
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