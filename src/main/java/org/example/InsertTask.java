package org.example;

import org.example.pooling.ConnectionPool;
import org.example.utils.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class InsertTask extends TimerTask {
    private final int testDuration;
    private final int threadCount;
    private static final String INSERT_SQL = "insert into test_table(description) values (?)";
    private final ConnectionPool pool;

    public InsertTask(int testDuration, int threadCount, ConnectionPool pool) {
        super();
        this.testDuration = testDuration;
        this.threadCount = threadCount;
        this.pool = pool;
    }

    public int getInsertCount() {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<Integer>> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            results.add(executorService.submit(() -> {
                AtomicInteger insertCount = new AtomicInteger();
                long finishTime = System.currentTimeMillis() + (long) testDuration * 60 * 1000;
                Connection connection = pool.assignConnection();
                if (connection == null) {
                    Logger.log("Thread:" + Thread.currentThread() + " has no connection", InsertTask.class);
                    return 0;
                }
                Logger.log("Thread:" + Thread.currentThread() + " has connection:" + connection, InsertTask.class);

                while (System.currentTimeMillis() < finishTime) {
                    PreparedStatement statement = connection.prepareStatement(INSERT_SQL);
                    statement.setString(1, "test");
                    int res = statement.executeUpdate();
                    insertCount.addAndGet(res);
                    statement.close();
                }
                boolean isReleaseSuccessful =  pool.releaseConnection(connection);
                synchronized (this) {
                    if (isReleaseSuccessful) {
                        Logger.log("Thread:" + Thread.currentThread() + " released it's connection", InsertTask.class);
                    }
                    Logger.log("Thread:" + Thread.currentThread() + " executed " + insertCount.get() + " inserts", InsertTask.class);
                }

                return insertCount.get();
            }));
        }

        executorService.shutdown();
        int totalInserts = 0;
        try {
            boolean terminated = executorService.awaitTermination(testDuration, TimeUnit.MINUTES);

            for (Future<Integer> result : results) {
                totalInserts += result.get();
            }
        } catch (Exception e) {
            Logger.log("[EXCEPTION] " + e.getMessage(), InsertTask.class);
        }

        return totalInserts;
    }

    @Override
    public void run() {
        Logger.log("Connections in pool:" + pool.getSize(), InsertTask.class);
        Logger.log("Total inserts:" + this.getInsertCount(), InsertTask.class);
    }
}
