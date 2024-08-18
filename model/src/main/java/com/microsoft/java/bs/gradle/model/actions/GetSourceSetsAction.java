// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.actions;

import com.microsoft.java.bs.gradle.model.BuildTargetDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.impl.DefaultBuildTargetDependency;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSet;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSets;
import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.GradleBuild;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link BuildAction} that retrieves {@link DefaultGradleSourceSet} from a Gradle build,
 * handling both normal and composite builds.
 */
public class GetSourceSetsAction implements BuildAction<GradleSourceSets> {

  /**
   * Executes the build action and retrieves source sets from the Gradle build.
   *
   * @return A {@link DefaultGradleSourceSets} object containing all retrieved source sets.
   */
  @Override
  public GradleSourceSets execute(BuildController buildController) {
    Collection<GradleBuild> builds = fetchIncludedBuilds(buildController);
    List<GradleSourceSet> sourceSets = fetchModels(buildController, builds);
    return new DefaultGradleSourceSets(sourceSets);
  }

  private Collection<GradleBuild> fetchIncludedBuilds(BuildController buildController) {
    Map<String, GradleBuild> builds = new HashMap<>();
    GradleBuild build = buildController.getBuildModel();
    String rootProjectName = build.getRootProject().getName();
    fetchIncludedBuilds(build, builds, rootProjectName);
    return builds.values();
  }

  private void fetchIncludedBuilds(GradleBuild build, Map<String,
      GradleBuild> builds, String rootProjectName) {
    if (builds.containsKey(rootProjectName)) {
      return;
    }
    builds.put(rootProjectName, build);
    // Cannot use GradleVersion.current() in BuildAction as that will return the Tooling API version
    // Cannot use BuildEnvironment to get GradleVersion as that doesn't work pre-3.0 even though
    // documentation has it added in version 1.
    // So just handle exceptions
    Set<? extends GradleBuild> moreBuilds;
    try {
      // added in 4.10
      moreBuilds = build.getEditableBuilds();
    } catch (Exception e1) {
      try {
        // added in 3.3
        moreBuilds = build.getIncludedBuilds();
      } catch (Exception e2) {
        moreBuilds = null;
      }
    }
    if (moreBuilds != null) {
      for (GradleBuild includedBuild : moreBuilds) {
        String includedBuildName = includedBuild.getRootProject().getName();
        fetchIncludedBuilds(includedBuild, builds, includedBuildName);
      }
    }
  }

  /**
   * Fetches source sets from the provided Gradle build model.
   *
   * @param buildController The Gradle build controller used to interact with the build.
   * @param builds The Gradle build models representing the build and included builds.
   */
  private List<GradleSourceSet> fetchModels(BuildController buildController,
                                            Collection<GradleBuild> builds) {

    List<GetSourceSetAction> projectActions = new ArrayList<>();
    for (GradleBuild build : builds) {
      for (BasicGradleProject project : build.getProjects()) {
        projectActions.add(new GetSourceSetAction(project));
      }
    }

    // since the model returned from Gradle TAPI is a wrapped object, here we re-construct it
    // via a copy constructorso we can treat as a DefaultGradleSourceSet and
    // populate source set dependencies.
    List<GradleSourceSet> sourceSets = buildController.run(projectActions).stream()
        .flatMap(ss -> ss.getGradleSourceSets().stream())
        .map(DefaultGradleSourceSet::new)
        .collect(Collectors.toList());

    populateInterProjectInfo(sourceSets);

    return sourceSets;
  }

  /**
   * {@link BuildAction} that retrieves {@link GradleSourceSets} for a single project.
   * This allows project models to be retrieved in parallel.
   */
  static class GetSourceSetAction implements BuildAction<GradleSourceSets> {
    private final BasicGradleProject project;

    public GetSourceSetAction(BasicGradleProject project) {
      this.project = project;
    }

    @Override
    public GradleSourceSets execute(BuildController controller) {
      return controller.getModel(project, GradleSourceSets.class);
    }
  }

  // Inter-sourceset dependencies must be built up after retrieval of all sourcesets
  // because they are not available before when using included builds.
  // Classpaths that reference other projects using jars are to be replaced with
  // source paths.
  private void populateInterProjectInfo(List<GradleSourceSet> sourceSets) {
    // map all output dirs to their source sets
    Map<File, List<File>> archivesToSourceOutput = new HashMap<>();
    Map<File, GradleSourceSet> outputsToSourceSet = new HashMap<>();
    for (GradleSourceSet sourceSet : sourceSets) {
      if (sourceSet.getSourceOutputDirs() != null) {
        for (File file : sourceSet.getSourceOutputDirs()) {
          outputsToSourceSet.put(file, sourceSet);
        }
      }
      if (sourceSet.getResourceOutputDirs() != null) {
        for (File file : sourceSet.getResourceOutputDirs()) {
          outputsToSourceSet.put(file, sourceSet);
        }
      }
      if (sourceSet.getArchiveOutputFiles() != null) {
        for (Map.Entry<File, List<File>> archive : sourceSet.getArchiveOutputFiles().entrySet()) {
          outputsToSourceSet.put(archive.getKey(), sourceSet);
          archivesToSourceOutput.computeIfAbsent(archive.getKey(), f -> new ArrayList<>())
              .addAll(archive.getValue());
        }
      }
    }

    // match any classpath entries to other project's output dirs/jars to create dependencies.
    // replace classpath entries that reference jars with classes dirs.
    for (GradleSourceSet sourceSet : sourceSets) {
      Set<BuildTargetDependency> dependencies = new HashSet<>();
      List<File> classpath = new ArrayList<>();
      for (File file : sourceSet.getCompileClasspath()) {
        // add project dependency
        GradleSourceSet otherSourceSet = outputsToSourceSet.get(file);
        if (otherSourceSet != null) {
          dependencies.add(new DefaultBuildTargetDependency(otherSourceSet));
        }
        // replace jar on classpath with source output on classpath
        List<File> sourceOutputDir = archivesToSourceOutput.get(file);
        if (sourceOutputDir == null) {
          classpath.add(file);
        } else {
          classpath.addAll(sourceOutputDir);
        }
      }
      if (sourceSet instanceof DefaultGradleSourceSet) {
        ((DefaultGradleSourceSet) sourceSet).setBuildTargetDependencies(dependencies);
        ((DefaultGradleSourceSet) sourceSet).setCompileClasspath(classpath);
      }
    }
  }
}