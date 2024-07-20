package org.example.pooling;

import org.example.configuration.Configuration;

import java.sql.Connection;

public interface ConnectionPool {
    int STARTUP_CONNECTION_COUNT = Integer.parseInt(Configuration.getProperty("pool.start.connection.count"));
    int MAX_CONNECTION_COUNT = Integer.parseInt(Configuration.getProperty("pool.max.connection.count"));
    long MAX_IDLE_PERIOD = Long.parseLong(Configuration.getProperty("pool.max.idle.period")) * 60 * 1000;
    long POOL_CLEANUP_RATE = Long.parseLong(Configuration.getProperty("pool.cleanup.rate")) * 60 * 1000;

    Connection assignConnection();
    boolean releaseConnection(Connection connection);
    void shutdown();
    int getSize();
    int removeIdle();
}
