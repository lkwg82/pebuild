package de.lgohlke.pebuild;

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class ChannelTest {
    private val channel = Channel<String>(2)

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
        channel.send("hello")
        channel.send("world")

        assertThat(channel.receive()).isEqualTo("hello")
        assertThat(channel.receive()).isEqualTo("world")
    }

    @Test
    fun `partial closed`() {
        channel.send("hello")
        channel.close()
        channel.send("world")

        assertThat(channel.receive()).isEqualTo("hello")
        assertThat(channel.receive()).isEqualTo("world")
    }

    @Test
    fun `should not be able to send after closed`() {
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

}