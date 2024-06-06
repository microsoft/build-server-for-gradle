// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.example.project;

import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.TestFactory;

/**
 * Test of complex test hierarchy.
 */
public class TestFactoryTests {
  @TestFactory
  Collection<DynamicContainer> testContainer() {
    List<DynamicContainer> containers = new ArrayList<>();
    containers.add(
        dynamicContainer("First Container",
          List.of(dynamicTest("First test of first container", () -> {}),
                  dynamicTest("Second test of first container", () -> {}))));

    containers.add(
        dynamicContainer("Second Container",
          List.of(dynamicTest("First test of second container", () -> {}),
                  dynamicTest("Second test of second container", () -> {}))));
    return containers;
  }
}

