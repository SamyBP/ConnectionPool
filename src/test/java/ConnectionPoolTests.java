import org.example.configuration.Configuration;
import org.example.pooling.BasicConnectionPool;
import org.example.pooling.ConnectionPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConnectionPoolTests {

    ConnectionPool connectionPool;
    int startConnectionCount;

    @BeforeEach
    public void init() {
        connectionPool = new BasicConnectionPool();
        startConnectionCount = Integer.parseInt(Configuration.getProperty("pool.start.connection.count"));
    }

    @Test
    public void ConnectionPool_ShutdownTest_ClosesAllConnections() {
        assertEquals(startConnectionCount, connectionPool.getSize());

        connectionPool.shutdown();

        assertEquals(0, connectionPool.getSize());
    }
}
