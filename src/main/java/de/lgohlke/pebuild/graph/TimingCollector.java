package de.lgohlke.pebuild.graph;

import de.lgohlke.pebuild.Configuration;
import de.lgohlke.pebuild.TimingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.TransferQueue;

@Slf4j
@RequiredArgsConstructor
class TimingCollector implements Runnable {
    private final TransferQueue<TimingContext> timingContextChannel;
    private final Callable<Boolean> keepRunning;

    // TODO test
    @Override
    public void run() {
        log.info("started");

        val reportDir = Configuration.REPORT_DIRECTORY.value();

        new File(reportDir).mkdirs();

        val timming = Paths.get(reportDir, "timings")
                           .toFile();
        try (OutputStream outputStream = new FileOutputStream(timming)) {
            do {
                try {
                    val timingContext = timingContextChannel.take();
                    log.info("received:{}", timingContext);

                    val content = (timingContext + "\n").getBytes();
                    outputStream.write(content);
                } catch (InterruptedException | IOException e) {
                    log.error(e.getMessage(), e);
                }
            } while (keepRunning.call());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            log.info("finished");
        }
    }
}
