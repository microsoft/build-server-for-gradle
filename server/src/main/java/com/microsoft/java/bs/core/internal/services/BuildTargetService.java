package com.microsoft.java.bs.core.internal.services;

import java.util.List;
import java.util.stream.Collectors;

import com.microsoft.java.bs.core.internal.managers.BuildTargetsManager;
import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;

/**
 * Service to handle build target related BSP requests.
 */
public class BuildTargetService {

  private BuildTargetsManager buildTargetsManager;

  public BuildTargetService(BuildTargetsManager buildTargetsManager) {
    this.buildTargetsManager = buildTargetsManager;
  }

  /**
   * Get the build targets of the workspace.
   */
  public WorkspaceBuildTargetsResult getWorkspaceBuildTargets() {
    List<GradleBuildTarget> allTargets = buildTargetsManager.getAllGradleBuildTargets();
    List<BuildTarget> targets = allTargets.stream()
        .map(GradleBuildTarget::getBuildTarget)
        .collect(Collectors.toList());
    return new WorkspaceBuildTargetsResult(targets);
  }
}
