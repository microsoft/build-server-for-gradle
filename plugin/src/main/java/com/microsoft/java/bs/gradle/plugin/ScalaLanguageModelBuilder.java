// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.plugin;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.impl.DefaultScalaExtension;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.tasks.scala.MinimalScalaCompileOptions;
import org.gradle.api.internal.tasks.scala.ScalaCompileSpec;
import org.gradle.api.internal.tasks.scala.ZincScalaCompilerArgumentsGenerator;
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
    return SupportedLanguages.SCALA.getBspName();
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
  public Object getExtensionsFor(Project project, SourceSet sourceSet,
      Set<GradleModuleDependency> moduleDependencies) {
    GradleModuleDependency scalaLibraryDependency = getScalaLibraryDependency(moduleDependencies);
    if (scalaLibraryDependency != null) {
      ScalaCompile scalaCompile = getScalaCompileTask(project, sourceSet);
      DefaultScalaExtension extension = new DefaultScalaExtension();
      extension.setScalaOrganization(getScalaOrganization(scalaLibraryDependency));
      extension.setScalaVersion(getScalaVersion(scalaLibraryDependency));
      extension.setScalaBinaryVersion(getScalaBinaryVersion(scalaLibraryDependency));
      extension.setScalaCompilerArgs(getScalaCompilerArgs(scalaCompile));
      extension.setScalaJars(getScalaJars(scalaCompile));
      return extension;
    }
    // Scala plugin found but Scala library not found - should not be possible
    return null;
  }

  private GradleModuleDependency findModule(String name,
                                            Set<GradleModuleDependency> moduleDependencies) {
    Optional<GradleModuleDependency> module = moduleDependencies.stream()
            .filter(f -> f.getModule().equals(name))
            .findAny();
    return module.orElse(null);
  }

  private GradleModuleDependency getScalaLibraryDependency(
          Set<GradleModuleDependency> moduleDependencies) {
    // scala 3 library takes precedence as scala 2 library can also be present.
    GradleModuleDependency scala3Library = findModule("scala3-library_3", moduleDependencies);
    if (scala3Library != null) {
      return scala3Library;
    }
    return findModule("scala-library", moduleDependencies);
  }

  private String getScalaOrganization(GradleModuleDependency scalaLibraryDependency) {
    return scalaLibraryDependency.getGroup();
  }

  private String getScalaVersion(GradleModuleDependency scalaLibraryDependency) {
    return scalaLibraryDependency.getVersion();
  }

  private String getScalaBinaryVersion(GradleModuleDependency scalaLibraryDependency) {
    String version = scalaLibraryDependency.getVersion();
    int idx1 = version.indexOf('.');
    int idx2 = version.indexOf('.', idx1 + 1);
    return version.substring(0, idx2);
  }

  private List<String> getScalaCompilerArgs(ScalaCompile scalaCompile) {
    ScalaCompileSpecContainer specContainer = new ScalaCompileSpecContainer(scalaCompile);
    ZincScalaCompilerArgumentsGenerator argGenerator = new ZincScalaCompilerArgumentsGenerator();
    return argGenerator.generate(specContainer);
  }

  private List<File> getScalaJars(ScalaCompile scalaCompile) {
    return new LinkedList<>(scalaCompile.getScalaClasspath().getFiles());
  }

  // a minimal implementation of ScalaCompileSpec so ZincScalaCompilerArgumentsGenerator can
  // be used to generate the Scala compile options instead of duplicating it.
  private static class ScalaCompileSpecContainer implements ScalaCompileSpec {

    private final ScalaCompile scalaCompile;

    ScalaCompileSpecContainer(ScalaCompile scalaCompile) {
      this.scalaCompile = scalaCompile;
    }

    @Override
    public List<File> getCompileClasspath() {
      throw new UnsupportedOperationException("Unimplemented method 'getCompileClasspath'");
    }

    @Override
    public File getDestinationDir() {
      throw new UnsupportedOperationException("Unimplemented method 'getDestinationDir'");
    }

    @Override
    public Integer getRelease() {
      throw new UnsupportedOperationException("Unimplemented method 'getRelease'");
    }

    @Override
    public String getSourceCompatibility() {
      throw new UnsupportedOperationException("Unimplemented method 'getSourceCompatibility'");
    }

    @Override
    public Iterable<File> getSourceFiles() {
      throw new UnsupportedOperationException("Unimplemented method 'getSourceFiles'");
    }

    @Override
    public List<File> getSourceRoots() {
      throw new UnsupportedOperationException("Unimplemented method 'getSourceRoots'");
    }

    @Override
    public String getTargetCompatibility() {
      throw new UnsupportedOperationException("Unimplemented method 'getTargetCompatibility'");
    }

    @Override
    public File getTempDir() {
      throw new UnsupportedOperationException("Unimplemented method 'getTempDir'");
    }

    @Override
    public File getWorkingDir() {
      throw new UnsupportedOperationException("Unimplemented method 'getWorkingDir'");
    }

    @Override
    public void setCompileClasspath(List<File> classpath) {
      throw new UnsupportedOperationException("Unimplemented method 'setCompileClasspath'");
    }

    @Override
    public void setDestinationDir(File destinationDir) {
      throw new UnsupportedOperationException("Unimplemented method 'setDestinationDir'");
    }

    @Override
    public void setRelease(Integer arg0) {
      throw new UnsupportedOperationException("Unimplemented method 'setRelease'");
    }

    @Override
    public void setSourceCompatibility(String arg0) {
      throw new UnsupportedOperationException("Unimplemented method 'setSourceCompatibility'");
    }

    @Override
    public void setSourceFiles(Iterable<File> sourceFiles) {
      throw new UnsupportedOperationException("Unimplemented method 'setSourceFiles'");
    }

    @Override
    public void setSourcesRoots(List<File> sourcesRoots) {
      throw new UnsupportedOperationException("Unimplemented method 'setSourcesRoots'");
    }

    @Override
    public void setTargetCompatibility(String arg0) {
      throw new UnsupportedOperationException("Unimplemented method 'setTargetCompatibility'");
    }

    @Override
    public void setTempDir(File tempDir) {
      throw new UnsupportedOperationException("Unimplemented method 'setTempDir'");
    }

    @Override
    public void setWorkingDir(File workingDir) {
      throw new UnsupportedOperationException("Unimplemented method 'setWorkingDir'");
    }

    @Override
    public File getAnalysisFile() {
      throw new UnsupportedOperationException("Unimplemented method 'getAnalysisFile'");
    }

    @Override
    public Map<File, File> getAnalysisMap() {
      throw new UnsupportedOperationException("Unimplemented method 'getAnalysisMap'");
    }

    @Override
    public long getBuildStartTimestamp() {
      throw new UnsupportedOperationException("Unimplemented method 'getBuildStartTimestamp'");
    }

    @Override
    public File getClassfileBackupDir() {
      throw new UnsupportedOperationException("Unimplemented method 'getClassfileBackupDir'");
    }

    @Override
    public MinimalScalaCompileOptions getScalaCompileOptions() {
      return new MinimalScalaCompileOptions(scalaCompile.getScalaCompileOptions());
    }

    @Override
    public Iterable<File> getScalaCompilerPlugins() {
      return scalaCompile.getScalaCompilerPlugins();
    }

    @Override
    public void setAnalysisFile(File analysisFile) {
      throw new UnsupportedOperationException("Unimplemented method 'setAnalysisFile'");
    }

    @Override
    public void setAnalysisMap(Map<File, File> analysisMap) {
      throw new UnsupportedOperationException("Unimplemented method 'setAnalysisMap'");
    }

    @Override
    public void setClassfileBackupDir(File classfileBackupDir) {
      throw new UnsupportedOperationException("Unimplemented method 'setClassfileBackupDir'");
    }

    @Override
    public void setScalaCompilerPlugins(Iterable<File> plugins) {
      throw new UnsupportedOperationException("Unimplemented method 'setScalaCompilerPlugins'");
    }
  }
}
