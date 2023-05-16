package com.microsoft.java.bs.core.services;

import java.util.Set;

import com.google.inject.Inject;
import com.microsoft.java.bs.core.contrib.BuildSupport;

import ch.epfl.scala.bsp4j.CleanCacheParams;
import ch.epfl.scala.bsp4j.CleanCacheResult;
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
    CompileResult result = new CompileResult(StatusCode.OK);
    for (BuildSupport buildSupport : buildSupports) {
      if (!buildSupport.applies()) {
        continue;
      }
      result.setStatusCode(buildSupport.build(params.getTargets()));
    }
    result.setOriginId(params.getOriginId());
    return result;
  }

  /**
   * Clean cache.
   */
  public CleanCacheResult buildTargetCleanCache(CleanCacheParams params) {
    CleanCacheResult result = new CleanCacheResult("", true);
    for (BuildSupport buildSupport : buildSupports) {
      if (!buildSupport.applies()) {
        continue;
      }
      result.setCleaned(buildSupport.cleanCache(params.getTargets()));
    }
    return result;
  }
}
