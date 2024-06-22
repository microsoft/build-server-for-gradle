// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.example.project;

import org.testng.annotations.*;

class ExceptionInBefore {

  @BeforeClass
  public static void beforeAll() throws Exception {
    throw new Exception("Exception in @BeforeAll");
  }

  @Test
  public void test() {

  }
}