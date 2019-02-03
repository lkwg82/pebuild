package de.lgohlke.pebuild.graph

import de.lgohlke.pebuild.ExecutionResult
import de.lgohlke.pebuild.StepExecutor
import de.lgohlke.pebuild.graph.ExecutionGraph.Builder.DuplicateJobException
import de.lgohlke.pebuild.graph.validators.CycleValidator.CycleDetected
import de.lgohlke.pebuild.graph.validators.ReferencedJobMissingValidator.ReferencedJobsMissing
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Duration.ofMillis
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.LongAdder
import kotlin.collections.ArrayList

class ExecutionGraphTest {

    //    @RepeatedTest(100)
    @Test
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

        ExecutionGraph.Builder()
            .addJobs(a, b, c, d, e, f)
            .build()
            .execute()

        assertThat(executions).startsWith("a", "c", "e")
        try {
            assertThat(executions).endsWith("d", "f", "b")
        } catch (e: AssertionError) {
            try {
                assertThat(executions).endsWith("f", "b", "d")
            } catch (e: AssertionError) {
                assertThat(executions).endsWith("f", "d", "b")
            }
        }

        Metrics.globalRegistry.meters.forEach { meter ->
            if (meter is Timer) {
                println(meter.id.name)
                println(" total ms: (#" + meter.count() + ") " + meter.totalTime(TimeUnit.MILLISECONDS))
            }
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

        val graph = ExecutionGraph.Builder().addJob(step).timeout(Duration.ofMillis(1000)).build()

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

        ExecutionGraph.Builder().addJob(step).timeout(Duration.ofMillis(1000)).build().execute()

        assertThat(cancelCounter.sum()).isEqualTo(1L)
    }

    @Test
    fun `graph execution timeout of step should cancel the graph`() {
        val executions = ArrayList<String>(2)
        val stepWithTimeout = object : StepExecutor("step", "command", ofMillis(1000)) {
            override fun runCommand(): ExecutionResult {
                executions.add(name)
                TimeUnit.MILLISECONDS.sleep(5000)
                return ExecutionResult(0)
            }
        }
        val stepAfter = object : DummyExecutor("2nd step") {
            override fun runCommand(): ExecutionResult {
                executions.add(name)
                return super.runCommand()
            }
        }
        stepAfter.waitFor(stepWithTimeout)

        val stepParallel = object : DummyExecutor("side step") {
            override fun runCommand(): ExecutionResult {
                executions.add(name)
                return super.runCommand()
            }
        }

        val graph = ExecutionGraph.Builder().addJobs(stepWithTimeout, stepParallel, stepAfter).build()
        graph.execute()

        assertThat(executions).containsOnly("step", "side step")
    }


    @Test
    fun `graph execution steps should not prevent timeouts`() {
        val stepWithTimeout = object : StepExecutor("step", "command", ofMillis(1000)) {
            override fun runCommand(): ExecutionResult {
                TimeUnit.MILLISECONDS.sleep(5000)
                return ExecutionResult(0)
            }
        }

        val stepParallel = object : DummyExecutor("side step") {
            override fun runCommand(): ExecutionResult {
                TimeUnit.MILLISECONDS.sleep(15000)
                return super.runCommand()
            }
        }

        val graph = ExecutionGraph.Builder().addJobs(stepWithTimeout, stepParallel).build()

        val start = System.currentTimeMillis()
        graph.execute()
        val end = System.currentTimeMillis()

        assertThat(end - start).isBetween(0, 1500)
    }

    // TODO
//    @Test
    internal fun `graph execution timeout of step should cancel other running steps`() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    //@Test
    // TODO
    fun printGraph() {
        val a = DummyExecutor("A")
        val b = DummyExecutor("B")
        b.waitFor(a)

        val graph = ExecutionGraph.Builder().addJobs(a, b).build()

        assertThat(graph.toString()).isEqualTo("(A [])->(B [A])")
    }

    @Test
    fun `should fail adding the same step twice`() {
        val a = DummyExecutor("A")

        val builder = ExecutionGraph.Builder().addJob(a)

        assertThrows<DuplicateJobException> { builder.addJob(a) }
    }

    @Test
    fun `should fail building a cycling graph`() {
        val a = DummyExecutor("A")
        val b = DummyExecutor("B")

        a.waitFor(b)
        b.waitFor(a)

        val builder = ExecutionGraph.Builder().addJob(a).addJob(b)

        assertThrows<CycleDetected> { builder.build() }
    }

    @Test
    fun `should fail building an incomplete graph`() {
        val a = DummyExecutor("A")
        val b = DummyExecutor("B")

        a.waitFor(b)
        b.waitFor(a)

        val builder = ExecutionGraph.Builder().addJob(a)

        assertThrows<ReferencedJobsMissing> { builder.build() }
    }

    // TODO see https://github.com/reactor/reactor-core/issues/1504
    @Test
    fun metrics() {
        Metrics.globalRegistry.add(SimpleMeterRegistry())

        val mono = Mono.fromRunnable<Any>(Runnable {
            println("sleep 1s")
            TimeUnit.SECONDS.sleep(1)
        }).name("test")

        mono.metrics()
            .subscribe()

        Metrics.globalRegistry.meters.forEach { m ->
            if (m is Timer) {
                print(m.id.name + " " + m.id.tags[1] + " " + m.id.tags[2].value)
                println(" total time " + m.totalTime(TimeUnit.MILLISECONDS) + "ms")
            }
        }
    }

    open class DummyExecutor(private val name2: String,
                             private val executions: MutableList<String> = ArrayList()) :
            StepExecutor(name2, "command $name2") {

        override fun runCommand(): ExecutionResult {
            executions.add(name2)
            return ExecutionResult(0)
        }
    }
}

