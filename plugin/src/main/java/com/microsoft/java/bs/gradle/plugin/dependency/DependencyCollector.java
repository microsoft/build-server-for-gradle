// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.plugin.dependency;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.result.ArtifactResolutionResult;
import org.gradle.api.artifacts.result.ArtifactResult;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.SourceSet;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;
import org.gradle.internal.component.local.model.ComponentFileArtifactIdentifier;
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier;
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

  private final Project project;
  private final Set<File> exclusionFromDependencies;
  private final Set<GradleModuleDependency> moduleDependencies;

  /**
   * Instantiates a new dependency collector.
   */
  public DependencyCollector(Project project, Set<File> exclusionFromDependencies) {
    this.project = project;
    this.exclusionFromDependencies = exclusionFromDependencies;
    this.moduleDependencies = new LinkedHashSet<>();
  }

  public Set<GradleModuleDependency> getModuleDependencies() {
    return moduleDependencies;
  }

  /**
   * Resolve and collect dependencies from a {@link SourceSet}.
   */
  public void collectByConfigurationNames(Set<String> configurationNames) {
    if (GradleVersion.current().compareTo(GradleVersion.version("3.3")) < 0) {
      List<ResolvedConfiguration> configs = project.getConfigurations().stream()
              .filter(configuration -> configurationNames.contains(configuration.getName()))
              .map(Configuration::getResolvedConfiguration)
              .collect(Collectors.toList());
      configs.stream().flatMap(config -> config.getResolvedArtifacts().stream())
              .forEach(this::resolveArtifact);

      // add as individual files for direct dependencies on jars
      configs.stream().flatMap(config -> config.getFiles(Specs.satisfyAll()).stream())
              .forEach(this::resolveFileDependency);
    } else {
      project.getConfigurations()
              .stream()
              .filter(configuration -> configurationNames.contains(configuration.getName()))
              .filter(Configuration::isCanBeResolved)
              .flatMap(configuration -> getConfigurationArtifacts(configuration).stream())
              .filter(artifact -> !exclusionFromDependencies.contains(artifact.getFile()))
              .forEach(this::resolveArtifact);
    }
  }

  private void resolveArtifact(ResolvedArtifactResult artifactResult) {
    ComponentArtifactIdentifier id = artifactResult.getId();
    resolveArtifact(id, artifactResult.getFile());
  }

  private void resolveArtifact(ResolvedArtifact resolvedArtifact) {
    ComponentArtifactIdentifier id = resolvedArtifact.getId();
    resolveArtifact(id, resolvedArtifact.getFile());
  }

  private void resolveArtifact(ComponentArtifactIdentifier id, File artifactFile) {
    if (id instanceof ModuleComponentArtifactIdentifier) {
      resolveModuleArtifactDependency((ModuleComponentArtifactIdentifier) id, artifactFile);
    } else if (id instanceof OpaqueComponentArtifactIdentifier) {
      resolveFileArtifactDependency((OpaqueComponentArtifactIdentifier) id, artifactFile);
    } else if (id instanceof ComponentFileArtifactIdentifier) {
      resolveFileArtifactDependency((ComponentFileArtifactIdentifier) id, artifactFile);
    }
  }

  private List<ResolvedArtifactResult> getConfigurationArtifacts(Configuration config) {
    return config.getIncoming()
            .artifactView(viewConfiguration -> {
              viewConfiguration.lenient(true);
              viewConfiguration.componentFilter(Specs.<ComponentIdentifier>satisfyAll());
            })
            .getArtifacts() // get ArtifactCollection from ArtifactView.
            .getArtifacts() // get a set of ResolvedArtifactResult from ArtifactCollection.
           .stream()
           .collect(Collectors.toList());
  }

  private void resolveModuleArtifactDependency(ModuleComponentArtifactIdentifier artifactIdentifier,
      File resolvedArtifactFile) {
    ArtifactResolutionResult resolutionResult = project.getDependencies()
        .createArtifactResolutionQuery()
        .forComponents(artifactIdentifier.getComponentIdentifier())
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
    File sourceJar = getArtifact(resolvedComponents, SourcesArtifact.class);
    if (sourceJar != null) {
      artifacts.add(new DefaultArtifact(sourceJar.toURI(), "sources"));
    }

    File javaDocJar = getArtifact(resolvedComponents, JavadocArtifact.class);
    if (javaDocJar != null) {
      artifacts.add(new DefaultArtifact(javaDocJar.toURI(), "javadoc"));
    }

    moduleDependencies.add(new DefaultGradleModuleDependency(
        artifactIdentifier.getComponentIdentifier().getGroup(),
        artifactIdentifier.getComponentIdentifier().getModule(),
        artifactIdentifier.getComponentIdentifier().getVersion(),
        artifacts
    ));
  }

  private File getArtifact(Set<ComponentArtifactsResult> resolvedComponents,
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

  private void resolveFileArtifactDependency(ComponentFileArtifactIdentifier artifactIdentifier,
      File resolvedArtifactFile) {
    moduleDependencies.add(getFileArtifactDependency(
        artifactIdentifier.getCapitalizedDisplayName(),
        resolvedArtifactFile
    ));
  }

  private void resolveFileArtifactDependency(OpaqueComponentArtifactIdentifier artifactIdentifier,
      File resolvedArtifactFile) {
    moduleDependencies.add(getFileArtifactDependency(
        artifactIdentifier.getCapitalizedDisplayName(),
        resolvedArtifactFile
    ));
  }

  private void resolveFileDependency(File resolvedArtifactFile) {
    moduleDependencies.add(getFileArtifactDependency(
            resolvedArtifactFile.getName(),
            resolvedArtifactFile
    ));
  }

  private GradleModuleDependency getFileArtifactDependency(String displayName,
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
