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

        @JvmStatic
        fun create(name: String,
                   inputStreams: Array<PrefixedInputStream>,
                   out: PrintStream,
                   outputStreams: Array<OutputStream>): MergingStreamFascade2 {
            val fascade2 = MergingStreamFascade2(name, inputStreams, out, outputStreams)
            fascade2.start()
            return fascade2
        }

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
}