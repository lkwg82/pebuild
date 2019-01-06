package de.lgohlke.streamutils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

@Slf4j
class NotifyWaiter {
    private final CountDownLatch receiverStopped = new CountDownLatch(1);
    private final CountDownLatch receiverStarted = new CountDownLatch(1);

    private final CountDownLatch senderStarted;
    private final CountDownLatch senderStopped;

    NotifyWaiter(int numberOfSender) {
        senderStarted = new CountDownLatch(numberOfSender);
        senderStopped = new CountDownLatch(numberOfSender);
    }

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
        log.debug("wait for receiver stopped :{}", receiverStopped);
        await(receiverStopped);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            // ok
            Thread.currentThread()
                  .interrupt();
        }
    }

    void notifySenderStarted() {
        senderStarted.countDown();
    }

    void notifySenderStopped() {
        senderStopped.countDown();
    }

    void notifyReceiverStopped() {
        log.debug("notify receiver stopped:{}", receiverStopped);
        receiverStopped.countDown();
    }

    void notifyReceiverStarted() {
        receiverStarted.countDown();
    }


}
