package de.lgohlke.streamutils;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * reads from many InputStreams and write them to
 * - a PrintStream (mostly System.out)
 * - and many other OutputStreams
 */
@Slf4j
public class MergingStreamFascade implements AutoCloseable {
    private final Channel<String> channel = new Channel<>();
    private final ExecutorService service;

    private final NotifyWaiter notifyWaiter;
    private final String jobName;
    private final PrintStream systemOut;
    private final OutputStream[] outputStreams;
    private final PrefixedInputStream[] inputStreams;

    private MergingStreamFascade(String jobName,
                                 PrefixedInputStream[] inputStreams,
                                 PrintStream systemOut,
                                 OutputStream[] outputStreams) {
        this.jobName = jobName;
        this.systemOut = systemOut;
        this.outputStreams = outputStreams;
        this.inputStreams = inputStreams;

        notifyWaiter = new NotifyWaiter(inputStreams.length);
        service = Executors.newFixedThreadPool(1 + inputStreams.length);
    }

    public static MergingStreamFascade create(@NonNull String name,
                                              @NonNull PrefixedInputStream[] inputStreams,
                                              @NonNull PrintStream systemOut,
                                              @NonNull OutputStream[] outputStreams) {
        val fascade = new MergingStreamFascade(name, inputStreams, systemOut, outputStreams);
        fascade.start();
        return fascade;
    }

    private void start() {
        service.submit(() -> receiver(jobName));

        for (PrefixedInputStream inputStream : inputStreams) {
            service.submit(() -> sender(inputStream));
        }
    }

    private void sender(PrefixedInputStream inputStream) {
        new DecoratingStreamer(inputStream, channel, notifyWaiter).capture();
    }

    @Override
    public void close() {
        log.debug("closing");
        notifyWaiter.waitForSenderStarted();
        notifyWaiter.waitForSenderStopped();
        channel.close();
        notifyWaiter.notifyReceiverStopped();

        try {
            service.shutdownNow();
            log.debug("shutting down");
            service.awaitTermination(1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            Thread.currentThread()
                  .interrupt();
        }
    }

    private void receiver(String jobName) {
        log.debug("started consumer: {}", jobName);

        Runnable receiverRun = () -> {
            try (val receiver = channel.registerReceiver()) {
                notifyWaiter.notifyReceiverStarted();
                String line;
                while (receiver.isConsumable()) {
                    try {
                        line = receiver.receive();
                    } catch (InterruptedException e) {
                        // dont log, this is intended
                        Thread.currentThread()
                              .interrupt();
                        return;
                    }

                    systemOut.println("[" + jobName + "] " + line);
                    for (val out : outputStreams) {
                        try {
                            out.write((line + "\n").getBytes());
                        } catch (IOException e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            }

        };

        val threadExecutor = Executors.newSingleThreadScheduledExecutor();
        threadExecutor.submit(receiverRun);

        notifyWaiter.waitForReceiverStopped();
        try {
            threadExecutor.shutdownNow();
            threadExecutor.awaitTermination(1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // dont log, this is intended
            Thread.currentThread()
                  .interrupt();
        }

        log.debug("finished consumer:{}", jobName);
    }
}
