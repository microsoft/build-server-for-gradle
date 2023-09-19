// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.log;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import com.microsoft.java.bs.core.Launcher;

import ch.epfl.scala.bsp4j.LogMessageParams;
import ch.epfl.scala.bsp4j.MessageType;

/**
 * The log appender to log server events to client.
 */
public class LogHandler extends Handler {

  @Override
  public void publish(LogRecord logRecord) {
    Format formatter = new SimpleDateFormat("HH:mm:ss");
    String timestamp = formatter.format(new Date());
    String logMessage = String.format("[%s - %s] %s", logRecord.getLevel().getName(),
        timestamp, logRecord.getMessage());
    Launcher.client.onBuildLogMessage(new LogMessageParams(
        convertLevelToMessageType(logRecord.getLevel()), logMessage));
  }

  @Override
  public void flush() {
    // do nothing
  }

  @Override
  public void close() throws SecurityException {
    // do nothing
  }

  private MessageType convertLevelToMessageType(Level level) {
    if (Level.SEVERE.equals(level)) {
      return MessageType.ERROR;
    } else if (Level.WARNING.equals(level)) {
      return MessageType.WARNING;
    } else {
      return MessageType.INFORMATION;
    }
  }
}
