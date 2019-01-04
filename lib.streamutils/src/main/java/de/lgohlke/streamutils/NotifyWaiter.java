package de.lgohlke.streamutils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

@Slf4j
class NotifyWaiter {
    private final CountDownLatch receiverStopped = new CountDownLatch(1);
    private final CountDownLatch receiverStarted = new CountDownLatch(1);
    private final CountDownLatch senderStarted = new CountDownLatch(2);
    private final CountDownLatch senderStopped = new CountDownLatch(2);

    void waitForReceiverStarted() {
        await(receiverStarted);
    }

    void waitForSenderStarted() {
        await(senderStarted);
    }

    void waitForSenderStopped() {
        await(senderStopped);
    }

    void waitForReceiverStopped() {
        await(receiverStopped);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    void notifySenderStarted() {
        senderStarted.countDown();
    }

    void notifySenderStopped() {
        senderStopped.countDown();
    }

    void notifyReceiverStopped() {
        receiverStopped.countDown();
    }

    void notifyReceiverStarted() {
        receiverStarted.countDown();
    }


}
