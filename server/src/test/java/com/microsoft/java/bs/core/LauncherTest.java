// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core;

import static com.microsoft.java.bs.core.Launcher.PROP_PLUGIN_DIR;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LauncherTest {

  @TempDir
  Path temp;

  @Test
  void testSystemPropertyCheckForPluginLocation() {
    System.clearProperty(PROP_PLUGIN_DIR);
    assertThrows(IllegalStateException.class, () -> {
      Launcher.main(null);
    });
  }

  @Test
  void findsPluginsInTheDistributionBesideTheServerJar() throws Exception {
    Path lib = Files.createDirectories(temp.resolve("lib"));
    Path plugins = Files.createDirectories(temp.resolve("plugins"));
    Path jar = Files.createFile(lib.resolve("server.jar"));

    assertNull(Launcher.pluginDirectoryBesideJar(jar));
    Files.createFile(plugins.resolve("init.gradle"));
    assertEquals(plugins, Launcher.pluginDirectoryBesideJar(jar));
    assertNull(Launcher.pluginDirectoryBesideJar(temp.resolve("server.jar")));
  }
}
