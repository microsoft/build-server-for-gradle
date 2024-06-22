// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.example.project;

import org.testng.Assert;
import org.testng.annotations.*;

class PassingTests {

  @Test
  void isBasicTest() {
    Assert.assertTrue(true);
  }

  @Test(description = "Description")
  void hasDisplayName() {
    Assert.assertTrue(true);
  }

  @DataProvider(name = "param")
  public Object[][] paramData() {
    return new Object[][] {
      { "0" },
      { "1" },
      { "2" },
    };
  }

  @Test(dataProvider = "param")
  void isParameterized(String input) {
    Assert.assertTrue(true);
  }
}
 