package org.example;


import org.example.pooling.BasicConnectionPool;
import org.example.pooling.CleanupTask;
import org.example.pooling.ConnectionPool;
import org.example.utils.Logger;

import java.util.Timer;
import java.util.TimerTask;

public class Main {

    public static void main(String[] args) {
        ConnectionPool connectionPool = new BasicConnectionPool();

        Logger.log(
                "[TEST] [MaxConnectionCount] " + ConnectionPool.MAX_CONNECTION_COUNT +
                " [MaxIdlePeriod] " + ConnectionPool.MAX_IDLE_PERIOD +
                " [CleanupRate] " + ConnectionPool.POOL_CLEANUP_RATE +
                " [TestDuration] " + 60000,
                Main.class
        );
        Logger.log("[TEST_CLEANUP] [EXPECTING] Closes one connection after first cleanup, 4 connections after second cleanup", Main.class);
        Logger.log("[TEST_CONNECTION_ASSIGNATION] [EXPECTING] Assigns 4 distinct connections at first task, creates 4 connections after second task", Main.class);

        Timer timer = new Timer();
        TimerTask cleanupTask = new CleanupTask(connectionPool);
        TimerTask testTask = new TestTask(1, 4, connectionPool);

        timer.schedule(cleanupTask, connectionPool.POOL_CLEANUP_RATE, connectionPool.POOL_CLEANUP_RATE);
        timer.schedule(testTask, 0, connectionPool.POOL_CLEANUP_RATE * 2);
    }
}