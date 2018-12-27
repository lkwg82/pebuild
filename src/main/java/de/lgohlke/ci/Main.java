package de.lgohlke.ci;

public class Main {
    public static void main(String... args) {
        String cmd = String.join(" ", args);

        System.out.println("executing '" + cmd + "'");
        ShellExecutor.executeInheritedIO(cmd);
    }
}
