package com.microsoft.java.bs.contrib.gradle.plugin.dependency;

import java.util.Set;

import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier;

import com.microsoft.java.bs.contrib.gradle.model.ModuleDependency;
import com.microsoft.java.bs.contrib.gradle.model.ProjectDependency;

/**
 * Dependency visitor.
 */
public interface DependencyVisitor {

  void visit(ModuleComponentArtifactIdentifier artifactIdentifier,
        ResolvedArtifactResult artifactResult);

  void visit(ProjectComponentIdentifier projectIdentifier);

  void visit(OpaqueComponentArtifactIdentifier artifactIdentifier,
        ResolvedArtifactResult artifactResult);

  Set<ModuleDependency> getModuleDependencies();

  Set<ProjectDependency> getProjectDependencies();
}
