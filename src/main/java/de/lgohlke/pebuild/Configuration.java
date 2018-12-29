package de.lgohlke.pebuild;

import java.nio.file.Paths;

public enum Configuration {

    FILE,
    REPORT_DIRECTORY;

    private final static String NAMESPACE = "PEBUILD";

    public String value() {
        return System.getProperty(fullName(), "");
    }

    private String fullName() {
        return NAMESPACE + "_" + name();
    }

    private void setIfMissing(String value) {
        if (!System.getProperties()
                   .containsKey(fullName())) {
            System.setProperty(fullName(), value);
        }
    }

    public static void configureDefaults() {
        FILE.setIfMissing("pebuild.yml");
        REPORT_DIRECTORY.setIfMissing(Paths.get("target", "pebuild.d")
                                           .toString());
    }

    public static void showConfig() {
        System.getProperties()
              .forEach((n, v) -> {
                  if (n instanceof String) {
                      String key = (String) n;
                      if (key.startsWith(NAMESPACE)) {
                          System.out.printf("%s = %s\n", n, v);
                      }
                  }
              });
    }
}
