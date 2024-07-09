// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.plugin;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

import com.microsoft.java.bs.gradle.model.SupportedLanguages;

/**
 * The customized Gradle plugin to get the project structure information.
 */
public class GradleBuildServerPlugin implements Plugin<Project> {

  public static final List<LanguageModelBuilder> SUPPORTED_LANGUAGE_BUILDERS = new LinkedList<>();
  public static final Set<String> EXPERIMENTAL_FEATURES = new HashSet<>();

  private final ToolingModelBuilderRegistry registry;

  /**
   * Constructor for the GradleBuildServerPlugin.
   */
  @Inject
  public GradleBuildServerPlugin(ToolingModelBuilderRegistry registry) {
    registerSupportedLanguages();
    registerExperimentalFeatures();
    this.registry = registry;
  }

  @Override
  public void apply(Project project) {
    registry.register(new SourceSetsModelBuilder());
  }

  private void registerSupportedLanguages() {
    String supportedLanguagesProps = System.getProperty("bsp.gradle.supportedLanguages");
    if (supportedLanguagesProps != null) {
      String[] supportedLanguages = supportedLanguagesProps.split(",");
      for (String language : supportedLanguages) {
        if (language.equalsIgnoreCase(SupportedLanguages.JAVA.getBspName())) {
          SUPPORTED_LANGUAGE_BUILDERS.add(new JavaLanguageModelBuilder());
        } else if (language.equalsIgnoreCase(SupportedLanguages.SCALA.getBspName())) {
          SUPPORTED_LANGUAGE_BUILDERS.add(new ScalaLanguageModelBuilder());
        }
      }
    }
  }

  private void registerExperimentalFeatures() {
    String experimentalFeatures = System.getProperty("bsp.gradle.experimentalFeatures");
    if (experimentalFeatures != null) {
      String[] features = experimentalFeatures.split(",");
      for (String feature : features) {
        EXPERIMENTAL_FEATURES.add(feature);
      }
    }
  }
}
