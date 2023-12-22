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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.api.Project;
import org.gradle.api.file.CopySpec;
import org.gradle.api.file.Directory;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.tasks.compile.DefaultJavaCompileSpec;
import org.gradle.api.internal.tasks.compile.JavaCompilerArgumentsBuilder;
import org.gradle.api.internal.file.copy.DefaultCopySpec;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.testing.Test;
import org.gradle.plugins.ide.internal.tooling.java.DefaultInstalledJdk;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.util.GradleVersion;

import com.microsoft.java.bs.gradle.model.BuildTargetDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.impl.DefaultBuildTargetDependency;
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
        String projectName = stripPathPrefix(gradleSourceSet.getProjectPath());
        if (projectName == null || projectName.length() == 0) {
          projectName = gradleSourceSet.getProjectName();
        }
        String displayName = projectName + " [" + gradleSourceSet.getSourceSetName() + ']';
        gradleSourceSet.setDisplayName(displayName);

        // source
        Set<File> srcDirs = sourceSet.getJava().getSrcDirs();
        gradleSourceSet.setSourceDirs(srcDirs);
        exclusionFromDependencies.addAll(srcDirs);
        Set<File> generatedSrcDirs = new HashSet<>();
        addAnnotationProcessingDir(project, sourceSet, generatedSrcDirs);
        addGeneratedSourceDirs(project, sourceSet, srcDirs, generatedSrcDirs);
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

        // jdk
        gradleSourceSet.setJavaHome(defaultJavaHome);
        gradleSourceSet.setJavaVersion(javaVersion);
        gradleSourceSet.setGradleVersion(gradleVersion);
        gradleSourceSet.setCompilerArgs(getCompilerArgs(project, sourceSet));
        gradleSourceSet.setSourceCompatibility(
            getSourceCompatibility(gradleSourceSet.getCompilerArgs()));
        gradleSourceSet.setTargetCompatibility(
            getTargetCompatibility(gradleSourceSet.getCompilerArgs()));
        gradleSourceSets.add(gradleSourceSet);

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

    // map all output dirs to their source sets
    Map<File, DefaultGradleSourceSet> outputsToSourceSet = new HashMap<>();
    for (DefaultGradleSourceSet sourceSet : sourceSetMap.values()) {
      if (sourceSet.getSourceOutputDir() != null) {
        outputsToSourceSet.put(sourceSet.getSourceOutputDir(), sourceSet);
      }
      if (sourceSet.getResourceOutputDir() != null) {
        outputsToSourceSet.put(sourceSet.getResourceOutputDir(), sourceSet);
      }
    }
    // map all output jars to their source sets
    for (Project project : allProject) {

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
            DefaultGradleSourceSet gradleSourceSet = sourceSetMap.get(sourceSet);
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
    for (DefaultGradleSourceSet sourceSet : sourceSetMap.values()) {
      Set<BuildTargetDependency> dependencies = new HashSet<>();
      for (File file : sourceSet.getCompileClasspath()) {
        DefaultGradleSourceSet otherSourceSet = outputsToSourceSet.get(file);
        if (otherSourceSet != null) {
          dependencies.add(new DefaultBuildTargetDependency(otherSourceSet));
        }
      }
      sourceSet.setBuildTargetDependencies(dependencies);
    }

    for (Project project : allProject) {
      SourceSetContainer sourceSets = getSourceSetContainer(project);
      if (sourceSets == null || sourceSets.isEmpty()) {
        continue;
      }

      // module dependencies
      sourceSets.forEach(sourceSet -> {
        DefaultGradleSourceSet gradleSourceSet = sourceSetMap.get(sourceSet);
        if (gradleSourceSet == null) {
          return;
        }
        DependencyCollector collector = new DependencyCollector(project,
            exclusionFromDependencies);
        collector.collectByConfigurationNames(getClasspathConfigurationNames(sourceSet));
        gradleSourceSet.setModuleDependencies(collector.getModuleDependencies());
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

  private Optional<String> findCompilerArg(List<String> compilerArgs, String arg) {
    int idx = compilerArgs.indexOf(arg);
    if (idx >= 0 && idx < compilerArgs.size() - 1) {
      return Optional.of(compilerArgs.get(idx + 1));
    }
    return Optional.empty();
  }

  private Optional<String> findFirstCompilerArgMatch(List<String> compilerArgs,
      Stream<String> args) {
    return args.map(arg -> findCompilerArg(compilerArgs, arg))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .findFirst();
  }

  /**
   * Get the source compatibility level of the source set.
   */
  private String getSourceCompatibility(List<String> compilerArgs) {
    return findFirstCompilerArgMatch(compilerArgs,
      Stream.of("-source", "--source", "--release"))
      .orElse("");
  }

  /**
   * Get the target compatibility level of the source set.
   */
  private String getTargetCompatibility(List<String> compilerArgs) {
    return findFirstCompilerArgMatch(compilerArgs,
      Stream.of("-target", "--target", "--release"))
      .orElse("");
  }

  private DefaultJavaCompileSpec getJavaCompileSpec(JavaCompile javaCompile) {
    CompileOptions options = javaCompile.getOptions();
    
    DefaultJavaCompileSpec specs = new DefaultJavaCompileSpec();
    specs.setCompileOptions(options);

    // check the project hasn't already got the target or source defined in the
    // compiler args so they're not overwritten below
    List<String> originalArgs = options.getCompilerArgs();
    String argsSourceCompatibility = getSourceCompatibility(originalArgs);
    String argsTargetCompatibility = getTargetCompatibility(originalArgs);

    if (!argsSourceCompatibility.isEmpty() && !argsTargetCompatibility.isEmpty()) {
      return specs;
    }

    if (GradleVersion.current().compareTo(GradleVersion.version("6.6")) >= 0) {
      if (options.getRelease().isPresent()) {
        specs.setRelease(options.getRelease().get());
        return specs;
      }
    }
    if (argsSourceCompatibility.isEmpty() && specs.getSourceCompatibility() == null) {
      String sourceCompatibility = javaCompile.getSourceCompatibility();
      if (sourceCompatibility != null) {
        specs.setSourceCompatibility(sourceCompatibility);
      }
    }
    if (argsTargetCompatibility.isEmpty() && specs.getTargetCompatibility() == null) {
      String targetCompatibility = javaCompile.getTargetCompatibility();
      if (targetCompatibility != null) {
        specs.setTargetCompatibility(targetCompatibility);
      }
    }
    return specs;
  }

  /**
   * Get the compilation arguments of the source set.
   */
  private List<String> getCompilerArgs(Project project, SourceSet sourceSet) {
    JavaCompile javaCompile = getJavaCompileTask(project, sourceSet);
    if (javaCompile != null) {
      CompileOptions options = javaCompile.getOptions();

      try {
        DefaultJavaCompileSpec specs = getJavaCompileSpec(javaCompile);

        JavaCompilerArgumentsBuilder builder = new JavaCompilerArgumentsBuilder(specs)
                .includeMainOptions(true)
                .includeClasspath(false)
                .includeSourceFiles(false)
                .includeLauncherOptions(false);
        return builder.build();
      } catch (Exception e) {
        // DefaultJavaCompileSpec and JavaCompilerArgumentsBuilder are internal so may not exist.
        // Fallback to returning just the compiler arguments the build has specified.
        // This will miss a lot of arguments derived from the CompileOptions e.g. sourceCompatibilty
        // Arguments must be cast and converted to String because Groovy can use GStringImpl
        // which then throws IllegalArgumentException when passed back over the tooling connection.
        List<Object> compilerArgs = new LinkedList<>(options.getCompilerArgs());
        return compilerArgs
            .stream()
            .map(Object::toString)
            .collect(Collectors.toList());
      }
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
