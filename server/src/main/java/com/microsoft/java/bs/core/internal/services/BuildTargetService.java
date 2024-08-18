// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.services;

import static com.microsoft.java.bs.core.Launcher.LOGGER;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.microsoft.java.bs.core.internal.gradle.GradleApiConnector;
import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.core.internal.reporter.CompileProgressReporter;
import com.microsoft.java.bs.core.internal.reporter.DefaultProgressReporter;
import com.microsoft.java.bs.core.internal.reporter.ProgressReporter;
import com.microsoft.java.bs.core.internal.utils.JsonUtils;
import com.microsoft.java.bs.core.internal.utils.TelemetryUtils;
import com.microsoft.java.bs.core.internal.utils.UriUtils;
import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.JavaExtension;
import com.microsoft.java.bs.gradle.model.ScalaExtension;
import com.microsoft.java.bs.gradle.model.SupportedLanguages;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetEvent;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.CleanCacheParams;
import ch.epfl.scala.bsp4j.CleanCacheResult;
import ch.epfl.scala.bsp4j.CompileParams;
import ch.epfl.scala.bsp4j.CompileResult;
import ch.epfl.scala.bsp4j.DependencyModule;
import ch.epfl.scala.bsp4j.DependencyModulesItem;
import ch.epfl.scala.bsp4j.DependencyModulesParams;
import ch.epfl.scala.bsp4j.DependencyModulesResult;
import ch.epfl.scala.bsp4j.DependencySourcesItem;
import ch.epfl.scala.bsp4j.DependencySourcesParams;
import ch.epfl.scala.bsp4j.DependencySourcesResult;
import ch.epfl.scala.bsp4j.JavacOptionsItem;
import ch.epfl.scala.bsp4j.JavacOptionsParams;
import ch.epfl.scala.bsp4j.JavacOptionsResult;
import ch.epfl.scala.bsp4j.DidChangeBuildTarget;
import ch.epfl.scala.bsp4j.MavenDependencyModule;
import ch.epfl.scala.bsp4j.MavenDependencyModuleArtifact;
import ch.epfl.scala.bsp4j.OutputPathItem;
import ch.epfl.scala.bsp4j.OutputPathItemKind;
import ch.epfl.scala.bsp4j.OutputPathsItem;
import ch.epfl.scala.bsp4j.OutputPathsParams;
import ch.epfl.scala.bsp4j.OutputPathsResult;
import ch.epfl.scala.bsp4j.ResourcesItem;
import ch.epfl.scala.bsp4j.ResourcesParams;
import ch.epfl.scala.bsp4j.ResourcesResult;
import ch.epfl.scala.bsp4j.ScalaTestClassesItem;
import ch.epfl.scala.bsp4j.ScalaTestParams;
import ch.epfl.scala.bsp4j.ScalaTestSuiteSelection;
import ch.epfl.scala.bsp4j.ScalaTestSuites;
import ch.epfl.scala.bsp4j.ScalacOptionsItem;
import ch.epfl.scala.bsp4j.ScalacOptionsParams;
import ch.epfl.scala.bsp4j.ScalacOptionsResult;
import ch.epfl.scala.bsp4j.SourceItem;
import ch.epfl.scala.bsp4j.SourceItemKind;
import ch.epfl.scala.bsp4j.SourcesItem;
import ch.epfl.scala.bsp4j.SourcesParams;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TestParams;
import ch.epfl.scala.bsp4j.TestParamsDataKind;
import ch.epfl.scala.bsp4j.TestResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import org.apache.commons.lang3.StringUtils;

/**
 * Service to handle build target related BSP requests.
 */
public class BuildTargetService {

  private static final String MAVEN_DATA_KIND = "maven";

  private final BuildTargetManager buildTargetManager;

  private final GradleApiConnector connector;

  private final PreferenceManager preferenceManager;

  private BuildClient client;

  private boolean firstTime;

  /**
   * Initialize the build target service.
   *
   * @param buildTargetManager the build target manager.
   * @param preferenceManager the preference manager.
   */
  public BuildTargetService(BuildTargetManager buildTargetManager,
      GradleApiConnector connector, PreferenceManager preferenceManager) {
    this.buildTargetManager = buildTargetManager;
    this.connector = connector;
    this.preferenceManager = preferenceManager;
    this.firstTime = true;
  }

  private List<BuildTargetIdentifier> updateBuildTargets() {
    GradleSourceSets sourceSets = connector.getGradleSourceSets(
        preferenceManager.getRootUri(), client);
    return buildTargetManager.store(sourceSets);
  }

  private BuildTargetManager getBuildTargetManager() {
    if (firstTime) {
      updateBuildTargets();
      firstTime = false;
      int buildTargetCount = buildTargetManager.getAllGradleBuildTargets().size();
      Map<String, String> map = TelemetryUtils.getMetadataMap("buildTargetCount",
          String.valueOf(buildTargetCount));
      LOGGER.log(Level.INFO, "Found " + buildTargetCount + " build targets during initialization.",
          map);
    }
    return buildTargetManager;
  }

  /**
   * reload the sourcesets from scratch and notify the BSP client if they have changed.
   */
  public void reloadWorkspace() {
    List<BuildTargetIdentifier> changedTargets = updateBuildTargets();
    if (!changedTargets.isEmpty()) {
      notifyBuildTargetsChanged(changedTargets);
    }
  }
  
  private void notifyBuildTargetsChanged(List<BuildTargetIdentifier> changedTargets) {
    List<BuildTargetEvent> events = changedTargets.stream()
        .map(BuildTargetEvent::new)
        .collect(Collectors.toList());
    DidChangeBuildTarget param = new DidChangeBuildTarget(events);
    client.onBuildTargetDidChange(param);
  }

  private GradleBuildTarget getGradleBuildTarget(BuildTargetIdentifier btId) {
    return getBuildTargetManager().getGradleBuildTarget(btId);
  }

  public void setClient(BuildClient client) {
    this.client = client;
  }

  /**
   * Get the build targets of the workspace.
   */
  public WorkspaceBuildTargetsResult getWorkspaceBuildTargets() {
    List<GradleBuildTarget> allTargets = getBuildTargetManager().getAllGradleBuildTargets();
    List<BuildTarget> targets = allTargets.stream()
        .map(GradleBuildTarget::getBuildTarget)
        .collect(Collectors.toList());
    return new WorkspaceBuildTargetsResult(targets);
  }

  /**
   * Get the sources.
   */
  public SourcesResult getBuildTargetSources(SourcesParams params) {
    List<SourcesItem> sourceItems = new ArrayList<>();
    for (BuildTargetIdentifier btId : params.getTargets()) {
      GradleBuildTarget target = getGradleBuildTarget(btId);
      if (target == null) {
        LOGGER.warning("Skip sources collection for the build target: " + btId.getUri()
            + ". Because it cannot be found in the cache.");
        continue;
      }

      GradleSourceSet sourceSet = target.getSourceSet();
      List<SourceItem> sources = new ArrayList<>();
      for (File sourceDir : sourceSet.getSourceDirs()) {
        sources.add(new SourceItem(sourceDir.toURI().toString(), SourceItemKind.DIRECTORY,
            false /* generated */));
      }
      for (File sourceDir : sourceSet.getGeneratedSourceDirs()) {
        sources.add(new SourceItem(sourceDir.toURI().toString(), SourceItemKind.DIRECTORY,
            true /* generated */));
      }
      SourcesItem item = new SourcesItem(btId, sources);
      sourceItems.add(item);
    }
    return new SourcesResult(sourceItems);
  }

  /**
   * Get the resources.
   */
  public ResourcesResult getBuildTargetResources(ResourcesParams params) {
    List<ResourcesItem> items = new ArrayList<>();
    for (BuildTargetIdentifier btId : params.getTargets()) {
      GradleBuildTarget target = getGradleBuildTarget(btId);
      if (target == null) {
        LOGGER.warning("Skip resources collection for the build target: " + btId.getUri()
            + ". Because it cannot be found in the cache.");
        continue;
      }

      GradleSourceSet sourceSet = target.getSourceSet();
      List<String> resources = new ArrayList<>();
      for (File resourceDir : sourceSet.getResourceDirs()) {
        resources.add(resourceDir.toURI().toString());
      }
      ResourcesItem item = new ResourcesItem(btId, resources);
      items.add(item);
    }
    return new ResourcesResult(items);
  }

  /**
   * Get the output paths.
   */
  public OutputPathsResult getBuildTargetOutputPaths(OutputPathsParams params) {
    List<OutputPathsItem> items = new ArrayList<>();
    for (BuildTargetIdentifier btId : params.getTargets()) {
      GradleBuildTarget target = getGradleBuildTarget(btId);
      if (target == null) {
        LOGGER.warning("Skip output collection for the build target: " + btId.getUri()
            + ". Because it cannot be found in the cache.");
        continue;
      }

      GradleSourceSet sourceSet = target.getSourceSet();
      List<OutputPathItem> outputPaths = new ArrayList<>();
      // Due to the BSP spec does not support additional flags for each output path,
      // we will leverage the query of the uri to mark whether this is a source/resource
      // output path.
      // TODO: file a BSP spec issue to support additional flags for each output path.

      Set<File> sourceOutputDirs = sourceSet.getSourceOutputDirs();
      if (sourceOutputDirs != null) {
        for (File sourceOutputDir : sourceOutputDirs) {
          outputPaths.add(new OutputPathItem(
              sourceOutputDir.toURI() + "?kind=source",
              OutputPathItemKind.DIRECTORY
          ));
        }
      }

      Set<File> resourceOutputDirs = sourceSet.getResourceOutputDirs();
      if (resourceOutputDirs != null) {
        for (File resourceOutputDir : resourceOutputDirs) {
          outputPaths.add(new OutputPathItem(
              resourceOutputDir.toURI() + "?kind=resource",
              OutputPathItemKind.DIRECTORY
          ));
        }
      }

      OutputPathsItem item = new OutputPathsItem(btId, outputPaths);
      items.add(item);
    }
    return new OutputPathsResult(items);
  }

  /**
   * Get artifacts dependencies - old way.
   */
  public DependencySourcesResult getBuildTargetDependencySources(DependencySourcesParams params) {
    List<DependencySourcesItem> items = new ArrayList<>();
    for (BuildTargetIdentifier btId : params.getTargets()) {
      GradleBuildTarget target = getGradleBuildTarget(btId);
      if (target == null) {
        LOGGER.warning("Skip output collection for the build target: " + btId.getUri()
                + ". Because it cannot be found in the cache.");
        continue;
      }

      GradleSourceSet sourceSet = target.getSourceSet();
      List<String> sources = new ArrayList<>();
      for (GradleModuleDependency dep : sourceSet.getModuleDependencies()) {
        List<String> artifacts = dep.getArtifacts().stream()
                .filter(a -> "sources".equals(a.getClassifier()))
                .map(a -> a.getUri().toString())
                .toList();
        sources.addAll(artifacts);
      }

      items.add(new DependencySourcesItem(btId, sources));
    }
    return new DependencySourcesResult(items);
  }

  /**
   * Get artifacts dependencies.
   */
  public DependencyModulesResult getBuildTargetDependencyModules(DependencyModulesParams params) {
    List<DependencyModulesItem> items = new ArrayList<>();
    for (BuildTargetIdentifier btId : params.getTargets()) {
      GradleBuildTarget target = getGradleBuildTarget(btId);
      if (target == null) {
        LOGGER.warning("Skip output collection for the build target: " + btId.getUri()
            + ". Because it cannot be found in the cache.");
        continue;
      }

      GradleSourceSet sourceSet = target.getSourceSet();
      List<DependencyModule> modules = new ArrayList<>();
      for (GradleModuleDependency dep : sourceSet.getModuleDependencies()) {
        DependencyModule module = new DependencyModule(dep.getModule(), dep.getVersion());
        module.setDataKind(MAVEN_DATA_KIND);
        List<MavenDependencyModuleArtifact> artifacts = dep.getArtifacts().stream().map(a -> {
          MavenDependencyModuleArtifact artifact = new MavenDependencyModuleArtifact(
              a.getUri().toString());
          artifact.setClassifier(a.getClassifier());
          return artifact;
        }).collect(Collectors.toList());
        MavenDependencyModule mavenModule = new MavenDependencyModule(
            dep.getGroup(),
            dep.getModule(),
            dep.getVersion(),
            artifacts
        );
        module.setData(mavenModule);
        modules.add(module);
      }

      DependencyModulesItem item = new DependencyModulesItem(btId, modules);
      items.add(item);
    }
    return new DependencyModulesResult(items);
  }

  /**
   * Compile the build targets.
   */
  public CompileResult compile(CompileParams params) {
    if (params.getTargets().isEmpty()) {
      return new CompileResult(StatusCode.OK);
    } else {
      ProgressReporter reporter = new CompileProgressReporter(client,
          params.getOriginId(), getFullTaskPathMap());
      StatusCode code = runTasks(params.getTargets(), this::getBuildTaskName, reporter);
      CompileResult result = new CompileResult(code);
      result.setOriginId(params.getOriginId());

      // Schedule a task to refetch the build targets after compilation, this is to
      // auto detect the source roots changes for those code generation framework,
      // such as Protocol Buffer.
      if (!Boolean.getBoolean("bsp.plugin.reloadworkspace.disabled")) {
        CompletableFuture.runAsync(this::reloadWorkspace);
      }
      return result;
    }
  }

  /**
   * clean the build targets.
   */
  public CleanCacheResult cleanCache(CleanCacheParams params) {
    ProgressReporter reporter = new DefaultProgressReporter(client);
    StatusCode code = runTasks(params.getTargets(), this::getCleanTaskName, reporter);
    return new CleanCacheResult(null, code == StatusCode.OK);
  }

  /**
   * create a map of all known taskpaths to the build targets they affect.
   * used to associate progress events to the correct target.
   */
  private Map<String, Set<BuildTargetIdentifier>> getFullTaskPathMap() {
    Map<String, Set<BuildTargetIdentifier>> fullTaskPathMap = new HashMap<>();
    for (GradleBuildTarget buildTarget : buildTargetManager.getAllGradleBuildTargets()) {
      Set<String> tasks = buildTarget.getSourceSet().getTaskNames();
      BuildTargetIdentifier btId = buildTarget.getBuildTarget().getId();
      for (String taskName : tasks) {
        fullTaskPathMap.computeIfAbsent(taskName, k -> new HashSet<>()).add(btId);
      }
    }
    return fullTaskPathMap;
  }

  /**
   * group targets by project root and execute the supplied tasks.
   */
  private StatusCode runTasks(List<BuildTargetIdentifier> targets,
      Function<BuildTargetIdentifier, String> taskNameCreator,
      ProgressReporter reporter) {
    Map<URI, Set<BuildTargetIdentifier>> groupedTargets = groupBuildTargetsByRootDir(targets);
    StatusCode code = StatusCode.OK;
    for (Map.Entry<URI, Set<BuildTargetIdentifier>> entry : groupedTargets.entrySet()) {
      // remove duplicates as some tasks will have the same name for each sourceset e.g. clean.
      String[] tasks = entry.getValue().stream().map(taskNameCreator).distinct()
        .toArray(String[]::new);
      code = connector.runTasks(entry.getKey(), reporter, tasks);
      if (code == StatusCode.ERROR) {
        break;
      }
    }
    return code;
  }

  /**
   * Get the Java compiler options.
   */
  public JavacOptionsResult getBuildTargetJavacOptions(JavacOptionsParams params) {
    List<JavacOptionsItem> items = new ArrayList<>();
    for (BuildTargetIdentifier btId : params.getTargets()) {
      GradleBuildTarget target = getGradleBuildTarget(btId);
      if (target == null) {
        LOGGER.warning("Skip javac options collection for the build target: " + btId.getUri()
            + ". Because it cannot be found in the cache.");
        continue;
      }

      GradleSourceSet sourceSet = target.getSourceSet();
      JavaExtension javaExtension = SupportedLanguages.JAVA.getExtension(sourceSet);
      if (javaExtension == null) {
        LOGGER.warning("Skip javac options collection for the build target: " + btId.getUri()
            + ". Because the java extension cannot be found from source set.");
        continue;
      }
      List<String> classpath = sourceSet.getCompileClasspath().stream()
          .map(file -> file.toURI().toString())
          .collect(Collectors.toList());
      String classesDir;
      if (javaExtension.getClassesDir() != null) {
        classesDir = javaExtension.getClassesDir().toURI().toString();
      } else {
        classesDir = "";
      }
      items.add(new JavacOptionsItem(
          btId,
          javaExtension.getCompilerArgs(),
          classpath,
          classesDir
      ));
    }
    return new JavacOptionsResult(items);
  }
  
  /**
   * Get the Scala compiler options.
   */
  public ScalacOptionsResult getBuildTargetScalacOptions(ScalacOptionsParams params) {
    List<ScalacOptionsItem> items = new ArrayList<>();
    for (BuildTargetIdentifier btId : params.getTargets()) {
      GradleBuildTarget target = getGradleBuildTarget(btId);
      if (target == null) {
        LOGGER.warning("Skip scalac options collection for the build target: " + btId.getUri()
            + ". Because it cannot be found in the cache.");
        continue;
      }

      GradleSourceSet sourceSet = target.getSourceSet();
      ScalaExtension scalaExtension = SupportedLanguages.SCALA.getExtension(sourceSet);
      if (scalaExtension == null) {
        LOGGER.warning("Skip scalac options collection for the build target: " + btId.getUri()
                + ". Because the scalac extension cannot be found from source set.");
        continue;
      }
      List<String> classpath = sourceSet.getCompileClasspath().stream()
          .map(file -> file.toURI().toString())
          .collect(Collectors.toList());
      String classesDir;
      if (scalaExtension.getClassesDir() != null) {
        classesDir = scalaExtension.getClassesDir().toURI().toString();
      } else {
        classesDir = "";
      }
      items.add(new ScalacOptionsItem(
          btId,
          scalaExtension.getScalaCompilerArgs(),
          classpath,
          classesDir
      ));
    }
    return new ScalacOptionsResult(items);
  }

  /**
   * Run the test classes.
   */
  public TestResult buildTargetTest(TestParams params) {
    TestResult testResult = new TestResult(StatusCode.OK);
    testResult.setOriginId(params.getOriginId());
    // running tests can trigger compilation that must be reported on
    CompileProgressReporter compileProgressReporter =
        new CompileProgressReporter(client, params.getOriginId(), getFullTaskPathMap());
    Map<URI, Set<BuildTargetIdentifier>> groupedTargets =
        groupBuildTargetsByRootDir(params.getTargets());

    for (Map.Entry<URI, Set<BuildTargetIdentifier>> entry : groupedTargets.entrySet()) {
      // TODO ideally BSP would have a jvmTestEnv style testkind for executing tests, not scala.
      StatusCode statusCode;
      if (TestParamsDataKind.SCALA_TEST.equals(params.getDataKind())) {
        // existing logic for scala test (class level)
        statusCode = runScalaTests(entry, params, compileProgressReporter);
      } else if ("scala-test-suites-selection".equals(params.getDataKind())) {
        statusCode = runScalaTestSuitesSelection(entry, params, compileProgressReporter);
      } else {
        LOGGER.warning("Test Data Kind " + params.getDataKind() + " not supported");
        statusCode = StatusCode.ERROR;
      }

      if (statusCode != StatusCode.OK) {
        testResult.setStatusCode(statusCode);
      }
    }

    return testResult;
  }

  private StatusCode runScalaTests(
      Map.Entry<URI, Set<BuildTargetIdentifier>> entry,
      TestParams params,
      CompileProgressReporter compileProgressReporter
  ) {
    // ScalaTestParams is for a list of classes only
    ScalaTestParams testParams = JsonUtils.toModel(params.getData(), ScalaTestParams.class);
    Map<BuildTargetIdentifier, Map<String, Set<String>>> testClasses = new HashMap<>();
    for (ScalaTestClassesItem testClassesItem : testParams.getTestClasses()) {
      Map<String, Set<String>> classesMethods = new HashMap<>();
      for (String classNames : testClassesItem.getClasses()) {
        classesMethods.put(classNames, null);
      }
      testClasses.put(testClassesItem.getTarget(), classesMethods);
    }
    return connector.runTests(entry.getKey(), testClasses, testParams.getJvmOptions(),
        params.getArguments(), null, client, params.getOriginId(),
        compileProgressReporter);
  }

  private StatusCode runScalaTestSuitesSelection(
      Map.Entry<URI, Set<BuildTargetIdentifier>> entry,
      TestParams params,
      CompileProgressReporter compileProgressReporter
  ) {
    // ScalaTestSuites is for a list of classes + methods
    // Since it doesn't supply the specific BuildTarget we require a single
    // build target in the params and reject any request that doesn't match this
    if (params.getTargets().size() != 1) {
      LOGGER.warning("Test params with Test Data Kind " + params.getDataKind()
          + " must contain only 1 build target");
      return StatusCode.ERROR;
    } else {
      ScalaTestSuites testSuites = JsonUtils.toModel(params.getData(), ScalaTestSuites.class);
      Map<String, String> envVars = null;
      boolean argsValid = true;
      if (testSuites.getEnvironmentVariables() != null) {
        // arg is of the form KEY=VALUE
        List<String[]> splitArgs = testSuites.getEnvironmentVariables()
            .stream()
            .map(arg -> arg.split("="))
            .toList();
        argsValid = splitArgs.stream().allMatch(arg -> arg.length == 2);
        if (argsValid) {
          envVars = splitArgs.stream().collect(Collectors.toMap(arg -> arg[0], arg -> arg[1]));
        }
      }
      if (!argsValid) {
        LOGGER.warning("Test params arguments must each be in the form KEY=VALUE. "
            + testSuites.getEnvironmentVariables());
        return StatusCode.ERROR;
      } else {
        Map<String, Set<String>> classesMethods = new HashMap<>();
        for (ScalaTestSuiteSelection testSuiteSelection : testSuites.getSuites()) {
          Set<String> methods = classesMethods
              .computeIfAbsent(testSuiteSelection.getClassName(), k -> new HashSet<>());
          methods.addAll(testSuiteSelection.getTests());
        }
        Map<BuildTargetIdentifier, Map<String, Set<String>>> testClasses = new HashMap<>();
        testClasses.put(params.getTargets().get(0), classesMethods);
        return connector.runTests(entry.getKey(), testClasses, testSuites.getJvmOptions(),
            params.getArguments(), envVars, client, params.getOriginId(),
            compileProgressReporter);
      }
    }
  }

  /**
   * Group the build targets by the project root directory,
   * projects with the same root directory can run their tasks
   * in one single call.
   */
  private Map<URI, Set<BuildTargetIdentifier>> groupBuildTargetsByRootDir(
      List<BuildTargetIdentifier> targets
  ) {
    Map<URI, Set<BuildTargetIdentifier>> groupedTargets = new HashMap<>();
    for (BuildTargetIdentifier btId : targets) {
      URI projectUri = getRootProjectUri(btId);
      if (projectUri == null) {
        continue;
      }
      groupedTargets.computeIfAbsent(projectUri, k -> new HashSet<>()).add(btId);
    }
    return groupedTargets;
  }

  /**
   * Try to get the project root directory uri. If root directory is not available,
   * return the uri of the build target.
   */
  private URI getRootProjectUri(BuildTargetIdentifier btId) {
    GradleBuildTarget gradleBuildTarget = getGradleBuildTarget(btId);
    if (gradleBuildTarget == null) {
      // TODO: https://github.com/microsoft/build-server-for-gradle/issues/50
      throw new IllegalArgumentException("The build target does not exist: " + btId.getUri());
    }
    BuildTarget buildTarget = gradleBuildTarget.getBuildTarget();
    if (buildTarget.getBaseDirectory() != null) {
      return UriUtils.getUriFromString(buildTarget.getBaseDirectory());
    }

    return UriUtils.getUriWithoutQuery(btId.getUri());
  }

  /**
   * Return a source set task name.
   */
  private String getProjectTaskName(BuildTargetIdentifier btId, String title,
      Function<GradleSourceSet, String> creator) {
    GradleBuildTarget gradleBuildTarget = getGradleBuildTarget(btId);
    if (gradleBuildTarget == null) {
      // TODO: https://github.com/microsoft/build-server-for-gradle/issues/50
      throw new IllegalArgumentException("The build target does not exist: " + btId.getUri());
    }
    String taskName = creator.apply(gradleBuildTarget.getSourceSet());
    if (StringUtils.isBlank(taskName)) {
      throw new IllegalArgumentException("The build target does not have a " + title + " task: "
          + btId.getUri());
    }
    return taskName;
  }

  /**
   * Return the build task name - [project path]:[task].
   */
  private String getBuildTaskName(BuildTargetIdentifier btId) {
    return getProjectTaskName(btId, "classes", GradleSourceSet::getClassesTaskName);
  }

  /**
   * Return the clean task name - [project path]:[task].
   */
  private String getCleanTaskName(BuildTargetIdentifier btId) {
    return getProjectTaskName(btId, "clean", GradleSourceSet::getCleanTaskName);
  }
}
