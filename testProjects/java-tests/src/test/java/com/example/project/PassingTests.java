// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.example.project;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PassingTests {

  @Test
  void isBasicTest() {
    assertTrue(true);
  }

  @Test
  @DisplayName("Display Name")
  void hasDisplayName() {
    assertTrue(true);
  }

  @ParameterizedTest(name = "{0}")
  @CsvSource({
      "0",
      "1",
      "2"
  })
  void isParameterized(int input) {
    assertTrue(true);
  }
}
 