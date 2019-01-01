package de.lgohlke.pebuild;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
class Channel<T> implements AutoCloseable, java.nio.channels.Channel {

    private final TransferQueue<T> channel = new LinkedTransferQueue<>();
    private final AtomicInteger consumers = new AtomicInteger();

    private final CountDownLatch numberOfProducers;

    /**
     * closes automatically when number of producers reached ZERO
     */
    Channel(int numberOfProducers) {
        this.numberOfProducers = new CountDownLatch(numberOfProducers);
    }

    public void send(T element) {
        if (!isOpen()) {
            throw new ChannelClosedException();
        }
        if (consumers.get() == 0) {
            throw new NoConsumerException();
        }
        channel.offer(element);
    }

    @Override
    public boolean isOpen() {
        return numberOfProducers.getCount() > 0;
    }

    public T receive() throws InterruptedException {
        try {
            consumers.incrementAndGet();
            while (isOpen()) {
                T item = channel.poll(10, TimeUnit.MILLISECONDS);
                if (null != item) {
                    return item;
                }
            }
            throw new InterruptedException("channel closed");
        } finally {
            consumers.decrementAndGet();
        }
    }

    @Override
    public void close() {
        numberOfProducers.countDown();
    }

    public static class ChannelClosedException extends RuntimeException {
    }

    public static class NoConsumerException extends RuntimeException {
    }
}
