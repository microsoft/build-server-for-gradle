package com.microsoft.java.bs.core.internal.gradle;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;

import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;

import com.microsoft.java.bs.gradle.model.GradleSourceSets;

/**
 * Connect to Gradle Daemon via Gradle Tooling API.
 */
public class GradleApiConnector {

  /**
   * Get the source sets of the Gradle project.
   *
   * @param projectUri uri of the project
   * @return an instance of {@link GradleSourceSets}
   */
  public GradleSourceSets getGradleSourceSets(URI projectUri) {
    File initScript = getInitScript();
    try (ProjectConnection connection = Utils.getProjectConnection(projectUri)) {
      ModelBuilder<GradleSourceSets> customModelBuilder = Utils.getModelBuilder(
          connection,
          GradleSourceSets.class
      );
      customModelBuilder.addArguments("--init-script", initScript.getAbsolutePath());
      return customModelBuilder.get();
    } catch (GradleConnectionException | IllegalStateException e) {
      // TODO: report the error to client via build server protocol
      throw e;
    }

  }

  /**
   * Return the init.gradle file instance, which having the customized plugin set.
   */
  File getInitScript() {
    File initScriptFile = Utils.getInitScriptFile();
    if (!initScriptFile.exists()) {
      copyInitScript(initScriptFile);
    }

    return initScriptFile;
  }

  /**
   * copy init script to target location.
   */
  private void copyInitScript(File target) {
    File pluginJarFile = Utils.getPluginFile();
    String pluginJarUnixPath = pluginJarFile.getAbsolutePath().replace("\\", "/");
    String initScriptContent = """
        initscript {
          dependencies {
            classpath files('%s')
          }
        }
        allprojects {
          afterEvaluate {
            it.getPlugins().apply(com.microsoft.java.bs.gradle.plugin.GradleBuildServerPlugin)
          }
        }
        """;
    initScriptContent = String.format(initScriptContent, pluginJarUnixPath);
    try {
      target.getParentFile().mkdirs();
      Files.write(target.toPath(), initScriptContent.getBytes());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to get init.script", e);
    }
  }
}
