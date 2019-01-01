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
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Slf4j
class CombinedStreamFascade {
    private final Channel<String> combinedOutput = new Channel<>();
    private final ExecutorService service = Executors.newFixedThreadPool(3);

    private final String jobName;
    private final InputStream stdout;
    private final InputStream stderr;
    private final Path file;

    void start() {
        service.submit(() -> printToFileAndOut(jobName));
        service.submit(() -> captureStream(stderr, "STDERR"));
        service.submit(() -> captureStream(stdout, "STDOUT"));
    }

    void stop() {
        try {
            service.awaitTermination(1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void printToFileAndOut(String jobName) {
        log.debug("started consumer: {}", jobName);
        try (val f = new FileOutputStream(file.toFile())) {
            try (val receiver = combinedOutput.registerReceiver()) {
                String line;
                while (receiver.isConsumable()) {
                    try {
                        line = receiver.receive();
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                        return;
                    }

                    System.out.println("[" + jobName + "] " + line);
                    f.write((line + "\n").getBytes());
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            log.debug("finished consumer:{}", jobName);
        }
    }

    private void captureStream(InputStream inputStream, String prefix) {
        try (val sc = new Scanner(inputStream)) {
            log.debug("scanner started: {}", prefix);
            while (sc.hasNextLine()) {
                combinedOutput.send(prefix + " " + sc.nextLine());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
