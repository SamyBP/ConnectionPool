package org.example.pooling;

import org.example.configuration.Configuration;
import org.example.utils.ConnectionFactory;


import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BasicConnectionPool implements ConnectionPool {

    private final List<Connection> assignedConnections;
    private final BlockingQueue<Connection> pool;
    private final Map<Connection, Long> connectionTimeMap;
    public BasicConnectionPool() {
        int MIN_CONNECTION_COUNT = Integer.parseInt(Configuration.getProperty("pool.start.connection.count"));
        this.assignedConnections = Collections.synchronizedList(new ArrayList<>(MAX_CONNECTION_COUNT));
        this.pool = new ArrayBlockingQueue<>(MAX_CONNECTION_COUNT);
        this.connectionTimeMap = new HashMap<>();

        for (int i = 0; i < MIN_CONNECTION_COUNT; i++) {
            Connection connection = ConnectionFactory.getConnection();
            pool.add(connection);
            connectionTimeMap.put(connection, System.currentTimeMillis());
        }
    }

    @Override
    public Connection assignConnection() throws Exception {
        synchronized (this) {
            if (pool.isEmpty()) {
                if (assignedConnections.size() < MAX_CONNECTION_COUNT) {
                    try {
                        pool.put(ConnectionFactory.getConnection());
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                } else {
                    throw new Exception("Max pool size reached");
                }
            }
        }
        Connection connection = null;

        try {
            connection = pool.take();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }


        if (ConnectionFactory.isClosed(connection)) {
            connection = ConnectionFactory.getConnection();
        }

        synchronized (connectionTimeMap) {
            connectionTimeMap.put(connection, System.currentTimeMillis());
        }

        assignedConnections.add(connection);
        return connection;
    }

    @Override
    public boolean releaseConnection(Connection connection)  {
        if (connection == null || ConnectionFactory.isClosed(connection)) {
            connection = ConnectionFactory.getConnection();
        }

        try {
            pool.put(connection);
            synchronized (connectionTimeMap) {
                connectionTimeMap.put(connection, System.currentTimeMillis());
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }

        return assignedConnections.remove(connection);
    }

    @Override
    public synchronized void shutdown() {
        assignedConnections.forEach(this::releaseConnection);
        pool.forEach(ConnectionFactory::close);
        pool.clear();
        connectionTimeMap.clear();
    }

    @Override
    public int getSize() {
        return pool.size();
    }

    @Override
    public synchronized int removeIdle() {
        long currentTime = System.currentTimeMillis();
        AtomicInteger removedConnectionCount = new AtomicInteger();
        connectionTimeMap.entrySet().stream()
                .filter(entry -> currentTime - entry.getValue() > MAX_IDLE_PERIOD)
                .map(Map.Entry::getKey)
                .forEach(connection -> {
                    if (pool.remove(connection)) {
                        removedConnectionCount.incrementAndGet();
                        ConnectionFactory.close(connection);
                    }
                });
        return removedConnectionCount.get();
    }
}
