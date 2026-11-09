package com.microsoft.java.bs.core.internal.server;

import java.util.List;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.TestParams;
import com.microsoft.java.bs.core.internal.context.BuildContextService.Reference;

/** Test parameters with an optional execution context. */
public class ContextTestParams extends TestParams {
  private Reference executionContext;

  public ContextTestParams(List<BuildTargetIdentifier> targets) {
    super(targets);
  }

  public Reference getExecutionContext() {
    return executionContext;
  }

  public void setExecutionContext(Reference executionContext) {
    this.executionContext = executionContext;
  }
}
