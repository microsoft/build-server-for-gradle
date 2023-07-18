package com.microsoft.java.bs.gradle.plugin.dependency;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.gradle.api.Project;
import org.gradle.api.artifacts.result.ArtifactResolutionResult;
import org.gradle.api.artifacts.result.ArtifactResult;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;
import org.gradle.internal.component.local.model.ComponentFileArtifactIdentifier;
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier;
import org.gradle.jvm.JvmLibrary;
import org.gradle.language.base.artifact.SourcesArtifact;
import org.gradle.language.java.artifact.JavadocArtifact;

import com.microsoft.java.bs.gradle.model.Artifact;
import com.microsoft.java.bs.gradle.model.ArtifactsDependency;
import com.microsoft.java.bs.gradle.plugin.model.DefaultArtifact;
import com.microsoft.java.bs.gradle.plugin.model.DefaultArtifactsDependency;

/**
 * Default implementation of {@link DependencyVisitor}.
 */
public class DefaultDependencyVisitor implements DependencyVisitor {
  private static final String UNKNOWN = "unknown";

  private Project project;
  private Set<ArtifactsDependency> moduleDependencies;

  /**
   * Creates a new instance of {@link DefaultDependencyVisitor}.
   *
   * @param project Gradle project.
   */
  public DefaultDependencyVisitor(Project project) {
    this.project = project;
    moduleDependencies = new HashSet<>();
  }

  @Override
  public void visit(ModuleComponentArtifactIdentifier artifactIdentifier,
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

    moduleDependencies.add(new DefaultArtifactsDependency(
        artifactIdentifier.getComponentIdentifier().getGroup(),
        artifactIdentifier.getComponentIdentifier().getModule(),
        artifactIdentifier.getComponentIdentifier().getVersion(),
        artifacts
    ));
  }

  @Override
  public void visit(ComponentFileArtifactIdentifier artifactIdentifier,
      ResolvedArtifactResult artifactResult) {
    List<Artifact> artifacts = new LinkedList<>();
    if (artifactResult.getFile() != null) {
      artifacts.add(new DefaultArtifact(artifactResult.getFile().toURI(), null));
    }

    moduleDependencies.add(new DefaultArtifactsDependency(
        UNKNOWN,
        artifactIdentifier.getCapitalizedDisplayName(),
        UNKNOWN,
        artifacts
    ));
  }

  @Override
  public void visit(OpaqueComponentArtifactIdentifier artifactIdentifier,
      ResolvedArtifactResult artifactResult) {
    List<Artifact> artifacts = new LinkedList<>();
    if (artifactResult.getFile() != null) {
      artifacts.add(new DefaultArtifact(artifactResult.getFile().toURI(), null));
    }

    moduleDependencies.add(new DefaultArtifactsDependency(
        UNKNOWN,
        artifactIdentifier.getCapitalizedDisplayName(),
        UNKNOWN,
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

  @Override
  public Set<ArtifactsDependency> getArtifactsDependencies() {
    return moduleDependencies;
  }
}
