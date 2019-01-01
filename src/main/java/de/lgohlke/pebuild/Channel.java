package de.lgohlke.pebuild;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
class Channel<T> {

    private final AtomicInteger consumers = new AtomicInteger();
    private final AtomicBoolean open = new AtomicBoolean(true);

    private final BlockingQueue<T> channel;

    Channel(int bufferSize) {
        channel = new ArrayBlockingQueue<>(bufferSize);
    }

    Channel() {
        this(1);
    }

    public void close() {
        log.debug("close channel");
        open.set(false);
    }

    public boolean isReadyForSend() {
        return open.get();
    }

    public void send(T element) {
        log.debug("try sending: {}", element);
        if (consumers.get() == 0) {
            throw new NoConsumerException();
        }

        if (isReadyForSend()) {
            try {
                channel.put(element);
                log.debug("sent: {}", element);
            } catch (InterruptedException e) {
                log.debug("could not send: {} ({})", element, e.getMessage());
            }
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
            while (isConsumable()) {
                T item = channel.poll(10, TimeUnit.MILLISECONDS);
                if (null != item) {
                    log.debug("received:{}", item);
                    return item;
                }
            }
            throw new InterruptedException("channel closed");
        }

        public boolean isConsumable() {
            boolean isOpen = open.get();
            boolean hasItem = !channel.isEmpty();
            log.debug("try receive open: {}", hasItem);
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
