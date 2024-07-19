package org.example.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Logger {
    private static final String LOG_FILE_PATH = "src/main/resources/logs.txt";

    public synchronized static void log(String event, Class<?> type) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE_PATH, true))){
            writer.println("[" + System.currentTimeMillis() + "] " + " [" + type.getSimpleName() + "]:" +  event);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
