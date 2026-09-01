// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.impl.DefaultArtifact;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleModuleDependency;
import org.junit.jupiter.api.Test;

class GetSourceSetsActionTest {

  @Test
  void testRemoveBuildTargetArtifactsPreservesOtherArtifacts() {
    URI buildTargetOutput = new File("project-a/build/classes/java/main").toURI();
    URI externalArtifact = new File("repository/external.jar").toURI();
    DefaultGradleModuleDependency dependency = new DefaultGradleModuleDependency(
        "group",
        "module",
        "1.0.0",
        Arrays.asList(
            new DefaultArtifact(buildTargetOutput, null),
            new DefaultArtifact(externalArtifact, null)));

    Set<GradleModuleDependency> filteredDependencies =
        GetSourceSetsAction.removeBuildTargetArtifacts(
            Collections.singleton(dependency),
            Collections.singleton(buildTargetOutput));

    assertEquals(1, filteredDependencies.size());
    GradleModuleDependency filteredDependency = filteredDependencies.iterator().next();
    assertEquals(1, filteredDependency.getArtifacts().size());
    assertEquals(externalArtifact, filteredDependency.getArtifacts().get(0).getUri());
    assertEquals(2, dependency.getArtifacts().size());
  }
}
