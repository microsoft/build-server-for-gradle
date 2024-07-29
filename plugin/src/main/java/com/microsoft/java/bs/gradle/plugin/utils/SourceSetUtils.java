package com.microsoft.java.bs.gradle.plugin.utils;

import com.microsoft.java.bs.gradle.plugin.model.AndroidSourceSet;
import org.gradle.api.Project;
import org.gradle.api.UnknownDomainObjectException;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class SourceSetUtils {

  private SourceSetUtils() {
  }

  public static boolean isAndroidProject(Project project) {

    try {
      project.getExtensions().getByName("android");
      return true;
    } catch (UnknownDomainObjectException e) {
      // do nothing
    }

    return false;

  }

  public static List<AndroidSourceSet> getAndroidSourceSets(Project project) {

    try {
      // TODO: Extensions not available for older gradle versions, use conventions instead?
      Object androidExtension = project.getExtensions().getByName("android");
      Method getSourceSets = androidExtension.getClass().getMethod("getSourceSets");
      List<Object> sourceSets = new ArrayList<>(((Collection<Object>) getSourceSets.invoke(androidExtension)));

      List<AndroidSourceSet> androidSourceSets = new LinkedList<>();
      for (Object sourceSet : sourceSets) {
        AndroidSourceSet androidSourceSet = convertToAndroidSourceSet(sourceSet);
        if (androidSourceSet != null) {
          androidSourceSets.add(androidSourceSet);
        }
      }
      return androidSourceSets;

    } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
      // do nothing
    }

    return new LinkedList<>();

  }

  public static AndroidSourceSet convertToAndroidSourceSet(Object object) {

    try {

      Class<?> clazz = object.getClass();

      String name = (String) clazz.getMethod("getName").invoke(object);
      Set<File> aidlDirs = (Set<File>) clazz.getMethod("getAidlDirectories").invoke(object);
      Set<File> assetsDirs = (Set<File>) clazz.getMethod("getAssetsDirectories").invoke(object);
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

      return new AndroidSourceSet(
          name, aidlDirs, assetsDirs, cppDirs, customDirs, javaDirs, kotlinDirs,
          manifestFile, mlModelsDirs, renderScriptDirs, resDirs, resourceDirs, shaderDirs
      );

    } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
      // do nothing
    }

    return null;

  }

}
