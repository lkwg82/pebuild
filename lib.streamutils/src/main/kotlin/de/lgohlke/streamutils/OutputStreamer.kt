package de.lgohlke.streamutils

import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.slf4j.LoggerFactory
import java.io.OutputStream

internal open class OutputStreamer(private val outputStream: OutputStream) : Subscriber<String> {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        private val log = LoggerFactory.getLogger(javaClass.enclosingClass)
    }

    override fun onSubscribe(s: Subscription?) {
        log.warn("subscribed")
    }

    override fun onNext(line: String?) {
        outputStream.write("$line\n".toByteArray())
    }

    override fun onComplete() {
        log.warn("complete")
    }

    override fun onError(t: Throwable?) {
        log.error(t?.message, t)
    }
}