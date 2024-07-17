// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.actions;

import com.microsoft.java.bs.gradle.model.BuildTargetDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.GradleSourceSetsMetadata;
import com.microsoft.java.bs.gradle.model.impl.DefaultBuildTargetDependency;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSet;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSets;
import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.model.gradle.GradleBuild;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;

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
    Set<String> traversedProjects = new HashSet<>();
    Map<GradleSourceSet, List<File>> sourceSetToClasspath = new HashMap<>();
    Map<File, GradleSourceSet> outputsToSourceSet = new HashMap<>();

    GradleBuild buildModel = buildController.getBuildModel();
    String rootProjectName = buildModel.getRootProject().getName();
    fetchModels(buildController,
        buildModel,
        traversedProjects,
        sourceSetToClasspath,
        outputsToSourceSet,
        rootProjectName);

    // Add dependencies
    List<GradleSourceSet> sourceSets = new ArrayList<>();
    for (Entry<GradleSourceSet, List<File>> entry : sourceSetToClasspath.entrySet()) {
      Set<BuildTargetDependency> dependencies = new HashSet<>();
      for (File file : entry.getValue()) {
        GradleSourceSet otherSourceSet = outputsToSourceSet.get(file);
        if (otherSourceSet != null && !Objects.equals(entry.getKey(), otherSourceSet)) {
          dependencies.add(new DefaultBuildTargetDependency(otherSourceSet));
        }
      }

      DefaultGradleSourceSet sourceSet = new DefaultGradleSourceSet(entry.getKey());
      sourceSet.setBuildTargetDependencies(dependencies);
      sourceSets.add(sourceSet);

    }

    return new DefaultGradleSourceSets(sourceSets);
  }

  /**
   * Fetches source sets from the provided Gradle build model and
   * stores them in a map categorized by project name.
   *
   * @param buildController      The Gradle build controller used to interact with the build.
   * @param build                The Gradle build model representing the current build.
   * @param traversedProjects    A set of traversed project names to avoid cyclic dependencies.
   * @param sourceSetToClasspath A map that associates GradleSourceSet objects with their
   *                             corresponding classpath files.
   * @param outputsToSourceSet   A map that associates output files with the GradleSourceSet
   *                             they belong to.
   * @param buildName            The name of the root project in the build.
   */
  private void fetchModels(
      BuildController buildController,
      GradleBuild build,
      Set<String> traversedProjects,
      Map<GradleSourceSet, List<File>> sourceSetToClasspath,
      Map<File, GradleSourceSet> outputsToSourceSet,
      String buildName
  ) {
    if (traversedProjects.contains(buildName)) {
      return;
    }
    GradleSourceSetsMetadata sourceSets = buildController
        .findModel(build.getRootProject(), GradleSourceSetsMetadata.class);

    traversedProjects.add(buildName);
    sourceSetToClasspath.putAll(sourceSets.getGradleSourceSetsToClasspath());
    outputsToSourceSet.putAll(sourceSets.getOutputsToSourceSet());

    for (GradleBuild includedBuild : build.getIncludedBuilds()) {
      String includedBuildName = includedBuild.getRootProject().getName();
      fetchModels(buildController,
          includedBuild,
          traversedProjects,
          sourceSetToClasspath,
          outputsToSourceSet,
          includedBuildName);
    }
  }

}
