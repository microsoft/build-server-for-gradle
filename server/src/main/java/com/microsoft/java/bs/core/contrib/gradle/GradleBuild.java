package com.microsoft.java.bs.core.contrib.gradle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.tooling.BuildException;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.events.OperationType;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTargets;
import com.microsoft.java.bs.core.contrib.BuildSupport;
import com.microsoft.java.bs.core.contrib.CompileProgressReporter;
import com.microsoft.java.bs.core.contrib.DefaultProgressReporter;
import com.microsoft.java.bs.core.log.InjectLogger;
import com.microsoft.java.bs.core.managers.BuildTargetsManager;
import com.microsoft.java.bs.core.managers.PreferencesManager;
import com.microsoft.java.bs.core.model.BuildTargetComponents;
import com.microsoft.java.bs.core.utils.UriUtils;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.StatusCode;

/**
 * Build support for Gradle projects.
 */
public class GradleBuild implements BuildSupport {

  private static final String PLUGIN_JAR_INTERNAL_LOCATION = "/plugin.jar";
  private static final String GRADLE_PLUGIN_JAR_TARGET_NAME = "gradle-plugin.jar";
  private static final String INIT_GRADLE_SCRIPT = "init.gradle";

  private static final String MESSAGE_DIGEST_ALGORITHM = "SHA-256";
  private static final String USER_HOME = "user.home";

  @InjectLogger
  Logger logger;

  @Inject
  BuildTargetsManager buildTargetsManager;

  @Inject
  PreferencesManager preferencesManager;

  @Override
  public JavaBuildTargets getSourceSetEntries(URI projectUri) {
    File initScript = getInitScript();
    if (initScript == null) {
      throw new IllegalStateException("Failed to get init.gradle");
    }

    TaskProgressReporter reporter = new TaskProgressReporter(new DefaultProgressReporter());
    final ProjectConnection connection = GradleBuildUtils.getProjectConnection(
        new File(projectUri),
        preferencesManager.getPreferences()
    );
    try (connection) {
      reporter.taskStarted("Connect to Gradle Daemon");
      ModelBuilder<JavaBuildTargets> customModelBuilder = GradleBuildUtils.getModelBuilder(
          connection,
          preferencesManager.getPreferences(),
          JavaBuildTargets.class
      );
      customModelBuilder.addProgressListener(
          reporter,
          OperationType.FILE_DOWNLOAD,
          OperationType.PROJECT_CONFIGURATION
      );
      customModelBuilder.addArguments("--init-script", initScript.getAbsolutePath());
      JavaBuildTargets model = customModelBuilder.get();
      reporter.taskFinished("", StatusCode.OK);
      return model;
    } catch (GradleConnectionException | IllegalStateException e) {
      reporter.taskFinished(e.getMessage(), StatusCode.ERROR);
      throw e;
    }
  }

  @Override
  public StatusCode build(List<BuildTargetIdentifier> targets) {
    Map<URI, Set<BuildTargetIdentifier>> groupedTargets = groupBuildTargets(targets);
    for (Map.Entry<URI, Set<BuildTargetIdentifier>> entry : groupedTargets.entrySet()) {
      Set<BuildTargetIdentifier> btIds = entry.getValue();
      String[] tasks = btIds.stream().map(this::getTaskName).toArray(String[]::new);
      StatusCode res = runGradleTasks(entry.getKey(), btIds, tasks);
      if (res == StatusCode.ERROR) {
        return StatusCode.ERROR;
      }
    }
    return StatusCode.OK;
  }

  @Override
  public boolean cleanCache(List<BuildTargetIdentifier> targets) {
    Map<URI, Set<BuildTargetIdentifier>> groupedTargets = groupBuildTargets(targets);
    for (Map.Entry<URI, Set<BuildTargetIdentifier>> entry : groupedTargets.entrySet()) {
      Set<BuildTargetIdentifier> btIds = entry.getValue();
      StatusCode res = runGradleTasks(entry.getKey(), btIds, "clean");
      if (res == StatusCode.ERROR) {
        return false;
      }
    }
    return true;
  }

  private StatusCode runGradleTasks(URI projectUri, Set<BuildTargetIdentifier> btIds,
      String... tasks) {
    // simply pick the first build target since all the build targets in the same project
    // and so far we don't distinguish them.
    TaskProgressReporter reporter = new TaskProgressReporter(new CompileProgressReporter(
        btIds.iterator().next()));
    final ByteArrayOutputStream errorOut = new ByteArrayOutputStream();
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final ProjectConnection connection = GradleBuildUtils.getProjectConnection(
        new File(projectUri),
        preferencesManager.getPreferences()
    );
    try (out; errorOut; connection) {
      reporter.taskStarted("Start to build");
      BuildLauncher launcher = GradleBuildUtils.getBuildLauncher(
          connection,
          preferencesManager.getPreferences()
      );
      launcher.addProgressListener(reporter)
          .setStandardError(errorOut)
          .setStandardOutput(out)
          .forTasks(tasks);
      launcher.run();
      reporter.taskFinished(out.toString(), StatusCode.OK);
    } catch (IOException e) {
      // caused by close the output stream, just simply log the error.
      logger.error(e.getMessage(), e);
    } catch (BuildException e) {
      reporter.taskFinished(errorOut.toString(), StatusCode.ERROR);
      return StatusCode.ERROR;
    }

    return StatusCode.OK;
  }

  /**
   * Group the build targets by the project base directory.
   */
  private Map<URI, Set<BuildTargetIdentifier>> groupBuildTargets(
      List<BuildTargetIdentifier> targets
  ) {
    Map<URI, Set<BuildTargetIdentifier>> groupedTargets = new HashMap<>();
    for (BuildTargetIdentifier btId : targets) {
      URI projectUri = getProjectUri(btId);
      if (projectUri == null) {
        continue;
      }
      groupedTargets.computeIfAbsent(projectUri, k -> new HashSet<>()).add(btId);
    }
    return groupedTargets;
  }

  /**
   * Try to get the project base directory uri. If base directory is not available,
   * return the uri of the build target.
   */
  private URI getProjectUri(BuildTargetIdentifier btId) {
    try {
      BuildTargetComponents components = buildTargetsManager.getComponents(btId);
      if (components != null) {
        BuildTarget buildTarget = components.getBuildTarget();
        if (buildTarget.getBaseDirectory() != null) {
          return new URI(buildTarget.getBaseDirectory());
        }
      }

      return UriUtils.uriWithoutQuery(btId.getUri());
    } catch (URISyntaxException e) {
      logger.error(e.getMessage(), e);
      return null;
    }
  }

  private String getTaskName(BuildTargetIdentifier btId) {
    String uri = btId.getUri();
    String sourceSetName = uri.substring(uri.lastIndexOf("?sourceset=") + "?sourceset=".length());
    String taskName = switch (sourceSetName) {
      case "main" -> "classes";
      case "test" -> "testClasses";
      // https://docs.gradle.org/current/userguide/java_plugin.html#java_source_set_tasks
      default -> sourceSetName + "Classes";
    };

    BuildTargetComponents components = buildTargetsManager.getComponents(btId);
    if (components == null) {
      throw new IllegalArgumentException("The build target does not exist: " + btId.getUri());
    }
    String modulePath = components.getModulePath();
    if (modulePath == null || modulePath.equals(":")) {
      return taskName;
    }
    return modulePath + ":" + taskName;
  }

  /**
   * Copy/update the required files to the target location.
   * And return the init.gradle file instance.
   */
  private File getInitScript() {
    File pluginJarFile = Paths.get(System.getProperty(USER_HOME), ".bsp",
        GRADLE_PLUGIN_JAR_TARGET_NAME).toFile();
    // copy plugin jar to target location
    final InputStream input = GradleBuild.class.getResourceAsStream(PLUGIN_JAR_INTERNAL_LOCATION);
    try (input) {
      byte[] pluginJarBytes = input.readAllBytes();
      byte[] pluginJarDigest = getContentDigest(pluginJarBytes);
      if (needReplaceContent(pluginJarFile, pluginJarDigest)) {
        pluginJarFile.getParentFile().mkdirs();
        Files.write(pluginJarFile.toPath(), pluginJarBytes);
      }
    } catch (IOException | NoSuchAlgorithmException e) {
      logger.error(e.getMessage(), e);
    }

    if (!pluginJarFile.exists()) {
      logger.error("Failed to get plugin.jar");
      return null;
    }

    // copy init script to target location
    String pluginJarUnixPath = pluginJarFile.getAbsolutePath().replace("\\", "/");
    String initScriptContent = """
        initscript {
          dependencies {
            classpath files('%s')
          }
        }
        allprojects {
          afterEvaluate {
            it.getPlugins().apply(com.microsoft.java.bs.contrib.gradle.plugin.BspGradlePlugin)
          }
        }
        """;
    initScriptContent = String.format(initScriptContent, pluginJarUnixPath);
    try {
      byte[] initScriptBytes = initScriptContent.getBytes();
      byte[] initScriptDigest = getContentDigest(initScriptBytes);
      File initScriptFile = Paths.get(System.getProperty(USER_HOME), ".bsp",
          INIT_GRADLE_SCRIPT).toFile();
      if (needReplaceContent(initScriptFile, initScriptDigest)) {
        initScriptFile.getParentFile().mkdirs();
        Files.write(initScriptFile.toPath(), initScriptBytes);
      }
      return initScriptFile;
    } catch (IOException | NoSuchAlgorithmException e) {
      logger.error(e.getMessage(), e);
    }
    return null;
  }

  private boolean needReplaceContent(File file, byte[] checksum)
      throws IOException, NoSuchAlgorithmException {
    if (!file.exists() || file.length() == 0) {
      return true;
    }

    byte[] digest = getContentDigest(Files.readAllBytes(file.toPath()));
    return !Arrays.equals(digest, checksum);
  }

  private byte[] getContentDigest(byte[] contentBytes) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
    md.update(contentBytes);
    return md.digest();
  }
}
