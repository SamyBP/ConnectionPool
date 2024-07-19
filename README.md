## Connection Pool

- The application provides a simple way to connect to a database through the JDBC driver
- Connecting to the database is achieved by retrieving the database credentials from the [configuration.properties](src/main/resources/configuration.properties) file

### 1. Setup

- **Database credentials**
  - **datasource.url**: represents the url for the database for which you want to connect
  - **datasource.diver-class-name**: represents the driver's class name, for example if you are using PostgreSQL then you should use **org.postgresql.Driver**
  - **datasource.username** & **datasource.password**: represents the username and password to connect to the database 
- **Connection pool configuration**
  - **pool.start.connection.count**: represents the number of connection in the pool at startup
    - **Note**: this does not represent the minimum number of connection available at any time
  - **pool.max.connection.count**: represents the maximum number of connections available at any time. 
    - **Note**: if the number of assigned connections reaches this number and a request was made for another connection before at least one returned in the pool then an exception is thrown and doesn't create a new connection
  - **pool.max.idle.period**: represents the maximum amount of time (in minutes) a connection can stay in the pool without being used
  - **pool.cleanup.rate**: represents the interval/rate (in minutes) a thread is awaken and tries to close and remove from the pool the connections (if any) that have not been used for **pool.max.idle.period** time

### 2. Implementation Notes

- The application provides a **Connection Pool** interface with the following key methods:
  - **assignConnection**: assigns a connection from the pool or creates a new one if the maximum number of connections specified is not greater then the one specified in the configuration, otherwise an exception is thrown and no connection is assigned
  - **releaseConnection**: returns a connection back in the pool if valid, otherwise it creates a new one and adds it in the pool
  - **shutdown**: returns all assigned connections back to the pool and closes and removes every connection from the pool.
  - **getSize**: returns the number of connection that are available in the pool
  - Creation of a pool is simple, as it uses the data in the configuration file, example:
    ```java
      ConnectionPool pool = new BasicConnectionPool();
    ```

- Each connection is created, closed and verified through the ConnectionFactory utility class    
- The CleanupTask is a TimerTask that is scheduled to run at a fixed rate (the one provided in the configuration file) and provides the following messages in a log file representing the number of connections it verifies and the number of connections that meet the requirement to be closed and removed 
  ````
    [timestamp]  [TimerTask] Running cleanup task for [x] connections
    [timestamp]  [TimerTask] Closed y connections
  ````  
### 3. Testing  
- All tests results are int the [logs.txt](src/main/resources/logs.txt) file
- For each test the number of database operation are counted, creating THREAD_COUNT threads that do an insert operation for a TEST_DURATION minutes with the following testing scenarios:
  - The initial number of connections in the pool is larger then the number of threads and the task is repeated at a rate slower then the cleanup.rate and idle.period => the CleanupTask will close and remove some connections 
  - The initial number of connection in the pool is smaller then the number of threads and the task is repeated at a rate much much slower then the cleanup.rate and idle.period => the pool will create some additional connections, and then the CleanupTask will remove some connections
  - The number of threads is larger then the max.connection.count => some threads will not get a connection
  - The initial numebr of connections in the pool is larger / smaller then the number of threads but the task will always be repeated before a connection reaches idle.time of not being used => no need to close any connection


### 4. Testing template

````java
public class Main {

    public static int TEST_DURATION = ; // duration of the insert task
    public static int THREAD_COUNT = ; // the number of threads for the insert task
    public static String EXPECTED_CLEANUP_BEHAVIOUR = ; // expected behaviour of the cleanup task
    public static String EXPECTED_ASSIGNATION_BEHAVIOUR = ; // expected behaviour of connection assignation
    public static void main(String[] args) {
        // pool initialization
        ConnectionPool connectionPool = new BasicConnectionPool();
        
        Logger.log(
                "[TEST] [MaxConnectionCount] " + ConnectionPool.MAX_CONNECTION_COUNT +
                " [MaxIdlePeriod] " + ConnectionPool.MAX_IDLE_PERIOD +
                " [CleanupRate] " + ConnectionPool.POOL_CLEANUP_RATE +
                " [TestDuration] " + TEST_DURATION * 60 * 1000,
                Main.class
        );
        Logger.log(EXPECTED_CLEANUP_BEHAVIOUR, Main.class);
        Logger.log(EXPECTED_ASSIGNATION_BEHAVIOUR, Main.class);
        
        // timer initialization
        Timer timer = new Timer();
        TimerTask cleanupTask = new CleanupTask(connectionPool);
        TimerTask testTask = new TestTask(TEST_DURATION, THREAD_COUNT, connectionPool);
        
        // scheduling tasks
        // @param1: the task to be scheduled
        // @param2: the delay from the start of the test for the first run of the task to start
        // @param3: the fixed_rate of the following runs of the task relative to the last run
        timer.schedule(cleanupTask, connectionPool.POOL_CLEANUP_RATE, connectionPool.POOL_CLEANUP_RATE);
        timer.schedule(testTask, 0, connectionPool.POOL_CLEANUP_RATE * 2);
    }
}
````