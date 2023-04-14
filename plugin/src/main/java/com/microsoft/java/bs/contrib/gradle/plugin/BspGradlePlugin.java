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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.plugins.ide.internal.tooling.java.DefaultInstalledJdk;

public class BspGradlePlugin implements Plugin<Project> {

    private final ToolingModelBuilderRegistry registry;

    @Inject
    public BspGradlePlugin(ToolingModelBuilderRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void apply(Project project) {
        registry.register(new BuildTargetsModelBuilder());
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
                JavaPluginExtension javaPlugin = project.getExtensions().findByType(JavaPluginExtension.class);
                if (javaPlugin == null) {
                    continue;
                }

                javaPlugin.getSourceSets().forEach(sourceSet -> {
                    DefaultJavaBuildTarget javaBuildTarget = new DefaultJavaBuildTarget();
                    javaBuildTarget.setProjectName(project.getName());
                    javaBuildTarget.setProjectDir(project.getProjectDir());

                    javaBuildTarget.setSourceSetName(sourceSet.getName());

                    javaBuildTarget.setSourceDirs(sourceSet.getJava().getSrcDirs());
                    Directory sourceOutputDir = sourceSet.getJava().getClassesDirectory().getOrNull();
                    if (sourceOutputDir != null) {
                        javaBuildTarget.setSourceOutputDir(sourceOutputDir.getAsFile());
                    }

                    javaBuildTarget.setResourceDirs(sourceSet.getResources().getSrcDirs());
                    File resourceOutputDir = sourceSet.getOutput().getResourcesDir();
                    if (resourceOutputDir != null) {
                        javaBuildTarget.setResourceOutputDirs(resourceOutputDir);
                    }

                    File apGeneratedDir = getApGeneratedDir(project, sourceSet);
                    javaBuildTarget.setApGeneratedDir(apGeneratedDir);

                    DependencyCollection dependency = getDependencies(project, sourceSet);
                    javaBuildTarget.setModuleDependencies(dependency.getModuleDependencies());
                    javaBuildTarget.setProjectDependencies(dependency.getProjectDependencies());

                    JdkPlatform jdkPlatform = getJdkPlatform(project, sourceSet);
                    javaBuildTarget.setJdkPlatform(jdkPlatform);

                    javaBuildTargets.add(javaBuildTarget);
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
                Directory generatedDir = options.getGeneratedSourceOutputDirectory().getOrNull();
                if (generatedDir != null) {
                    return generatedDir.getAsFile();
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
            // See: https://github.com/gradle/gradle/blob/85ebea10e4e150ce485184adba811ed3eeaa2622/subprojects/
            //      ide/src/main/java/org/gradle/plugins/ide/internal/tooling/EclipseModelBuilder.java#L348
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

        private DependencyCollection getDependencies(Project project, SourceSet sourceSet) {
            JavaCompile javaCompile = getJavaCompileTask(project, sourceSet);
            if (javaCompile != null) {
                Set<File> dependencies = javaCompile.getClasspath().getFiles();
                return getDependencyCollection(project, dependencies);
            }
            return null;
        }

        private DependencyCollection getDependencyCollection(Project project, Set<File> files) {
            List<ResolvedArtifactResult> resolvedResult = project.getConfigurations()
                .stream()
                .filter(configuration -> configuration.isCanBeResolved())
                .flatMap(configuration -> {
                    return getConfigurationArtifacts(configuration).stream();
                })
                .filter(r -> {
                    return r.getFile() != null && files.contains(r.getFile());
                })
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

        private DependencyCollection resolveProjectDependency(List<ResolvedArtifactResult> resolvedResults, Project project) {
            DependencySet dependencySet = new DependencySet(resolvedResults);
            DependencyVisitor visitor = new DefaultDependencyVisitor(project);
            dependencySet.accept(visitor);
            return new DependencyCollection(
                visitor.getModuleDependencies(),
                visitor.getProjectDependencies()
            );
        }
    }

}
