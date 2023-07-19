package com.microsoft.java.bs.core;

import static com.microsoft.java.bs.core.Launcher.PROP_BUILD_SERVER_STORAGE;
import static com.microsoft.java.bs.core.Launcher.PROP_PLUGIN_LOCATION;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LauncherTest {

  @Test
  void testSystemPropertyCheckForServerStorage() {
    System.clearProperty(PROP_BUILD_SERVER_STORAGE);
    assertThrows(IllegalStateException.class, () -> {
      Launcher.main(null);
    });
  }

  @Test
  void testSystemPropertyCheckForPluginLocation() {
    System.clearProperty(PROP_PLUGIN_LOCATION);
    System.setProperty(PROP_BUILD_SERVER_STORAGE, "test");
    assertThrows(IllegalStateException.class, () -> {
      Launcher.main(null);
    });
  }
}
