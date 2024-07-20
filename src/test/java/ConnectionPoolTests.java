import org.example.pooling.BasicConnectionPool;
import org.example.pooling.ConnectionPool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ConnectionPoolTests {

    ConnectionPool pool1;
    ConnectionPool pool2;

    @BeforeEach
    public void before() {
        pool1 = new BasicConnectionPool();
        pool2 = new BasicConnectionPool();
    }

    @AfterEach
    public void after() {
        pool1.shutdown();
        pool2.shutdown();
    }

    @Test
    public void ConnectionPool_CorrectStartupConnectionCount() {
        assertEquals(ConnectionPool.STARTUP_CONNECTION_COUNT, pool1.getSize());
    }

    @Test
    public void ConnectionPool_Shutdown_ClosesAllConnections() {
        pool1.shutdown();
        assertEquals(0, pool1.getSize());
    }

    @Test
    public void ConnectionPool_ReleaseConnection_ReturnsFalseForNullConnections() {
        assertFalse(pool1.releaseConnection(null));
    }

    @Test
    public void ConnectionPool_ReleaseConnection_ReturnsFalseForConnectionNotCreatedByPool()  {
        Connection connection = pool1.assignConnection();
        assertFalse(pool2.releaseConnection(connection));
    }

    @Test
    public void ConnectionPool_AssignReleaseConnection_ReturnsValidConnectionReleaseSuccessful() {
        Connection connection = pool1.assignConnection();
        assertNotNull(connection, "There are " + ConnectionPool.STARTUP_CONNECTION_COUNT + " should return not null");
        assertEquals(ConnectionPool.STARTUP_CONNECTION_COUNT - 1, pool1.getSize());
        assertTrue(pool1.releaseConnection(connection));
        assertEquals(ConnectionPool.STARTUP_CONNECTION_COUNT, pool1.getSize());
    }

    @Test
    public void ConnectionPool_RemoveIdleConnections_RemovesAllConnections() throws InterruptedException {
        int connectionTotal = pool1.getSize();

        Thread.sleep(ConnectionPool.MAX_IDLE_PERIOD);
        int removedConnectionsCount = pool1.removeIdle();

        assertEquals(connectionTotal, removedConnectionsCount);
    }

    @Test
    public void ConnectionPool_RemoveIdleConnections_WithReleasedConnectionRemovesCorrect() throws InterruptedException {
        List<Connection> connections = new ArrayList<>();
        int numberOfConnectionsToAssign = ConnectionPool.STARTUP_CONNECTION_COUNT - 3;
        for (int i = 0; i < numberOfConnectionsToAssign; i++) {
            connections.add(pool1.assignConnection());
        }

        Thread.sleep(5000);
        connections.forEach(connection -> pool1.releaseConnection(connection));
        Thread.sleep(ConnectionPool.MAX_IDLE_PERIOD - 5000);
        int removedConnectionsCount = pool1.removeIdle();

        assertEquals(ConnectionPool.STARTUP_CONNECTION_COUNT - numberOfConnectionsToAssign, removedConnectionsCount);
    }

    @Test
    public void ConnectionPool_RemoveIdleConnections_WithAssignedConnectionsRemovesCorrect() throws InterruptedException {
        Connection connection = pool1.assignConnection();

        Thread.sleep(ConnectionPool.MAX_IDLE_PERIOD);
        int removedConnectionsCount = pool1.removeIdle();
        pool1.releaseConnection(connection);

        assertEquals(ConnectionPool.STARTUP_CONNECTION_COUNT - 1, removedConnectionsCount);
    }
}
