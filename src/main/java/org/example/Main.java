package org.example;


import org.example.pooling.BasicConnectionPool;
import org.example.pooling.CleanupTask;
import org.example.pooling.ConnectionPool;
import org.example.utils.Logger;

import java.util.Timer;
import java.util.TimerTask;

public class Main {

    public static int INSERT_TASK_DURATION = 1;
    public static long TEST_DURATION = 150_000;
    public static int THREAD_COUNT = 4;
    public static String EXPECTED_CLEANUP_BEHAVIOUR = "[TEST_CLEANUP] [EXPECTING] Closes one connection at first cleanup, at second cleanup will not remove any connection";
    public static String EXPECTED_ASSIGNATION_BEHAVIOUR = "[TEST_CONNECTION_ASSIGNATION] [EXPECTING] Assigns 4 distinct connections at first run";
    public static void main(String[] args) {
        ConnectionPool connectionPool = new BasicConnectionPool();

        Logger.log(
                "[TEST] [MaxConnectionCount] " + ConnectionPool.MAX_CONNECTION_COUNT +
                " [MaxIdlePeriod] " + ConnectionPool.MAX_IDLE_PERIOD +
                " [CleanupRate] " + ConnectionPool.POOL_CLEANUP_RATE +
                " [TestDuration] " + TEST_DURATION +
                " [InsertTaskDuration] " + INSERT_TASK_DURATION * 60 * 1000 +
                " [ThreadCount] " + THREAD_COUNT,
                Main.class
        );
        Logger.log(EXPECTED_CLEANUP_BEHAVIOUR, Main.class);
        Logger.log(EXPECTED_ASSIGNATION_BEHAVIOUR, Main.class);

        Timer timer = new Timer();
        TimerTask cleanupTask = new CleanupTask(connectionPool);
        TimerTask testTask = new InsertTask(INSERT_TASK_DURATION, THREAD_COUNT, connectionPool);

        timer.scheduleAtFixedRate(cleanupTask, connectionPool.POOL_CLEANUP_RATE, connectionPool.POOL_CLEANUP_RATE);
        timer.scheduleAtFixedRate(testTask, 0, 60_000);

        TimerTask stopTask = new TimerTask() {
            @Override
            public void run() {
                Logger.log("Test execution finished", Main.class);
                cleanupTask.cancel();
                testTask.cancel();
                timer.cancel();
                Logger.log("All scheduled tasks stopped", Main.class);
                Logger.log("Shutdown. Connections in pool:" + connectionPool.getSize(), Main.class);
                connectionPool.shutdown();
                Logger.log("Connections in the pool after shutdown:" + connectionPool.getSize(), Main.class);
            }
        };

        timer.schedule(stopTask, TEST_DURATION, TEST_DURATION);
    }
}