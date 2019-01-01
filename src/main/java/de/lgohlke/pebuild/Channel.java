package de.lgohlke.pebuild;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
class Channel<T> implements java.nio.channels.Channel {

    private final AtomicInteger consumers = new AtomicInteger();
    private final AtomicBoolean open = new AtomicBoolean(true);

    private final BlockingQueue<T> channel;

    Channel(int bufferSize) {
        if (bufferSize > 1) {
            channel = new ArrayBlockingQueue<>(bufferSize);
        } else {
            channel = new SynchronousQueue<>();
        }
    }

    Channel() {
        this(1);
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public void close() {
        log.debug("close channel");
        open.set(false);
    }

    public void send(T element) {
        if (consumers.get() == 0) {
            throw new NoConsumerException();
        }

        if (isOpen()) {
            channel.offer(element);
        } else {
            throw new ChannelClosedException();
        }
    }


    public static class ChannelClosedException extends RuntimeException {
    }

    public static class NoConsumerException extends RuntimeException {
    }

    public Receiver registerReceiver() {
        return new Receiver();
    }

    public class Receiver implements AutoCloseable {

        private Receiver() {
            consumers.incrementAndGet();
        }

        public T receive() throws InterruptedException {
            log.debug("receiving");
            while (isOpen() || channel.size() > 0) {
                log.debug("try receive");
                T item = channel.poll(10, TimeUnit.MILLISECONDS);
                if (null != item) {
                    log.debug("received:{}", item);
                    return item;
                }
            }
            throw new InterruptedException("channel closed");
        }

        @Override
        public void close() {
            log.debug("close receiver");
            consumers.decrementAndGet();
        }
    }
}
