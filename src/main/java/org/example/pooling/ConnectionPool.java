package org.example.pooling;

import org.example.configuration.Configuration;

import java.sql.Connection;

public interface ConnectionPool {
    int MAX_CONNECTION_COUNT = Integer.parseInt(Configuration.getProperty("pool.max.connection.count"));
    long MAX_IDLE_PERIOD = Long.parseLong(Configuration.getProperty("pool.max.idle.period"));
    long POOL_CLEANUP_RATE = Long.parseLong(Configuration.getProperty("pool.cleanup.rate"));
    Connection assignConnection() throws Exception;
    boolean releaseConnection(Connection connection);
    void shutdown();
    int getSize();
    int removeIdle();
}
