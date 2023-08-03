package com.microsoft.java.bs.core.internal.log;

/**
 * The Object passed to the logger.
 */
public class LogEntity {
  private String rootCauseMessage;
  private String stackTrace;
  private String bspRequestName;
  private String buildServerVersion;
  private String time;

  private LogEntity() {
  }

  /**
   * Builder.
   */
  public static class Builder {
    private final LogEntity logEntity = new LogEntity();

    public Builder rootCauseMessage(String rootCauseMessage) {
      logEntity.rootCauseMessage = rootCauseMessage;
      return this;
    }

    public Builder stackTrace(String stackTrace) {
      logEntity.stackTrace = stackTrace;
      return this;
    }

    public Builder bspRequestName(String bspRequestName) {
      logEntity.bspRequestName = bspRequestName;
      return this;
    }

    public Builder time(String time) {
      logEntity.time = time;
      return this;
    }

    public LogEntity build() {
      return logEntity;
    }
  }

  // below are mutable fields that will be updated by {@link TelemetryHandler}.
  public void setBuildServerVersion(String buildServerVersion) {
    this.buildServerVersion = buildServerVersion;
  }
}
