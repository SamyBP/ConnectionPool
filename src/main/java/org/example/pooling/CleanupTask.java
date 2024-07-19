package org.example.pooling;

import org.example.utils.Logger;

import java.util.TimerTask;

public class CleanupTask extends TimerTask {
    private final ConnectionPool pool;

    public CleanupTask(ConnectionPool pool) {
        super();
        this.pool = pool;
    }

    @Override
    public void run() {
        synchronized (this) {
            Logger.log("Running cleanup task for [" + pool.getSize() + "] connections", CleanupTask.class);
            Logger.log("Closed " + pool.removeIdle() + " connections", CleanupTask.class);
        }
    }
}
