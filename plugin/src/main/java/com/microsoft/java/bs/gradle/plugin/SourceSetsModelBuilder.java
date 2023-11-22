// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.plugins.ide.internal.tooling.java.DefaultInstalledJdk;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.util.GradleVersion;

import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSet;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSets;
import com.microsoft.java.bs.gradle.plugin.dependency.DependencyCollector;

/**
 * Model builder used to get information of source set.
 */
public class SourceSetsModelBuilder implements ToolingModelBuilder {

  @Override
  public boolean canBuild(String modelName) {
    return modelName.equals(GradleSourceSets.class.getName());
  }

  @Override
  public Object buildAll(String modelName, Project rootProject) {
    Set<Project> allProject = rootProject.getAllprojects();
    List<GradleSourceSet> gradleSourceSets = new ArrayList<>();
    // mapping Gradle source set to our customized model.
    Map<SourceSet, DefaultGradleSourceSet> sourceSetMap = new HashMap<>();
    // this set is used to eliminate the source, resource and output
    // directories from the module dependencies.
    Set<File> exclusionFromDependencies = new HashSet<>();
    for (Project project : allProject) {
      SourceSetContainer sourceSets = getSourceSetContainer(project);
      if (sourceSets == null || sourceSets.isEmpty()) {
        continue;
      }

      File defaultJavaHome = DefaultInstalledJdk.current().getJavaHome();
      String javaVersion = DefaultInstalledJdk.current().getJavaVersion().getMajorVersion();
      String gradleVersion = project.getGradle().getGradleVersion();
      sourceSets.forEach(sourceSet -> {
        DefaultGradleSourceSet gradleSourceSet = new DefaultGradleSourceSet();
        gradleSourceSet.setProjectName(project.getName());
        gradleSourceSet.setProjectPath(project.getPath());
        gradleSourceSet.setProjectDir(project.getProjectDir());
        gradleSourceSet.setRootDir(project.getRootDir());
        sourceSetMap.put(sourceSet, gradleSourceSet);
        gradleSourceSet.setSourceSetName(sourceSet.getName());
        gradleSourceSet.setClassesTaskName(sourceSet.getClassesTaskName());

        // source
        Set<File> srcDirs = sourceSet.getJava().getSrcDirs();
        gradleSourceSet.setSourceDirs(srcDirs);
        exclusionFromDependencies.addAll(srcDirs);
        Set<File> generatedSrcDirs = new HashSet<>();
        addAnnotationProcessingDir(project, sourceSet, generatedSrcDirs);
        addGeneratedSourceDirs(project, sourceSet, srcDirs, generatedSrcDirs);
        gradleSourceSet.setGeneratedSourceDirs(generatedSrcDirs);
        exclusionFromDependencies.addAll(generatedSrcDirs);

        // source output dir
        File sourceOutputDir = getSourceOutputDir(sourceSet);
        if (sourceOutputDir != null) {
          gradleSourceSet.setSourceOutputDir(sourceOutputDir);
          exclusionFromDependencies.add(sourceOutputDir);
        }

        // resource
        Set<File> resourceDirs = sourceSet.getResources().getSrcDirs();
        gradleSourceSet.setResourceDirs(resourceDirs);
        exclusionFromDependencies.addAll(resourceDirs);

        // resource output dir
        File resourceOutputDir = sourceSet.getOutput().getResourcesDir();
        if (resourceOutputDir != null) {
          gradleSourceSet.setResourceOutputDir(resourceOutputDir);
          exclusionFromDependencies.add(resourceOutputDir);
        }

        // jdk
        gradleSourceSet.setJavaHome(defaultJavaHome);
        gradleSourceSet.setJavaVersion(javaVersion);
        gradleSourceSet.setGradleVersion(gradleVersion);
        gradleSourceSet.setSourceCompatibility(getSourceCompatibility(project, sourceSet));
        gradleSourceSet.setTargetCompatibility(getTargetCompatibility(project, sourceSet));
        gradleSourceSet.setCompilerArgs(getCompilerArgs(project, sourceSet));
        gradleSourceSets.add(gradleSourceSet);
      });
    }

    // run through twice to cope with any name clashes
    for (DefaultGradleSourceSet sourceSet : sourceSetMap.values()) {
      if ("main".equals(sourceSet.getSourceSetName())) {
        sourceSet.setDisplayName(stripPathPrefix(sourceSet.getProjectPath()));
      }
    }
    for (DefaultGradleSourceSet sourceSet : sourceSetMap.values()) {
      if (!"main".equals(sourceSet.getSourceSetName())) {
        String uniqueName = createUniqueDisplayName(sourceSet, sourceSetMap);
        sourceSet.setDisplayName(uniqueName);
      }
    }

    for (Project project : allProject) {
      SourceSetContainer sourceSets = getSourceSetContainer(project);
      if (sourceSets == null || sourceSets.isEmpty()) {
        continue;
      }

      // dependencies
      sourceSets.forEach(sourceSet -> {
        DefaultGradleSourceSet gradleSourceSet = sourceSetMap.get(sourceSet);
        if (gradleSourceSet == null) {
          return;
        }
        DependencyCollector collector = new DependencyCollector(project,
            exclusionFromDependencies);
        collector.collectByConfigurationNames(getClasspathConfigurationNames(sourceSet));
        gradleSourceSet.setModuleDependencies(collector.getModuleDependencies());
        gradleSourceSet.setProjectDependencies(collector.getProjectDependencies());
      });
    }

    return new DefaultGradleSourceSets(gradleSourceSets);
  }

  private String stripPathPrefix(String projectPath) {
    if (projectPath.startsWith(":")) {
      return projectPath.substring(1);
    }
    return projectPath;
  }

  private String createUniqueDisplayName(DefaultGradleSourceSet sourceSet,
      Map<SourceSet, DefaultGradleSourceSet> sourceSetMap) {

    String fullName = stripPathPrefix(sourceSet.getProjectPath()) + "-"
        + sourceSet.getSourceSetName();
    // has the suffix caused a clash - apply a numbered suffix
    String usedName = fullName;
    int i = 2;
    boolean found = true;
    while (found) {
      String nameToTest = usedName;
      if (sourceSetMap.values().stream().noneMatch(ss -> nameToTest.equals(ss.getDisplayName()))) {
        found = false;
      } else {
        usedName = fullName + i;
        i++;
      }
    }
    return usedName;
  }

  private SourceSetContainer getSourceSetContainer(Project project) {
    if (!project.getPlugins().hasPlugin("java")) {
      return null;
    }

    if (GradleVersion.current().compareTo(GradleVersion.version("7.1")) >= 0) {
      JavaPluginExtension javaPlugin = project.getExtensions()
          .findByType(JavaPluginExtension.class);
      if (javaPlugin != null) {
        return javaPlugin.getSourceSets();
      }
    } else {
      Object javaPluginConvention = project.getConvention().getPlugins().get("java");
      if (javaPluginConvention != null) {
        try {
          Method getSourceSetsMethod = javaPluginConvention.getClass().getMethod("getSourceSets");
          return (SourceSetContainer) getSourceSetsMethod.invoke(javaPluginConvention);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException
          | IllegalArgumentException | InvocationTargetException e) {
        // ignore
        }
      }
    }
    return null;
  }

  private void addAnnotationProcessingDir(Project project, SourceSet sourceSet,
      Set<File> generatedSrcDirs) {
    JavaCompile javaCompile = getJavaCompileTask(project, sourceSet);
    if (javaCompile != null) {
      CompileOptions options = javaCompile.getOptions();
      if (GradleVersion.current().compareTo(GradleVersion.version("6.3")) >= 0) {
        Directory generatedDir = options.getGeneratedSourceOutputDirectory().getOrNull();
        if (generatedDir != null) {
          generatedSrcDirs.add(generatedDir.getAsFile());
        }
      } else if (GradleVersion.current().compareTo(GradleVersion.version("4.3")) >= 0) {
        File generatedDir = options.getAnnotationProcessorGeneratedSourcesDirectory();
        if (generatedDir != null) {
          generatedSrcDirs.add(generatedDir);
        }
      }
    }
  }

  private JavaCompile getJavaCompileTask(Project project, SourceSet sourceSet) {
    String taskName = sourceSet.getCompileJavaTaskName();
    return (JavaCompile) project.getTasks().getByName(taskName);
  }

  private void addGeneratedSourceDirs(Project project, SourceSet sourceSet,
      Set<File> srcDirs, Set<File> generatedSrcDirs) {
    JavaCompile javaCompile = getJavaCompileTask(project, sourceSet);
    if (javaCompile != null) {
      Set<File> filesToCompile = javaCompile.getSource().getFiles();
      for (File file : filesToCompile) {
        if (canSkipInferSourceRoot(file, srcDirs, generatedSrcDirs)) {
          continue;
        }

        // the file is not in the source directories, so it must be a generated file.
        // we need to find the source directory for the generated file.
        File srcDir = findSourceDirForGeneratedFile(file);
        if (srcDir != null) {
          generatedSrcDirs.add(srcDir);
        }
      }
    }
  }

  /**
   * Skip the source root inference if:
   * <ul>
   * <li>File is not a Java file.</li>
   * <li>File already belongs to srcDirs.</li>
   * <li>File already belongs to generatedSrcDirs.</li>
   * </ul>
   * Return <code>true</code> if the source root inference can be skipped.
   */
  private boolean canSkipInferSourceRoot(File sourceFile, Set<File> srcDirs,
      Set<File> generatedSrcDirs) {
    if (!sourceFile.isFile() || !sourceFile.exists() || !sourceFile.getName().endsWith(".java")) {
      return true;
    }

    if (srcDirs.stream().anyMatch(dir -> sourceFile.getAbsolutePath()
        .startsWith(dir.getAbsolutePath()))) {
      return true;
    }

    return generatedSrcDirs.stream().anyMatch(dir -> sourceFile.getAbsolutePath()
        .startsWith(dir.getAbsolutePath()));
  }

  /**
   * read the file content and find the package declaration.
   * Then find the source directory that contains the package declaration.
   * If the package declaration is not found, then return <code>null</code>.
   */
  private File findSourceDirForGeneratedFile(File file) {
    Pattern packagePattern = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;\\s*$");
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = reader.readLine()) != null) {
        Matcher matcher = packagePattern.matcher(line);
        if (matcher.matches()) {
          String packageName = matcher.group(1);
          String relativeToRoot = packageName.replace(".", File.separator)
              .concat(File.separator).concat(file.getName());
          String absolutePath = file.getAbsolutePath();
          if (!absolutePath.endsWith(relativeToRoot)) {
            return null;
          }
          return new File(absolutePath.substring(
                0, absolutePath.length() - relativeToRoot.length()));
        }
      }
    } catch (IOException e) {
      return null;
    }

    return null;
  }

  private File getSourceOutputDir(SourceSet sourceSet) {
    if (GradleVersion.current().compareTo(GradleVersion.version("6.1")) >= 0) {
      Directory sourceOutputDir = sourceSet.getJava().getClassesDirectory().getOrNull();
      if (sourceOutputDir != null) {
        return sourceOutputDir.getAsFile();
      }
      return null;
    } else if (GradleVersion.current().compareTo(GradleVersion.version("4.0")) >= 0) {
      try {
        // https://docs.gradle.org/4.0/javadoc/org/gradle/api/file/SourceDirectorySet.html#getOutputDir()
        Method getOutputDirMethod = SourceDirectorySet.class.getMethod("getOutputDir");
        return (File) getOutputDirMethod.invoke(sourceSet.getJava());
      } catch (NoSuchMethodException | SecurityException | IllegalAccessException
          | IllegalArgumentException | InvocationTargetException e) {
        // ignore
      }
    }

    return null;
  }

  /**
   * Get the source compatibility level of the source set.
   */
  private String getSourceCompatibility(Project project, SourceSet sourceSet) {
    JavaCompile javaCompile = getJavaCompileTask(project, sourceSet);
    if (javaCompile != null) {
      return javaCompile.getSourceCompatibility();
    }

    return "";
  }

  /**
   * Get the target compatibility level of the source set.
   */
  private String getTargetCompatibility(Project project, SourceSet sourceSet) {
    JavaCompile javaCompile = getJavaCompileTask(project, sourceSet);
    if (javaCompile != null) {
      return javaCompile.getTargetCompatibility();
    }

    return "";
  }

  /**
   * Get the compilation arguments of the source set.
   */
  private List<String> getCompilerArgs(Project project, SourceSet sourceSet) {
    JavaCompile javaCompile = getJavaCompileTask(project, sourceSet);
    if (javaCompile != null) {
      return javaCompile.getOptions().getCompilerArgs();
    }

    return Collections.emptyList();
  }

  private Set<String> getClasspathConfigurationNames(SourceSet sourceSet) {
    Set<String> configurationNames = new HashSet<>();
    configurationNames.add(sourceSet.getCompileClasspathConfigurationName());
    configurationNames.add(sourceSet.getRuntimeClasspathConfigurationName());
    return configurationNames;
  }
}
