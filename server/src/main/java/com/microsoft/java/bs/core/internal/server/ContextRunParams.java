package com.microsoft.java.bs.core.internal.server;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.RunParams;
import com.microsoft.java.bs.core.internal.context.BuildContextService.Reference;

/** Run parameters with an optional execution context. */
public class ContextRunParams extends RunParams {
  private Reference executionContext;

  public ContextRunParams(BuildTargetIdentifier target) {
    super(target);
  }

  public Reference getExecutionContext() {
    return executionContext;
  }

  public void setExecutionContext(Reference executionContext) {
    this.executionContext = executionContext;
  }
}
