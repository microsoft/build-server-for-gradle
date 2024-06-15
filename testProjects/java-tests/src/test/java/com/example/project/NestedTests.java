// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.example.project;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NestedTests {
  @Nested
  class NestedClassA {
    @Test
    public void test() { }
  }

  @Nested
  class NestedClassB {
    @Test
    void test() { }

    @Nested
    class ADeeperClass {
      @Test
      void test() {
      }
    }
  }
}