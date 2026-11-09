package com.microsoft.java.bs.core.internal.server;

import java.util.List;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.DebugSessionParams;
import com.microsoft.java.bs.core.internal.context.BuildContextService.Reference;

/** Debug parameters with an optional execution context. */
public class ContextDebugParams extends DebugSessionParams {
  private Reference executionContext;

  public ContextDebugParams(List<BuildTargetIdentifier> targets, String dataKind, Object data) {
    super(targets, dataKind, data);
  }

  public Reference getExecutionContext() {
    return executionContext;
  }

  public void setExecutionContext(Reference executionContext) {
    this.executionContext = executionContext;
  }
}
