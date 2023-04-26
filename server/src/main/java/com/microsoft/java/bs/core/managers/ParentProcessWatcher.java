package com.microsoft.java.bs.core.managers;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.eclipse.lsp4j.jsonrpc.MessageConsumer;

import ch.epfl.scala.bsp4j.BuildServer;

/**
 * Watches the parent process PID and invokes exit if it is no longer available.
 * This implementation waits for periods of inactivity to start querying the PIDs.
 */
public final class ParentProcessWatcher implements Runnable, Function<MessageConsumer,
    MessageConsumer> {
  private static final long INACTIVITY_DELAY_SECS = 30 * 1000;
  private static final int POLL_DELAY_SECS = 10;
  private volatile long lastActivityTime;
  private final BuildServer server;
  private long parentPid = 0;
  private ScheduledFuture<?> task;
  private ScheduledExecutorService service;

  /**
   * Constructor.
   */
  public ParentProcessWatcher(BuildServer server) {
    this.server = server;
    if (ProcessHandle.current().parent().isPresent()) {
      parentPid = ProcessHandle.current().parent().get().pid();
    }
    service = Executors.newScheduledThreadPool(1);
    task = service.scheduleWithFixedDelay(this, POLL_DELAY_SECS, POLL_DELAY_SECS, TimeUnit.SECONDS);
  }

  @Override
  public void run() {
    if (!parentProcessStillRunning()) {
      task.cancel(true);
      server.onBuildExit();
    }
  }

  /**
   * Checks whether the parent process is still running.
   * If not, then we assume it has crashed, and we have to terminate the Java Language Server.
   *
   * @return true if the parent process is still running
   */
  private boolean parentProcessStillRunning() {
    if (parentPid == 0 || lastActivityTime > (System.currentTimeMillis() - INACTIVITY_DELAY_SECS)) {
      return true;
    }
    
    try {
      return ProcessHandle.of(parentPid).isPresent();
    } catch (UnsupportedOperationException | SecurityException e) {
      // ignore.
    }

    return true;
  }

  @Override
  public MessageConsumer apply(final MessageConsumer consumer) {
    //inject our own consumer to refresh the timestamp
    return message -> {
      lastActivityTime = System.currentTimeMillis();
      consumer.consume(message);
    };
  }
}
