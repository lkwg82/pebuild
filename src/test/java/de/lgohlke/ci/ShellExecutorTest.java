package de.lgohlke.ci;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static org.assertj.core.api.Assertions.assertThat;

class ShellExecutorTest {
    @Test
    void executeInShell() throws IOException {
        String command = "env";

        String output = ShellExecutor.execute(command);

        assertThat(output).contains("HOME=/home");
    }

    private static class ShellExecutor {
        private static String execute(String command) throws IOException {
            String[] wrappedInShell = new String[]{"sh", "-c", command};

            Process process = new ProcessBuilder(wrappedInShell)
                    .start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));
            }
            return builder.toString();
        }
    }
}