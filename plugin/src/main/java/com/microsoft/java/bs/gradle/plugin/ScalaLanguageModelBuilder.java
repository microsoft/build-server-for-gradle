// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.plugin;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.ScalaSourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.scala.ScalaCompile;

import com.microsoft.java.bs.gradle.model.SupportedLanguages;

/**
 * The language model builder for Scala language.
 */
public class ScalaLanguageModelBuilder extends LanguageModelBuilder {

  @Override
  public boolean appliesFor(Project project, SourceSet sourceSet) {
    return getScalaCompileTask(project, sourceSet) != null;
  }

  @Override
  public String getLanguageId() {
    return SupportedLanguages.SCALA;
  }

  @Override
  public Collection<File> getSourceFoldersFor(Project project, SourceSet sourceSet) {
    SourceDirectorySet sourceDirectorySet = sourceSet.getExtensions()
        .findByType(ScalaSourceDirectorySet.class);
    return sourceDirectorySet == null ? Collections.emptySet() : sourceDirectorySet.getSrcDirs();
  }

  @Override
  public Collection<File> getGeneratedSourceFoldersFor(Project project, SourceSet sourceSet) {
    return Collections.emptySet();
  }

  private ScalaCompile getScalaCompileTask(Project project, SourceSet sourceSet) {
    return (ScalaCompile) getLanguageCompileTask(SupportedLanguages.SCALA, project, sourceSet);
  }

  @Override
  public Object getExtensionsFor(Project project, SourceSet sourceSet) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'getExtensionsFor'");
  }
}
