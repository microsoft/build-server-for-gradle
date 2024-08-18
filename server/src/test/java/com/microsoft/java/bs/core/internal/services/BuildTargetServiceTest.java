// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.epfl.scala.bsp4j.DependencySourcesParams;
import ch.epfl.scala.bsp4j.DependencySourcesResult;
import ch.epfl.scala.bsp4j.JvmBuildTarget;
import ch.epfl.scala.bsp4j.ScalaBuildTarget;
import ch.epfl.scala.bsp4j.ScalacOptionsParams;
import ch.epfl.scala.bsp4j.ScalacOptionsResult;
import com.microsoft.java.bs.gradle.model.ScalaExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.internal.gradle.GradleApiConnector;
import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.core.internal.model.Preferences;
import com.microsoft.java.bs.gradle.model.Artifact;
import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.JavaExtension;
import com.microsoft.java.bs.gradle.model.LanguageExtension;
import com.microsoft.java.bs.gradle.model.SupportedLanguages;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.DependencyModule;
import ch.epfl.scala.bsp4j.DependencyModulesParams;
import ch.epfl.scala.bsp4j.DependencyModulesResult;
import ch.epfl.scala.bsp4j.JavacOptionsParams;
import ch.epfl.scala.bsp4j.JavacOptionsResult;
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

  private BuildTargetManager buildTargetManager;
  private GradleApiConnector connector;
  private PreferenceManager preferenceManager;

  @BeforeEach
  void setUp() {
    buildTargetManager = mock(BuildTargetManager.class);
    connector = mock(GradleApiConnector.class);
    preferenceManager = mock(PreferenceManager.class);
    Preferences preferences = new Preferences();
    when(preferenceManager.getPreferences()).thenReturn(preferences);
  }

  @Test
  void testJvmWorkspaceBuildTargets() {
    BuildTarget target = mock(BuildTarget.class);
    when(target.getBaseDirectory()).thenReturn("foo/bar");
    when(target.getDataKind()).thenReturn("jvm");
    when(target.getData()).thenReturn(new JvmBuildTarget(null, null));
    GradleBuildTarget gradleBuildTarget = new GradleBuildTarget(target,
        mock(GradleSourceSet.class));
    when(buildTargetManager.getAllGradleBuildTargets())
        .thenReturn(Arrays.asList(gradleBuildTarget));
    
    BuildTargetService buildTargetService = new BuildTargetService(buildTargetManager,
        connector, preferenceManager);

    WorkspaceBuildTargetsResult response = buildTargetService.getWorkspaceBuildTargets();

    assertEquals(1, response.getTargets().size());
    assertEquals("foo/bar", response.getTargets().get(0).getBaseDirectory());
    assertEquals("jvm", response.getTargets().get(0).getDataKind());
    assertInstanceOf(JvmBuildTarget.class, response.getTargets().get(0).getData());
  }

  @Test
  void testScalaWorkspaceBuildTargets() {
    BuildTarget target = mock(BuildTarget.class);
    when(target.getBaseDirectory()).thenReturn("foo/bar");
    when(target.getDataKind()).thenReturn("scala");
    when(target.getData()).thenReturn(new ScalaBuildTarget(null, null, null, null, null));
    GradleBuildTarget gradleBuildTarget = new GradleBuildTarget(target,
            mock(GradleSourceSet.class));
    when(buildTargetManager.getAllGradleBuildTargets())
            .thenReturn(Arrays.asList(gradleBuildTarget));

    BuildTargetService buildTargetService = new BuildTargetService(buildTargetManager,
            connector, preferenceManager);

    WorkspaceBuildTargetsResult response = buildTargetService.getWorkspaceBuildTargets();

    assertEquals(1, response.getTargets().size());
    assertEquals("foo/bar", response.getTargets().get(0).getBaseDirectory());
    assertEquals("scala", response.getTargets().get(0).getDataKind());
    assertInstanceOf(ScalaBuildTarget.class, response.getTargets().get(0).getData());
  }

  @Test
  void testGetBuildTargetSources() {
    GradleBuildTarget gradleBuildTarget = mock(GradleBuildTarget.class);
    when(buildTargetManager.getGradleBuildTarget(any())).thenReturn(gradleBuildTarget);

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

    BuildTargetService buildTargetService = new BuildTargetService(buildTargetManager,
        connector, preferenceManager);
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
    GradleBuildTarget gradleBuildTarget = mock(GradleBuildTarget.class);
    when(buildTargetManager.getGradleBuildTarget(any())).thenReturn(gradleBuildTarget);

    GradleSourceSet gradleSourceSet = mock(GradleSourceSet.class);
    when(gradleBuildTarget.getSourceSet()).thenReturn(gradleSourceSet);

    File resourceDir = new File(("resourceDir"));
    Set<File> resourceDirs = new HashSet<>();
    resourceDirs.add(resourceDir);

    when(gradleSourceSet.getResourceDirs()).thenReturn(resourceDirs);

    BuildTargetService buildTargetService = new BuildTargetService(buildTargetManager,
        connector, preferenceManager);
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
    GradleBuildTarget gradleBuildTarget = mock(GradleBuildTarget.class);
    when(buildTargetManager.getGradleBuildTarget(any())).thenReturn(gradleBuildTarget);

    GradleSourceSet gradleSourceSet = mock(GradleSourceSet.class);
    when(gradleBuildTarget.getSourceSet()).thenReturn(gradleSourceSet);

    File sourceOutputDir = new File(("sourceOutputDir"));
    Set<File> sourceOutputDirs = new HashSet<>();
    sourceOutputDirs.add(sourceOutputDir);
    when(gradleSourceSet.getSourceOutputDirs()).thenReturn(sourceOutputDirs);
    File resourceOutputDir = new File(("resourceOutputDir"));
    Set<File> resourceOutputDirs = new HashSet<>();
    resourceOutputDirs.add(resourceOutputDir);
    when(gradleSourceSet.getResourceOutputDirs()).thenReturn(resourceOutputDirs);

    BuildTargetService buildTargetService = new BuildTargetService(buildTargetManager,
        connector, preferenceManager);
    OutputPathsResult  outputPathsResult = buildTargetService.getBuildTargetOutputPaths(
        new OutputPathsParams(Arrays.asList(new BuildTargetIdentifier("test"))));
    assertEquals(1, outputPathsResult.getItems().size());
    assertEquals(2, outputPathsResult.getItems().get(0).getOutputPaths().size());
  }

  @Test
  void testGetBuildTargetDependencySources() {
    GradleBuildTarget gradleBuildTarget = mock(GradleBuildTarget.class);
    when(buildTargetManager.getGradleBuildTarget(any())).thenReturn(gradleBuildTarget);

    GradleSourceSet gradleSourceSet = mock(GradleSourceSet.class);
    when(gradleBuildTarget.getSourceSet()).thenReturn(gradleSourceSet);

    Set<GradleModuleDependency> moduleDependencies = getGradleModuleDependencies();
    when(gradleSourceSet.getModuleDependencies()).thenReturn(moduleDependencies);

    BuildTargetService buildTargetService = new BuildTargetService(buildTargetManager,
            connector, preferenceManager);
    DependencySourcesResult res = buildTargetService.getBuildTargetDependencySources(
            new DependencySourcesParams(Arrays.asList(new BuildTargetIdentifier("test"))));
    assertEquals(1, res.getItems().size());

    List<String> sources = res.getItems().get(0).getSources();
    assertEquals(1, sources.size());
  }

  @Test
  void testGetBuildTargetDependencyModules() {
    GradleBuildTarget gradleBuildTarget = mock(GradleBuildTarget.class);
    when(buildTargetManager.getGradleBuildTarget(any())).thenReturn(gradleBuildTarget);

    GradleSourceSet gradleSourceSet = mock(GradleSourceSet.class);
    when(gradleBuildTarget.getSourceSet()).thenReturn(gradleSourceSet);

    Set<GradleModuleDependency> moduleDependencies = getGradleModuleDependencies();
    when(gradleSourceSet.getModuleDependencies()).thenReturn(moduleDependencies);

    BuildTargetService buildTargetService = new BuildTargetService(buildTargetManager,
        connector, preferenceManager);
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

  private static Set<GradleModuleDependency> getGradleModuleDependencies() {
    GradleModuleDependency moduleDependency = new GradleModuleDependency() {
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
    Set<GradleModuleDependency> moduleDependencies = new HashSet<>();
    moduleDependencies.add(moduleDependency);
    return moduleDependencies;
  }

  @Test
  void testGetJavacOptions() {
    GradleBuildTarget gradleBuildTarget = mock(GradleBuildTarget.class);
    when(buildTargetManager.getGradleBuildTarget(any())).thenReturn(gradleBuildTarget);

    GradleSourceSet gradleSourceSet = mock(GradleSourceSet.class);
    when(gradleBuildTarget.getSourceSet()).thenReturn(gradleSourceSet);

    JavaExtension mockedJavaExtension = mock(JavaExtension.class);
    when(mockedJavaExtension.isJavaExtension()).thenReturn(true);
    when(mockedJavaExtension.getAsJavaExtension()).thenReturn(mockedJavaExtension);
    List<String> compilerArgs = new ArrayList<>();
    compilerArgs.add("--add-opens");
    compilerArgs.add("java.base/java.lang=ALL-UNNAMED");
    when(mockedJavaExtension.getCompilerArgs()).thenReturn(compilerArgs);
    Map<String, LanguageExtension> extensions = new HashMap<>();
    extensions.put(SupportedLanguages.JAVA.getBspName(), mockedJavaExtension);
    when(gradleSourceSet.getExtensions()).thenReturn(extensions);

    BuildTargetService buildTargetService = new BuildTargetService(buildTargetManager,
        connector, preferenceManager);
    JavacOptionsResult javacOptions = buildTargetService.getBuildTargetJavacOptions(
        new JavacOptionsParams(Arrays.asList(new BuildTargetIdentifier("test"))));
  
    assertEquals(1, javacOptions.getItems().size());
    assertEquals(2, javacOptions.getItems().get(0).getOptions().size());
  }

  @Test
  void testGetScalacOptions() {
    GradleBuildTarget gradleBuildTarget = mock(GradleBuildTarget.class);
    when(buildTargetManager.getGradleBuildTarget(any())).thenReturn(gradleBuildTarget);

    GradleSourceSet gradleSourceSet = mock(GradleSourceSet.class);
    when(gradleBuildTarget.getSourceSet()).thenReturn(gradleSourceSet);

    List<String> compilerArgs = new ArrayList<>();
    compilerArgs.add("-deprecation");
    compilerArgs.add("-unchecked");
    compilerArgs.add("-encoding");
    compilerArgs.add("utf8");
    ScalaExtension mockedScalaExtension = mock(ScalaExtension.class);
    when(mockedScalaExtension.isScalaExtension()).thenReturn(true);
    when(mockedScalaExtension.getAsScalaExtension()).thenReturn(mockedScalaExtension);
    when(mockedScalaExtension.getScalaCompilerArgs()).thenReturn(compilerArgs);
    Map<String, LanguageExtension> extensions = new HashMap<>();
    extensions.put(SupportedLanguages.SCALA.getBspName(), mockedScalaExtension);
    when(gradleSourceSet.getExtensions()).thenReturn(extensions);

    BuildTargetService buildTargetService = new BuildTargetService(buildTargetManager,
            connector, preferenceManager);
    ScalacOptionsResult scalacOptions = buildTargetService.getBuildTargetScalacOptions(
            new ScalacOptionsParams(Arrays.asList(new BuildTargetIdentifier("test"))));

    assertEquals(1, scalacOptions.getItems().size());
    assertEquals(4, scalacOptions.getItems().get(0).getOptions().size());
  }
}
