package com.microsoft.java.bs.gradle.plugin.dependency;

import java.util.Set;

import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;
import org.gradle.internal.component.local.model.ComponentFileArtifactIdentifier;
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier;

import com.microsoft.java.bs.gradle.model.ModuleDependency;

/**
 * Dependency visitor.
 */
public interface DependencyVisitor {

  /**
   * visit the Maven module artifact dependency.
   */
  void visit(ModuleComponentArtifactIdentifier artifactIdentifier,
      ResolvedArtifactResult artifactResult);

  /**
   * visit the artifact dependency explicitly declared by: 
   * <code>files(xxx.jar)</code> or
   * <code>fileTree('libs') { include '*.jar' }</code>.
   */
  void visit(OpaqueComponentArtifactIdentifier artifactIdentifier,
      ResolvedArtifactResult artifactResult);

  /**
   * visit the artifact dependency injected by plugin, for example, the 'java-gradle-plugin'
   * will inject the Gradle Tooling API artifacts.
   */
  void visit(ComponentFileArtifactIdentifier artifactIdentifier,
      ResolvedArtifactResult artifactResult);

  Set<ModuleDependency> getModuleDependencies();
}
