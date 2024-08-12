package com.microsoft.java.bs.gradle.plugin.utils;

import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSet;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class AndroidUtils {

  private AndroidUtils() {
  }

  public static boolean isAndroidProject(Project project) {
    return getAndroidExtension(project) != null;
  }

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

    String methodName = "";
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
    }

    try {
      Set<Object> variants = (Set<Object>) androidExtension.getClass().getMethod(methodName).invoke(androidExtension);
      for (Object variant : variants) {
        GradleSourceSet sourceSet = convertVariantToGradleSourceSet(project, variant);
        if (sourceSet == null) {
          continue;
        }
        sourceSets.add(sourceSet);
      }
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | ClassCastException e) {
      // do nothing
    }

    return sourceSets;

  }

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

      // TODO: Get classes task equivalent in android build variant

      // TODO: Get clean task equivalent in android build variant

      // TODO: Set task names

      String projectName = stripPathPrefix(projectPath);
      if (projectName.isEmpty()) {
        projectName = project.getName();
      }
      String displayName = projectName + " [" + variantName + ']';
      gradleSourceSet.setDisplayName(displayName);

      // TODO: Set Module dependencies

      // TODO: Extensions, SourceOutputDirs

      // source
      Object sourceSets = getProperty(variant, "sourceSets");
      Set<File> sourceDirs = new HashSet<>();
      if (sourceSets instanceof Iterable) {
        for (Object sourceSet : (Iterable<?>) sourceSets) {
          Set<File> javaDirs = (Set<File>) getProperty(sourceSet, "javaDirectories");
          sourceDirs.addAll(javaDirs);
        }
      }
      gradleSourceSet.setSourceDirs(sourceDirs);

      // generated source TODO: Not complete
      Set<File> generatedOutputs = new HashSet<>();
      Provider<Task> javaCompileTask = (Provider<Task>) getProperty(variant, "javaCompileProvider");
      if (javaCompileTask != null) {
        generatedOutputs.addAll(javaCompileTask.get().getOutputs().getFiles().getFiles());
      }
      gradleSourceSet.setGeneratedSourceDirs(generatedOutputs);

      // classpath
      Object compileConfig = variant.getClass().getMethod("getCompileConfiguration").invoke(variant);
      Set<File> classpathFiles = (Set<File>) compileConfig.getClass().getMethod("getFiles").invoke(compileConfig);
      gradleSourceSet.setCompileClasspath(new LinkedList<>(classpathFiles));

      // resource dirs TODO: Needed?

      // TODO: Set Archive output dirs

      // TODO: Set if has Tests

      return gradleSourceSet;

    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      return null;
    }

  }

  private static Object getAndroidExtension(Project project) {

    Object extension = null;

    try {
      Object convention = project.getClass().getMethod("getConvention").invoke(project);
      Object extensionMap = convention.getClass().getMethod("getAsMap").invoke(convention);
      extension = extensionMap.getClass().getMethod("get", Object.class).invoke(extensionMap, "android");
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      // do nothing
    }

    return extension;

  }

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

  private static Object getProperty(Object obj, String propertyName)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    return obj.getClass().getMethod("getProperty", String.class).invoke(obj, propertyName);
  }

  private enum AndroidProjectType {
    APPLICATION,
    LIBRARY,
    DYNAMIC_FEATURE,
    INSTANT_APP_FEATURE,
    ANDROID_TEST
  }

  private static String stripPathPrefix(String projectPath) {
    if (projectPath.startsWith(":")) {
      return projectPath.substring(1);
    }
    return projectPath;
  }

}
