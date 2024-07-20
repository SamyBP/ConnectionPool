package org.example.pooling;

import java.sql.Connection;

public class ConnectionWrapper {
    private Connection connection;
    private long lastUsageTimestamp;

    public ConnectionWrapper(Connection connection) {
        this.connection = connection;
        this.lastUsageTimestamp = System.currentTimeMillis();
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public long getLastUsageTimestamp() {
        return lastUsageTimestamp;
    }

    public void setLastUsageTimestamp(long lastUsageTimestamp) {
        this.lastUsageTimestamp = lastUsageTimestamp;
    }
}
