package com.microsoft.java.bs.core.services;

import java.util.Set;

import com.google.inject.Inject;
import com.microsoft.java.bs.core.contrib.BuildSupport;

import ch.epfl.scala.bsp4j.CompileParams;
import ch.epfl.scala.bsp4j.CompileResult;
import ch.epfl.scala.bsp4j.StatusCode;

/**
 * Service to deal with compilation related BSP requests.
 */
public class CompileService {
  @Inject
  Set<BuildSupport> buildSupports;
  
  /**
   * Compile.
   */
  public CompileResult buildTargetCompile(CompileParams params) {
    for (BuildSupport buildSupport : buildSupports) {
      if (!buildSupport.applies()) {
        continue;
      }
      buildSupport.build(params.getTargets());
    }
    return new CompileResult(StatusCode.OK);
  }
}
