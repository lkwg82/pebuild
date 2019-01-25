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
import reactor.core.scheduler.Schedulers.parallel
import reactor.test.test
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.LongAdder
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

        val graph = createExecutionGraph(end, cachedNodes).log(null, Level.WARNING)

        graph.subscribeOn(parallel())
                .subscribe { v -> print(" $v ->") }

        TimeUnit.MILLISECONDS.sleep(500)

        assertThat(executedNodes).startsWith(root, a, c, e, f, b, d)
    }

    private fun createExecutionGraph(node: Node, cachedNodes: Map<Node, Mono<Node>>): Flux<Node> {
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

        constructor(name: String, vararg predecessors: Node) : this(name, predecessors.toSet())

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