// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.example.project;

import org.testng.Assert;
import org.testng.annotations.*;

class FailingTests {

  @Test
  void failingTest() {
    Assert.assertTrue(false);
  }
}
