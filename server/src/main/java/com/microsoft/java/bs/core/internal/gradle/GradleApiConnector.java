package com.microsoft.java.bs.core.internal.gradle;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;

import com.microsoft.java.bs.gradle.model.GradleSourceSets;

/**
 * Connect to Gradle Daemon via Gradle Tooling API.
 */
public class GradleApiConnector {

  /**
   * The internal location of the plugin jar.
   */
  private static final String PLUGIN_JAR_INTERNAL_LOCATION = "/plugin.jar";

  /**
   * The name of the plugin jar after pasting.
   */
  public static final String GRADLE_PLUGIN_JAR_TARGET_NAME = "gradle-plugin.jar";

  /**
   * The name of the init script.
   */
  public static final String INIT_GRADLE_SCRIPT = "init.gradle";

  /**
   * The digest algorithm used to verify the content.
   */
  private static final String MESSAGE_DIGEST_ALGORITHM = "SHA-256";

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
   * Copy/update the required files to the target location.
   * And return the init.gradle file instance.
   */
  File getInitScript() {
    File pluginJarFile = copyPluginJarFile();
    return copyInitScript(pluginJarFile);
  }

  private File copyPluginJarFile() {
    File pluginJarFile = Utils.getCachedFile(GRADLE_PLUGIN_JAR_TARGET_NAME);
    // copy plugin jar to target location
    try (InputStream input = GradleApiConnector.class
          .getResourceAsStream(PLUGIN_JAR_INTERNAL_LOCATION)) {
      byte[] pluginJarBytes = input.readAllBytes();
      byte[] pluginJarDigest = getContentDigest(pluginJarBytes);
      if (needReplaceContent(pluginJarFile, pluginJarDigest)) {
        pluginJarFile.getParentFile().mkdirs();
        Files.write(pluginJarFile.toPath(), pluginJarBytes);
      }
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new IllegalStateException("Failed to get plugin jar.", e);
    }
    return pluginJarFile;
  }

  /**
   * copy init script to target location.
   */
  private File copyInitScript(File pluginJarFile) {
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
      byte[] initScriptBytes = initScriptContent.getBytes();
      byte[] initScriptDigest = getContentDigest(initScriptBytes);
      File initScriptFile = Utils.getCachedFile(INIT_GRADLE_SCRIPT);
      if (needReplaceContent(initScriptFile, initScriptDigest)) {
        initScriptFile.getParentFile().mkdirs();
        Files.write(initScriptFile.toPath(), initScriptBytes);
      }
      return initScriptFile;
    } catch (IOException | NoSuchAlgorithmException e) {
      throw new IllegalStateException("Failed to get init.script", e);
    }
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
