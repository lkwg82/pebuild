package de.lgohlke.streamutils;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

        this.notifyWaiter = new NotifyWaiter(inputStreams.length);
        this.service = Executors.newFixedThreadPool(1 + inputStreams.length);
    }

    @Deprecated
    public static MergingStreamFascade create(String name,
                                              InputStream inputStream,
                                              InputStream errorStream,
                                              OutputStream outputStream) {

        return create(name,
                      new PrefixedInputStream[]{
                              new PrefixedInputStream(inputStream, "STDOUT"),
                              new PrefixedInputStream(errorStream, "STDERR")},
                      System.out,
                      new OutputStream[]{outputStream});
    }

    public static MergingStreamFascade create(String name,
                                              PrefixedInputStream[] inputStreams,
                                              PrintStream systemOut,
                                              OutputStream[] outputStreams) {

        val fascade = new MergingStreamFascade(name, inputStreams, systemOut, outputStreams);
        fascade.start();
        return fascade;
    }

    private void start() {
        service.submit(() -> printToFileAndOut(jobName));
        for (final PrefixedInputStream inputStream : inputStreams) {
            service.submit(() -> captureStream(inputStream));
        }
    }

    private void captureStream(PrefixedInputStream prefixedInputStream) {
        new DecoratingStreamer(prefixedInputStream, channel, notifyWaiter).capture();
    }

    @Override
    public void close() {
        notifyWaiter.waitForSenderStarted();
        notifyWaiter.waitForSenderStopped();
        channel.close();
        notifyWaiter.notifyReceiverStopped();

        try {
            service.awaitTermination(1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }

    private void printToFileAndOut(String jobName) {
        log.debug("started consumer: {}", jobName);

        Runnable receiverRun = () -> {
            try (val receiver = channel.registerReceiver()) {
                notifyWaiter.notifyReceiverStarted();
                String line;
                while (receiver.isConsumable()) {
                    try {
                        line = receiver.receive();
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                        Thread.currentThread().interrupt();
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
            threadExecutor.awaitTermination(1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        log.debug("finished consumer:{}", jobName);
    }
}
