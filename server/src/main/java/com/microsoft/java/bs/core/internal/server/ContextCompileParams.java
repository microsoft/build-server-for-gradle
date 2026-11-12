package com.microsoft.java.bs.core.internal.server;

import java.util.List;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.CompileParams;
import com.microsoft.java.bs.core.internal.context.BuildContextService.Reference;

/** Compile parameters with an optional execution context. */
public class ContextCompileParams extends CompileParams {
  private Reference executionContext;

  public ContextCompileParams(List<BuildTargetIdentifier> targets) {
    super(targets);
  }

  public Reference getExecutionContext() {
    return executionContext;
  }

  public void setExecutionContext(Reference executionContext) {
    this.executionContext = executionContext;
  }
}
