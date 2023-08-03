package com.microsoft.java.bs.core.internal.log;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.google.gson.Gson;
import com.microsoft.java.bs.core.Constants;
import com.microsoft.java.bs.core.Launcher;

import ch.epfl.scala.bsp4j.LogMessageParams;
import ch.epfl.scala.bsp4j.MessageType;

/**
 * The log appender to send bi data.
 */
public class TelemetryHandler extends Handler {

  @Override
  public void publish(LogRecord logRecord) {
    Object[] property = logRecord.getParameters();
    if (property == null || property.length == 0 || !(property[0] instanceof LogEntity)) {
      return;
    }

    LogEntity entity = (LogEntity) property[0];
    entity.setBuildServerVersion(Constants.SERVER_VERSION);

    String jsonStr = new Gson().toJson(entity);
    Launcher.client.onBuildLogMessage(new LogMessageParams(MessageType.LOG, jsonStr));
  }

  @Override
  public void flush() {
    // do nothing
  }

  @Override
  public void close() throws SecurityException {
    // do nothing
  }
}
