package com.microsoft.java.bs.gradle.plugin.dependency;

import java.util.List;

import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;
import org.gradle.internal.component.local.model.ComponentFileArtifactIdentifier;
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier;

/**
 * Dependency set which can accept a {@link DependencyVisitor}.
 */
public class DependencySet {
  private List<ResolvedArtifactResult> resolvedResults;

  public DependencySet(List<ResolvedArtifactResult> resolvedResults) {
    this.resolvedResults = resolvedResults;
  }

  /**
   * Accepts a {@link DependencyVisitor}.
   */
  public void accept(DependencyVisitor visitor) {
    for (ResolvedArtifactResult artifactResult : resolvedResults) {
      ComponentArtifactIdentifier id = artifactResult.getId();
      if (id instanceof ModuleComponentArtifactIdentifier) {
        visitor.visit((ModuleComponentArtifactIdentifier) id, artifactResult);
      } else if (id instanceof OpaqueComponentArtifactIdentifier) {
        visitor.visit((OpaqueComponentArtifactIdentifier) id, artifactResult);
      } else if (id instanceof ComponentFileArtifactIdentifier) {
        visitor.visit((ComponentFileArtifactIdentifier) id, artifactResult);
      }
    }
  }
}
