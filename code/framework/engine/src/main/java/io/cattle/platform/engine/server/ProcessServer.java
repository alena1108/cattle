package io.cattle.platform.engine.server;

public interface ProcessServer {

    void runOutstandingJobs();

    Long getRemainingTasks(long processId);

}
