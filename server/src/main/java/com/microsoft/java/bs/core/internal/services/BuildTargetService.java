package com.microsoft.java.bs.core.internal.services;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.OutputPathItem;
import ch.epfl.scala.bsp4j.OutputPathItemKind;
import ch.epfl.scala.bsp4j.OutputPathsItem;
import ch.epfl.scala.bsp4j.OutputPathsParams;
import ch.epfl.scala.bsp4j.OutputPathsResult;
import ch.epfl.scala.bsp4j.ResourcesItem;
import ch.epfl.scala.bsp4j.ResourcesParams;
import ch.epfl.scala.bsp4j.ResourcesResult;
import ch.epfl.scala.bsp4j.SourceItem;
import ch.epfl.scala.bsp4j.SourceItemKind;
import ch.epfl.scala.bsp4j.SourcesItem;
import ch.epfl.scala.bsp4j.SourcesParams;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;

/**
 * Service to handle build target related BSP requests.
 */
public class BuildTargetService {

  private static final Logger logger = LoggerFactory.getLogger(BuildTargetService.class);

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

  /**
   * Get the sources.
   */
  public SourcesResult getBuildTargetSources(SourcesParams params) {
    List<SourcesItem> sourceItems = new ArrayList<>();
    for (BuildTargetIdentifier btId : params.getTargets()) {
      GradleBuildTarget target = buildTargetManager.getGradleBuildTarget(btId);
      if (target == null) {
        logger.warn("Skip sources collection for the build target: {}"
            + "because it cannot be found in the cache.", btId.getUri());
        continue;
      }

      GradleSourceSet sourceSet = target.getSourceSet();
      List<SourceItem> sources = new ArrayList<>();
      for (File sourceDir : sourceSet.getSourceDirs()) {
        sources.add(new SourceItem(sourceDir.toURI().toString(), SourceItemKind.DIRECTORY,
            false /* generated */));
      }
      for (File sourceDir : sourceSet.getGeneratedSourceDirs()) {
        sources.add(new SourceItem(sourceDir.toURI().toString(), SourceItemKind.DIRECTORY,
            true /* generated */));
      }
      SourcesItem item = new SourcesItem(btId, sources);
      sourceItems.add(item);
    }
    return new SourcesResult(sourceItems);
  }

  /**
   * Get the resources.
   */
  public ResourcesResult getBuildTargetResources(ResourcesParams params) {
    List<ResourcesItem> items = new ArrayList<>();
    for (BuildTargetIdentifier btId : params.getTargets()) {
      GradleBuildTarget target = buildTargetManager.getGradleBuildTarget(btId);
      if (target == null) {
        logger.warn("Skip resources collection for the build target: {}"
            + "because it cannot be found in the cache.", btId.getUri());
        continue;
      }

      GradleSourceSet sourceSet = target.getSourceSet();
      List<String> resources = new ArrayList<>();
      for (File resourceDir : sourceSet.getResourceDirs()) {
        resources.add(resourceDir.toURI().toString());
      }
      ResourcesItem item = new ResourcesItem(btId, resources);
      items.add(item);
    }
    return new ResourcesResult(items);
  }

  /**
   * Get the output paths.
   */
  public OutputPathsResult getBuildTargetOutputPaths(OutputPathsParams params) {
    List<OutputPathsItem> items = new ArrayList<>();
    for (BuildTargetIdentifier btId : params.getTargets()) {
      GradleBuildTarget target = buildTargetManager.getGradleBuildTarget(btId);
      if (target == null) {
        logger.warn("Skip output collection for the build target: {}"
            + "because it cannot be found in the cache.", btId.getUri());
        continue;
      }

      GradleSourceSet sourceSet = target.getSourceSet();
      List<OutputPathItem> outputPaths = new ArrayList<>();
      // Due to the BSP spec does not support additional flags for each output path,
      // we will leverage the query of the uri to mark whether this is a source/resource
      // output path.
      // TODO: file a BSP spec issue to support additional flags for each output path.

      File sourceOutputDir = sourceSet.getSourceOutputDir();
      if (sourceOutputDir != null) {
        outputPaths.add(new OutputPathItem(
            sourceOutputDir.toURI().toString() + "?kind=source",
            OutputPathItemKind.DIRECTORY
        ));
      }

      File resourceOutputDir = sourceSet.getResourceOutputDir();
      if (resourceOutputDir != null) {
        outputPaths.add(new OutputPathItem(
            resourceOutputDir.toURI().toString() + "?kind=resource",
            OutputPathItemKind.DIRECTORY
        ));
      }

      OutputPathsItem item = new OutputPathsItem(btId, outputPaths);
      items.add(item);
    }
    return new OutputPathsResult(items);
  }
}
