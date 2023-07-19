package com.microsoft.java.bs.core.internal.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.core.internal.model.TestGradleSourceSet;
import com.microsoft.java.bs.core.internal.model.TestGradleSourceSets;
import com.microsoft.java.bs.gradle.model.GradleProjectDependency;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.JvmBuildTarget;

class BuildTargetManagerTest {

  @Test
  void testStore() {
    BuildTargetManager manager = new BuildTargetManager();
    TestGradleSourceSets testGradleSourceSets = new TestGradleSourceSets();
    TestGradleSourceSet testGradleSourceSet = new TestGradleSourceSet();
    testGradleSourceSet.setSourceSetName("test");
    testGradleSourceSets.setGradleSourceSets(Arrays.asList(testGradleSourceSet));

    manager.store(testGradleSourceSets);

    List<GradleBuildTarget> list = manager.getAllGradleBuildTargets();
    BuildTarget buildTarget = list.get(0).getBuildTarget();
    assertTrue(buildTarget.getTags().contains("test"));
    assertTrue(buildTarget.getId().getUri().contains("?sourceset=test"));
  }

  @Test
  void testJvmExtension() {
    BuildTargetManager manager = new BuildTargetManager();
    TestGradleSourceSets testGradleSourceSets = new TestGradleSourceSets();
    TestGradleSourceSet testGradleSourceSet = new TestGradleSourceSet();
    testGradleSourceSet.setJavaVersion("17");
    testGradleSourceSets.setGradleSourceSets(Arrays.asList(testGradleSourceSet));
    manager.store(testGradleSourceSets);

    List<GradleBuildTarget> list = manager.getAllGradleBuildTargets();
    BuildTarget buildTarget = list.get(0).getBuildTarget();

    assertEquals("jvm", buildTarget.getDataKind());
    JvmBuildTarget jvmBt = (JvmBuildTarget) buildTarget.getData();
    assertEquals("17", jvmBt.getJavaVersion());
  }

  @Test
  void testBuildTargetDependency() {
    TestGradleSourceSet sourceSetFoo = new TestGradleSourceSet();
    sourceSetFoo.setProjectPath(":foo");
    sourceSetFoo.setProjectDir(new File("foo"));
    TestGradleSourceSet sourceSetBar = new TestGradleSourceSet();
    sourceSetBar.setProjectPath(":bar");
    sourceSetBar.setProjectDir(new File("bar"));
    Set<GradleProjectDependency> dependencies = new HashSet<>();
    dependencies.add(new GradleProjectDependency() {
      @Override
      public String getProjectPath() {
        return ":foo";
      }
    });
    sourceSetBar.setProjectDependencies(dependencies);

    TestGradleSourceSets testGradleSourceSets = new TestGradleSourceSets();
    testGradleSourceSets.setGradleSourceSets(Arrays.asList(sourceSetFoo, sourceSetBar));

    BuildTargetManager manager = new BuildTargetManager();
    manager.store(testGradleSourceSets);

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
}
