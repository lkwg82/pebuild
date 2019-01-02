package de.lgohlke.pebuild;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
class CombinedStreamFascade implements AutoCloseable {
    private final Channel<String> combinedOutput = new Channel<>();
    private final ExecutorService service = Executors.newFixedThreadPool(3);
    private final CountDownLatch shutDownReceiver = new CountDownLatch(1);
    private final CountDownLatch receiverStarted = new CountDownLatch(1);
    private final CountDownLatch senderAtLeastStarted = new CountDownLatch(2);

    private final String jobName;
    private final InputStream stdout;
    private final InputStream stderr;
    private final Path file;

    static CombinedStreamFascade create(String name, InputStream inputStream, InputStream errorStream, Path outputFile) {
        val fascade = new CombinedStreamFascade(name, inputStream, errorStream, outputFile);
        fascade.start();
        return fascade;
    }

    private void start() {
        service.submit(() -> printToFileAndOut(jobName));
        service.submit(() -> captureStream(stderr, "STDERR"));
        service.submit(() -> captureStream(stdout, "STDOUT"));
    }

    @Override
    public void close() {
        try {
            senderAtLeastStarted.await();
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
        combinedOutput.close();
        shutDownReceiver.countDown();

        try {
            service.awaitTermination(1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void printToFileAndOut(String jobName) {
        log.debug("started consumer: {}", jobName);

        Runnable receiverRun = () -> {
            try (val f = new FileOutputStream(file.toFile())) {
                try (val receiver = combinedOutput.registerReceiver()) {
                    receiverStarted.countDown();
                    String line;
                    while (receiver.isConsumable()) {
                        try {
                            line = receiver.receive();
                        } catch (InterruptedException e) {
                            log.error(e.getMessage(), e);
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

        try {
            shutDownReceiver.await();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                threadExecutor.awaitTermination(1, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                log.error(e.getMessage(), e);
            }
        }

        log.debug("finished consumer:{}", jobName);
    }

    private void captureStream(InputStream inputStream, String prefix) {
        try {
            receiverStarted.await();
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }

        try (val sc = new Scanner(inputStream)) {
            log.debug("scanner started: {}", prefix);
            senderAtLeastStarted.countDown();
            while (sc.hasNextLine()) {
                combinedOutput.send(prefix + " " + sc.nextLine());
            }
            log.debug("scanner closed: {}", prefix);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
