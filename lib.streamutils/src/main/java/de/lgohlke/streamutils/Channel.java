package de.lgohlke.streamutils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
class Channel<T> {

    private final BlockingQueue<T> channel = new SynchronousQueue<>();

    private final AtomicBoolean closed = new AtomicBoolean(true);
    private final AtomicInteger consumers = new AtomicInteger();

    public void close() {
        log.debug("close channel");
        closed.set(true);
    }

    public boolean isClosed() {
        return closed.get();
    }

    public void send(T element) {
        log.debug("try sending: {}", element);
        if (consumers.get() == 0) {
            throw new NoConsumerException();
        }

        if (isClosed()) {
            throw new ChannelClosedException();
        }

        try {
            channel.put(element);
            log.debug("sent: {}", element);
        } catch (InterruptedException e) {
            log.debug("could not send: {} ({})", element, e.getMessage());
            Thread.currentThread().interrupt();
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
            T item = channel.take();
            log.debug("received:{}", item);
            return item;
        }

        public boolean isConsumable() {
            boolean isOpen = !closed.get();
            boolean hasItem = !channel.isEmpty();
            log.debug("try receive closed: {}", hasItem);
            log.debug("try receive has items: {}", isOpen);
            return isOpen || hasItem;
        }

        @Override
        public void close() {
            log.debug("close receiver");
            consumers.decrementAndGet();
        }
    }
}
