package org.example;

import org.example.pooling.ConnectionPool;
import org.example.utils.ConnectionFactory;
import org.example.utils.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TestTask extends TimerTask {
    private final int testDuration;
    private final int threadCount;
    private static final String INSERT_SQL = "insert into test_table(description) values (?)";
    private final ConnectionPool pool;

    public TestTask(int testDuration, int threadCount, ConnectionPool pool) {
        super();
        this.testDuration = testDuration;
        this.threadCount = threadCount;
        this.pool = pool;
    }

    public int getInsertCount() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<Future<Integer>> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            results.add(executorService.submit(() -> {
                AtomicInteger insertCount = new AtomicInteger();
                long finishTime = System.currentTimeMillis() + (long) testDuration * 60 * 1000;
                Connection connection = null;
                try {
                    connection = pool.assignConnection();
                    Logger.log("Thread:" + Thread.currentThread() + " has connection:" + connection, TestTask.class);
                } catch (Exception e) {
                    Logger.log("Thread:" + Thread.currentThread() + " has no connection:" + e.getMessage(), TestTask.class);
                }

                while (System.currentTimeMillis() < finishTime) {
                    PreparedStatement statement = connection.prepareStatement(INSERT_SQL);
                    statement.setString(1, "test");
                    statement.executeUpdate();
                    insertCount.incrementAndGet();
                    ConnectionFactory.close(statement);
                }
                boolean isReleaseSuccessful =  pool.releaseConnection(connection);
                synchronized (this) {
                    if (isReleaseSuccessful) {
                        Logger.log("Thread:" + Thread.currentThread() + " released it's connection", TestTask.class);
                    }
                    Logger.log("Thread:" + Thread.currentThread() + " executed " + insertCount.get() + " inserts", TestTask.class);
                }

                return insertCount.get();
            }));
        }

        executorService.shutdown();
        executorService.awaitTermination(testDuration, TimeUnit.MINUTES);
        int totalInserts = 0;
        for (Future<Integer> result : results) {
            totalInserts += result.get();
        }

        return totalInserts;
    }

    @Override
    public void run() {
        try {
            Logger.log("Connections in pool:" + pool.getSize(), TestTask.class);
            Logger.log("Total inserts:" + this.getInsertCount(), TestTask.class);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
