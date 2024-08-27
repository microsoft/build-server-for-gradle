// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

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
    File pluginFolder = Paths.get(System.getProperty("user.dir"),
        "build", "libs").toFile();
    File pluginJarFile = pluginFolder.listFiles()[0];
    String pluginJarUnixPath = pluginJarFile.getAbsolutePath().replace("\\", "/");
    String initScriptContent =
        "initscript {\n"
        + "  dependencies {\n"
        + "    classpath files('%s')\n"
        + "  }\n"
        + "}\n"
        + "allprojects {\n"
        + "  apply plugin: com.microsoft.java.bs.gradle.plugin.GradleBuildServerPlugin\n"
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
