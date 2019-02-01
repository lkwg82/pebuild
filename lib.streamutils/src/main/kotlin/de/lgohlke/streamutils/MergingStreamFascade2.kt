package de.lgohlke.streamutils

import org.reactivestreams.Subscriber
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.io.OutputStream
import java.io.PrintStream
import java.util.*

/**
 * reads from many InputStreams and write them to
 * - a PrintStream (mostly System.out)
 * - and many other OutputStreams
 */
class MergingStreamFascade2(private val jobName: String,
                            private val inputStreams: Array<PrefixedInputStream>,
                            private val stdout: PrintStream,
                            private val outputStreams: Array<OutputStream>) : AutoCloseable {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    private var connectableFlux: Flux<String> = Flux.empty()

    private fun start() {

        val inputs = Flux.fromIterable(inputStreams.map { DecoratingStreamer2(it) })
        val outputs = ArrayList<Subscriber<String>>(outputStreams.map { OutputStreamer(it) })
        outputs.add(StdoutStreamer(jobName, stdout))

        val source = Flux.merge(inputs)
        connectableFlux = source.publish().autoConnect(outputs.size)

        outputs.forEach { out ->
            connectableFlux.subscribeOn(Schedulers.elastic()).subscribe { out.onNext(it) }
        }
    }

    override fun close() {
        log.warn("closed")
    }

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
}