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

    private val channel = Channel<String>(2)
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
    fun `is open`() {
        assertThat(channel.isOpen).isTrue()
    }

    @Test
    fun `is closed`() {
        channel.close()
        channel.close()

        assertThat(channel.isOpen).isFalse()
    }

    @Test
    fun `happy path`() {
        startReceiver()


        oneTimeSender("hello")
        oneTimeSender("world")

        assertThat(received.take()).isEqualTo("hello")
        assertThat(received.take()).isEqualTo("world")
    }

    private fun startReceiver() {
        service.submit(receiver)
        TimeUnit.MILLISECONDS.sleep(100)
    }

    private fun oneTimeSender(message: String) {
        val sender = Runnable {
            channel.registerSender().use {
                it.send(message)
            }
        }
        service.submit(sender)
    }

    @Test
    fun `partial closed`() {
        startReceiver()
        oneTimeSender("hello")
        channel.close()
        oneTimeSender("world")


        assertThat(received.take()).isEqualTo("hello")
        assertThat(received.take()).isEqualTo("world")
    }

    @Test
    fun `should not be able to send after closed`() {
        startReceiver()

        oneTimeSender("hello")
        channel.close()
        channel.close()

        try {
            channel.registerSender().send("world")
            fail("should fail")
        } catch (e: Channel.ChannelClosedException) {
            // ok
        }
    }

    @Test
    fun `should not be able to send when there is no consumer`() {
        try {
            channel.registerSender().send("hello")
            fail("should fail when consumer is missing")
        } catch (e: Channel.NoConsumerException) {
            // ok
        }
    }
}