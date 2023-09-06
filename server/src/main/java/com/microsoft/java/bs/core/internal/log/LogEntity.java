package com.microsoft.java.bs.core.internal.log;

import com.microsoft.java.bs.core.Constants;

/**
 * The Object passed to the logger.
 */
public class LogEntity {
  private final String buildServerVersion;
  private final String operationName;
  private final String duration;
  private final String trace;
  private final String rootCauseMessage;

  private LogEntity(Builder builder) {
    this.buildServerVersion = builder.buildServerVersion;
    this.operationName = builder.operationName;
    this.duration = builder.duration;
    this.trace = builder.trace;
    this.rootCauseMessage = builder.rootCauseMessage;
  }

  public String getRootCauseMessage() {
    return rootCauseMessage;
  }

  public String getTrace() {
    return trace;
  }

  public String getOperationName() {
    return operationName;
  }

  public String getBuildServerVersion() {
    return buildServerVersion;
  }

  public String getDuration() {
    return duration;
  }

  /**
   * Builder.
   */
  public static class Builder {
    private final String buildServerVersion;
    private String rootCauseMessage;
    private String trace;
    private String operationName;
    private String duration;

    public Builder() {
      this.buildServerVersion = Constants.SERVER_VERSION;
    }

    public Builder rootCauseMessage(String rootCauseMessage) {
      this.rootCauseMessage = rootCauseMessage;
      return this;
    }

    public Builder trace(String trace) {
      this.trace = trace;
      return this;
    }

    public Builder operationName(String operationName) {
      this.operationName = operationName;
      return this;
    }

    public Builder duration(String duration) {
      this.duration = duration;
      return this;
    }

    public LogEntity build() {
      return new LogEntity(this);
    }
  }
}
