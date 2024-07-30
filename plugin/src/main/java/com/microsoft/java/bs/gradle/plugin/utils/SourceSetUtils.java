package com.microsoft.java.bs.gradle.plugin.utils;

import com.microsoft.java.bs.gradle.plugin.model.AndroidSourceSet;
import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.util.GradleVersion;

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
        AndroidSourceSet androidSourceSet = convertToAndroidSourceSet(sourceSet, project);
        if (androidSourceSet != null) {
          androidSourceSets.add(androidSourceSet);
        }
      }

    } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException | UnknownDomainObjectException e) {
      // do nothing
    }

    return androidSourceSets;

  }

  public static AndroidSourceSet convertToAndroidSourceSet(Object object, Project project) {

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
      if (GradleVersion.current().compareTo(GradleVersion.version("5.0")) >= 0) {
        // Extension is supported
        extension = project.getExtensions().findByName(extensionName);
      } else {
        // Fallback to Convention
        Object convention = project.getClass().getMethod("getConvention").invoke(project);
        extension = convention.getClass().getMethod("getByName").invoke(convention, extensionName);
      }
    } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
      // do nothing
    }

    return extension;

  }

}
