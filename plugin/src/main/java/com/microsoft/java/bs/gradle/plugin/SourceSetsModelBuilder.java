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

import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.copy.DefaultCopySpec;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.testing.Test;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.util.GradleVersion;

import com.microsoft.java.bs.gradle.model.BuildTargetDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.impl.DefaultBuildTargetDependency;
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

  @Override
  public Object buildAll(String modelName, Project rootProject) {
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
        gradleSourceSet.setGradleVersion(project.getGradle().getGradleVersion());
        gradleSourceSet.setProjectName(project.getName());
        gradleSourceSet.setProjectPath(project.getPath());
        gradleSourceSet.setProjectDir(project.getProjectDir());
        gradleSourceSet.setRootDir(project.getRootDir());
        gradleSourceSet.setSourceSetName(sourceSet.getName());
        gradleSourceSet.setClassesTaskName(sourceSet.getClassesTaskName());
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
        List<File> compileClasspath = new LinkedList<>(sourceSet.getCompileClasspath().getFiles());
        gradleSourceSet.setCompileClasspath(compileClasspath);

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

        // tests
        if (sourceOutputDir != null) {
          TaskCollection<Test> testTasks = project.getTasks().withType(Test.class);
          for (Test testTask : testTasks) {
            FileCollection files = testTask.getTestClassesDirs();
            if (files.contains(sourceOutputDir)) {
              gradleSourceSet.setHasTests(true);
              break;
            }
          }
        }
      });
    }

    setSourceSetDependencies(cache);
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

      Map<String, Object> extensions = new HashMap<>();
      for (LanguageModelBuilder languageModelBuilder :
          GradleBuildServerPlugin.SUPPORTED_LANGUAGE_BUILDERS) {

        if (languageModelBuilder.appliesFor(project, sourceSet)) {
          Object extension = languageModelBuilder.getExtensionsFor(project, sourceSet,
              gradleSourceSet.getModuleDependencies());
          if (extension != null) {
            extensions.put(languageModelBuilder.getLanguageId(), extension);
          }
        }
      }
      gradleSourceSet.setExtensions(extensions);

    }

    return new DefaultGradleSourceSets(new LinkedList<>(cache.getAllGradleSourceSets()));
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

  private void setSourceSetDependencies(SourceSetCache cache) {
    // map all output dirs to their source sets
    Map<File, DefaultGradleSourceSet> outputsToSourceSet = new HashMap<>();
    for (DefaultGradleSourceSet sourceSet : cache.getAllGradleSourceSets()) {
      if (sourceSet.getSourceOutputDir() != null) {
        outputsToSourceSet.put(sourceSet.getSourceOutputDir(), sourceSet);
      }
      if (sourceSet.getResourceOutputDir() != null) {
        outputsToSourceSet.put(sourceSet.getResourceOutputDir(), sourceSet);
      }
    }

    // map all output jars to their source sets
    for (Project project : cache.getAllProjects()) {
      SourceSetContainer sourceSets = getSourceSetContainer(project);
      if (sourceSets == null || sourceSets.isEmpty()) {
        continue;
      }

      // get all archive tasks for this project and find the dirs that are included in the archive
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

    // match any classpath entries to other project's output dirs/jars to create dependencies
    for (SourceSet sourceSet : cache.getAllSourceSets()) {
      Set<BuildTargetDependency> dependencies = new HashSet<>();
      for (File file : sourceSet.getCompileClasspath()) {
        DefaultGradleSourceSet otherSourceSet = outputsToSourceSet.get(file);
        if (otherSourceSet != null) {
          dependencies.add(new DefaultBuildTargetDependency(otherSourceSet));
        }
      }
      cache.getGradleSourceSet(sourceSet).setBuildTargetDependencies(dependencies);
    }
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
    }

    return null;
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
    configurationNames.add(sourceSet.getRuntimeClasspathConfigurationName());
    return configurationNames;
  }
}
