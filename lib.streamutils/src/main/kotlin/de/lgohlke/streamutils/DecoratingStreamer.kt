package de.lgohlke.streamutils

import org.slf4j.LoggerFactory
import reactor.core.CoreSubscriber
import reactor.core.publisher.Flux
import java.util.*

internal class DecoratingStreamer(private val prefixedInputStream: PrefixedInputStream) : Flux<String>() {
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
            if (e is NullPointerException) {
                log.error("", e)
            } else {
                log.error(e.message, e)
            }
            p0.onError(e)
        }
        log.warn("scanner closed: {}", prefix)
    }
}