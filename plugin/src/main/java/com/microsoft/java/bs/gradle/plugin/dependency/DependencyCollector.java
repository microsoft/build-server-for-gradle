// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.plugin.dependency;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ArtifactResolutionResult;
import org.gradle.api.artifacts.result.ArtifactResult;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.JvmLibrary;
import org.gradle.language.base.artifact.SourcesArtifact;
import org.gradle.language.java.artifact.JavadocArtifact;

import com.microsoft.java.bs.gradle.model.Artifact;
import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.impl.DefaultArtifact;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleModuleDependency;
import org.gradle.util.GradleVersion;

/**
 * Collects dependencies from a {@link SourceSet}.
 */
public class DependencyCollector {

  private static final String UNKNOWN = "unknown";

  /**
   * Resolve and collect dependencies from a {@link SourceSet}.
   */
  public static Set<GradleModuleDependency> getModuleDependencies(Project project,
      Set<String> configurationNames) {
    if (GradleVersion.current().compareTo(GradleVersion.version("4.0")) < 0) {
      try {
        List<ResolvedConfiguration> configs = project.getConfigurations().stream()
            .filter(configuration -> configurationNames.contains(configuration.getName()))
              .map(Configuration::getResolvedConfiguration)
            .collect(Collectors.toList());
        Stream<DefaultGradleModuleDependency> dependencies = configs.stream()
            .flatMap(config -> config.getResolvedArtifacts().stream())
            .map(artifact -> getArtifact(project, artifact));

        // add as individual files for direct dependencies on jars
        // ResolvedConfiguration.getFiles(Spec) was removed in Gradle 9.0,
        // use reflection for compatibility with older Gradle versions
        Stream<DefaultGradleModuleDependency> directDependencies = configs.stream()
            .flatMap(config -> {
              try {
                java.lang.reflect.Method getFiles = config.getClass()
                    .getMethod("getFiles", org.gradle.api.specs.Spec.class);
                @SuppressWarnings("unchecked")
                Set<File> files = (Set<File>) getFiles.invoke(config, Specs.satisfyAll());
                return files.stream();
              } catch (Exception e) {
                return Stream.<File>empty();
              }
            })
            .map(DependencyCollector::getFileDependency);
        return Stream.concat(dependencies, directDependencies)
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
      } catch (GradleException ex) {
        // handle build with unresolvable dependencies e.g. missing repository
        return new HashSet<>();
      }
    } else {
      return project.getConfigurations()
        .stream()
        .filter(configuration -> configurationNames.contains(configuration.getName()))
        .filter(Configuration::isCanBeResolved)
        .flatMap(configuration -> getConfigurationArtifacts(configuration).stream())
        .map(artifactResult -> getArtifact(project, artifactResult))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    }
  }

  private static DefaultGradleModuleDependency getArtifact(Project project,
      ResolvedArtifactResult artifactResult) {
    ComponentArtifactIdentifier id = artifactResult.getId();
    return getArtifact(project, id, artifactResult.getFile());
  }

  private static DefaultGradleModuleDependency getArtifact(Project project,
      ResolvedArtifact resolvedArtifact) {
    ComponentArtifactIdentifier id = resolvedArtifact.getId();
    return getArtifact(project, id, resolvedArtifact.getFile());
  }

  private static DefaultGradleModuleDependency getArtifact(Project project,
      ComponentArtifactIdentifier id, File artifactFile) {
    // Use public API types instead of internal Gradle classes
    // (ModuleComponentArtifactIdentifier, OpaqueComponentArtifactIdentifier,
    // ComponentFileArtifactIdentifier) which may be relocated across versions
    if (id.getComponentIdentifier() instanceof ModuleComponentIdentifier) {
      return getModuleArtifactDependency(project,
          (ModuleComponentIdentifier) id.getComponentIdentifier(), artifactFile);
    }
    return getFileArtifactDependency(id.getDisplayName(), artifactFile);
  }

  private static List<ResolvedArtifactResult> getConfigurationArtifacts(Configuration config) {
    return new ArrayList<>(config.getIncoming()
        .artifactView(viewConfiguration -> {
          viewConfiguration.lenient(true);
          viewConfiguration.componentFilter(Specs.satisfyAll());
        })
        .getArtifacts() // get ArtifactCollection from ArtifactView.
        .getArtifacts());
  }

  private static DefaultGradleModuleDependency getModuleArtifactDependency(Project project,
      ModuleComponentIdentifier componentId, File resolvedArtifactFile) {
    @SuppressWarnings({"unchecked", "UnstableApiUsage"})
    ArtifactResolutionResult resolutionResult = project.getDependencies()
        .createArtifactResolutionQuery()
        .forComponents(componentId)
        .withArtifacts(
          JvmLibrary.class /* componentType */,
          JavadocArtifact.class, SourcesArtifact.class /*artifactTypes*/
        )
        .execute();

    List<Artifact> artifacts = new LinkedList<>();
    if (resolvedArtifactFile != null) {
      artifacts.add(new DefaultArtifact(resolvedArtifactFile.toURI(), null));
    }

    Set<ComponentArtifactsResult> resolvedComponents = resolutionResult.getResolvedComponents();
    File sourceJar = getNonClassesArtifact(resolvedComponents, SourcesArtifact.class);
    if (sourceJar != null) {
      artifacts.add(new DefaultArtifact(sourceJar.toURI(), "sources"));
    }

    File javaDocJar = getNonClassesArtifact(resolvedComponents, JavadocArtifact.class);
    if (javaDocJar != null) {
      artifacts.add(new DefaultArtifact(javaDocJar.toURI(), "javadoc"));
    }

    return new DefaultGradleModuleDependency(
        componentId.getGroup(),
        componentId.getModule(),
        componentId.getVersion(),
        artifacts
    );
  }

  private static File getNonClassesArtifact(Set<ComponentArtifactsResult> resolvedComponents,
      Class<? extends org.gradle.api.component.Artifact> artifactClass) {
    for (ComponentArtifactsResult component : resolvedComponents) {
      Set<ArtifactResult> artifacts = component.getArtifacts(artifactClass);
      for (ArtifactResult artifact : artifacts) {
        if (artifact instanceof ResolvedArtifactResult) {
          // TODO: only return the first found result, might be wrong!
          return ((ResolvedArtifactResult) artifact).getFile();
        }
      }
    }
    return null;
  }

  private static DefaultGradleModuleDependency getFileDependency(File resolvedArtifactFile) {
    return getFileArtifactDependency(
            resolvedArtifactFile.getName(),
            resolvedArtifactFile
    );
  }

  private static DefaultGradleModuleDependency getFileArtifactDependency(String displayName,
      File resolvedArtifactFile) {
    List<Artifact> artifacts = new LinkedList<>();
    if (resolvedArtifactFile != null) {
      artifacts.add(new DefaultArtifact(resolvedArtifactFile.toURI(), null));
    }
  
    return new DefaultGradleModuleDependency(
        UNKNOWN,
        displayName,
        UNKNOWN,
        artifacts
    );
  }
}
