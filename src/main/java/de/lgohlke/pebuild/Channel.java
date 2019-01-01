package de.lgohlke.pebuild;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;

class Channel<T> implements AutoCloseable {

    private final TransferQueue<T> channel = new LinkedTransferQueue<>();

    private final CountDownLatch numberOfProducers;

    Channel(int numberOfProducers) {
        this.numberOfProducers = new CountDownLatch(numberOfProducers);
    }

    public void send(T element) {
        if (!isOpen()) {
            throw new ChannelClosedException();
        }
        channel.offer(element);
    }

    public boolean isOpen() {
        return numberOfProducers.getCount() > 0;
    }

    public T receive() throws InterruptedException {
        return channel.take();
    }

    @Override
    public void close() {
        numberOfProducers.countDown();
    }

    public static class ChannelClosedException extends RuntimeException {
    }
}
