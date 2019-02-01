package de.lgohlke.streamutils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.RepeatedTest
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.LoggerFactory
import reactor.core.CoreSubscriber
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers.elastic
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class MergingStreamFascade2Test {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)

        init {
            System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.streamutils.MergingStreamFascade", "DEBUG")
            System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.streamutils.NotifyWaiter", "DEBUG")

            System.setProperty("org.slf4j.simpleLogger.showDateTime", "true")
            System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true")
            System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "HH:mm:ss:SSS")
        }
    }

    private val stdoutIntern = ByteArrayOutputStream()
    private val stdout = PrintStream(stdoutIntern, true)
    private val fileOutputStream = ByteArrayOutputStream()

    @RepeatedTest(10)
    fun `should have STDOUT output collected in file`() {
        val inputStreams = createInputStreams("ok", "")

        mergingStreamFascadeCreate("test", inputStreams, System.out, arrayOf(fileOutputStream)).use {
            doSomeThing()
        }

        val content = String(fileOutputStream.toByteArray())
        assertThat(content).contains("STDOUT ok")
    }

    class MergingStreamFascade2(private val jobName: String,
                                private val inputStreams: Array<PrefixedInputStream>,
                                private val stdout: PrintStream,
                                private val outputStreams: Array<OutputStream>) : AutoCloseable {

        private var connectableFlux: Flux<String> = Flux.empty()

        class Builder {
            var name = "noname"
            var inputStreams: Array<PrefixedInputStream> = arrayOf()
            var outputStreams: Array<OutputStream> = arrayOf()
            var stdout: PrintStream = System.out

            fun name(name: String): Builder {
                this.name = name
                return this
            }

            fun inputStreams(inputStreams: Array<PrefixedInputStream>): Builder {
                this.inputStreams = inputStreams
                return this
            }

            fun outputStreams(outputStreams: Array<OutputStream>): Builder {
                this.outputStreams = outputStreams
                return this
            }

            fun stdout(stdout: PrintStream): Builder {
                this.stdout = stdout
                return this
            }

            fun build(): MergingStreamFascade2 {
                val fascade2 = MergingStreamFascade2(name, inputStreams, stdout, outputStreams)
                fascade2.start()
                return fascade2
            }
        }

        private fun start() {

            val inputs = Flux.fromIterable(inputStreams.map { DecoratingStreamer(it) })
            val outputs = ArrayList<Subscriber<String>>(outputStreams.map { OutputStreamer(it) })
            outputs.add(StdoutStreamer(jobName, stdout))

            val source = Flux.merge(inputs)
            connectableFlux = source.publish().autoConnect(outputs.size)

            outputs.forEach { out ->
                connectableFlux.subscribeOn(elastic()).subscribe { out.onNext(it) }
            }
        }

        override fun close() {
            log.warn("closed")
        }

        internal open class OutputStreamer(private val outputStream: OutputStream) : Subscriber<String> {
            companion object {
                @Suppress("JAVA_CLASS_ON_COMPANION")
                private val log =
                        LoggerFactory
                            .getLogger(javaClass.enclosingClass)
            }

            override fun onSubscribe(s: Subscription?) {
                log
                    .warn("subscribed")
            }

            override fun onNext(line: String?) {
                outputStream
                    .write("$line\n".toByteArray())
            }

            override fun onComplete() {
                log
                    .warn("complete")
            }

            override fun onError(t: Throwable?) {
                log
                    .error(t?.message, t)
            }
        }

        internal class StdoutStreamer(private val jobName: String,
                                      private val stdout: PrintStream) : OutputStreamer(stdout) {
            override fun onNext(line: String?) {
                stdout.println("[$jobName] $line")
            }
        }

        class DecoratingStreamer(private val prefixedInputStream: PrefixedInputStream) : Flux<String>() {
            companion object {
                @Suppress("JAVA_CLASS_ON_COMPANION")
                private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
            }

            override fun subscribe(p0: CoreSubscriber<in String>) {
                val prefix = prefixedInputStream.prefix
                try {
                    Scanner(prefixedInputStream.stream).use { scanner ->
                        log.warn("scanner started: {}", prefix)
                        while (scanner.hasNextLine()) {
                            val nextLine = scanner.nextLine()
                            log.warn("scanner [{}] send: {}", prefix, nextLine)
                            p0.onNext("$prefix $nextLine")
                        }
                        p0.onComplete()
                    }
                } catch (e: Exception) {
                    log.error(e.message, e)
                    p0.onError(e)
                }
                log.warn("scanner closed: {}", prefix)
            }
        }
    }

    @RepeatedTest(10)
    fun `should have STDOUT output printed to System out`() {
        val inputStreams = createInputStreams("ok", "")

        mergingStreamFascadeCreate("test", inputStreams, stdout, arrayOf()).use {
            doSomeThing()
        }

        val content = String(stdoutIntern.toByteArray())
        assertThat(content).contains("[test] STDOUT ok")
    }

    private fun mergingStreamFascadeCreate(name: String,
                                           inputStreams: Array<PrefixedInputStream>,
                                           stdout: PrintStream,
                                           outputStreams: Array<OutputStream>): MergingStreamFascade2 {
        return MergingStreamFascade2
            .Builder()
            .name(name)
            .inputStreams(inputStreams)
            .stdout(stdout)
            .outputStreams(outputStreams)
            .build()
    }

    @RepeatedTest(10)
    fun `should have STDERR output printed to SystemOut`() {
        val inputStreams = createInputStreams("", "err")

        mergingStreamFascadeCreate("test", inputStreams, stdout, arrayOf()).use {
            doSomeThing()
        }

        val content = String(stdoutIntern.toByteArray())
        assertThat(content).contains("[test] STDERR err")
    }

    private fun doSomeThing() {
        log.warn("do something")
        TimeUnit.MILLISECONDS.sleep(50)
    }

    @RepeatedTest(10)
    fun `should have STDERR output collected in filestream`() {
        val inputStreams = createInputStreams("", "err")

        mergingStreamFascadeCreate("test", inputStreams, stdout, arrayOf(fileOutputStream)).use {
            doSomeThing()
        }

        val content = String(fileOutputStream.toByteArray())
        assertThat(content).contains("STDERR err")
    }

    private fun createInputStreams(stdout: String,
                                   stderr: String): Array<PrefixedInputStream> {
        val out = PrefixedInputStream(ByteArrayInputStream(stdout.toByteArray()), "STDOUT")
        val err = PrefixedInputStream(ByteArrayInputStream(stderr.toByteArray()), "STDERR")
        return arrayOf(out, err)
    }
}