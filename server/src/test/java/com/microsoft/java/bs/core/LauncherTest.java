package com.microsoft.java.bs.core;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LauncherTest {

  @Test
  void testSystemPropertyCheck() {
    System.clearProperty("buildServerStorage");
    assertThrows(IllegalStateException.class, () -> {
      Launcher.main(null);
    });
  }
}
