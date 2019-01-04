package de.lgohlke.streamutils;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.InputStream;
import java.util.Scanner;

@Slf4j
class DecoratingStreamer {
    private final Channel<String> channel;
    private final NotifyWaiter notifyWaiter;
    private final PrefixedInputStream prefixedInputStream;

    @Deprecated
    DecoratingStreamer(InputStream inputStream,
                       String prefix,
                       Channel<String> channel,
                       NotifyWaiter notifyWaiter) {
        this(new PrefixedInputStream(inputStream, prefix), channel, notifyWaiter);
    }

    DecoratingStreamer(PrefixedInputStream prefixedInputStream,
                       Channel<String> channel,
                       NotifyWaiter notifyWaiter) {
        this.prefixedInputStream = prefixedInputStream;
        this.channel = channel;
        this.notifyWaiter = notifyWaiter;
    }


    void capture() {
        notifyWaiter.waitForReceiverStarted();

        try (val scanner = new Scanner(prefixedInputStream.getStream())) {
            String prefix = prefixedInputStream.getPrefix();

            log.debug("scanner started: {}", prefix);
            notifyWaiter.notifySenderStarted();

            sendLinesOverChannel(scanner);

            log.debug("scanner closed: {}", prefix);
            notifyWaiter.notifySenderStopped();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void sendLinesOverChannel(Scanner sc) {
        String prefix = prefixedInputStream.getPrefix();
        while (sc.hasNextLine()) {
            channel.send(prefix + " " + sc.nextLine());
        }
    }
}
