// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.plugin;

import java.util.Set;

import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.LanguageExtension;
import com.microsoft.java.bs.gradle.model.SupportedLanguage;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.tasks.SourceSet;

/**
 * The language model builder for different languages.
 */
public abstract class LanguageModelBuilder {

  public abstract SupportedLanguage<?> getLanguage();

  public final String getLanguageId() {
    return getLanguage().getBspName();
  }

  public abstract LanguageExtension getExtensionFor(Project project, SourceSet sourceSet,
      Set<GradleModuleDependency> moduleDependencies);

  protected final Task getLanguageCompileTask(Project project, SourceSet sourceSet) {
    String taskName = sourceSet.getCompileTaskName(getLanguage().getGradleName());
    try {
      return project.getTasks().getByName(taskName);
    } catch (UnknownTaskException e) {
      return null;
    }
  }
}
