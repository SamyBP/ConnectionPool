package org.example;


import org.example.pooling.BasicConnectionPool;
import org.example.pooling.CleanupTask;
import org.example.pooling.ConnectionPool;
import org.example.utils.Logger;

import java.util.Timer;
import java.util.TimerTask;

public class Main {

    public static int TEST_DURATION = 1;
    public static int THREAD_COUNT = 4;
    public static String EXPECTED_CLEANUP_BEHAVIOUR = "[TEST_CLEANUP] [EXPECTING] Closes one connection after first cleanup, 4 connections after second cleanup";
    public static String EXPECTED_ASSIGNATION_BEHAVIOUR = "[TEST_CONNECTION_ASSIGNATION] [EXPECTING] Assigns 4 distinct connections at first task, creates 4 connections after second task";
    public static void main(String[] args) {
        ConnectionPool connectionPool = new BasicConnectionPool();

        Logger.log(
                "[TEST] [MaxConnectionCount] " + ConnectionPool.MAX_CONNECTION_COUNT +
                " [MaxIdlePeriod] " + ConnectionPool.MAX_IDLE_PERIOD +
                " [CleanupRate] " + ConnectionPool.POOL_CLEANUP_RATE +
                " [TestDuration] " + TEST_DURATION * 60 * 1000,
                Main.class
        );
        Logger.log(EXPECTED_CLEANUP_BEHAVIOUR, Main.class);
        Logger.log(EXPECTED_ASSIGNATION_BEHAVIOUR, Main.class);

        Timer timer = new Timer();
        TimerTask cleanupTask = new CleanupTask(connectionPool);
        TimerTask testTask = new TestTask(TEST_DURATION, THREAD_COUNT, connectionPool);

        timer.schedule(cleanupTask, connectionPool.POOL_CLEANUP_RATE, connectionPool.POOL_CLEANUP_RATE);
        timer.schedule(testTask, 0, connectionPool.POOL_CLEANUP_RATE * 2);
    }
}