package com.microsoft.java.bs.gradle.plugin.dependency;

import com.microsoft.java.bs.gradle.model.Artifact;
import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.impl.DefaultArtifact;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleModuleDependency;
import com.microsoft.java.bs.gradle.plugin.utils.AndroidUtils;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.result.ArtifactResolutionResult;
import org.gradle.api.artifacts.result.ArtifactResult;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.component.Component;
import org.gradle.api.specs.Specs;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;
import org.gradle.internal.component.local.model.ComponentFileArtifactIdentifier;
import org.gradle.internal.component.local.model.OpaqueComponentArtifactIdentifier;
import org.gradle.language.base.artifact.SourcesArtifact;
import org.gradle.language.java.artifact.JavadocArtifact;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

// WORK IN PROGRESS

/**
 * TODO: JavaDoc.
 */
public class AndroidDependencyCollector {

  private static final String UNKNOWN = "unknown";

  /**
   * Resolve and collect dependencies from an Android variant.
   */
  public static Set<GradleModuleDependency> getModuleDependencies(Project project, Object variant) {
    Set<GradleModuleDependency> dependencies = new HashSet<>();
    ClassLoader agpClassLoader = variant.getClass().getClassLoader();

    try {
      // Retrieve and process compile configuration
      Configuration compileConfiguration =
          (Configuration) AndroidUtils.getProperty(variant, "compileConfiguration");
      dependencies.addAll(resolveConfigurationDependencies(
          project,
          compileConfiguration,
          agpClassLoader)
      );

      // Retrieve and process runtime configuration
      Configuration runtimeConfiguration =
          (Configuration) AndroidUtils.getProperty(variant, "runtimeConfiguration");
      dependencies.addAll(resolveConfigurationDependencies(
          project,
          runtimeConfiguration,
          agpClassLoader)
      );
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    return dependencies;
  }

  /**
   * Resolve and collect dependencies from a given configuration.
   */
  private static Set<GradleModuleDependency> resolveConfigurationDependencies(
      Project project,
      Configuration configuration,
      ClassLoader agpClassLoader
  ) {
    return configuration.getIncoming()
        .artifactView(viewConfiguration -> {
          viewConfiguration.lenient(true);
          viewConfiguration.componentFilter(Specs.satisfyAll());
        })
        .getArtifacts()
        .getArtifacts()
        .stream()
        .map(artifactResult -> getArtifact(project, artifactResult, agpClassLoader))
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
  }

  private static DefaultGradleModuleDependency getArtifact(
      Project project,
      ResolvedArtifactResult artifactResult,
      ClassLoader agpClassLoader
  ) {
    ComponentArtifactIdentifier id = artifactResult.getId();
    File artifactFile = artifactResult.getFile();
    if (id instanceof ModuleComponentArtifactIdentifier) {
      return getModuleArtifactDependency(
          project,
          (ModuleComponentArtifactIdentifier) id,
          artifactFile,
          agpClassLoader
      );
    }
    if (id instanceof OpaqueComponentArtifactIdentifier) {
      return getFileArtifactDependency((OpaqueComponentArtifactIdentifier) id, artifactFile);
    }
    if (id instanceof ComponentFileArtifactIdentifier) {
      return getFileArtifactDependency((ComponentFileArtifactIdentifier) id, artifactFile);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static DefaultGradleModuleDependency getModuleArtifactDependency(
      Project project,
      ModuleComponentArtifactIdentifier artifactIdentifier,
      File resolvedArtifactFile,
      ClassLoader agpClassLoader
  ) {

    try {
      Class<? extends Component> androidLibClazz =
          (Class<? extends Component>) agpClassLoader
              .loadClass("com.android.build.api.variant.LibraryVariant");

      ArtifactResolutionResult resolutionResult = project.getDependencies()
          .createArtifactResolutionQuery()
          .forComponents(artifactIdentifier.getComponentIdentifier())
          .withArtifacts(
              androidLibClazz /* componentType */,
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
    } catch (ClassNotFoundException e) {
      return null;
    }

  }

  private static File getNonClassesArtifact(
      Set<ComponentArtifactsResult> resolvedComponents,
      Class<? extends org.gradle.api.component.Artifact> artifactClass
  ) {
    for (ComponentArtifactsResult component : resolvedComponents) {
      Set<ArtifactResult> artifacts = component.getArtifacts(artifactClass);
      for (ArtifactResult artifact : artifacts) {
        if (artifact instanceof ResolvedArtifactResult) {
          return ((ResolvedArtifactResult) artifact).getFile();
        }
      }
    }
    return null;
  }

  private static DefaultGradleModuleDependency getFileArtifactDependency(
      ComponentFileArtifactIdentifier artifactIdentifier,
      File resolvedArtifactFile
  ) {
    return getFileArtifactDependency(
        artifactIdentifier.getCapitalizedDisplayName(),
        resolvedArtifactFile
    );
  }

  private static DefaultGradleModuleDependency getFileArtifactDependency(
      OpaqueComponentArtifactIdentifier artifactIdentifier,
      File resolvedArtifactFile
  ) {
    return getFileArtifactDependency(
        artifactIdentifier.getCapitalizedDisplayName(),
        resolvedArtifactFile
    );
  }

  private static DefaultGradleModuleDependency getFileArtifactDependency(
      String displayName,
      File resolvedArtifactFile
  ) {
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
