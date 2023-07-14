package com.microsoft.java.bs.gradle.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Helpers for plugin tests.
 */
public class PluginHelper {
  private PluginHelper() {}

  /**
   * Returns the init script file.
   */
  public static File getInitScript() throws IOException {
    File pluginJarFile = Paths.get(System.getProperty("user.dir"),
        "build", "libs", "plugin.jar").toFile();
    String pluginJarUnixPath = pluginJarFile.getAbsolutePath().replace("\\", "/");
    String initScriptContent = 
        "initscript {\n"
        + "  dependencies {\n"
        + "    classpath files('%s')\n"
        + "  }\n"
        + "}\n"
        + "allprojects {\n"
        + "  afterEvaluate {\n"
        + "    it.getPlugins().apply(com.microsoft.java.bs.gradle.plugin.GradleBuildServerPlugin)\n"
        + "  }\n"
        + "}\n";
    initScriptContent = String.format(initScriptContent, pluginJarUnixPath);

    byte[] initScriptBytes = initScriptContent.getBytes();
    File initScriptFile = File.createTempFile("init", ".gradle");
    if (!initScriptFile.getParentFile().exists()) {
      initScriptFile.getParentFile().mkdirs();
    }
    Files.write(initScriptFile.toPath(), initScriptBytes);
    initScriptFile.deleteOnExit();
    return initScriptFile;
  }
}
