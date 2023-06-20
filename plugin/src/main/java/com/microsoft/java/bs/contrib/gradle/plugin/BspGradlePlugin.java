package com.microsoft.java.bs.contrib.gradle.plugin;

import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;
import org.gradle.util.GradleVersion;

import com.microsoft.java.bs.contrib.gradle.model.JdkPlatform;
import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTargets;
import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTarget;
import com.microsoft.java.bs.contrib.gradle.plugin.dependency.DefaultDependencyVisitor;
import com.microsoft.java.bs.contrib.gradle.plugin.dependency.DependencySet;
import com.microsoft.java.bs.contrib.gradle.plugin.dependency.DependencyVisitor;
import com.microsoft.java.bs.contrib.gradle.plugin.model.DefaultJdkPlatform;
import com.microsoft.java.bs.contrib.gradle.plugin.model.DefaultJavaBuildTargets;
import com.microsoft.java.bs.contrib.gradle.plugin.model.DependencyCollection;
import com.microsoft.java.bs.contrib.gradle.plugin.model.DefaultJavaBuildTarget;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.plugins.ide.internal.tooling.java.DefaultInstalledJdk;

/**
 * The main entry point of the plugin.
 */
public class BspGradlePlugin implements Plugin<Project> {

  private final ToolingModelBuilderRegistry registry;

  @Inject
  public BspGradlePlugin(ToolingModelBuilderRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void apply(Project project) {
    project.afterEvaluate(p -> registry.register(new BuildTargetsModelBuilder()));
  }

  private static class BuildTargetsModelBuilder implements ToolingModelBuilder {
    @Override
    public boolean canBuild(String modelName) {
      return modelName.equals(JavaBuildTargets.class.getName());
    }

    @Override
    public Object buildAll(String modelName, Project rootProject) {
      final GradleVersion current = GradleVersion.current().getBaseVersion();
      if (GradleVersion.version("5.2").compareTo(current) > 0) {
        return null;
      }
      Set<Project> allProject = rootProject.getAllprojects();
      List<JavaBuildTarget> javaBuildTargets = new ArrayList<>();
      for (Project project : allProject) {
        SourceSetContainer sourceSets = getSourceSetContainer(project);
        if (sourceSets == null) {
          continue;
        }

        // this set is used to eliminate the source, resource and output
        // directories from the module dependencies.
        Set<File> exclusionFromDependencies = new HashSet<>();
        sourceSets.forEach(sourceSet -> {
          DefaultJavaBuildTarget javaBuildTarget = new DefaultJavaBuildTarget();
          javaBuildTarget.setProjectName(project.getName());
          javaBuildTarget.setModulePath(project.getPath());
          javaBuildTarget.setProjectDir(project.getProjectDir());
          javaBuildTarget.setRootDir(project.getRootDir());

          javaBuildTarget.setSourceSetName(sourceSet.getName());

          Set<File> srcDirs = sourceSet.getJava().getSrcDirs();
          javaBuildTarget.setSourceDirs(srcDirs);
          exclusionFromDependencies.addAll(srcDirs);
          Directory sourceOutputDir = sourceSet.getJava().getClassesDirectory().getOrNull();
          if (sourceOutputDir != null) {
            javaBuildTarget.setSourceOutputDir(sourceOutputDir.getAsFile());
            exclusionFromDependencies.add(sourceOutputDir.getAsFile());
          }

          Set<File> reDirs = sourceSet.getResources().getSrcDirs();
          javaBuildTarget.setResourceDirs(reDirs);
          exclusionFromDependencies.addAll(reDirs);
          File resourceOutputDir = sourceSet.getOutput().getResourcesDir();
          if (resourceOutputDir != null) {
            javaBuildTarget.setResourceOutputDirs(resourceOutputDir);
            exclusionFromDependencies.add(resourceOutputDir);
          }

          File apGeneratedDir = getApGeneratedDir(project, sourceSet);
          javaBuildTarget.setApGeneratedDir(apGeneratedDir);
          exclusionFromDependencies.add(apGeneratedDir);

          JdkPlatform jdkPlatform = getJdkPlatform(project, sourceSet);
          javaBuildTarget.setJdkPlatform(jdkPlatform);

          javaBuildTargets.add(javaBuildTarget);
        });

        sourceSets.forEach(sourceSet -> {
          javaBuildTargets.forEach(target -> {
            if (Objects.equals(target.getSourceSetName(), sourceSet.getName())
                && Objects.equals(target.getProjectName(), project.getName())) {
              DependencyCollection dependency = getDependencies(project, sourceSet,
                  exclusionFromDependencies);
              ((DefaultJavaBuildTarget) target)
                  .setModuleDependencies(dependency.getModuleDependencies());
              ((DefaultJavaBuildTarget) target)
                  .setProjectDependencies(dependency.getProjectDependencies());
            }
          });
        });
      }
      DefaultJavaBuildTargets result = new DefaultJavaBuildTargets();
      result.setJavaBuildTargets(javaBuildTargets);
      return result;
    }

    private File getApGeneratedDir(Project project, SourceSet sourceSet) {
      JavaCompile javaCompile = getJavaCompileTask(project, sourceSet);
      if (javaCompile != null) {
        CompileOptions options = javaCompile.getOptions();
        try {
          Directory generatedDir = options.getGeneratedSourceOutputDirectory().getOrNull();
          if (generatedDir != null) {
            return generatedDir.getAsFile();
          }
        } catch (NoSuchMethodError e) {
          // to be compatible with Gradle < 6.3
          return options.getAnnotationProcessorGeneratedSourcesDirectory();
        }
      }
      return null;
    }

    /**
     * Get the JDK platform for the given source set. Note that the build will be
     * delegated to Gradle, so there is no need to take care about the fork JDK.
     */
    private JdkPlatform getJdkPlatform(Project project, SourceSet sourceSet) {
      DefaultJdkPlatform platform = new DefaultJdkPlatform();
      // See: https://github.com/gradle/gradle/blob/85ebea10e4e150ce485184adba811ed3eeaa2622/subprojects/ide/src/main/java/org/gradle/plugins/ide/internal/tooling/EclipseModelBuilder.java#L348
      platform.setJavaHome(DefaultInstalledJdk.current().getJavaHome());
      platform.setJavaVersion(DefaultInstalledJdk.current().getJavaVersion().getMajorVersion());

      JavaCompile javaCompile = getJavaCompileTask(project, sourceSet);
      if (javaCompile != null) {
        platform.setSourceLanguageLevel(javaCompile.getSourceCompatibility());
        platform.setTargetBytecodeVersion(javaCompile.getTargetCompatibility());
      }
      
      return platform;
    }

    private JavaCompile getJavaCompileTask(Project project, SourceSet sourceSet) {
      String taskName = sourceSet.getCompileJavaTaskName();
      return (JavaCompile) project.getTasks().getByName(taskName);
    }

    private DependencyCollection getDependencies(Project project, SourceSet sourceSet,
        Set<File> exclusionFromDependencies) {
      Set<String> configurationNames = new HashSet<>();
      configurationNames.add(sourceSet.getCompileClasspathConfigurationName());
      configurationNames.add(sourceSet.getRuntimeClasspathConfigurationName());
      return getDependencyCollection(project, configurationNames, exclusionFromDependencies);
    }

    private DependencyCollection getDependencyCollection(Project project,
        Set<String> configurationNames, Set<File> exclusionFromDependencies) {
      List<ResolvedArtifactResult> resolvedResult = project.getConfigurations()
          .stream()
          .filter(configuration -> configurationNames.contains(configuration.getName())
              && configuration.isCanBeResolved())
          .flatMap(configuration -> getConfigurationArtifacts(configuration).stream())
          .filter(artifact -> !exclusionFromDependencies.contains(artifact.getFile()))
          .collect(Collectors.toList());
      return resolveProjectDependency(resolvedResult, project);
    }

    private List<ResolvedArtifactResult> getConfigurationArtifacts(Configuration config) {
      return config.getIncoming()
        .artifactView(viewConfiguration -> {
          viewConfiguration.lenient(true);
          viewConfiguration.componentFilter(Specs.<ComponentIdentifier>satisfyAll());
        })
        .getArtifacts()
        .getArtifacts()
        .stream()
        .collect(Collectors.toList());
    }

    private DependencyCollection resolveProjectDependency(
          List<ResolvedArtifactResult> resolvedResults, Project project) {
      DependencySet dependencySet = new DependencySet(resolvedResults);
      DependencyVisitor visitor = new DefaultDependencyVisitor(project);
      dependencySet.accept(visitor);
      return new DependencyCollection(
        visitor.getModuleDependencies(),
        visitor.getProjectDependencies()
      );
    }

    private SourceSetContainer getSourceSetContainer(Project project) {
      JavaPluginExtension javaPlugin = project.getExtensions()
          .findByType(JavaPluginExtension.class);
      if (javaPlugin != null) {
        try {
          return javaPlugin.getSourceSets();
        } catch (NoSuchMethodError e) {
          // to be compatible with Gradle < 7.1
          JavaPluginConvention convention = project.getConvention()
              .getPlugin(JavaPluginConvention.class);
          if (convention != null) {
            return convention.getSourceSets();
          }
        }
      }
      return null;
    }
  }
}
