package de.lgohlke.pebuild;

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.concurrent.Executors
import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.TimeUnit

class ChannelTest {
    companion object {
        init {
            System.setProperty("org.slf4j.simpleLogger.log.de.lgohlke.pebuild.Channel", "DEBUG")
        }
    }

    private val channel = Channel<String>()
    private val service = Executors.newFixedThreadPool(10)
    private val received = LinkedTransferQueue<String>()
    private val receiver = Runnable {
        channel.registerReceiver().use { r ->
            while (channel.isOpen) {
                received.put(r.receive())
            }
        }
    }

    @AfterEach
    internal fun tearDown() {
        service.awaitTermination(10, TimeUnit.MILLISECONDS)
    }

    @Test
    fun `should be open without any former registered sender`() {
        assertThat(channel.isOpen).isTrue()
    }

    @Test
    fun `is closed`() {
        channel.close()

        assertThat(channel.isOpen).isFalse()
    }

    @Test
    fun `happy path`() {
        startReceiver()

        channel.send("hello")
        assertThat(received.take()).isEqualTo("hello")

        channel.send("world")
        assertThat(received.take()).isEqualTo("world")
    }

    private fun startReceiver() {
        service.submit(receiver)
        TimeUnit.MILLISECONDS.sleep(10)
    }

    @Test
    fun `should not be able to send after closed`() {
        startReceiver()

        channel.send("hello")
        channel.close()

        try {
            channel.send("world")
            fail("should fail")
        } catch (e: Channel.ChannelClosedException) {
            // ok
        }
    }

    @Test
    fun `should not be able to send when there is no consumer`() {
        try {
            channel.send("hello")
            fail("should fail when consumer is missing")
        } catch (e: Channel.NoConsumerException) {
            // ok
        }
    }
}