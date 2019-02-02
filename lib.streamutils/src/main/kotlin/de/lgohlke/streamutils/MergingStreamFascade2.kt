package de.lgohlke.streamutils

import org.reactivestreams.Subscriber
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers.elastic
import java.io.OutputStream
import java.io.PrintStream
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * reads from many InputStreams and write them to
 * - a PrintStream (mostly System.out)
 * - and many other OutputStreams
 */
class MergingStreamFascade2(private val jobName: String,
                            private val inputStreams: Array<PrefixedInputStream>,
                            private val stdout: PrintStream,
                            private val outputStreams: Array<OutputStream>) {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    private var connectableFlux: Flux<String> = Flux.empty()

    fun install() {

        val inputs = Flux.fromIterable(inputStreams.map { DecoratingStreamer(it) })
        val outputs = ArrayList<Subscriber<String>>(outputStreams.map { OutputStreamer(it) })
        outputs.add(StdoutStreamer(jobName, stdout))

        val countDownLatch = CountDownLatch(outputs.size)

        val source = Flux.merge(inputs)
        connectableFlux =
                source.publish().autoConnect(outputs.size).doOnSubscribe { countDownLatch.countDown() }

        outputs.forEach { out -> connectableFlux.subscribeOn(elastic()).subscribe { out.onNext(it) } }

        countDownLatch.await()

        // needed to give threads some time to fire up
        // else it could be the source streams already closed before everything was consumed
        // this is mostly for very short running executions (e.g. in case of errors)
        TimeUnit.MILLISECONDS.sleep(50)
    }
}