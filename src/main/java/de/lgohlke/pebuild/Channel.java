package de.lgohlke.pebuild;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
class Channel<T> implements AutoCloseable, java.nio.channels.Channel {

    private final BlockingQueue<T> channel = new LinkedTransferQueue<>();
    private final AtomicInteger consumers = new AtomicInteger();
    private final AtomicInteger producers = new AtomicInteger();

    private final CountDownLatch numberOfProducers;

    /**
     * closes automatically when number of producers reached ZERO
     */
    Channel(int numberOfProducers) {
        this.numberOfProducers = new CountDownLatch(numberOfProducers);
    }

    @Override
    public boolean isOpen() {
        return numberOfProducers.getCount() > 0;
    }

    @Override
    @Deprecated
    public void close() {
        numberOfProducers.countDown();
    }

    public static class ChannelClosedException extends RuntimeException {
    }

    public static class NoConsumerException extends RuntimeException {
    }

    public Receiver registerReceiver() {
        return new Receiver();
    }

    public Sender registerSender() {
        return new Sender();
    }

    public class Receiver implements AutoCloseable {

        private Receiver() {
            consumers.incrementAndGet();
        }

        public T receive() throws InterruptedException {
            log.debug("receiving");
            while (isOpen()) {
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

    public class Sender implements AutoCloseable {
        private Sender() {
            producers.incrementAndGet();
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
        public void close() {
            log.debug("close sender");
            producers.decrementAndGet();
        }
    }
}
