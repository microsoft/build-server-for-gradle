package com.microsoft.java.bs.gradle.plugin.utils;

import com.microsoft.java.bs.gradle.model.Artifact;
import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.LanguageExtension;
import com.microsoft.java.bs.gradle.model.SupportedLanguages;
import com.microsoft.java.bs.gradle.model.impl.DefaultJavaExtension;
import com.microsoft.java.bs.gradle.model.impl.DefaultArtifact;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleModuleDependency;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSet;
import com.microsoft.java.bs.gradle.plugin.dependency.AndroidDependencyCollector;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFile;
import org.gradle.api.internal.tasks.compile.DefaultJavaCompileSpec;
import org.gradle.api.internal.tasks.compile.JavaCompilerArgumentsBuilder;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.util.GradleVersion;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
  public static List<GradleSourceSet> getBuildVariantsAsGradleSourceSets(Project project) {

    List<GradleSourceSet> sourceSets = new LinkedList<>();

    Object androidExtension = getAndroidExtension(project);
    if (androidExtension == null) {
      return sourceSets;
    }

    AndroidProjectType androidProjectType = getProjectType(project);
    if (androidProjectType == null) {
      return sourceSets;
    }

    List<Object> variants = new LinkedList<>();
    switch (androidProjectType) {
      case APPLICATION:
      case DYNAMIC_FEATURE:
        variants = getVariants(androidExtension, "getApplicationVariants", "getTestVariants");
        break;
      case LIBRARY:
        variants = getVariants(androidExtension, "getLibraryVariants", "getTestVariants");
        break;
      case INSTANT_APP_FEATURE:
        variants = getVariants(androidExtension, "getFeatureVariants", "getTestVariants");
        break;
      case ANDROID_TEST:
        variants = getVariants(androidExtension, "getTestVariants");
        break;
      default:
    }

    for (Object variant : variants) {
      GradleSourceSet sourceSet = convertVariantToGradleSourceSet(project, variant, false);
      if (sourceSet != null) {
        sourceSets.add(sourceSet);
      }
    }

    if (androidProjectType != AndroidProjectType.ANDROID_TEST) {
      for (Object variant : getVariants(androidExtension, "getUnitTestVariants")) {
        GradleSourceSet sourceSet = convertVariantToGradleSourceSet(project, variant, true);
        if (sourceSet != null) {
          sourceSets.add(sourceSet);
        }
      }
    }

    return sourceSets;

  }

  /**
   * Returns a list of variants extracted with the listed method names from the given
   * android extension.
   *
   * @param androidExtension AndroidExtension object from which the variants are to be extracted.
   * @param methodNames name of different methods to invoke to get all the variants.
   */
  @SuppressWarnings("unchecked")
  private static List<Object> getVariants(Object androidExtension, String... methodNames) {
    List<Object> variants = new LinkedList<>();
    for (String methodName : methodNames) {
      try {
        variants.addAll((Collection<Object>) invokeMethod(androidExtension, methodName));
      } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
        // do nothing
      }
    }
    return variants;
  }

  /**
   * Returns a GradleSourceSet which has been populated with respective
   * Android build variant data.
   *
   * @param project Gradle project to populate GradleSourceSet properties
   * @param variant Android Build Variant object to populate GradleSourceSet properties
   * @param isUnitTest Indicates if the given variant is a unit test variant
   */
  private static GradleSourceSet convertVariantToGradleSourceSet(
      Project project,
      Object variant,
      boolean isUnitTest
  ) {

    try {

      DefaultGradleSourceSet gradleSourceSet = new DefaultGradleSourceSet();
      gradleSourceSet.setBuildTargetDependencies(new HashSet<>());

      gradleSourceSet.setGradleVersion(project.getGradle().getGradleVersion());
      gradleSourceSet.setProjectName(project.getName());
      String projectPath = project.getPath();
      gradleSourceSet.setProjectPath(projectPath);
      gradleSourceSet.setProjectDir(project.getProjectDir());
      gradleSourceSet.setRootDir(project.getRootDir());

      String variantName = (String) invokeMethod(variant, "getName");
      gradleSourceSet.setSourceSetName(variantName);

      // classes task equivalent in android (assembleRelease)
      gradleSourceSet.setClassesTaskName(
          SourceSetUtils.getFullTaskName(projectPath, "assemble" + capitalize(variantName))
      );

      gradleSourceSet.setCleanTaskName(SourceSetUtils.getFullTaskName(projectPath, "clean"));

      // compile task in android (compileReleaseJavaWithJavac)
      HashSet<String> tasks = new HashSet<>();
      String compileTaskName = "compile" + capitalize(variantName) + "JavaWithJavac";
      tasks.add(SourceSetUtils.getFullTaskName(projectPath, compileTaskName));
      gradleSourceSet.setTaskNames(tasks);

      String projectName = SourceSetUtils.stripPathPrefix(projectPath);
      if (projectName.isEmpty()) {
        projectName = project.getName();
      }
      String displayName = projectName + " [" + variantName + ']';
      gradleSourceSet.setDisplayName(displayName);

      // module dependencies
      addModuleDependencies(gradleSourceSet, project, variant);

      // source and resource
      addSourceAndResources(gradleSourceSet, variant, isUnitTest);

      // resource outputs
      addResourceOutputs(gradleSourceSet, variant, isUnitTest);

      List<String> compilerArgs = new ArrayList<>();

      // generated sources and source outputs
      addGeneratedSourceAndSourceOutputs(gradleSourceSet, variant, compilerArgs);

      // classpath
      addClasspath(gradleSourceSet, variant);

      // Archive output dirs (not relevant in case of android build variants)
      gradleSourceSet.setArchiveOutputFiles(new HashMap<>());

      // has tests
      gradleSourceSet.setHasTests((boolean) hasProperty(variant, "testedVariant"));

      // extensions
      addExtensions(gradleSourceSet, compilerArgs);

      return gradleSourceSet;

    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
      // do nothing
    }

    return null;

  }

  /**
   * Add module dependencies to the given GradleSourceSet.
   *
   * @param gradleSourceSet Instance of DefaultGradleSourceSet
   * @param project Instance of Project
   * @param variant Instance of Build Variant
   */
  @SuppressWarnings("unchecked")
  private static void addModuleDependencies(
      DefaultGradleSourceSet gradleSourceSet,
      Project project,
      Object variant
  ) {

    Set<GradleModuleDependency> moduleDependencies =
        AndroidDependencyCollector.getModuleDependencies(project, variant);

    try {
      // add Android SDK
      Object androidComponents = getAndroidComponentExtension(project);
      if (androidComponents != null) {
        Object sdkComponents = getProperty(androidComponents, "sdkComponents");
        Object bootClasspath =
            ((Provider<?>) getProperty(sdkComponents, "bootclasspathProvider")).get();
        try {
          List<RegularFile> bootClasspathFiles =
              (List<RegularFile>) invokeMethod(bootClasspath, "get");
          List<File> sdkClasspath =
              bootClasspathFiles.stream().map(RegularFile::getAsFile).collect(Collectors.toList());
          for (File file : sdkClasspath) {
            moduleDependencies.add(mockModuleDependency(file.toURI()));
          }
        } catch (IllegalStateException | InvocationTargetException e) {
          // failed to retrieve android sdk classpath
          // do nothing
        }
      }
      // add R.jar file
      String taskName = "process" + capitalize(gradleSourceSet.getSourceSetName()) + "Resources";
      Task processResourcesTask = project.getTasks().findByName(taskName);
      if (processResourcesTask != null) {
        Object output = invokeMethod(processResourcesTask, "getRClassOutputJar");
        RegularFile file = (RegularFile) invokeMethod(output, "get");
        File jarFile = file.getAsFile();
        if (jarFile.exists()) {
          moduleDependencies.add(mockModuleDependency(jarFile.toURI()));
        }
      }
    } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
      // do nothing
    }

    gradleSourceSet.setModuleDependencies(moduleDependencies);

  }

  /**
   * Add source and resource directories to the given GradleSourceSet.
   *
   * @param gradleSourceSet Instance of DefaultGradleSourceSet
   * @param variant Instance of Build Variant
   * @param isUnitTest Indicates if the given variant is a unit test variant
   */
  @SuppressWarnings("unchecked")
  private static void addSourceAndResources(
      DefaultGradleSourceSet gradleSourceSet,
      Object variant,
      boolean isUnitTest
  ) {

    Set<File> sourceDirs = new HashSet<>();
    Set<File> resourceDirs = new HashSet<>();

    try {
      Object sourceSets = getProperty(variant, "sourceSets");
      if (sourceSets instanceof Iterable) {
        for (Object sourceSet : (Iterable<?>) sourceSets) {
          Set<File> javaDirectories =
              (Set<File>) getProperty(sourceSet, "javaDirectories");
          sourceDirs.addAll(javaDirectories);
          if (!isUnitTest) {
            resourceDirs.addAll((Set<File>) getProperty(sourceSet, "resDirectories"));
          }
          resourceDirs.addAll((Set<File>) getProperty(sourceSet, "resourcesDirectories"));
        }
      }
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      // do nothing
    }

    gradleSourceSet.setSourceDirs(sourceDirs);
    gradleSourceSet.setResourceDirs(resourceDirs);

  }

  /**
   * Add resource output directories to the given GradleSourceSet.
   *
   * @param gradleSourceSet Instance of DefaultGradleSourceSet
   * @param variant Instance of Build Variant
   * @param isUnitTest Indicates if the given variant is a unit test variant
   */
  @SuppressWarnings("unchecked")
  private static void addResourceOutputs(
      DefaultGradleSourceSet gradleSourceSet,
      Object variant,
      boolean isUnitTest
  ) {

    Set<File> resourceOutputs = new HashSet<>();

    try {
      Provider<Task> resourceProvider =
          (Provider<Task>) getProperty(variant, "processJavaResourcesProvider");
      if (resourceProvider != null) {
        Task resTask = resourceProvider.get();
        File outputDir = (File) invokeMethod(resTask, "getDestinationDir");
        resourceOutputs.add(outputDir);
      }

      if (!isUnitTest) {
        Provider<Task> resProvider =
            (Provider<Task>) getProperty(variant, "mergeResourcesProvider");
        if (resProvider != null) {
          Task resTask = resProvider.get();
          Object outputDir = invokeMethod(resTask, "getOutputDir");
          File output = ((Provider<File>) invokeMethod(outputDir, "getAsFile")).get();
          resourceOutputs.add(output);
        }
      }
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      // do nothing
    }

    gradleSourceSet.setResourceOutputDirs(resourceOutputs);

  }

  /**
   * Add source output and generated source output directories to the given GradleSourceSet.
   *
   * @param gradleSourceSet Instance of DefaultGradleSourceSet
   * @param variant Instance of Build Variant
   * @param compilerArgs List to be populated from the java compiler arguments.
   */
  @SuppressWarnings("unchecked")
  private static void addGeneratedSourceAndSourceOutputs(
      DefaultGradleSourceSet gradleSourceSet,
      Object variant,
      List<String> compilerArgs
  ) {

    Set<File> generatedSources = new HashSet<>();
    Set<File> sourceOutputs = new HashSet<>();

    try {
      Provider<Task> javaCompileProvider =
          (Provider<Task>) getProperty(variant, "javaCompileProvider");
      if (javaCompileProvider != null) {
        Task javaCompileTask = javaCompileProvider.get();

        compilerArgs.addAll(getCompilerArgs((JavaCompile) javaCompileTask));

        File outputDir = (File) invokeMethod(javaCompileTask, "getDestinationDir");
        sourceOutputs.add(outputDir);

        Object source = invokeMethod(javaCompileTask, "getSource");
        Set<File> compileSources = (Set<File>) invokeMethod(source, "getFiles");

        // generated = compile source - source
        for (File compileSource : compileSources) {
          boolean inSourceDir = gradleSourceSet.getSourceDirs().stream()
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
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      // do nothing
    }

    gradleSourceSet.setGeneratedSourceDirs(generatedSources);
    gradleSourceSet.setSourceOutputDirs(sourceOutputs);

  }

  /**
   * Add classpath files to the given GradleSourceSet.
   *
   * @param gradleSourceSet Instance of DefaultGradleSourceSet
   * @param variant Instance of Build Variant
   */
  @SuppressWarnings("unchecked")
  private static void addClasspath(DefaultGradleSourceSet gradleSourceSet, Object variant) {

    Set<File> classpathFiles = new HashSet<>();

    try {
      Object compileConfig = invokeMethod(variant, "getCompileConfiguration");
      classpathFiles.addAll((Set<File>) invokeMethod(compileConfig, "getFiles"));
    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      // do nothing
    }

    gradleSourceSet.setCompileClasspath(new LinkedList<>(classpathFiles));

  }

  /**
   * Add language extension to the given GradleSourceSet.
   *
   * @param gradleSourceSet Instance of DefaultGradleSourceSet
   * @param compilerArgs List of compiler arguments needed to build the language extension.
   */
  private static void addExtensions(
      DefaultGradleSourceSet gradleSourceSet,
      List<String> compilerArgs
  ) {
    Map<String, LanguageExtension> extensions = new HashMap<>();
    boolean isJavaSupported = Arrays.stream(SourceSetUtils.getSupportedLanguages())
        .anyMatch(l -> Objects.equals(l, SupportedLanguages.JAVA.getBspName()));
    if (isJavaSupported) {
      DefaultJavaExtension extension = new DefaultJavaExtension();

      extension.setCompilerArgs(compilerArgs);
      extension.setSourceCompatibility(getSourceCompatibility(compilerArgs));
      extension.setTargetCompatibility(getTargetCompatibility(compilerArgs));

      extensions.put(SupportedLanguages.JAVA.getBspName(), extension);
    }
    gradleSourceSet.setExtensions(extensions);
  }

  /**
   * Extracts the AndroidExtension from the given project.
   *
   * @param project Gradle project to extract the AndroidExtension object.
   */
  private static Object getAndroidExtension(Project project) {
    return getExtension(project, "android");
  }

  /**
   * Extracts the AndroidComponentsExtension from the given project.
   *
   * @param project Gradle project to extract the AndroidComponentsExtension object.
   */
  private static Object getAndroidComponentExtension(Project project) {
    return getExtension(project, "androidComponents");
  }

  /**
   * Extracts the given extension from the given project.
   *
   * @param project Gradle project to extract the extension object.
   * @param extensionName Name of the extension to extract.
   */
  private static Object getExtension(Project project, String extensionName) {
    Object extension = null;

    try {
      Object convention = invokeMethod(project, "getConvention");
      Object extensionMap = invokeMethod(convention, "getAsMap");
      extension = extensionMap.getClass()
          .getMethod("get", Object.class).invoke(extensionMap, extensionName);
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
   * Checks if the given property exists in the given object with {@code hasProperty} method.
   *
   * @param obj object from which the property is to be extracted
   * @param propertyName name of the property to be extracted
   */
  public static Object hasProperty(Object obj, String propertyName)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    return obj.getClass().getMethod("hasProperty", String.class).invoke(obj, propertyName);
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

  /**
   * Mocks GradleModuleDependency with a single artifact.
   *
   * @param jarUri Uri for the artifact to include in the ModuleDependency object.
   */
  private static GradleModuleDependency mockModuleDependency(URI jarUri) {

    final String unknown = "UNKNOWN";

    List<Artifact> artifacts = new LinkedList<>();
    artifacts.add(new DefaultArtifact(jarUri, null));

    return new DefaultGradleModuleDependency(
        unknown,
        unknown,
        unknown,
        artifacts
    );

  }

  private static Object invokeMethod(Object object, String methodName)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    return object.getClass().getMethod(methodName).invoke(object);
  }

  // region TODO: Duplicate code from JavaLanguageModelBuilder

  /**
   * Get the compilation arguments of the build variant.
   */
  private static List<String> getCompilerArgs(JavaCompile javaCompile) {
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

  private static DefaultJavaCompileSpec getJavaCompileSpec(JavaCompile javaCompile) {
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
   * Get the source compatibility level of the build variant.
   */
  private static String getSourceCompatibility(List<String> compilerArgs) {
    return findFirstCompilerArgMatch(compilerArgs,
        Stream.of("-source", "--source", "--release"))
        .orElse("");
  }

  /**
   * Get the target compatibility level of the build variant.
   */
  private static String getTargetCompatibility(List<String> compilerArgs) {
    return findFirstCompilerArgMatch(compilerArgs,
        Stream.of("-target", "--target", "--release"))
        .orElse("");
  }

  private static Optional<String> findCompilerArg(List<String> compilerArgs, String arg) {
    int idx = compilerArgs.indexOf(arg);
    if (idx >= 0 && idx < compilerArgs.size() - 1) {
      return Optional.of(compilerArgs.get(idx + 1));
    }
    return Optional.empty();
  }

  private static Optional<String> findFirstCompilerArgMatch(List<String> compilerArgs,
                                                            Stream<String> args) {
    return args.map(arg -> findCompilerArg(compilerArgs, arg))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst();
  }
  // endregion

}
