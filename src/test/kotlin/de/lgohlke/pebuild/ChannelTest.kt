package de.lgohlke.pebuild;

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.concurrent.CountDownLatch
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
    private val service = Executors.newCachedThreadPool()
    private val received = LinkedTransferQueue<String>()
    private val receiver = Runnable {
        channel.registerReceiver().use { r ->
            while (r.isConsumable) {
                received.put(r.receive())
            }
        }
    }

    @AfterEach
    internal fun tearDown() {
        service.awaitTermination(10, TimeUnit.MILLISECONDS)
    }

    @Test
    fun `should be open`() {
        assertThat(channel.isReadyForSend).isTrue()
    }

    @Test
    fun `is closed`() {
        channel.close()

        assertThat(channel.isReadyForSend).isFalse()
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

    //    @Test
    @RepeatedTest(100)
    fun `should consume last item on slow receiver when sender closed already`() {
        val receiverStarted = CountDownLatch(1)
        val receiverFinished = CountDownLatch(1)

        val receiver = Runnable {
            channel.registerReceiver().use { r ->
                while (r.isConsumable) {
                    receiverStarted.countDown()
                    val receive = r.receive()
                    TimeUnit.MILLISECONDS.sleep(10)
                    println("received")
                    received.put(receive)
                }
                receiverFinished.countDown()
            }
        }
        service.submit(receiver)

        receiverStarted.await()
        channel.send("hello1")
        channel.send("hello2")
        channel.close()

        receiverFinished.await()
        assertThat(received.size).isEqualTo(2)
    }
}