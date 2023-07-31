package com.microsoft.java.bs.gradle.plugin.dependency;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
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
import com.microsoft.java.bs.gradle.model.GradleProjectDependency;
import com.microsoft.java.bs.gradle.plugin.model.DefaultArtifact;
import com.microsoft.java.bs.gradle.plugin.model.DefaultGradleModuleDependency;
import com.microsoft.java.bs.gradle.plugin.model.DefaultGradleProjectDependency;

/**
 * Collects dependencies from a {@link SourceSet}.
 */
public class DependencyCollector {

  private static final String UNKNOWN = "unknown";

  private Project project;
  private Set<File> exclusionFromDependencies;
  private Set<GradleModuleDependency> moduleDependencies;
  private Set<GradleProjectDependency> projectDependencies;

  /**
   * Instantiates a new dependency collector.
   */
  public DependencyCollector(Project project, Set<File> exclusionFromDependencies) {
    this.project = project;
    this.exclusionFromDependencies = exclusionFromDependencies;
    this.moduleDependencies = new HashSet<>();
    this.projectDependencies = new HashSet<>();
  }

  public Set<GradleModuleDependency> getModuleDependencies() {
    return moduleDependencies;
  }

  public Set<GradleProjectDependency> getProjectDependencies() {
    return projectDependencies;
  }

  /**
   * Resolve and collect dependencies from a {@link SourceSet}.
   */
  public void collectByConfigurationNames(Set<String> configurationNames) {
    List<ResolvedArtifactResult> resolvedResult = project.getConfigurations()
        .stream()
        .filter(configuration -> configurationNames.contains(configuration.getName())
            && configuration.isCanBeResolved())
        .flatMap(configuration -> getConfigurationArtifacts(configuration).stream())
        .filter(artifact -> !exclusionFromDependencies.contains(artifact.getFile()))
        .collect(Collectors.toList());
    for (ResolvedArtifactResult artifactResult : resolvedResult) {
      ComponentArtifactIdentifier id = artifactResult.getId();
      if (id instanceof ModuleComponentArtifactIdentifier) {
        resolveModuleArtifactDependency((ModuleComponentArtifactIdentifier) id, artifactResult);
      } else if (id instanceof OpaqueComponentArtifactIdentifier) {
        resolveFileArtifactDependency((OpaqueComponentArtifactIdentifier) id, artifactResult);
      } else if (id instanceof ComponentFileArtifactIdentifier) {
        resolveFileArtifactDependency((ComponentFileArtifactIdentifier) id, artifactResult);
      } else if (id.getComponentIdentifier() instanceof ProjectComponentIdentifier) {
        resolveProjectDependency((ProjectComponentIdentifier) id.getComponentIdentifier());
      }
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
      ResolvedArtifactResult artifactResult) {
    ArtifactResolutionResult resolutionResult = project.getDependencies()
        .createArtifactResolutionQuery()
        .forComponents(artifactIdentifier.getComponentIdentifier())
        .withArtifacts(
          JvmLibrary.class /* componentType */,
          JavadocArtifact.class, SourcesArtifact.class /*artifactTypes*/
        )
        .execute();

    List<Artifact> artifacts = new LinkedList<>();
    if (artifactResult.getFile() != null) {
      artifacts.add(new DefaultArtifact(artifactResult.getFile().toURI(), null));
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
      ResolvedArtifactResult artifactResult) {
    moduleDependencies.add(getFileArtifactDependency(
        artifactIdentifier.getCapitalizedDisplayName(),
        artifactResult
    ));
  }

  private void resolveFileArtifactDependency(OpaqueComponentArtifactIdentifier artifactIdentifier,
      ResolvedArtifactResult artifactResult) {
    moduleDependencies.add(getFileArtifactDependency(
        artifactIdentifier.getCapitalizedDisplayName(),
        artifactResult
    ));
  }

  private GradleModuleDependency getFileArtifactDependency(String displayName,
      ResolvedArtifactResult artifactResult) {
    List<Artifact> artifacts = new LinkedList<>();
    if (artifactResult.getFile() != null) {
      artifacts.add(new DefaultArtifact(artifactResult.getFile().toURI(), null));
    }
  
    return new DefaultGradleModuleDependency(
        UNKNOWN,
        displayName,
        UNKNOWN,
        artifacts
    );
  }

  private void resolveProjectDependency(ProjectComponentIdentifier id) {
    if (Objects.equals(id.getProjectPath(), project.getPath())) {
      return;
    }

    projectDependencies.add(new DefaultGradleProjectDependency(
        id.getProjectPath()
    ));
  }
}
