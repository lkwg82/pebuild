package de.lgohlke.pebuild;

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.concurrent.Executors
import java.util.concurrent.LinkedTransferQueue
import java.util.concurrent.TimeUnit

class ChannelTest {
    private val channel = Channel<String>(2)
    private val service = Executors.newFixedThreadPool(1)
    private val received = LinkedTransferQueue<String>()
    private val receiver = Runnable {
        while (true) {
            received.put(channel.receive())
        }
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

        channel.send("hello")
        channel.send("world")

        assertThat(received.take()).isEqualTo("hello")
        assertThat(received.take()).isEqualTo("world")
    }

    private fun startReceiver() {
        service.submit(receiver)
        TimeUnit.MILLISECONDS.sleep(10)
    }

    @Test
    fun `partial closed`() {
        startReceiver()
        channel.send("hello")
        channel.close()
        channel.send("world")

        assertThat(received.take()).isEqualTo("hello")
        assertThat(received.take()).isEqualTo("world")
    }

    @Test
    fun `should not be able to send after closed`() {
        startReceiver()
        
        channel.send("hello")
        channel.close()
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