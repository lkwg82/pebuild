package de.lgohlke.pebuild.graph

import de.lgohlke.pebuild.ExecutionResult
import de.lgohlke.pebuild.StepExecutor
import de.lgohlke.pebuild.graph.ExecutionGraph2.Builder.DuplicateJobException
import de.lgohlke.pebuild.graph.validators.CycleValidator.CycleDetected
import de.lgohlke.pebuild.graph.validators.ReferencedJobMissingValidator.ReferencedJobsMissing
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Duration.ZERO
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.LongAdder
import kotlin.collections.ArrayList

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

        assertThrows<DuplicateJobException> { builder.addJob(a) }
    }

    @Test
    fun `should fail building a cycling graph`() {
        val a = DummyExecutor("A")
        val b = DummyExecutor("B")

        a.waitFor(b)
        b.waitFor(a)

        val builder = ExecutionGraph2.Builder()
                .addJob(a)
                .addJob(b)

        assertThrows<CycleDetected> { builder.build() }
    }

    @Test
    fun `should fail building an incomplete graph`() {
        val a = DummyExecutor("A")
        val b = DummyExecutor("B")

        a.waitFor(b)
        b.waitFor(a)

        val builder = ExecutionGraph2.Builder()
                .addJob(a)

        assertThrows<ReferencedJobsMissing> { builder.build() }
    }

    open class DummyExecutor(private val name2: String,
                             private val executions: MutableList<String> = ArrayList()) :
            StepExecutor(name2, "command $name2", ZERO) {

        override fun runCommand(): ExecutionResult {
            executions.add(name2)
            return ExecutionResult(0)
        }
    }
}

