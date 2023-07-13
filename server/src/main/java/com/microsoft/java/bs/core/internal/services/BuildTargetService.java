package com.microsoft.java.bs.core.internal.services;

import java.util.List;
import java.util.stream.Collectors;

import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;

/**
 * Service to handle build target related BSP requests.
 */
public class BuildTargetService {

  private BuildTargetManager buildTargetManager;

  public BuildTargetService(BuildTargetManager buildTargetManager) {
    this.buildTargetManager = buildTargetManager;
  }

  /**
   * Get the build targets of the workspace.
   */
  public WorkspaceBuildTargetsResult getWorkspaceBuildTargets() {
    List<GradleBuildTarget> allTargets = buildTargetManager.getAllGradleBuildTargets();
    List<BuildTarget> targets = allTargets.stream()
        .map(GradleBuildTarget::getBuildTarget)
        .collect(Collectors.toList());
    return new WorkspaceBuildTargetsResult(targets);
  }
}
