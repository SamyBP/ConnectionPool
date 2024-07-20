package org.example.configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Configuration {
    private static final Properties CONFIG_PROPERTIES = new Properties();
    private static final String CONFIG_FILE_PATH = "src/main/resources/configuration-dev.properties";

    static {
        try (FileInputStream inputStream = new FileInputStream(CONFIG_FILE_PATH)){
            CONFIG_PROPERTIES.load(inputStream);
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getProperty(String key) {
        return CONFIG_PROPERTIES.getProperty(key);
    }
}
