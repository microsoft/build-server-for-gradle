// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.plugin;

import java.io.File;
import java.util.Collection;
import java.util.Set;

import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.SupportedLanguage;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.tasks.SourceSet;

/**
 * The language model builder for different languages.
 */
public abstract class LanguageModelBuilder {
  public abstract boolean appliesFor(Project project, SourceSet sourceSet);

  public abstract String getLanguageId();

  public abstract Collection<File> getSourceFoldersFor(Project project, SourceSet sourceSet);

  public abstract Collection<File> getGeneratedSourceFoldersFor(Project project,
      SourceSet sourceSet);

  public abstract Object getExtensionsFor(Project project, SourceSet sourceSet,
      Set<GradleModuleDependency> moduleDependencies);

  protected Task getLanguageCompileTask(SupportedLanguage<?> language,
      Project project, SourceSet sourceSet) {
    String taskName = sourceSet.getCompileTaskName(language.getGradleName());
    try {
      return project.getTasks().getByName(taskName);
    } catch (UnknownTaskException e) {
      return null;
    }
  }
}
