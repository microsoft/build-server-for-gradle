package com.microsoft.java.bs.core.internal.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.gradle.model.Artifact;
import com.microsoft.java.bs.gradle.model.ModuleDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.DependencyModule;
import ch.epfl.scala.bsp4j.DependencyModulesParams;
import ch.epfl.scala.bsp4j.DependencyModulesResult;
import ch.epfl.scala.bsp4j.MavenDependencyModule;
import ch.epfl.scala.bsp4j.MavenDependencyModuleArtifact;
import ch.epfl.scala.bsp4j.OutputPathsParams;
import ch.epfl.scala.bsp4j.OutputPathsResult;
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

  @Test
  void testGetBuildTargetOutputPaths() {
    BuildTargetManager manager = mock(BuildTargetManager.class);
    GradleBuildTarget gradleBuildTarget = mock(GradleBuildTarget.class);
    when(manager.getGradleBuildTarget(any())).thenReturn(gradleBuildTarget);

    GradleSourceSet gradleSourceSet = mock(GradleSourceSet.class);
    when(gradleBuildTarget.getSourceSet()).thenReturn(gradleSourceSet);

    File sourceOutputDir = new File(("sourceOutputDir"));
    when(gradleSourceSet.getSourceOutputDir()).thenReturn(sourceOutputDir);
    File resourceOutputDir = new File(("resourceOutputDir"));
    when(gradleSourceSet.getResourceOutputDir()).thenReturn(resourceOutputDir);

    BuildTargetService buildTargetService = new BuildTargetService(manager);
    OutputPathsResult  outputPathsResult = buildTargetService.getBuildTargetOutputPaths(
        new OutputPathsParams(Arrays.asList(new BuildTargetIdentifier("test"))));
    assertEquals(1, outputPathsResult.getItems().size());
    assertEquals(2, outputPathsResult.getItems().get(0).getOutputPaths().size());
  }

  @Test
  void testGetBuildTargetDependencyModules() {
    BuildTargetManager manager = mock(BuildTargetManager.class);
    GradleBuildTarget gradleBuildTarget = mock(GradleBuildTarget.class);
    when(manager.getGradleBuildTarget(any())).thenReturn(gradleBuildTarget);

    GradleSourceSet gradleSourceSet = mock(GradleSourceSet.class);
    when(gradleBuildTarget.getSourceSet()).thenReturn(gradleSourceSet);

    ModuleDependency moduleDependency = new ModuleDependency() {
      @Override
      public String getGroup() {
        return "group";
      }

      @Override
      public String getModule() {
        return "module";
      }

      @Override
      public String getVersion() {
        return "1.0.0";
      }

      @Override
      public List<Artifact> getArtifacts() {
        return Arrays.asList(new Artifact() {
          @Override
          public URI getUri() {
            return new File("artifact").toURI();
          }

          @Override
          public String getClassifier() {
            return "sources";
          }
        });
      }
    };
    Set<ModuleDependency> moduleDependencies = new HashSet<>();
    moduleDependencies.add(moduleDependency);
    when(gradleSourceSet.getModuleDependencies()).thenReturn(moduleDependencies);

    BuildTargetService buildTargetService = new BuildTargetService(manager);
    DependencyModulesResult res = buildTargetService.getBuildTargetDependencyModules(
        new DependencyModulesParams(Arrays.asList(new BuildTargetIdentifier("test"))));
    assertEquals(1, res.getItems().size());

    List<DependencyModule> modules = res.getItems().get(0).getModules();
    assertEquals(1, modules.size());
    
    MavenDependencyModule module = (MavenDependencyModule) modules.get(0).getData();
    assertEquals("group", module.getOrganization());
    assertEquals("module", module.getName());
    assertEquals("1.0.0", module.getVersion());
    assertEquals(1, module.getArtifacts().size());

    MavenDependencyModuleArtifact artifact = module.getArtifacts().get(0);
    assertEquals("sources", artifact.getClassifier());
  }
}
