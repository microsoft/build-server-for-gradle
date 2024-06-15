// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.example.project;

import org.testng.Assert;
import org.testng.annotations.*;

class EnvVarTests {

  @Test
  void envVarSetTest() {
    Assert.assertEquals(System.getenv("EnvVar"), "Test");
  }

  @Test
  void envVarNotSetTest() {
    Assert.assertNull(System.getenv("EnvVar"));
  }
}
 