package org.example.pooling;


import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class BasicConnectionPool implements ConnectionPool {
    private final BlockingQueue<ConnectionWrapper> connectionPool;
    private final List<Connection> assignedConnections;

    public BasicConnectionPool() {
        this.assignedConnections = Collections.synchronizedList(new ArrayList<>(MAX_CONNECTION_COUNT));
        this.connectionPool = new ArrayBlockingQueue<>(MAX_CONNECTION_COUNT);

        for (int i = 0; i < STARTUP_CONNECTION_COUNT; i++) {
            connectionPool.add(new ConnectionWrapper(ConnectionFactory.getConnection()));
        }
    }

    @Override
    public Connection assignConnection() {
        synchronized (this) {
            if (connectionPool.isEmpty() && assignedConnections.size() >= MAX_CONNECTION_COUNT) {
                return null;
            }

            if (connectionPool.isEmpty()) {
                try {
                    connectionPool.put(new ConnectionWrapper(ConnectionFactory.getConnection()));
                } catch (InterruptedException e) {
                    return null;
                }
            }
        }

        ConnectionWrapper connectionWrapper;

        try {
            connectionWrapper = connectionPool.take();
        } catch (InterruptedException e) {
            return null;
        }

        synchronized (this) {
            if (ConnectionFactory.isClosed(connectionWrapper.getConnection())) {
                connectionWrapper.setConnection(ConnectionFactory.getConnection());
            }
            connectionWrapper.setLastUsageTimestamp(System.currentTimeMillis());
        }

        assignedConnections.add(connectionWrapper.getConnection());
        return connectionWrapper.getConnection();
    }

    @Override
    public synchronized boolean releaseConnection(Connection connection)  {
        if (connection == null || !assignedConnections.contains(connection)) {
            return false;
        }

        boolean isReleaseSuccessful = assignedConnections.remove(connection);

        if (ConnectionFactory.isClosed(ConnectionFactory.getConnection())) {
           connection = ConnectionFactory.getConnection();
        }

        ConnectionWrapper connectionWrapper = new ConnectionWrapper(connection);
        try {
            connectionPool.put(connectionWrapper);
        } catch (InterruptedException e) {
            isReleaseSuccessful = false;
        }

        return isReleaseSuccessful;
    }

    @Override
    public synchronized void shutdown() {
        int index = 0;
        while (!assignedConnections.isEmpty()) {
            this.releaseConnection(assignedConnections.get(index));
        }
        connectionPool.forEach(wrapper -> ConnectionFactory.close(wrapper.getConnection()));
        connectionPool.clear();
        assignedConnections.clear();
    }

    @Override
    public int getSize() {
        return connectionPool.size();
    }

    @Override
    public synchronized int removeIdle() {
        long currentTime = System.currentTimeMillis();
        AtomicInteger removedConnectionsCount = new AtomicInteger();

        List<ConnectionWrapper> idleConnections =  connectionPool.stream()
                .filter(wrapper -> currentTime - wrapper.getLastUsageTimestamp() > MAX_IDLE_PERIOD)
                .toList();

        idleConnections.forEach(wrapper -> {
            if (connectionPool.remove(wrapper)) {
                ConnectionFactory.close(wrapper.getConnection());
                removedConnectionsCount.incrementAndGet();
            }
        });

        return removedConnectionsCount.get();
    }
}
