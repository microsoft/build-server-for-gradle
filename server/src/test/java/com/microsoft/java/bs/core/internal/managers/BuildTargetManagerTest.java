// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.gradle.model.BuildTargetDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.JvmBuildTarget;
import ch.epfl.scala.bsp4j.extended.JvmBuildTargetEx;

class BuildTargetManagerTest {

  @Test
  void testStore() {
    GradleSourceSet gradleSourceSet = getMockedTestGradleSourceSet();
    when(gradleSourceSet.getSourceSetName()).thenReturn("test");
    when(gradleSourceSet.getDisplayName()).thenReturn("test name");
    when(gradleSourceSet.hasTests()).thenReturn(true);
    GradleSourceSets gradleSourceSets = mock(GradleSourceSets.class);
    when(gradleSourceSets.getGradleSourceSets()).thenReturn(Arrays.asList(gradleSourceSet));

    BuildTargetManager manager = new BuildTargetManager();
    manager.store(gradleSourceSets);

    List<GradleBuildTarget> list = manager.getAllGradleBuildTargets();
    BuildTarget buildTarget = list.get(0).getBuildTarget();
    assertTrue(buildTarget.getTags().contains("test"));
    assertTrue(buildTarget.getId().getUri().contains("?sourceset=test"));
    assertEquals("test name", buildTarget.getDisplayName());
  }

  @Test
  void testJvmExtension() {
    GradleSourceSet gradleSourceSet = getMockedTestGradleSourceSet();
    when(gradleSourceSet.getJavaVersion()).thenReturn("17");
    GradleSourceSets gradleSourceSets = mock(GradleSourceSets.class);
    when(gradleSourceSets.getGradleSourceSets()).thenReturn(Arrays.asList(gradleSourceSet));
    
    BuildTargetManager manager = new BuildTargetManager();
    manager.store(gradleSourceSets);

    List<GradleBuildTarget> list = manager.getAllGradleBuildTargets();
    BuildTarget buildTarget = list.get(0).getBuildTarget();

    assertEquals("jvm", buildTarget.getDataKind());
    JvmBuildTarget jvmBt = (JvmBuildTarget) buildTarget.getData();
    assertEquals("17", jvmBt.getJavaVersion());
  }

  @Test
  void testJvmExtensionEx() {
    GradleSourceSet gradleSourceSet = getMockedTestGradleSourceSet();
    when(gradleSourceSet.getGradleVersion()).thenReturn("8.0");
    when(gradleSourceSet.getSourceCompatibility()).thenReturn("17");
    when(gradleSourceSet.getTargetCompatibility()).thenReturn("17");
    GradleSourceSets gradleSourceSets = mock(GradleSourceSets.class);
    when(gradleSourceSets.getGradleSourceSets()).thenReturn(Arrays.asList(gradleSourceSet));
    
    BuildTargetManager manager = new BuildTargetManager();
    manager.store(gradleSourceSets);

    List<GradleBuildTarget> list = manager.getAllGradleBuildTargets();
    BuildTarget buildTarget = list.get(0).getBuildTarget();

    assertEquals("jvm", buildTarget.getDataKind());
    JvmBuildTargetEx jvmBt = (JvmBuildTargetEx) buildTarget.getData();
    assertEquals("8.0", jvmBt.getGradleVersion());
    assertEquals("17", jvmBt.getSourceCompatibility());
    assertEquals("17", jvmBt.getTargetCompatibility());
  }

  @Test
  void testBuildTargetDependency() {
    GradleSourceSet gradleSourceSetFoo = getMockedTestGradleSourceSet();
    when(gradleSourceSetFoo.getProjectPath()).thenReturn(":foo");
    when(gradleSourceSetFoo.getProjectDir()).thenReturn(new File("foo"));


    BuildTargetDependency buildTargetDependency = mock(BuildTargetDependency.class);
    when(buildTargetDependency.getProjectPath()).thenReturn(":foo");
    Set<BuildTargetDependency> dependencies = new HashSet<>();
    dependencies.add(buildTargetDependency);
    GradleSourceSet gradleSourceSetBar = getMockedTestGradleSourceSet();
    when(gradleSourceSetBar.getProjectPath()).thenReturn(":bar");
    when(gradleSourceSetBar.getProjectDir()).thenReturn(new File("bar"));
    when(gradleSourceSetBar.getBuildTargetDependencies()).thenReturn(dependencies);

    GradleSourceSets gradleSourceSets = mock(GradleSourceSets.class);
    when(gradleSourceSets.getGradleSourceSets()).thenReturn(
        Arrays.asList(gradleSourceSetFoo, gradleSourceSetBar));

    BuildTargetManager manager = new BuildTargetManager();
    manager.store(gradleSourceSets);

    List<GradleBuildTarget> list = manager.getAllGradleBuildTargets();
    BuildTarget buildTargetFoo = list.stream()
        .filter(bt -> bt.getBuildTarget().getId().getUri().contains("foo"))
        .findFirst()
        .get()
        .getBuildTarget();
    BuildTarget buildTargetBar = list.stream()
        .filter(bt -> bt.getBuildTarget().getId().getUri().contains("bar"))
        .findFirst()
        .get()
        .getBuildTarget();

    assertTrue(buildTargetBar.getDependencies().contains(buildTargetFoo.getId()));
  }

  private GradleSourceSet getMockedTestGradleSourceSet() {
    GradleSourceSet mocked = mock(GradleSourceSet.class);
    when(mocked.getProjectDir()).thenReturn(new File("test"));
    when(mocked.getRootDir()).thenReturn(new File("test"));
    when(mocked.getSourceSetName()).thenReturn("main");
    when(mocked.getSourceDirs()).thenReturn(Collections.emptySet());
    when(mocked.getGeneratedSourceDirs()).thenReturn(Collections.emptySet());
    when(mocked.getResourceDirs()).thenReturn(Collections.emptySet());
    when(mocked.getModuleDependencies()).thenReturn(Collections.emptySet());
    when(mocked.getBuildTargetDependencies()).thenReturn(Collections.emptySet());
    return mocked;
  }
}
