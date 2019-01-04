package de.lgohlke.streamutils;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class MergingStreamFascade implements AutoCloseable {
    private final Channel<String> combinedOutput = new Channel<>();
    private final ExecutorService service = Executors.newFixedThreadPool(3);

    private final NotifyWaiter notifyWaiter = new NotifyWaiter();
    private final String jobName;
    private final InputStream stdout;
    private final InputStream stderr;
    private final Path file;

    public static MergingStreamFascade create(String name,
                                              InputStream inputStream,
                                              InputStream errorStream,
                                              Path outputFile) {
        val fascade = new MergingStreamFascade(name, inputStream, errorStream, outputFile);
        fascade.start();
        return fascade;
    }

    private void start() {
        service.submit(() -> printToFileAndOut(jobName));
        service.submit(() -> captureStream(stderr, "STDERR"));
        service.submit(() -> captureStream(stdout, "STDOUT"));
    }

    private void captureStream(InputStream inputStream, String prefix) {
        new DecoratingStreamer(inputStream, prefix, combinedOutput, notifyWaiter).capture();
    }

    @Override
    public void close() {
        notifyWaiter.waitForSenderStarted();
        notifyWaiter.waitForSenderStopped();
        combinedOutput.close();
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
            try (val f = new FileOutputStream(file.toFile())) {
                try (val receiver = combinedOutput.registerReceiver()) {
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

                        System.out.println("[" + jobName + "] " + line);
                        try {
                            f.write((line + "\n").getBytes());
                        } catch (IOException e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
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
