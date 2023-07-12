package com.microsoft.java.bs.core.internal.managers;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

import ch.epfl.scala.bsp4j.BuildTarget;

class BuildTargetsManagerTest {

  @Test
  void testStore() {
    BuildTargetsManager manager = new BuildTargetsManager();
    manager.store(new TestGradleSourceSets());

    List<GradleBuildTarget> list = manager.getAllGradleBuildTargets();
    BuildTarget buildTarget = list.get(0).getBuildTarget();
    assertTrue(buildTarget.getTags().contains("test"));
    assertTrue(buildTarget.getId().getUri().contains("?sourceset=test"));
  }

  class TestGradleSourceSets implements GradleSourceSets {

    @Override
    public List<GradleSourceSet> getGradleSourceSets() {
      return Arrays.asList(new TestGradleSourceSet());
    }
  }

  class TestGradleSourceSet implements GradleSourceSet {

    @Override
    public String getProjectName() {
      return "test";
    }

    @Override
    public String getProjectPath() {
      return ":";
    }

    @Override
    public File getProjectDir() {
      return Paths.get(System.getProperty("user.dir")).toFile();
    }

    @Override
    public File getRootDir() {
      return Paths.get(System.getProperty("user.dir")).toFile();
    }

    @Override
    public String getSourceSetName() {
      return "test";
    }
  }
}
