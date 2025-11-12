// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.plugin.dependency;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
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

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.stream.Stream;

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
      return getModuleDependenciesForOldGradle(project, configurationNames);
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

  /**
   * Support for Gradle versions before 4.0 using reflection.
   */
  @SuppressWarnings("unchecked")
  private static Set<GradleModuleDependency> getModuleDependenciesForOldGradle(Project project,
      Set<String> configurationNames) {
    try {
      List<Object> configs = project.getConfigurations().stream()
          .filter(configuration -> configurationNames.contains(configuration.getName()))
          .map(configuration -> {
            try {
              Method getResolvedConfiguration = Configuration.class
                  .getMethod("getResolvedConfiguration");
              return getResolvedConfiguration.invoke(configuration);
            } catch (Exception e) {
              return null;
            }
          })
          .filter(Objects::nonNull)
          .collect(Collectors.toList());

      Stream<DefaultGradleModuleDependency> dependencies = configs.stream()
          .flatMap(config -> {
            try {
              Method getResolvedArtifacts = config.getClass()
                  .getMethod("getResolvedArtifacts");
              Set<?> artifacts = (Set<?>) getResolvedArtifacts.invoke(config);
              return artifacts.stream().map(artifact -> {
                try {
                  Method getId = artifact.getClass().getMethod("getId");
                  Method getFile = artifact.getClass().getMethod("getFile");
                  ComponentArtifactIdentifier id = 
                      (ComponentArtifactIdentifier) getId.invoke(artifact);
                  File file = (File) getFile.invoke(artifact);
                  return getArtifact(project, id, file);
                } catch (Exception e) {
                  return null;
                }
              });
            } catch (Exception e) {
              return Stream.empty();
            }
          })
          .filter(Objects::nonNull);

      // add as individual files for direct dependencies on jars
      Stream<DefaultGradleModuleDependency> directDependencies = configs.stream()
          .flatMap(config -> {
            try {
              Method getFiles = config.getClass().getMethod("getFiles", 
                  Class.forName("org.gradle.api.specs.Spec"));
              Object filesObj = getFiles.invoke(config, Specs.satisfyAll());
              Set<File> files = (Set<File>) filesObj;
              return files.stream().map(DependencyCollector::getFileDependency);
            } catch (Exception e) {
              return Stream.empty();
            }
          })
          .filter(Objects::nonNull);

      return Stream.concat(dependencies, directDependencies)
          .filter(Objects::nonNull)
          .collect(Collectors.toSet());
    } catch (Exception ex) {
      // handle build with unresolvable dependencies e.g. missing repository
      return new HashSet<>();
    }
  }

  private static DefaultGradleModuleDependency getFileDependency(File resolvedArtifactFile) {
    return getFileArtifactDependency(
        resolvedArtifactFile.getName(),
        resolvedArtifactFile
    );
  }

  private static DefaultGradleModuleDependency getArtifact(Project project,
      ResolvedArtifactResult artifactResult) {
    ComponentArtifactIdentifier id = artifactResult.getId();
    return getArtifact(project, id, artifactResult.getFile());
  }

  private static DefaultGradleModuleDependency getArtifact(Project project,
      ComponentArtifactIdentifier id, File artifactFile) {
    if (id instanceof ModuleComponentArtifactIdentifier) {
      return getModuleArtifactDependency(project, (ModuleComponentArtifactIdentifier) id,
        artifactFile);
    }
    if (id instanceof OpaqueComponentArtifactIdentifier) {
      return getFileArtifactDependency((OpaqueComponentArtifactIdentifier) id, artifactFile);
    }
    if (id instanceof ComponentFileArtifactIdentifier) {
      return getFileArtifactDependency((ComponentFileArtifactIdentifier) id, artifactFile);
    }
    return null;
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
      ModuleComponentArtifactIdentifier artifactIdentifier, File resolvedArtifactFile) {
    @SuppressWarnings({"unchecked", "UnstableApiUsage"})
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
    File sourceJar = getNonClassesArtifact(resolvedComponents, SourcesArtifact.class);
    if (sourceJar != null) {
      artifacts.add(new DefaultArtifact(sourceJar.toURI(), "sources"));
    }

    File javaDocJar = getNonClassesArtifact(resolvedComponents, JavadocArtifact.class);
    if (javaDocJar != null) {
      artifacts.add(new DefaultArtifact(javaDocJar.toURI(), "javadoc"));
    }

    return new DefaultGradleModuleDependency(
        artifactIdentifier.getComponentIdentifier().getGroup(),
        artifactIdentifier.getComponentIdentifier().getModule(),
        artifactIdentifier.getComponentIdentifier().getVersion(),
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

  private static DefaultGradleModuleDependency getFileArtifactDependency(
      ComponentFileArtifactIdentifier artifactIdentifier, File resolvedArtifactFile) {
    return getFileArtifactDependency(
        artifactIdentifier.getCapitalizedDisplayName(),
        resolvedArtifactFile
    );
  }

  private static DefaultGradleModuleDependency getFileArtifactDependency(
      OpaqueComponentArtifactIdentifier artifactIdentifier, File resolvedArtifactFile) {
    return getFileArtifactDependency(
        artifactIdentifier.getCapitalizedDisplayName(),
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
