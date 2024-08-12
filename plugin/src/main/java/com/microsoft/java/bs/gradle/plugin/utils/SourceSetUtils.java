package com.microsoft.java.bs.gradle.plugin.utils;

import com.microsoft.java.bs.gradle.plugin.model.AndroidVariant;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class SourceSetUtils {

  private SourceSetUtils() {
  }

  public static boolean isAndroidProject(Project project) {
    return getProjectExtension(project, "android") != null;
  }

  @SuppressWarnings("unchecked")
  public static List<AndroidVariant> getAndroidBuildVariants(Project project) {

    List<AndroidVariant> androidBuildVariants = new LinkedList<>();

    Object androidExtension = getProjectExtension(project, "android");
    if (androidExtension == null) {
      return androidBuildVariants;
    }

    AndroidProjectType type = getProjectType(project);
    if (type == null) {
      return androidBuildVariants;
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
        AndroidVariant androidVariant = convertToAndroidVariant(project, variant);
        if (androidVariant == null) {
          continue;
        }
        androidBuildVariants.add(androidVariant);
      }
    } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | ClassCastException e) {
      // do nothing
    }

    return androidBuildVariants;

  }

  @SuppressWarnings("unchecked")
  public static AndroidVariant convertToAndroidVariant(Project project, Object variant) {

    try {

      AndroidVariant androidVariant = new AndroidVariant();

      androidVariant.setGradleVersion(project.getGradle().getGradleVersion());
      androidVariant.setProjectName(project.getName());
      androidVariant.setDisplayName(project.getDisplayName());
      androidVariant.setProjectPath(project.getPath());
      androidVariant.setProjectDir(project.getProjectDir());
      androidVariant.setRootDir(project.getRootDir());

      String variantName = (String) variant.getClass().getMethod("getName").invoke(variant);
      androidVariant.setVariantName(variantName);

      // classpath
      Object compileConfig = variant.getClass().getMethod("getCompileConfiguration").invoke(variant);
      Set<File> classpathFiles = (Set<File>) compileConfig.getClass().getMethod("getFiles").invoke(compileConfig);
      androidVariant.setCompileClasspath(classpathFiles);

      // source
      Object sourceSets = getProperty(variant, "sourceSets");
      Set<File> sourceDirs = new HashSet<>();
      if (sourceSets instanceof Iterable) {
        for (Object sourceSet : (Iterable<?>) sourceSets) {
          Set<File> javaDirs = (Set<File>) getProperty(sourceSet, "javaDirectories");
          sourceDirs.addAll(javaDirs);
        }
      }
      androidVariant.setSourceDirs(sourceDirs);

      // generated source
      Set<File> generatedOutputs = new HashSet<>();

      Provider<Task> javaCompileTask = (Provider<Task>) getProperty(variant, "javaCompileProvider");
      if (javaCompileTask != null) {
        generatedOutputs.addAll(javaCompileTask.get().getOutputs().getFiles().getFiles());
      }

      androidVariant.setGeneratedSourceDirs(generatedOutputs);

      // source output

      return androidVariant;

    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      return null;
    }

  }

  public static Object getProjectExtension(Project project, String extensionName) {

    Object extension = null;

    try {
      Object convention = project.getClass().getMethod("getConvention").invoke(project);
      Object extensionMap = convention.getClass().getMethod("getAsMap").invoke(convention);
      extension = extensionMap.getClass().getMethod("get", Object.class).invoke(extensionMap, extensionName);
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      // do nothing
    }

    return extension;

  }

  public static AndroidProjectType getProjectType(Project project) {

    if (getProjectExtension(project, "android") == null) {
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

  public enum AndroidProjectType {
    APPLICATION,
    LIBRARY,
    DYNAMIC_FEATURE,
    INSTANT_APP_FEATURE,
    ANDROID_TEST
  }

}
