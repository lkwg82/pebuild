package de.lgohlke.streamutils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.io.InputStream;
import java.util.Scanner;

@RequiredArgsConstructor
@Slf4j
class DecoratingStreamer {
    private final InputStream inputStream;
    private final String prefix;
    private final Channel<String> channelTodelegateTo;
    private final NotifyWaiter notifyWaiter;

    void capture() {
        notifyWaiter.waitForReceiverStarted();

        try (val scanner = new Scanner(inputStream)) {
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
        while (sc.hasNextLine()) {
            channelTodelegateTo.send(prefix + " " + sc.nextLine());
        }
    }
}
