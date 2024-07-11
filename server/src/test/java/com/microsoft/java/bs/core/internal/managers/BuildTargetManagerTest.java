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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.gradle.model.BuildTargetDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.JavaExtension;
import com.microsoft.java.bs.gradle.model.LanguageExtension;
import com.microsoft.java.bs.gradle.model.SupportedLanguages;
import com.microsoft.java.bs.gradle.model.impl.DefaultBuildTargetDependency;

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
    File fooProjectDir = new File("foo");
    String fooSourceSetName = "main";
    GradleSourceSet gradleSourceSetFoo = getMockedTestGradleSourceSet();
    when(gradleSourceSetFoo.getProjectPath()).thenReturn(":foo");
    when(gradleSourceSetFoo.getProjectDir()).thenReturn(fooProjectDir);
    when(gradleSourceSetFoo.getSourceSetName()).thenReturn(fooSourceSetName);

    BuildTargetDependency buildTargetDependency = new DefaultBuildTargetDependency(
        fooProjectDir.getAbsolutePath(), fooSourceSetName);
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
    when(mocked.getGradleVersion()).thenReturn("8.0");
    when(mocked.getProjectDir()).thenReturn(new File("test"));
    when(mocked.getRootDir()).thenReturn(new File("test"));
    when(mocked.getSourceSetName()).thenReturn("main");
    when(mocked.getSourceDirs()).thenReturn(Collections.emptySet());
    when(mocked.getGeneratedSourceDirs()).thenReturn(Collections.emptySet());
    when(mocked.getResourceDirs()).thenReturn(Collections.emptySet());
    when(mocked.getModuleDependencies()).thenReturn(Collections.emptySet());
    when(mocked.getBuildTargetDependencies()).thenReturn(Collections.emptySet());
    JavaExtension mockedJavaExtension = mock(JavaExtension.class);
    when(mockedJavaExtension.isJavaExtension()).thenReturn(true);
    when(mockedJavaExtension.getAsJavaExtension()).thenReturn(mockedJavaExtension);
    when(mockedJavaExtension.getJavaVersion()).thenReturn("17");
    when(mockedJavaExtension.getSourceCompatibility()).thenReturn("17");
    when(mockedJavaExtension.getTargetCompatibility()).thenReturn("17");
    Map<String, LanguageExtension> extensions = new HashMap<>();
    extensions.put(SupportedLanguages.JAVA.getBspName(), mockedJavaExtension);
    when(mocked.getExtensions()).thenReturn(extensions);
    return mocked;
  }
}
