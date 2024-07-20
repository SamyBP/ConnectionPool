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
### 3. Testing  
- All unit tests are in the [ConnectionPoolTests.java](src/test/java/ConnectionPoolTests.java) file
- All tests results are in the [logs.txt](src/main/resources/logs.txt) file where the behaviour of the pool can be seen:
  - For each test an InsertTask is scheduled to run at a fixed rate with a fixed number of threads for a fixed interval doing the following:
    - Get a connection from the pool if possible
    - Do an insert into **test_table** and count the number of operation it (the thread) does
    - Release the connection after it finished using it
    - The task will log how many connections are in the pool when it starts, what connection was assigned for each thread, the number of operations per thread and the total, along with the timestamp the events happened
  - The CleanupTask is scheduled to run with a fixed delay equal with the fixed rate (the rate provided in the configuration.properties) and will attempt to remove the idle connections if any 
    - It will also log its events providing how many connections are in the pool when it starts and how many it closes
  - Each test runs for a TEST_DURATION period after which the tasks will be unscheduled and the pool will be shutdown

### 4. Testing template

````java
public class Main {

  public static int INSERT_TASK_DURATION =  // the duration of the insert task in minutes
  public static int INSERT_TASK_INTERVAL = // the interval at which the insert task is repeated in ms
  public static long TEST_DURATION =    // the duration of the entire test in ms
  public static int THREAD_COUNT =  // the number of threads to do the insert task
  public static String EXPECTED_CLEANUP_BEHAVIOUR = "[TEST_CLEANUP] [EXPECTING] .........";    // the expected behaviour of the CleanupTask 
  public static String EXPECTED_ASSIGNATION_BEHAVIOUR = "[TEST_CONNECTION_ASSIGNATION] [EXPECTING] ........";   // the expectations of how the pool assigns and or creates connections
  public static void main(String[] args) {
    ConnectionPool connectionPool = new BasicConnectionPool();

    Logger.log(
            "[TEST] [MaxConnectionCount] " + ConnectionPool.MAX_CONNECTION_COUNT +
                    " [MaxIdlePeriod] " + ConnectionPool.MAX_IDLE_PERIOD +
                    " [CleanupRate] " + ConnectionPool.POOL_CLEANUP_RATE +
                    " [TestDuration] " + TEST_DURATION +
                    " [InsertTaskDuration] " + INSERT_TASK_DURATION * 60 * 1000 +
                    " [ThreadCount] " + THREAD_COUNT,
            Main.class
    );
    Logger.log(EXPECTED_CLEANUP_BEHAVIOUR, Main.class);
    Logger.log(EXPECTED_ASSIGNATION_BEHAVIOUR, Main.class);
    
    // initialization of the timer and tasks
    Timer timer = new Timer();
    TimerTask cleanupTask = new CleanupTask(connectionPool);
    TimerTask testTask = new InsertTask(INSERT_TASK_DURATION, THREAD_COUNT, connectionPool);
    
    // scheduling the tasks
    // @param2: the delay from the start of the test for the first run of the task to start
    // @param3: the fixed_rate of the following runs of the task relative to the last run
    timer.scheduleAtFixedRate(cleanupTask, connectionPool.POOL_CLEANUP_RATE, connectionPool.POOL_CLEANUP_RATE);
    timer.scheduleAtFixedRate(testTask, 0, INSERT_TASK_INTERVAL);
    
    //stopping the test after TEST_DURATION
    TimerTask stopTask = new TimerTask() {
      @Override
      public void run() {
        Logger.log("Test execution finished", Main.class);
        cleanupTask.cancel();
        testTask.cancel();
        timer.cancel();
        Logger.log("All scheduled tasks stopped", Main.class);
        Logger.log("Shutdown. Connections in pool:" + connectionPool.getSize(), Main.class);
        connectionPool.shutdown();
        Logger.log("Connections in the pool after shutdown:" + connectionPool.getSize(), Main.class);
      }
    };

    timer.schedule(stopTask, TEST_DURATION, TEST_DURATION);
  }
}
````
### 5. Results

- The test results are separated by two lines
  - The tests were done with the following configuration:
    - Test 1:
      - At startup there will be 5 connections in the pool, 1 of them will be assigned to each of the 4 threads => one remaining connection in the pool
      - After one minute the InsertTasks ends and the connections will be released, also the CleanupTask starts (there can be 1 or 5 connections in the pool, but only one of them was unused for idle time) and removes one connection 
      - After another minute the CleanupTask starts again (the InsertTask has not yet started), now 4 connections were unused for 1 minute = idle.time => 4 connections were removed 
      - After 2.5 minutes from the start of the test there will be no connections in the pool, and the test will end
    ````
      INSERT_TASK_DURATION = 1
      INSERT_TASK_INTERVAL = 180_000 (3 minutes)
      TEST_DURATION = 150_00 (2.5 minutes)
      THREAD_COUNT = 4
      pool.start.connection.count=5
      pool.max.connection.count=20
      pool.max.idle.period=1
      pool.cleanup.rate=1
    ````
    - Test 2:
      - At startup there will be 2 connections in the pool, both of them will be assigned to 2 of the threads and 2 other connections will be created and assigned to the other 2 threads
      - After one minute the InsertTasks ends and the connections will be released back in the pool, also the CleanupTask starts (there are now 4 connections in the pool) and removes none of them
      - After another minute the CleanupTask starts again (the InsertTask has not yet started), now 4 connections were unused for 1 minute = idle.time => 4 connections were removed
      - After 2.5 minutes from the start of the test there will be no connections in the pool, and the test will end
    ````
      INSERT_TASK_DURATION = 1
      INSERT_TASK_INTERVAL = 180_000 (3 minutes)
      TEST_DURATION = 150_00 (2.5 minutes)
      THREAD_COUNT = 4
      pool.start.connection.count=2
      pool.max.connection.count=20
      pool.max.idle.period=1
      pool.cleanup.rate=1
    ````
    - Test 3:
      - At startup there will be 2 connections in the pool, both of them will be assigned to 2 of the threads the pool will create another connection for one of the threads and as the maximum amount of connection was reached there won't be any connection for the fourth thread 
      - After one minute the InsertTasks ends and the connections will be released back in the pool, also the CleanupTask starts (there are now 3 connections in the pool) and removes none of them
      - After 1.5 minutes from the start of the test there will be 3 connections in the pool, as the test ends the pool shuts down end all connections will be closed
    ````
      INSERT_TASK_DURATION = 1
      INSERT_TASK_INTERVAL = 120_000 (2 minutes)
      TEST_DURATION = 90_00 (1.5 minutes)
      THREAD_COUNT = 4
      pool.start.connection.count=2
      pool.max.connection.count=3
      pool.max.idle.period=1
      pool.cleanup.rate=1
    ````
