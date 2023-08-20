package com.microsoft.java.bs.gradle.plugin;

import javax.inject.Inject;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

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
}
