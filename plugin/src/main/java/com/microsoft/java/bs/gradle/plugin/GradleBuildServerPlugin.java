package com.microsoft.java.bs.gradle.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.plugin.model.DefaultGradleSourceSet;
import com.microsoft.java.bs.gradle.plugin.model.DefaultGradleSourceSets;

/**
 * The customized Gradle plugin to get the project structure information.
 */
public class GradleBuildServerPlugin implements Plugin<Project> {
  private final ToolingModelBuilderRegistry registry;

  @Inject
  public GradleBuildServerPlugin(ToolingModelBuilderRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void apply(Project project) {
    project.afterEvaluate(p -> registry.register(new SourceSetsModelBuilder()));
  }

  private static class SourceSetsModelBuilder implements ToolingModelBuilder {
    @Override
    public boolean canBuild(String modelName) {
      return modelName.equals(GradleSourceSets.class.getName());
    }

    @Override
    public Object buildAll(String modelName, Project rootProject) {
      Set<Project> allProject = rootProject.getAllprojects();
      List<GradleSourceSet> gradleSourceSets = new ArrayList<>();
      for (Project project : allProject) {
        SourceSetContainer sourceSets = getSourceSetContainer(project);
        if (sourceSets == null || sourceSets.isEmpty()) {
          continue;
        }

        sourceSets.forEach(sourceSet -> {
          DefaultGradleSourceSet gradleSourceSet = new DefaultGradleSourceSet();
          gradleSourceSet.setProjectName(project.getName());
          gradleSourceSet.setProjectPath(project.getPath());
          gradleSourceSet.setProjectDir(project.getProjectDir());
          gradleSourceSet.setRootDir(project.getRootDir());
          gradleSourceSet.setSourceSetName(sourceSet.getName());

          gradleSourceSets.add(gradleSourceSet);
        });
      }
      DefaultGradleSourceSets result = new DefaultGradleSourceSets();
      result.setGradleSourceSets(gradleSourceSets);
      return result;
    }

    private SourceSetContainer getSourceSetContainer(Project project) {
      try {
        JavaPluginExtension javaPlugin = project.getExtensions()
            .findByType(JavaPluginExtension.class);
        if (javaPlugin != null) {
          return javaPlugin.getSourceSets();
        }
      } catch (NoClassDefFoundError | NoSuchMethodError e) {
        // to be compatible with Gradle < 7.1
        JavaPluginConvention convention = project.getConvention()
            .getPlugin(JavaPluginConvention.class);
        if (convention != null) {
          return convention.getSourceSets();
        }
      }
      return null;
    }
  }
}
