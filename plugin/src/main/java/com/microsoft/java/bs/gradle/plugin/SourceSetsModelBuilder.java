// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.plugin;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSetsMetadata;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.copy.DefaultCopySpec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceSetOutput;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.testing.Test;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.util.GradleVersion;

import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSet;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSetsMetadata;
import com.microsoft.java.bs.gradle.model.LanguageExtension;
import com.microsoft.java.bs.gradle.plugin.dependency.DependencyCollector;

/**
 * The model builder for Gradle source sets.
 */
public class SourceSetsModelBuilder implements ToolingModelBuilder {
  @Override
  public boolean canBuild(String modelName) {
    return modelName.equals(GradleSourceSetsMetadata.class.getName());
  }

  @Override
  public Object buildAll(String modelName, Project rootProject) {

    Map<GradleSourceSet, List<File>> sourceSetsToClasspath = new HashMap<>();
    Map<File, GradleSourceSet> outputsToSourceSet = new HashMap<>();

    Set<Project> allProject = rootProject.getAllprojects();
    SourceSetCache cache = new SourceSetCache();
    // this set is used to eliminate the source, resource and output
    // directories from the module dependencies.
    Set<File> exclusionFromDependencies = new HashSet<>();
    // mapping Gradle source set to our customized model.
    for (Project project : allProject) {
      SourceSetContainer sourceSets = getSourceSetContainer(project);
      if (sourceSets == null || sourceSets.isEmpty()) {
        continue;
      }
      sourceSets.forEach(sourceSet -> {
        DefaultGradleSourceSet gradleSourceSet = new DefaultGradleSourceSet();
        cache.addGradleSourceSet(sourceSet, gradleSourceSet);
        cache.addProject(sourceSet, project);
        gradleSourceSet.setBuildTargetDependencies(new HashSet<>());
        gradleSourceSet.setGradleVersion(project.getGradle().getGradleVersion());
        gradleSourceSet.setProjectName(project.getName());
        String projectPath = project.getPath();
        gradleSourceSet.setProjectPath(projectPath);
        gradleSourceSet.setProjectDir(project.getProjectDir());
        gradleSourceSet.setRootDir(project.getRootDir());
        gradleSourceSet.setSourceSetName(sourceSet.getName());
        String classesTaskName = getFullTaskName(projectPath, sourceSet.getClassesTaskName());
        gradleSourceSet.setClassesTaskName(classesTaskName);
        String cleanTaskName = getFullTaskName(projectPath, "clean");
        gradleSourceSet.setCleanTaskName(cleanTaskName);
        Set<String> taskNames = new HashSet<>();
        gradleSourceSet.setTaskNames(taskNames);
        String projectName = stripPathPrefix(gradleSourceSet.getProjectPath());
        if (projectName == null || projectName.length() == 0) {
          projectName = gradleSourceSet.getProjectName();
        }
        String displayName = projectName + " [" + gradleSourceSet.getSourceSetName() + ']';
        gradleSourceSet.setDisplayName(displayName);

        // source
        Set<File> srcDirs = new HashSet<>();
        Set<File> generatedSrcDirs = new HashSet<>();
        for (LanguageModelBuilder languageModelBuilder :
            GradleBuildServerPlugin.SUPPORTED_LANGUAGE_BUILDERS) {
          if (languageModelBuilder.appliesFor(project, sourceSet)) {
            Task compileTask = languageModelBuilder.getLanguageCompileTask(project, sourceSet);
            if (compileTask != null) {
              String compileTaskName = getFullTaskName(projectPath, compileTask.getName());
              taskNames.add(compileTaskName);
            }
            srcDirs.addAll(languageModelBuilder.getSourceFoldersFor(project, sourceSet));
            generatedSrcDirs.addAll(
                languageModelBuilder.getGeneratedSourceFoldersFor(project, sourceSet));
          }
        }
        gradleSourceSet.setSourceDirs(srcDirs);
        exclusionFromDependencies.addAll(srcDirs);
        gradleSourceSet.setGeneratedSourceDirs(generatedSrcDirs);
        exclusionFromDependencies.addAll(generatedSrcDirs);

        // classpath
        List<File> compileClasspath = new LinkedList<>();
        try {
          compileClasspath.addAll(sourceSet.getCompileClasspath().getFiles());
        } catch (GradleException e) {
          // ignore
        }
        gradleSourceSet.setCompileClasspath(compileClasspath);
        sourceSetsToClasspath.put(gradleSourceSet, compileClasspath);

        // source output dir
        File sourceOutputDir = getSourceOutputDir(sourceSet);
        if (sourceOutputDir != null) {
          gradleSourceSet.setSourceOutputDir(sourceOutputDir);
          outputsToSourceSet.put(sourceOutputDir, gradleSourceSet);
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
          outputsToSourceSet.put(resourceOutputDir, gradleSourceSet);
          exclusionFromDependencies.add(resourceOutputDir);
        }

        // tests
        if (sourceOutputDir != null) {
          TaskCollection<Test> testTasks = project.getTasks().withType(Test.class);
          for (Test testTask : testTasks) {
            if (GradleVersion.current().compareTo(GradleVersion.version("4.0")) >= 0) {
              FileCollection files = testTask.getTestClassesDirs();
              if (files.contains(sourceOutputDir)) {
                gradleSourceSet.setHasTests(true);
                break;
              }
            } else {
              try {
                Method getTestClassesDir = testTask.getClass().getMethod("getTestClassesDir");
                Object testClassesDir = getTestClassesDir.invoke(testTask);
                if (sourceOutputDir.equals(testClassesDir)) {
                  gradleSourceSet.setHasTests(true);
                  break;
                }
              } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                       | IllegalArgumentException | InvocationTargetException  e) {
                // ignore
              }
            }
          }
        }
      });

      if (!sourceSets.isEmpty()) {
        gatherArchiveTasks(outputsToSourceSet, cache, project, sourceSets);
      }
    }

    setModuleDependencies(cache, exclusionFromDependencies);

    for (SourceSet sourceSet : cache.getAllSourceSets()) {
      DefaultGradleSourceSet gradleSourceSet = cache.getGradleSourceSet(sourceSet);
      if (gradleSourceSet == null) {
        continue;
      }

      Project project = cache.getProject(sourceSet);
      if (project == null) {
        continue;
      }

      Map<String, LanguageExtension> extensions = new HashMap<>();
      for (LanguageModelBuilder languageModelBuilder :
          GradleBuildServerPlugin.SUPPORTED_LANGUAGE_BUILDERS) {
        if (languageModelBuilder.appliesFor(project, sourceSet)) {
          LanguageExtension extension = languageModelBuilder.getExtensionsFor(project, sourceSet,
              gradleSourceSet.getModuleDependencies());
          if (extension != null) {
            extensions.put(languageModelBuilder.getLanguageId(), extension);
          }
        }
      }
      gradleSourceSet.setExtensions(extensions);
    }

    return new DefaultGradleSourceSetsMetadata(sourceSetsToClasspath, outputsToSourceSet);
  }

  private void setModuleDependencies(SourceSetCache cache, Set<File> exclusionFromDependencies) {
    for (SourceSet sourceSet : cache.getAllSourceSets()) {
      DefaultGradleSourceSet gradleSourceSet = cache.getGradleSourceSet(sourceSet);
      if (gradleSourceSet == null) {
        continue;
      }
      DependencyCollector collector = new DependencyCollector(cache.getProject(sourceSet),
          exclusionFromDependencies);
      collector.collectByConfigurationNames(getClasspathConfigurationNames(sourceSet));
      gradleSourceSet.setModuleDependencies(collector.getModuleDependencies());
    }
  }

  private SourceSetContainer getSourceSetContainer(Project project) {
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
             | IllegalArgumentException | InvocationTargetException  e) {
      // ignore
    }
    return null;
  }

  /**
   * Return a project task name - [project path]:[task].
   */
  private String getFullTaskName(String modulePath, String taskName) {
    if (taskName == null) {
      return null;
    }
    if (taskName.isEmpty()) {
      return taskName;
    }

    if (modulePath == null || modulePath.equals(":")) {
      // must be prefixed with ":" as taskPaths are reported back like that in progress messages
      return ":" + taskName;
    }
    return modulePath + ":" + taskName;
  }

  private String stripPathPrefix(String projectPath) {
    if (projectPath.startsWith(":")) {
      return projectPath.substring(1);
    }
    return projectPath;
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
    } else {
      // get all output dirs and filter out resources output
      SourceSetOutput output = sourceSet.getOutput();
      Set<File> allOutputDirs = output.getFiles();
      File resourceOutputDir = output.getResourcesDir();
      allOutputDirs.remove(resourceOutputDir);
      if (!allOutputDirs.isEmpty()) {
        return allOutputDirs.iterator().next();
      }
    }

    return null;
  }

  /**
   * get all archive tasks for this project and maintain the archive file
   * to source set mapping.
   */
  private void gatherArchiveTasks(Map<File, GradleSourceSet> outputsToSourceSet,
      SourceSetCache cache, Project project, SourceSetContainer sourceSets) {
    TaskCollection<AbstractArchiveTask> archiveTasks =
        project.getTasks().withType(AbstractArchiveTask.class);
    for (AbstractArchiveTask archiveTask : archiveTasks) {
      Set<Object> archiveSourcePaths = getArchiveSourcePaths(archiveTask.getRootSpec());
      for (Object sourcePath : archiveSourcePaths) {
        sourceSets.forEach(sourceSet -> {
          DefaultGradleSourceSet gradleSourceSet = cache.getGradleSourceSet(sourceSet);
          if (gradleSourceSet == null) {
            return;
          }

          if (sourceSet.getOutput().equals(sourcePath)) {
            File archiveFile;
            if (GradleVersion.current().compareTo(GradleVersion.version("5.1")) >= 0) {
              archiveFile = archiveTask.getArchiveFile().get().getAsFile();
            } else {
              archiveFile = archiveTask.getArchivePath();
            }
            outputsToSourceSet.put(archiveFile, gradleSourceSet);
          }
        });
      }
    }
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
