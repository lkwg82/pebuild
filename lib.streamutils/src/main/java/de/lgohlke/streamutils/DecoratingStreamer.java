package de.lgohlke.streamutils;

import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Scanner;

@Slf4j
class DecoratingStreamer {
    private final Channel<String> channel;
    private final NotifyWaiter notifyWaiter;
    private final PrefixedInputStream prefixedInputStream;

    DecoratingStreamer(PrefixedInputStream prefixedInputStream,
                       Channel<String> channel,
                       NotifyWaiter notifyWaiter) {
        this.prefixedInputStream = prefixedInputStream;
        this.channel = channel;
        this.notifyWaiter = notifyWaiter;
    }


    void capture() {
        notifyWaiter.waitForReceiverStarted();
        String prefix = prefixedInputStream.getPrefix();

        try (val scanner = new Scanner(prefixedInputStream.getStream())) {

            log.debug("scanner started: {}", prefix);
            notifyWaiter.notifySenderStarted();

            while (scanner.hasNextLine()) {
                channel.send(prefix + " " + scanner.nextLine());
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        log.debug("scanner closed: {}", prefix);
        notifyWaiter.notifySenderStopped();
    }
}
