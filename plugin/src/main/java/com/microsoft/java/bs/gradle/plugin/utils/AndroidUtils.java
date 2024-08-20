package com.microsoft.java.bs.gradle.plugin.utils;

import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSet;
import com.microsoft.java.bs.gradle.plugin.dependency.AndroidDependencyCollector;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;

/**
 * Utility class for android related operations.
 */
public class AndroidUtils {

  private AndroidUtils() {
  }

  /**
   * Checks if the given project is an Android project.
   *
   * @param project Gradle project to check
   */
  public static boolean isAndroidProject(Project project) {
    return getAndroidExtension(project) != null;
  }

  /**
   * Extracts build variants from the given Android project and converts
   * them into list of GradleSourceSets.
   *
   * @param project Gradle project for extracting the build variants
   */
  @SuppressWarnings("unchecked")
  public static List<GradleSourceSet> getBuildVariantsAsGradleSourceSets(Project project) {

    List<GradleSourceSet> sourceSets = new LinkedList<>();

    Object androidExtension = getAndroidExtension(project);
    if (androidExtension == null) {
      return sourceSets;
    }

    AndroidProjectType type = getProjectType(project);
    if (type == null) {
      return sourceSets;
    }

    String methodName;
    switch (type) {
      case APPLICATION:
      case DYNAMIC_FEATURE:
        methodName = "getApplicationVariants";
        break;
      case LIBRARY:
        methodName = "getLibraryVariants";
        break;
      case INSTANT_APP_FEATURE:
        methodName = "getFeatureVariants";
        break;
      case ANDROID_TEST:
        methodName = "getTestVariants";
        break;
      default:
        methodName = "";
    }

    try {
      Set<Object> variants =
          (Set<Object>) androidExtension.getClass().getMethod(methodName).invoke(androidExtension);
      for (Object variant : variants) {
        GradleSourceSet sourceSet = convertVariantToGradleSourceSet(project, variant);
        if (sourceSet == null) {
          continue;
        }
        sourceSets.add(sourceSet);
      }
    } catch (IllegalAccessException | NoSuchMethodException
             | InvocationTargetException | ClassCastException e) {
      // do nothing
    }

    return sourceSets;

  }

  /**
   * Returns a GradleSourceSet populated with the given Android build variant data.
   *
   * @param project Gradle project to populate GradleSourceSet properties
   * @param variant Android Build Variant object to populate GradleSourceSet properties
   */
  @SuppressWarnings("unchecked")
  private static GradleSourceSet convertVariantToGradleSourceSet(Project project, Object variant) {

    try {

      DefaultGradleSourceSet gradleSourceSet = new DefaultGradleSourceSet();
      gradleSourceSet.setBuildTargetDependencies(new HashSet<>());

      gradleSourceSet.setGradleVersion(project.getGradle().getGradleVersion());
      gradleSourceSet.setProjectName(project.getName());
      String projectPath = project.getPath();
      gradleSourceSet.setProjectPath(projectPath);
      gradleSourceSet.setProjectDir(project.getProjectDir());
      gradleSourceSet.setRootDir(project.getRootDir());

      String variantName = (String) variant.getClass().getMethod("getName").invoke(variant);
      gradleSourceSet.setSourceSetName(variantName);

      // classes task equivalent in android (assembleRelease)
      gradleSourceSet.setClassesTaskName(
          SourceSetUtils.getFullTaskName(projectPath, "assemble" + capitalize(variantName))
      );

      gradleSourceSet.setCleanTaskName(SourceSetUtils.getFullTaskName(projectPath, "clean"));

      // compile task in android (compileReleaseJavaWithJavac)
      HashSet<String> tasks = new HashSet<>();
      tasks.add("compile" + capitalize(variantName) + "JavaWithJavac");
      gradleSourceSet.setTaskNames(tasks);

      String projectName = SourceSetUtils.stripPathPrefix(projectPath);
      if (projectName.isEmpty()) {
        projectName = project.getName();
      }
      String displayName = projectName + " [" + variantName + ']';
      gradleSourceSet.setDisplayName(displayName);

      // TODO: Set Module dependencies
      Set<GradleModuleDependency> moduleDependencies =
          AndroidDependencyCollector.getModuleDependencies(project, variant);
      gradleSourceSet.setModuleDependencies(moduleDependencies);

      // extensions
      gradleSourceSet.setExtensions(new HashMap<>());

      // source and resource
      Object sourceSets = getProperty(variant, "sourceSets");
      Set<File> sourceDirs = new HashSet<>();
      Set<File> resourceDirs = new HashSet<>();
      if (sourceSets instanceof Iterable) {
        for (Object sourceSet : (Iterable<?>) sourceSets) {
          Set<File> javaDirectories =
              (Set<File>) getProperty(sourceSet, "javaDirectories");
          Set<File> resDirectories =
              (Set<File>) getProperty(sourceSet, "resDirectories");
          Set<File> resourceDirectories =
              (Set<File>) getProperty(sourceSet, "resourcesDirectories");
          sourceDirs.addAll(javaDirectories);
          resourceDirs.addAll(resDirectories);
          resourceDirs.addAll(resourceDirectories);
        }
      }
      gradleSourceSet.setSourceDirs(sourceDirs);
      gradleSourceSet.setResourceDirs(resourceDirs);

      // resource outputs
      Set<File> resourceOutputs = new HashSet<>();
      Provider<Task> resourceProvider =
          (Provider<Task>) getProperty(variant, "processJavaResourcesProvider");
      if (resourceProvider != null) {
        Task resTask = resourceProvider.get();
        File outputDir =
            (File) resTask.getClass().getMethod("getDestinationDir").invoke(resTask);
        resourceOutputs.add(outputDir);
      }
      Provider<Task> resProvider =
          (Provider<Task>) getProperty(variant, "mergeResourcesProvider");
      if (resProvider != null) {
        Task resTask = resProvider.get();
        Object outputDir =
            resTask.getClass().getMethod("getOutputDir").invoke(resTask);
        File output =
            ((Provider<File>) outputDir.getClass().getMethod("getAsFile").invoke(outputDir)).get();
        resourceOutputs.add(output);
      }
      gradleSourceSet.setResourceOutputDirs(resourceOutputs);

      // generated sources and source outputs
      Set<File> generatedSources = new HashSet<>();
      Set<File> sourceOutputs = new HashSet<>();
      Provider<Task> javaCompileProvider =
          (Provider<Task>) getProperty(variant, "javaCompileProvider");
      if (javaCompileProvider != null) {
        Task javaCompileTask = javaCompileProvider.get();

        File outputDir = (File) javaCompileTask.getClass().getMethod("getDestinationDir")
            .invoke(javaCompileTask);
        sourceOutputs.add(outputDir);

        Object source = javaCompileTask.getClass().getMethod("getSource").invoke(javaCompileTask);
        Set<File> compileSources =
            (Set<File>) source.getClass().getMethod("getFiles").invoke(source);

        // generated = compile source - source
        for (File compileSource : compileSources) {
          boolean inSourceDir = sourceDirs.stream()
              .anyMatch(dir -> compileSource.getAbsolutePath().startsWith(dir.getAbsolutePath()));
          if (inSourceDir) {
            continue;
          }
          boolean inGeneratedSourceDir = generatedSources.stream()
              .anyMatch(dir -> compileSource.getAbsolutePath().startsWith(dir.getAbsolutePath()));
          if (inGeneratedSourceDir) {
            continue;
          }
          generatedSources.add(compileSource);
        }
      }
      gradleSourceSet.setGeneratedSourceDirs(generatedSources);
      gradleSourceSet.setSourceOutputDirs(sourceOutputs);

      // classpath
      Object compileConfig = variant.getClass()
          .getMethod("getCompileConfiguration").invoke(variant);
      Set<File> classpathFiles = (Set<File>) compileConfig.getClass()
          .getMethod("getFiles").invoke(compileConfig);
      // add R.jar file
      String taskName = "process" + capitalize(variantName) + "Resources";
      Task processResourcesTask = project.getTasks().findByName(taskName);
      if (processResourcesTask != null) {
        Object output = processResourcesTask.getClass()
            .getMethod("getRClassOutputJar").invoke(processResourcesTask);
        RegularFile file = (RegularFile) output.getClass()
            .getMethod("get").invoke(output);
        File jarFile = file.getAsFile();
        if (jarFile.exists()) {
          classpathFiles.add(jarFile);
        }
      }
      gradleSourceSet.setCompileClasspath(new LinkedList<>(classpathFiles));

      // Archive output dirs (not relevant in case of android build variants)
      gradleSourceSet.setArchiveOutputFiles(new HashMap<>());

      // has tests
      Object unitTestVariant = variant.getClass().getMethod("getUnitTestVariant").invoke(variant);
      Object testVariant = variant.getClass().getMethod("getTestVariant").invoke(variant);
      gradleSourceSet.setHasTests(unitTestVariant != null || testVariant != null);

      return gradleSourceSet;

    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      return null;
    }

  }

  /**
   * Extracts the AndroidExtension from the given project.
   *
   * @param project Gradle project to extract the AndroidExtension object.
   */
  private static Object getAndroidExtension(Project project) {

    Object extension = null;

    try {
      Object convention = project.getClass().getMethod("getConvention").invoke(project);
      Object extensionMap = convention.getClass().getMethod("getAsMap").invoke(convention);
      extension = extensionMap.getClass()
          .getMethod("get", Object.class).invoke(extensionMap, "android");
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      // do nothing
    }

    return extension;

  }

  /**
   * Returns the AndroidProjectType based on the plugin applied to the given project.
   *
   * @param project Gradle project to check for plugin and return the corresponding project type.
   */
  private static AndroidProjectType getProjectType(Project project) {

    if (getAndroidExtension(project) == null) {
      return null;
    }

    AndroidProjectType projectType = null;

    if (project.getPluginManager().hasPlugin("com.android.application")) {
      projectType = AndroidProjectType.APPLICATION;
    } else if (project.getPluginManager().hasPlugin("com.android.library")) {
      projectType = AndroidProjectType.LIBRARY;
    } else if (project.getPluginManager().hasPlugin("com.android.dynamic-feature")) {
      projectType = AndroidProjectType.DYNAMIC_FEATURE;
    } else if (project.getPluginManager().hasPlugin("com.android.feature")) {
      projectType = AndroidProjectType.INSTANT_APP_FEATURE;
    } else if (project.getPluginManager().hasPlugin("com.android.test")) {
      projectType = AndroidProjectType.ANDROID_TEST;
    }

    return projectType;

  }

  /**
   * Extracts the given property from the given object with {@code getProperty} method.
   *
   * @param obj object from which the property is to be extracted
   * @param propertyName name of the property to be extracted
   */
  public static Object getProperty(Object obj, String propertyName)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    return obj.getClass().getMethod("getProperty", String.class).invoke(obj, propertyName);
  }

  /**
   * Enum class representing different types of Android projects.
   */
  private enum AndroidProjectType {
    APPLICATION,
    LIBRARY,
    DYNAMIC_FEATURE,
    INSTANT_APP_FEATURE,
    ANDROID_TEST
  }

  /**
   * Returns the given string with its first letter capitalized.
   *
   * @param s String to capitalize
   */
  private static String capitalize(String s) {
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

}
