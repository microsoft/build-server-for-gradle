package com.microsoft.java.bs.core.internal.gradle.actions;

import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSets;
import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.model.gradle.GradleBuild;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * {@link BuildAction} that retrieves {@link GradleSourceSets} from a Gradle build,
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
    Map<String, List<GradleSourceSet>> sourceSets = new HashMap<>();
    GradleBuild buildModel = buildController.getBuildModel();
    String rootProjectName = buildModel.getRootProject().getName();
    fetchModels(buildController, buildModel, sourceSets, rootProjectName);
    return new DefaultGradleSourceSets(sourceSets.values().stream().flatMap(List::stream)
        .collect(Collectors.toList()));
  }

  /**
   * Fetches source sets from the provided Gradle build model and stores them in a map categorized by project name.
   *
   * @param buildController The Gradle build controller used to interact with the build.
   * @param build The Gradle build model representing the current build.
   * @param sourceSets A map to store the retrieved source sets categorized by project name.
   * @param buildName The name of the root project in the build.
   */
  private void fetchModels(
      BuildController buildController,
      GradleBuild build,
      Map<String, List<GradleSourceSet>> sourceSets,
      String buildName
  ) {
    if (sourceSets.containsKey(buildName)) {
      return;
    }
    sourceSets.put(
        buildName,
        buildController
            .findModel(build.getRootProject(), GradleSourceSets.class)
            .getGradleSourceSets()
    );
    for (GradleBuild includedBuild : build.getIncludedBuilds()) {
      String includedBuildName = includedBuild.getRootProject().getName();
      fetchModels(buildController, includedBuild, sourceSets, includedBuildName);
    }
  }

}
