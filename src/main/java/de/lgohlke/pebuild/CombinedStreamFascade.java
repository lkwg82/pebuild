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

// TODO test
@RequiredArgsConstructor
@Slf4j
class CombinedStreamFascade {
    private final TransferQueue<String> combinedOutput = new LinkedTransferQueue<>();

    private final String jobName;
    private final InputStream stdout;
    private final InputStream stderr;
    private final Path file;

    private final ExecutorService service = Executors.newFixedThreadPool(3);

    void start() {
        service.submit(createRunnable(stderr, "STDERR"));
        service.submit(createRunnable(stdout, "STDOUT"));
        service.submit(consumer(jobName));
    }

    void stop() {
        try {
            service.awaitTermination(1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Runnable consumer(String jobName) {
        return () -> {
            try (val f = new FileOutputStream(file.toFile())) {
                String line;
                while (true) {
                    try {
                        line = combinedOutput.take();
                        System.out.println("[" + jobName + "] " + line);
                        f.write((line + "\n").getBytes());
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        };
    }

    private Runnable createRunnable(InputStream inputStream, String prefix) {
        return () -> {
            Scanner sc = new Scanner(inputStream);
            while (sc.hasNextLine()) {
                combinedOutput.offer(prefix + " " + sc.nextLine());
            }
        };
    }
}
