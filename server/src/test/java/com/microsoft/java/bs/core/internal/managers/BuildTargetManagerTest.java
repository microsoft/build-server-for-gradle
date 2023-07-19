package com.microsoft.java.bs.core.internal.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.JvmBuildTarget;

class BuildTargetManagerTest {

  @Test
  void testStore() {
    BuildTargetManager manager = new BuildTargetManager();
    manager.store(new TestGradleSourceSets());

    List<GradleBuildTarget> list = manager.getAllGradleBuildTargets();
    BuildTarget buildTarget = list.get(0).getBuildTarget();
    assertTrue(buildTarget.getTags().contains("test"));
    assertTrue(buildTarget.getId().getUri().contains("?sourceset=test"));
  }

  @Test
  void testJvmExtension() {
    BuildTargetManager manager = new BuildTargetManager();
    manager.store(new TestGradleSourceSets());

    List<GradleBuildTarget> list = manager.getAllGradleBuildTargets();
    BuildTarget buildTarget = list.get(0).getBuildTarget();

    assertEquals("jvm", buildTarget.getDataKind());
    JvmBuildTarget jvmBt = (JvmBuildTarget) buildTarget.getData();
    assertEquals("17", jvmBt.getJavaVersion());
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

    @Override
    public Set<File> getSourceDirs() {
      return Collections.emptySet();
    }

    @Override
    public Set<File> getGeneratedSourceDirs() {
      return Collections.emptySet();
    }

    @Override
    public File getSourceOutputDir() {
      return null;
    }

    @Override
    public Set<File> getResourceDirs() {
      return Collections.emptySet();
    }

    @Override
    public File getResourceOutputDir() {
      return null;
    }

    @Override
    public File getJavaHome() {
      return new File("javaHome");
    }

    @Override
    public String getJavaVersion() {
      return "17";
    }

    @Override
    public Set<GradleModuleDependency> getModuleDependencies() {
      return null;
    }
  }
}
