package com.microsoft.java.bs.core.internal.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.ResourcesParams;
import ch.epfl.scala.bsp4j.ResourcesResult;
import ch.epfl.scala.bsp4j.SourcesParams;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;

class BuildTargetServiceTest {

  @Test
  void testWorkspaceBuildTargets() {
    BuildTargetManager manager = mock(BuildTargetManager.class);
    BuildTarget target = mock(BuildTarget.class);
    when(target.getBaseDirectory()).thenReturn("foo/bar");
    GradleBuildTarget gradleBuildTarget = new GradleBuildTarget(target,
        mock(GradleSourceSet.class));
    when(manager.getAllGradleBuildTargets()).thenReturn(Arrays.asList(gradleBuildTarget));
    
    BuildTargetService buildTargetService = new BuildTargetService(manager);

    WorkspaceBuildTargetsResult response = buildTargetService.getWorkspaceBuildTargets();

    assertEquals(1, response.getTargets().size());
    assertEquals("foo/bar", response.getTargets().get(0).getBaseDirectory());
  }

  @Test
  void testGetBuildTargetSources() {
    BuildTargetManager manager = mock(BuildTargetManager.class);
    GradleBuildTarget gradleBuildTarget = mock(GradleBuildTarget.class);
    when(manager.getGradleBuildTarget(any())).thenReturn(gradleBuildTarget);

    GradleSourceSet gradleSourceSet = mock(GradleSourceSet.class);
    when(gradleBuildTarget.getSourceSet()).thenReturn(gradleSourceSet);

    File srcDir = new File(("srcDir"));
    Set<File> srcDirs = new HashSet<>();
    srcDirs.add(srcDir);

    File generatedSrcDir = new File(("generatedSrcDir"));
    Set<File> generatedSrcDirs = new HashSet<>();
    generatedSrcDirs.add(generatedSrcDir);

    when(gradleSourceSet.getSourceDirs()).thenReturn(srcDirs);
    when(gradleSourceSet.getGeneratedSourceDirs()).thenReturn(generatedSrcDirs);

    BuildTargetService buildTargetService = new BuildTargetService(manager);
    SourcesResult buildTargetSources = buildTargetService.getBuildTargetSources(new SourcesParams(
            Arrays.asList(new BuildTargetIdentifier("test"))));
    buildTargetSources.getItems().forEach(item -> {
      item.getSources().forEach(sourceItem -> {
        if (sourceItem.getGenerated()) {
          assertTrue(sourceItem.getUri().contains("generatedSrcDir"));
        } else {
          assertTrue(sourceItem.getUri().contains("srcDir"));
        }
      });
    });
  }

  @Test
  void testGetBuildTargetResources() {
    BuildTargetManager manager = mock(BuildTargetManager.class);
    GradleBuildTarget gradleBuildTarget = mock(GradleBuildTarget.class);
    when(manager.getGradleBuildTarget(any())).thenReturn(gradleBuildTarget);

    GradleSourceSet gradleSourceSet = mock(GradleSourceSet.class);
    when(gradleBuildTarget.getSourceSet()).thenReturn(gradleSourceSet);

    File resourceDir = new File(("resourceDir"));
    Set<File> resourceDirs = new HashSet<>();
    resourceDirs.add(resourceDir);

    when(gradleSourceSet.getResourceDirs()).thenReturn(resourceDirs);

    BuildTargetService buildTargetService = new BuildTargetService(manager);
    ResourcesResult buildTargetResources = buildTargetService.getBuildTargetResources(
        new ResourcesParams(Arrays.asList(new BuildTargetIdentifier("test"))));
    buildTargetResources.getItems().forEach(item -> {
      item.getResources().forEach(resource -> {
        assertTrue(resource.contains("resourceDir"));
      });
    });
  }
}
