// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.services;

import static com.microsoft.java.bs.core.Launcher.LOGGER;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.microsoft.java.bs.core.Launcher;
import com.microsoft.java.bs.core.internal.gradle.GradleApiConnector;
import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.core.internal.utils.TelemetryUtils;
import com.microsoft.java.bs.core.internal.utils.UriUtils;
import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetEvent;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.CleanCacheParams;
import ch.epfl.scala.bsp4j.CleanCacheResult;
import ch.epfl.scala.bsp4j.CompileParams;
import ch.epfl.scala.bsp4j.CompileResult;
import ch.epfl.scala.bsp4j.DependencyModule;
import ch.epfl.scala.bsp4j.DependencyModulesItem;
import ch.epfl.scala.bsp4j.DependencyModulesParams;
import ch.epfl.scala.bsp4j.DependencyModulesResult;
import ch.epfl.scala.bsp4j.JavacOptionsItem;
import ch.epfl.scala.bsp4j.JavacOptionsParams;
import ch.epfl.scala.bsp4j.JavacOptionsResult;
import ch.epfl.scala.bsp4j.DidChangeBuildTarget;
import ch.epfl.scala.bsp4j.MavenDependencyModule;
import ch.epfl.scala.bsp4j.MavenDependencyModuleArtifact;
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
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;

/**
 * Service to handle build target related BSP requests.
 */
public class BuildTargetService {

  private static final String MAVEN_DATA_KIND = "maven";

  private BuildTargetManager buildTargetManager;

  private GradleApiConnector connector;

  private PreferenceManager preferenceManager;

  /**
   * Initialize the build target service.
   *
   * @param buildTargetManager the build target manager.
   * @param preferenceManager the preference manager.
   */
  public BuildTargetService(BuildTargetManager buildTargetManager,
      GradleApiConnector connector, PreferenceManager preferenceManager) {
    this.buildTargetManager = buildTargetManager;
    this.connector = connector;
    this.preferenceManager = preferenceManager;
  }

  /**
   * Get the build targets of the workspace.
   */
  public WorkspaceBuildTargetsResult getWorkspaceBuildTargets() {
    List<GradleBuildTarget> allTargets = buildTargetManager.getAllGradleBuildTargets();
    List<BuildTarget> targets = allTargets.stream()
        .map(GradleBuildTarget::getBuildTarget)
        .collect(Collectors.toList());
    Map<String, String> map = TelemetryUtils.getMetadataMap("buildTargetCount",
        String.valueOf(targets.size()));
    LOGGER.log(Level.INFO, "Found " + targets.size() + " build targets", map);
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
        LOGGER.warning("Skip sources collection for the build target: " + btId.getUri()
            + ". Because it cannot be found in the cache.");
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
        LOGGER.warning("Skip resources collection for the build target: " + btId.getUri()
            + ". Because it cannot be found in the cache.");
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
        LOGGER.warning("Skip output collection for the build target: " + btId.getUri()
            + ". Because it cannot be found in the cache.");
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

  /**
   * Get artifacts dependencies.
   */
  public DependencyModulesResult getBuildTargetDependencyModules(DependencyModulesParams params) {
    List<DependencyModulesItem> items = new ArrayList<>();
    for (BuildTargetIdentifier btId : params.getTargets()) {
      GradleBuildTarget target = buildTargetManager.getGradleBuildTarget(btId);
      if (target == null) {
        LOGGER.warning("Skip output collection for the build target: " + btId.getUri()
            + ". Because it cannot be found in the cache.");
        continue;
      }

      GradleSourceSet sourceSet = target.getSourceSet();
      List<DependencyModule> modules = new ArrayList<>();
      for (GradleModuleDependency dep : sourceSet.getModuleDependencies()) {
        DependencyModule module = new DependencyModule(dep.getModule(), dep.getVersion());
        module.setDataKind(MAVEN_DATA_KIND);
        List<MavenDependencyModuleArtifact> artifacts = dep.getArtifacts().stream().map(a -> {
          MavenDependencyModuleArtifact artifact = new MavenDependencyModuleArtifact(
              a.getUri().toString());
          artifact.setClassifier(a.getClassifier());
          return artifact;
        }).collect(Collectors.toList());
        MavenDependencyModule mavenModule = new MavenDependencyModule(
            dep.getGroup(),
            dep.getModule(),
            dep.getVersion(),
            artifacts
        );
        module.setData(mavenModule);
        modules.add(module);
      }

      DependencyModulesItem item = new DependencyModulesItem(btId, modules);
      items.add(item);
    }
    return new DependencyModulesResult(items);
  }

  /**
   * Compile the build targets.
   */
  public CompileResult compile(CompileParams params) {
    StatusCode code = runTasks(params.getTargets(), this::getBuildTaskName);
    CompileResult result = new CompileResult(code);
    result.setOriginId(params.getOriginId());

    // Schedule a task to refetch the build targets after compilation, this is to
    // auto detect the source roots changes for those code generation framework,
    // such as Protocol Buffer.
    CompletableFuture.runAsync(new RefetchBuildTargetTask());
    return result;
  }

  /**
   * clean the build targets.
   */
  public CleanCacheResult cleanCache(CleanCacheParams params) {
    StatusCode code = runTasks(params.getTargets(), this::getCleanTaskName);
    return new CleanCacheResult(null, code == StatusCode.OK);
  }

  /**
   * group targets by project root and execute the supplied tasks.
   */
  private StatusCode runTasks(List<BuildTargetIdentifier> targets,
      Function<BuildTargetIdentifier, String> taskNameCreator) {
    Map<URI, Set<BuildTargetIdentifier>> groupedTargets = groupBuildTargetsByRootDir(targets);
    StatusCode code = StatusCode.OK;
    for (Map.Entry<URI, Set<BuildTargetIdentifier>> entry : groupedTargets.entrySet()) {
      Set<BuildTargetIdentifier> btIds = entry.getValue();
      // remove duplicates as some tasks will have the same name for each sourceset e.g. clean.
      String[] tasks = btIds.stream().map(taskNameCreator).distinct().toArray(String[]::new);
      code = connector.runTasks(entry.getKey(), btIds, tasks);
      if (code == StatusCode.ERROR) {
        break;
      }
    }
    return code;
  }

  /**
   * Get the compiler options.
   */
  public JavacOptionsResult getBuildTargetJavacOptions(JavacOptionsParams params) {
    List<JavacOptionsItem> items = new ArrayList<>();
    for (BuildTargetIdentifier btId : params.getTargets()) {
      GradleBuildTarget target = buildTargetManager.getGradleBuildTarget(btId);
      if (target == null) {
        LOGGER.warning("Skip javac options collection for the build target: " + btId.getUri()
            + ". Because it cannot be found in the cache.");
        continue;
      }

      GradleSourceSet sourceSet = target.getSourceSet();
      List<String> classpath = sourceSet.getCompileClasspath().stream()
          .map(file -> file.toURI().toString())
          .collect(Collectors.toList());
      String classesDir;
      if (sourceSet.getSourceOutputDir() != null) {
        classesDir = sourceSet.getSourceOutputDir().toURI().toString();
      } else {
        classesDir = "";
      }
      items.add(new JavacOptionsItem(
          btId,
          sourceSet.getCompilerArgs(),
          classpath,
          classesDir
      ));
    }
    return new JavacOptionsResult(items);
  }

  /**
   * Group the build targets by the project root directory,
   * projects with the same root directory can run their tasks
   * in one single call.
   */
  private Map<URI, Set<BuildTargetIdentifier>> groupBuildTargetsByRootDir(
      List<BuildTargetIdentifier> targets
  ) {
    Map<URI, Set<BuildTargetIdentifier>> groupedTargets = new HashMap<>();
    for (BuildTargetIdentifier btId : targets) {
      URI projectUri = getRootProjectUri(btId);
      if (projectUri == null) {
        continue;
      }
      groupedTargets.computeIfAbsent(projectUri, k -> new HashSet<>()).add(btId);
    }
    return groupedTargets;
  }

  /**
   * Try to get the project root directory uri. If root directory is not available,
   * return the uri of the build target.
   */
  private URI getRootProjectUri(BuildTargetIdentifier btId) {
    GradleBuildTarget gradleBuildTarget = buildTargetManager.getGradleBuildTarget(btId);
    if (gradleBuildTarget == null) {
      // TODO: https://github.com/microsoft/build-server-for-gradle/issues/50
      throw new IllegalArgumentException("The build target does not exist: " + btId.getUri());
    }
    BuildTarget buildTarget = gradleBuildTarget.getBuildTarget();
    if (buildTarget.getBaseDirectory() != null) {
      return UriUtils.getUriFromString(buildTarget.getBaseDirectory());
    }

    return UriUtils.getUriWithoutQuery(btId.getUri());
  }

  /**
   * Return the build task name - [project path]:[task].
   */
  private String getBuildTaskName(BuildTargetIdentifier btId) {
    GradleBuildTarget gradleBuildTarget = buildTargetManager.getGradleBuildTarget(btId);
    if (gradleBuildTarget == null) {
      // TODO: https://github.com/microsoft/build-server-for-gradle/issues/50
      throw new IllegalArgumentException("The build target does not exist: " + btId.getUri());
    }
    GradleSourceSet sourceSet = gradleBuildTarget.getSourceSet();
    String classesTaskName = sourceSet.getClassesTaskName();
    if (StringUtils.isBlank(classesTaskName)) {
      throw new IllegalArgumentException("The build target does not have a classes task: "
          + btId.getUri());
    }

    String modulePath = sourceSet.getProjectPath();
    if (modulePath == null || modulePath.equals(":")) {
      return classesTaskName;
    }
    return modulePath + ":" + classesTaskName;
  }

  /**
   * Return the clean task name - [project path]:[task].
   */
  private String getCleanTaskName(BuildTargetIdentifier btId) {
    GradleBuildTarget gradleBuildTarget = buildTargetManager.getGradleBuildTarget(btId);
    if (gradleBuildTarget == null) {
      // TODO: https://github.com/microsoft/build-server-for-gradle/issues/50
      throw new IllegalArgumentException("The build target does not exist: " + btId.getUri());
    }
    GradleSourceSet sourceSet = gradleBuildTarget.getSourceSet();
    String classesTaskName = "clean";

    String modulePath = sourceSet.getProjectPath();
    if (modulePath == null || modulePath.equals(":")) {
      return classesTaskName;
    }
    return modulePath + ":" + classesTaskName;
  }

  class RefetchBuildTargetTask implements Runnable {

    @Override
    public void run() {
      GradleSourceSets sourceSets = connector.getGradleSourceSets(
          preferenceManager.getRootUri());
      List<BuildTargetIdentifier> changedTargets = buildTargetManager.store(sourceSets);
      if (!changedTargets.isEmpty()) {
        notifyBuildTargetsChanged(changedTargets);
      }
    }

    private void notifyBuildTargetsChanged(List<BuildTargetIdentifier> changedTargets) {
      List<BuildTargetEvent> events = changedTargets.stream()
          .map(BuildTargetEvent::new)
          .collect(Collectors.toList());
      DidChangeBuildTarget param = new DidChangeBuildTarget(events);
      Launcher.client.onBuildTargetDidChange(param);
    }
  }
}
