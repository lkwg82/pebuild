package de.lgohlke.pebuild;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;

@RequiredArgsConstructor
@Slf4j
class CombinedStreamFascade {
    private final TransferQueue<String> combinedOutput = new LinkedTransferQueue<>();

    private final String jobName;
    private final InputStream stdout;
    private final InputStream stderr;
    private final Path file;

    private final static String MAGIC_END_STRING = "\0END\0";

    private final ExecutorService service = Executors.newFixedThreadPool(3);

    void start() {
        service.submit(createRunnable(stderr, "STDERR"));
        service.submit(createRunnable(stdout, "STDOUT"));
        service.submit(printToFileAndOut(jobName));
    }

    void stop() {
        try {
            service.awaitTermination(1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Runnable printToFileAndOut(String jobName) {
        return () -> {
            log.debug("started consumer: {}", jobName);
            try (val f = new FileOutputStream(file.toFile())) {
                int finishCounter = 0;
                String line;
                while (true) {
                    try {
                        line = combinedOutput.take();
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                        return;
                    }

                    if (line.startsWith(MAGIC_END_STRING)) {
                        finishCounter++;
                        if (log.isDebugEnabled()) {
                            log.debug("received end from {}", line.replaceFirst(MAGIC_END_STRING, ""));
                        }
                        if (2 == finishCounter) {
                            return;
                        }
                    } else {
                        System.out.println("[" + jobName + "] " + line);
                        f.write((line + "\n").getBytes());
                    }
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            } finally {
                log.debug("finished consumer:{}", jobName);
            }
        };
    }

    private Runnable createRunnable(InputStream inputStream, String prefix) {
        return () -> {
            try (val sc = new Scanner(inputStream)) {
                log.debug("scanner started: {}", prefix);
                while (sc.hasNextLine()) {
                    combinedOutput.offer(prefix + " " + sc.nextLine());
                }
                log.debug("scanner finished: {}", prefix);
                combinedOutput.offer(MAGIC_END_STRING + prefix);
            }
        };
    }
}
