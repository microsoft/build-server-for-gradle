// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.example.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class EnvVarTests {

  @Test
  void envVarSetTest() {
    assertEquals("Test", System.getenv("EnvVar"));
  }

  @Test
  void envVarNotSetTest() {
    assertNull(System.getenv("EnvVar"));
  }
}
 