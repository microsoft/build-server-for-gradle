// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.log;

import com.microsoft.java.bs.core.Constants;

/**
 * The Object passed to the logger.
 */
public class BspTraceEntity {
  private final String kind;
  private final String schemaVersion;
  private final String buildServerVersion;
  private final String operationName;
  private final String duration;
  private final String trace;
  private final String rootCauseMessage;

  private BspTraceEntity(Builder builder) {
    this.kind = "bsptrace";
    this.schemaVersion = "1.0";
    this.buildServerVersion = Constants.SERVER_VERSION;
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
    private String rootCauseMessage;
    private String trace;
    private String operationName;
    private String duration;

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

    public BspTraceEntity build() {
      return new BspTraceEntity(this);
    }
  }
}
