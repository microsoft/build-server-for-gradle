// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.plugin;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.microsoft.java.bs.gradle.plugin.utils.AndroidUtils;
import com.microsoft.java.bs.gradle.plugin.utils.SourceSetUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.copy.DefaultCopySpec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.testing.Test;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.util.GradleVersion;

import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.LanguageExtension;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSet;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSets;
import com.microsoft.java.bs.gradle.plugin.dependency.DependencyCollector;

/**
 * The model builder for Gradle source sets.
 */
public class SourceSetsModelBuilder implements ToolingModelBuilder {
  @Override
  public boolean canBuild(String modelName) {
    return modelName.equals(GradleSourceSets.class.getName());
  }

  @SuppressWarnings("NullableProblems")
  @Override
  public Object buildAll(String modelName, Project project) {
    // mapping Gradle source set to our customized model.
    List<GradleSourceSet> sourceSets;

    // Fetch source sets depending on the project type
    if (AndroidUtils.isAndroidProject(project)) {
      sourceSets = AndroidUtils.getBuildVariantsAsGradleSourceSets(project);
    } else {
      sourceSets = getSourceSetContainer(project).stream()
          .map(ss -> getSourceSet(project, ss)).collect(Collectors.toList());
    }

    excludeSourceDirsFromModules(sourceSets);

    return new DefaultGradleSourceSets(sourceSets);
  }

  private DefaultGradleSourceSet getSourceSet(Project project, SourceSet sourceSet) {
    DefaultGradleSourceSet gradleSourceSet = new DefaultGradleSourceSet();
    // dependencies are populated by the GradleSourceSetsAction.  Make sure not null.
    gradleSourceSet.setBuildTargetDependencies(new HashSet<>());
    gradleSourceSet.setGradleVersion(project.getGradle().getGradleVersion());
    gradleSourceSet.setProjectName(project.getName());
    String projectPath = project.getPath();
    gradleSourceSet.setProjectPath(projectPath);
    gradleSourceSet.setProjectDir(project.getProjectDir());
    gradleSourceSet.setRootDir(project.getRootDir());
    gradleSourceSet.setSourceSetName(sourceSet.getName());
    String classesTaskName =
        SourceSetUtils.getFullTaskName(projectPath, sourceSet.getClassesTaskName());
    gradleSourceSet.setClassesTaskName(classesTaskName);
    String cleanTaskName = SourceSetUtils.getFullTaskName(projectPath, "clean");
    gradleSourceSet.setCleanTaskName(cleanTaskName);
    Set<String> taskNames = new HashSet<>();
    gradleSourceSet.setTaskNames(taskNames);
    String projectName = SourceSetUtils.stripPathPrefix(projectPath);
    if (projectName.isEmpty()) {
      projectName = project.getName();
    }
    String displayName = projectName + " [" + gradleSourceSet.getSourceSetName() + ']';
    gradleSourceSet.setDisplayName(displayName);

    // setup module dependencies before language support check.
    gradleSourceSet.setModuleDependencies(getModuleDependencies(project, sourceSet));

    // specific languages
    Map<String, LanguageExtension> extensions = new HashMap<>();
    Set<File> srcDirs = new HashSet<>();
    Set<File> generatedSrcDirs = new HashSet<>();
    Set<File> sourceOutputDirs = new HashSet<>();
    for (LanguageModelBuilder languageModelBuilder
        : SourceSetUtils.getSupportedLanguageModelBuilders()) {
      LanguageExtension extension = languageModelBuilder.getExtensionFor(project, sourceSet,
          gradleSourceSet.getModuleDependencies());
      if (extension != null) {
        String compileTaskName =
            SourceSetUtils.getFullTaskName(projectPath, extension.getCompileTaskName());
        taskNames.add(compileTaskName);

        srcDirs.addAll(extension.getSourceDirs());
        generatedSrcDirs.addAll(extension.getGeneratedSourceDirs());
        sourceOutputDirs.add(extension.getClassesDir());

        extensions.put(languageModelBuilder.getLanguageId(), extension);
      }
    }
    gradleSourceSet.setSourceDirs(srcDirs);
    gradleSourceSet.setGeneratedSourceDirs(generatedSrcDirs);
    gradleSourceSet.setExtensions(extensions);
    gradleSourceSet.setSourceOutputDirs(sourceOutputDirs);

    // classpath
    List<File> compileClasspath = new LinkedList<>();
    try {
      compileClasspath.addAll(sourceSet.getCompileClasspath().getFiles());
    } catch (GradleException e) {
      // ignore
    }
    gradleSourceSet.setCompileClasspath(compileClasspath);

    // resource
    Set<File> resourceDirs = sourceSet.getResources().getSrcDirs();
    gradleSourceSet.setResourceDirs(resourceDirs);

    // resource output dir
    File resourceOutputDir = sourceSet.getOutput().getResourcesDir();
    Set<File> resourceOutputDirs = new HashSet<>();
    if (resourceOutputDir != null) {
      resourceOutputDirs.add(resourceOutputDir);
    }
    gradleSourceSet.setResourceOutputDirs(resourceOutputDirs);

    // archive output dirs
    Map<File, List<File>> archiveOutputFiles = getArchiveOutputFiles(project, sourceSet);
    gradleSourceSet.setArchiveOutputFiles(archiveOutputFiles);

    // tests
    if (!sourceOutputDirs.isEmpty()) {
      Set<Test> testTasks = tasksWithType(project, Test.class);
      for (Test testTask : testTasks) {
        if (GradleVersion.current().compareTo(GradleVersion.version("4.0")) >= 0) {
          FileCollection files = testTask.getTestClassesDirs();
          for (File sourceOutputDir : sourceOutputDirs) {
            if (files.contains(sourceOutputDir)) {
              gradleSourceSet.setHasTests(true);
              break;
            }
          }
          if (gradleSourceSet.hasTests()) {
            break;
          }
        } else {
          try {
            Method getTestClassesDir = testTask.getClass().getMethod("getTestClassesDir");
            Object testClassesDir = getTestClassesDir.invoke(testTask);
            for (File sourceOutputDir : sourceOutputDirs) {
              if (sourceOutputDir.equals(testClassesDir)) {
                gradleSourceSet.setHasTests(true);
                break;
              }
            }
            if (gradleSourceSet.hasTests()) {
              break;
            }
          } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                   | IllegalArgumentException | InvocationTargetException e) {
            // ignore
          }
        }
      }
    }

    return gradleSourceSet;
  }

  private <T extends Task> Set<T> tasksWithType(Project project, Class<T> clazz) {
    // Gradle gives concurrentmodification exceptions if multiple threads resolve
    // the tasks concurrently, which happens on multi-project builds
    synchronized (project.getRootProject()) {
      return new HashSet<>(project.getTasks().withType(clazz));
    }
  }

  /**
   * get all archive tasks for this project and maintain the archive file
   * to source set mapping.
   */
  @SuppressWarnings("deprecation")
  private Map<File, List<File>> getArchiveOutputFiles(Project project, SourceSet sourceSet) {
    // get all archive tasks for this project and find the dirs that are included in the archive
    Set<AbstractArchiveTask> archiveTasks = tasksWithType(project, AbstractArchiveTask.class);
    Map<File, List<File>> archiveOutputFiles = new HashMap<>();
    for (AbstractArchiveTask archiveTask : archiveTasks) {
      Set<Object> archiveSourcePaths = getArchiveSourcePaths(archiveTask.getRootSpec());
      for (Object sourcePath : archiveSourcePaths) {
        if (sourceSet.getOutput().equals(sourcePath)) {
          File archiveFile;
          if (GradleVersion.current().compareTo(GradleVersion.version("5.1")) >= 0) {
            archiveFile = archiveTask.getArchiveFile().get().getAsFile();
          } else {
            archiveFile = archiveTask.getArchivePath();
          }
          List<File> sourceSetOutputs = new LinkedList<>(sourceSet.getOutput().getFiles());
          archiveOutputFiles.put(archiveFile, sourceSetOutputs);
        }
      }
    }
    return archiveOutputFiles;
  }

  private Set<GradleModuleDependency> getModuleDependencies(Project project, SourceSet sourceSet) {
    Set<String> configurationNames = getClasspathConfigurationNames(sourceSet);
    return DependencyCollector.getModuleDependencies(project, configurationNames);
  }

  // remove source set dirs from modules
  private void excludeSourceDirsFromModules(List<GradleSourceSet> sourceSets) {
    Set<File> exclusions = new HashSet<>();
    for (GradleSourceSet sourceSet : sourceSets) {
      if (sourceSet.getSourceDirs() != null) {
        exclusions.addAll(sourceSet.getSourceDirs());
      }
      if (sourceSet.getGeneratedSourceDirs() != null) {
        exclusions.addAll(sourceSet.getGeneratedSourceDirs());
      }
      if (sourceSet.getResourceDirs() != null) {
        exclusions.addAll(sourceSet.getResourceDirs());
      }
      if (sourceSet.getSourceOutputDirs() != null) {
        exclusions.addAll(sourceSet.getSourceOutputDirs());
      }
      if (sourceSet.getResourceOutputDirs() != null) {
        exclusions.addAll(sourceSet.getResourceOutputDirs());
      }
      if (sourceSet.getArchiveOutputFiles() != null) {
        exclusions.addAll(sourceSet.getArchiveOutputFiles().keySet());
      }
    }

    Set<URI> exclusionUris = exclusions.stream().map(File::toURI).collect(Collectors.toSet());

    for (GradleSourceSet sourceSet : sourceSets) {
      Set<GradleModuleDependency> filteredModuleDependencies = sourceSet.getModuleDependencies()
          .stream().filter(mod -> mod.getArtifacts()
              .stream().anyMatch(art -> !exclusionUris.contains(art.getUri())))
          .collect(Collectors.toSet());
      if (sourceSet instanceof DefaultGradleSourceSet) {
        ((DefaultGradleSourceSet) sourceSet).setModuleDependencies(filteredModuleDependencies);
      }
    }
  }

  private Collection<SourceSet> getSourceSetContainer(Project project) {
    if (GradleVersion.current().compareTo(GradleVersion.version("5.0")) >= 0) {
      SourceSetContainer sourceSetContainer = project.getExtensions()
          .findByType(SourceSetContainer.class);
      if (sourceSetContainer != null) {
        return sourceSetContainer;
      }
    }
    try {
      // query the java plugin.  This limits support to Java only if other
      // languages add their own sourcesets
      // use reflection because `getConvention` will be removed in Gradle 9.0
      Method getConvention = project.getClass().getMethod("getConvention");
      Object convention = getConvention.invoke(project);
      Method getPlugins = convention.getClass().getMethod("getPlugins");
      Object plugins = getPlugins.invoke(convention);
      Method getGet = plugins.getClass().getMethod("get", Object.class);
      Object pluginConvention = getGet.invoke(plugins, "java");
      if (pluginConvention != null) {
        Method getSourceSetsMethod = pluginConvention.getClass().getMethod("getSourceSets");
        return (SourceSetContainer) getSourceSetsMethod.invoke(pluginConvention);
      }
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException
             | IllegalArgumentException | InvocationTargetException e) {
      // ignore
    }
    return new LinkedList<>();
  }

  private Set<Object> getArchiveSourcePaths(CopySpec copySpec) {
    Set<Object> sourcePaths = new HashSet<>();
    if (copySpec instanceof DefaultCopySpec) {
      DefaultCopySpec defaultCopySpec = (DefaultCopySpec) copySpec;
      sourcePaths.addAll(defaultCopySpec.getSourcePaths());
      // DefaultCopySpec#getChildren changed from Iterable to Collection
      if (GradleVersion.current().compareTo(GradleVersion.version("6.2")) >= 0) {
        for (CopySpec child : defaultCopySpec.getChildren()) {
          sourcePaths.addAll(getArchiveSourcePaths(child));
        }
      } else {
        try {
          Method getChildren = defaultCopySpec.getClass().getMethod("getChildren");
          Object children = getChildren.invoke(defaultCopySpec);
          if (children instanceof Iterable) {
            for (Object child : (Iterable<?>) children) {
              if (child instanceof CopySpec) {
                sourcePaths.addAll(getArchiveSourcePaths((CopySpec) child));
              }
            }
          }
        } catch (NoSuchMethodException | IllegalAccessException
                 | IllegalArgumentException | InvocationTargetException e) {
          // cannot get archive information
        }
      }
    }
    return sourcePaths;
  }

  private Set<String> getClasspathConfigurationNames(SourceSet sourceSet) {
    Set<String> configurationNames = new HashSet<>();
    configurationNames.add(sourceSet.getCompileClasspathConfigurationName());
    if (GradleVersion.current().compareTo(GradleVersion.version("3.4")) >= 0) {
      configurationNames.add(sourceSet.getRuntimeClasspathConfigurationName());
    }
    return configurationNames;
  }
}
