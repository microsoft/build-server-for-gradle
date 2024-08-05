package com.microsoft.java.bs.gradle.plugin.utils;

import com.microsoft.java.bs.gradle.plugin.model.AndroidSourceSet;
import com.microsoft.java.bs.gradle.plugin.model.AndroidVariant;
import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.artifacts.Configuration;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class SourceSetUtils {

  private SourceSetUtils() {
  }

  public static boolean isAndroidProject(Project project) {
    return getProjectExtension(project, "android") != null;
  }

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
      androidBuildVariants.size();
    }

    return androidBuildVariants;

  }

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

      Object compileConfig = variant.getClass().getMethod("getCompileConfiguration").invoke(variant);
      Set<File> classpathFiles = (Set<File>) compileConfig.getClass().getMethod("getFiles").invoke(compileConfig);
      androidVariant.setCompileClasspath(classpathFiles);

      return androidVariant;

    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      return null;
    }

  }

  public static List<AndroidSourceSet> getAndroidSourceSets(Project project) {

    List<AndroidSourceSet> androidSourceSets = new LinkedList<>();

    Object androidExtension = getProjectExtension(project, "android");
    if (androidExtension == null) {
      return androidSourceSets;
    }

    try {

      Method getSourceSets = androidExtension.getClass().getMethod("getSourceSets");
      List<Object> sourceSets = new ArrayList<>(((Collection<Object>) getSourceSets.invoke(androidExtension)));

      for (Object sourceSet : sourceSets) {
        AndroidSourceSet androidSourceSet = convertToAndroidSourceSet(project, sourceSet);
        if (androidSourceSet != null) {
          androidSourceSets.add(androidSourceSet);
        }
      }

    } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException |
             UnknownDomainObjectException e) {
      // do nothing
    }

    return androidSourceSets;

  }

  public static AndroidSourceSet convertToAndroidSourceSet(Project project, Object object) {

    try {

      Class<?> clazz = object.getClass();

      String name = (String) clazz.getMethod("getName").invoke(object);
      Set<File> aidlDirs = (Set<File>) clazz.getMethod("getAidlDirectories").invoke(object);
      Set<File> assetsDirs = (Set<File>) clazz.getMethod("getAssetsDirectories").invoke(object);
      Set<File> cDirs = (Set<File>) clazz.getMethod("getCDirectories").invoke(object);
      Set<File> cppDirs = (Set<File>) clazz.getMethod("getCppDirectories").invoke(object);
      List<File> customDirs = (List<File>) clazz.getMethod("getCustomDirectories").invoke(object);
      Set<File> javaDirs = (Set<File>) clazz.getMethod("getJavaDirectories").invoke(object);
      Set<File> kotlinDirs = (Set<File>) clazz.getMethod("getKotlinDirectories").invoke(object);
      File manifestFile = (File) clazz.getMethod("getManifestFile").invoke(object);
      Set<File> mlModelsDirs = (Set<File>) clazz.getMethod("getMlModelsDirectories").invoke(object);
      Set<File> renderScriptDirs = (Set<File>) clazz.getMethod("getRenderscriptDirectories").invoke(object);
      Set<File> resDirs = (Set<File>) clazz.getMethod("getResDirectories").invoke(object);
      Set<File> resourceDirs = (Set<File>) clazz.getMethod("getResourcesDirectories").invoke(object);
      Set<File> shaderDirs = (Set<File>) clazz.getMethod("getShadersDirectories").invoke(object);

      Set<File> classpath = new HashSet<>();
      String compileConfigName = (String) clazz.getMethod("getCompileConfigurationName").invoke(object);

      Configuration compileConfig = project.getConfigurations().findByName(compileConfigName);
      if (compileConfig != null) {
        classpath.addAll(compileConfig.getFiles());
      }

      return new AndroidSourceSet(
          name, aidlDirs, assetsDirs, cDirs, cppDirs, customDirs, javaDirs, kotlinDirs, manifestFile,
          mlModelsDirs, renderScriptDirs, resDirs, resourceDirs, shaderDirs, classpath
      );

    } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
      // do nothing
    }

    return null;

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

  public enum AndroidProjectType {
    APPLICATION,
    LIBRARY,
    DYNAMIC_FEATURE,
    INSTANT_APP_FEATURE,
    ANDROID_TEST
  }

}
