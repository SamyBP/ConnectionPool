package org.example.pooling;

import org.example.configuration.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class ConnectionFactory {
    private static final ConnectionFactory connectionFactory = new ConnectionFactory();

    private ConnectionFactory() {
        try {
            Class.forName(Configuration.getProperty("datasource.driver-class-name"));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Connection createConnection() {
        Connection connection = null;

        try {
            connection = DriverManager.getConnection(
                    Configuration.getProperty("datasource.url"),
                    Configuration.getProperty("datasource.username"),
                    Configuration.getProperty("datasource.password")
            );
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return connection;
    }

    static Connection getConnection() {
        return connectionFactory.createConnection();
    }

    static void close(Connection connection) {
        if (connection == null)
            return;

        try {
            connection.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    static boolean isClosed(Connection connection) {
        if (connection == null) {
            return true;
        }

        boolean isClosed = false;
        try {
            isClosed = connection.isClosed();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return isClosed;
    }
}
