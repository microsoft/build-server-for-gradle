package com.microsoft.java.bs.gradle.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.Directory;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.plugins.ide.internal.tooling.java.DefaultInstalledJdk;
import org.gradle.tooling.provider.model.ToolingModelBuilder;
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry;

import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.plugin.dependency.DependencyCollector;
import com.microsoft.java.bs.gradle.plugin.model.DefaultGradleSourceSet;
import com.microsoft.java.bs.gradle.plugin.model.DefaultGradleSourceSets;

/**
 * The customized Gradle plugin to get the project structure information.
 */
public class GradleBuildServerPlugin implements Plugin<Project> {
  private final ToolingModelBuilderRegistry registry;

  @Inject
  public GradleBuildServerPlugin(ToolingModelBuilderRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void apply(Project project) {
    project.afterEvaluate(p -> registry.register(new SourceSetsModelBuilder()));
  }

  private static class SourceSetsModelBuilder implements ToolingModelBuilder {
    @Override
    public boolean canBuild(String modelName) {
      return modelName.equals(GradleSourceSets.class.getName());
    }

    @Override
    public Object buildAll(String modelName, Project rootProject) {
      Set<Project> allProject = rootProject.getAllprojects();
      List<GradleSourceSet> gradleSourceSets = new ArrayList<>();
      // mapping Gradle source set to our customized model.
      Map<SourceSet, DefaultGradleSourceSet> sourceSetMap = new HashMap<>();
      for (Project project : allProject) {
        SourceSetContainer sourceSets = getSourceSetContainer(project);
        if (sourceSets == null || sourceSets.isEmpty()) {
          continue;
        }

        // this set is used to eliminate the source, resource and output
        // directories from the module dependencies.
        Set<File> exclusionFromDependencies = new HashSet<>();
        sourceSets.forEach(sourceSet -> {
          DefaultGradleSourceSet gradleSourceSet = new DefaultGradleSourceSet(project);
          sourceSetMap.put(sourceSet, gradleSourceSet);
          gradleSourceSet.setSourceSetName(sourceSet.getName());

          // source
          Set<File> srcDirs = sourceSet.getJava().getSrcDirs();
          gradleSourceSet.setSourceDirs(srcDirs);
          exclusionFromDependencies.addAll(srcDirs);
          Set<File> generatedSrcDirs = new HashSet<>();
          addAnnotationProcessingDir(project, sourceSet, generatedSrcDirs);
          addGeneratedSourceDirs(project, sourceSet, srcDirs, generatedSrcDirs);
          gradleSourceSet.setGeneratedSourceDirs(generatedSrcDirs);
          exclusionFromDependencies.addAll(generatedSrcDirs);

          // source output dir
          File sourceOutputDir = getSourceOutputDir(sourceSet);
          if (sourceOutputDir != null) {
            gradleSourceSet.setSourceOutputDir(sourceOutputDir);
            exclusionFromDependencies.add(sourceOutputDir);
          }

          // resource
          Set<File> resourceDirs = sourceSet.getResources().getSrcDirs();
          gradleSourceSet.setResourceDirs(resourceDirs);
          exclusionFromDependencies.addAll(resourceDirs);

          // resource output dir
          File resourceOutputDir = sourceSet.getOutput().getResourcesDir();
          if (resourceOutputDir != null) {
            gradleSourceSet.setResourceOutputDir(resourceOutputDir);
            exclusionFromDependencies.add(resourceOutputDir);
          }

          // jdk
          gradleSourceSet.setJavaHome(DefaultInstalledJdk.current().getJavaHome());
          gradleSourceSet.setJavaVersion(getJavaVersion(project, sourceSet));

          gradleSourceSets.add(gradleSourceSet);
        });

        // dependencies
        sourceSets.forEach(sourceSet -> {
          DefaultGradleSourceSet gradleSourceSet = sourceSetMap.get(sourceSet);
          if (gradleSourceSet == null) {
            return;
          }
          DependencyCollector collector = new DependencyCollector(project,
              exclusionFromDependencies);
          collector.collectByConfigurationNames(getClasspathConfigurationNames(sourceSet));
          gradleSourceSet.setModuleDependencies(collector.getModuleDependencies());
          gradleSourceSet.setProjectDependencies(collector.getProjectDependencies());
        });
      }
      DefaultGradleSourceSets result = new DefaultGradleSourceSets();
      result.setGradleSourceSets(gradleSourceSets);
      return result;
    }

    private SourceSetContainer getSourceSetContainer(Project project) {
      try {
        JavaPluginExtension javaPlugin = project.getExtensions()
            .findByType(JavaPluginExtension.class);
        if (javaPlugin != null) {
          return javaPlugin.getSourceSets();
        }
      } catch (NoClassDefFoundError | NoSuchMethodError e) {
        // to be compatible with Gradle < 7.1
        JavaPluginConvention convention = project.getConvention()
            .getPlugin(JavaPluginConvention.class);
        if (convention != null) {
          return convention.getSourceSets();
        }
      }
      return null;
    }

    private void addAnnotationProcessingDir(Project project, SourceSet sourceSet,
        Set<File> generatedSrcDirs) {
      JavaCompile javaCompile = getJavaCompileTask(project, sourceSet);
      if (javaCompile != null) {
        CompileOptions options = javaCompile.getOptions();
        try {
          Directory generatedDir = options.getGeneratedSourceOutputDirectory().getOrNull();
          if (generatedDir != null) {
            generatedSrcDirs.add(generatedDir.getAsFile());
          }
        } catch (NoSuchMethodError e) {
          // to be compatible with Gradle < 6.3
          generatedSrcDirs.add(options.getAnnotationProcessorGeneratedSourcesDirectory());
        }
      }
    }

    private JavaCompile getJavaCompileTask(Project project, SourceSet sourceSet) {
      String taskName = sourceSet.getCompileJavaTaskName();
      return (JavaCompile) project.getTasks().getByName(taskName);
    }

    private void addGeneratedSourceDirs(Project project, SourceSet sourceSet,
        Set<File> srcDirs, Set<File> generatedSrcDirs) {
      JavaCompile javaCompile = getJavaCompileTask(project, sourceSet);
      if (javaCompile != null) {
        Set<File> filesToCompile = javaCompile.getSource().getFiles();
        for (File file : filesToCompile) {
          if (canSkipInferSourceRoot(file, srcDirs, generatedSrcDirs)) {
            continue;
          }

          // the file is not in the source directories, so it must be a generated file.
          // we need to find the source directory for the generated file.
          File srcDir = findSourceDirForGeneratedFile(file);
          if (srcDir != null) {
            generatedSrcDirs.add(srcDir);
          }
        }
      }
    }

    /**
     * Skip the source root inference if:
     * <ul>
     * <li>File is not a Java file.</li>
     * <li>File already belongs to srcDirs.</li>
     * <li>File already belongs to generatedSrcDirs.</li>
     * </ul>
     * Return <code>true</code> if the source root inference can be skipped.
     */
    private boolean canSkipInferSourceRoot(File sourceFile, Set<File> srcDirs,
        Set<File> generatedSrcDirs) {
      if (!sourceFile.isFile() || !sourceFile.exists() || !sourceFile.getName().endsWith(".java")) {
        return true;
      }

      if (srcDirs.stream().anyMatch(dir -> sourceFile.getAbsolutePath()
          .startsWith(dir.getAbsolutePath()))) {
        return true;
      }

      return generatedSrcDirs.stream().anyMatch(dir -> sourceFile.getAbsolutePath()
          .startsWith(dir.getAbsolutePath()));
    }

    /**
     * read the file content and find the package declaration.
     * Then find the source directory that contains the package declaration.
     * If the package declaration is not found, then return <code>null</code>.
     */
    private File findSourceDirForGeneratedFile(File file) {
      Pattern packagePattern = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;\\s*$");
      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        String line;
        while ((line = reader.readLine()) != null) {
          Matcher matcher = packagePattern.matcher(line);
          if (matcher.matches()) {
            String packageName = matcher.group(1);
            String relativeToRoot = packageName.replace(".", File.separator)
                .concat(File.separator).concat(file.getName());
            String absolutePath = file.getAbsolutePath();
            if (!absolutePath.endsWith(relativeToRoot)) {
              return null;
            }
            return new File(absolutePath.substring(
                  0, absolutePath.length() - relativeToRoot.length()));
          }
        }
      } catch (IOException e) {
        return null;
      }

      return null;
    }

    private File getSourceOutputDir(SourceSet sourceSet) {
      try {
        Directory sourceOutputDir = sourceSet.getJava().getClassesDirectory().getOrNull();
        if (sourceOutputDir != null) {
          return sourceOutputDir.getAsFile();
        }
        return null;
      } catch (NoSuchMethodError e) {
        // to be compatible with Gradle < 6.1
        return sourceSet.getOutput().getClassesDirs().getSingleFile();
      }
    }

    private String getJavaVersion(Project project, SourceSet sourceSet) {
      JavaCompile javaCompile = getJavaCompileTask(project, sourceSet);
      if (javaCompile != null) {
        return javaCompile.getTargetCompatibility();
      }

      return "";
    }

    private Set<String> getClasspathConfigurationNames(SourceSet sourceSet) {
      Set<String> configurationNames = new HashSet<>();
      configurationNames.add(sourceSet.getCompileClasspathConfigurationName());
      configurationNames.add(sourceSet.getRuntimeClasspathConfigurationName());
      return configurationNames;
    }
  }
}
