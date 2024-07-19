package org.example.utils;

import org.example.configuration.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ConnectionFactory {
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

    public static Connection getConnection() {
        return connectionFactory.createConnection();
    }

    public static void close(Connection connection) {
        if (connection == null)
            return;

        try {
            connection.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static boolean isClosed(Connection connection) {
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

    public static void close(PreparedStatement statement) {
        if (statement == null)
            return;

        try {
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
