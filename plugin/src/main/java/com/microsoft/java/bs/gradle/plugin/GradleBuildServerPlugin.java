package com.microsoft.java.bs.gradle.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import com.microsoft.java.bs.gradle.model.JdkPlatform;
import com.microsoft.java.bs.gradle.plugin.model.DefaultGradleSourceSet;
import com.microsoft.java.bs.gradle.plugin.model.DefaultGradleSourceSets;
import com.microsoft.java.bs.gradle.plugin.model.DefaultJdkPlatform;

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
      for (Project project : allProject) {
        SourceSetContainer sourceSets = getSourceSetContainer(project);
        if (sourceSets == null || sourceSets.isEmpty()) {
          continue;
        }

        sourceSets.forEach(sourceSet -> {
          DefaultGradleSourceSet gradleSourceSet = new DefaultGradleSourceSet(project);
          gradleSourceSet.setSourceSetName(sourceSet.getName());

          // source
          Set<File> srcDirs = sourceSet.getJava().getSrcDirs();
          gradleSourceSet.setSourceDirs(srcDirs);
          Set<File> generatedSrcDirs = new HashSet<>();
          addAnnotationProcessingDir(project, sourceSet, generatedSrcDirs);
          addGeneratedSourceDirs(project, sourceSet, srcDirs, generatedSrcDirs);
          gradleSourceSet.setGeneratedSourceDirs(generatedSrcDirs);

          // source output dir
          gradleSourceSet.setSourceOutputDir(getSourceOutputDir(sourceSet));

          // resource
          Set<File> resourceDirs = sourceSet.getResources().getSrcDirs();
          gradleSourceSet.setResourceDirs(resourceDirs);

          // resource output dir
          gradleSourceSet.setResourceOutputDir(sourceSet.getOutput().getResourcesDir());

          // jdk
          JdkPlatform jdkPlatform = getJdkPlatform(project, sourceSet);
          gradleSourceSet.setJdkPlatform(jdkPlatform);

          gradleSourceSets.add(gradleSourceSet);
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

    private JdkPlatform getJdkPlatform(Project project, SourceSet sourceSet) {
      DefaultJdkPlatform platform = new DefaultJdkPlatform();
      // See: https://github.com/gradle/gradle/blob/85ebea10e4e150ce485184adba811ed3eeaa2622/subprojects/ide/src/main/java/org/gradle/plugins/ide/internal/tooling/EclipseModelBuilder.java#L348
      platform.setJavaHome(DefaultInstalledJdk.current().getJavaHome());
      platform.setJavaVersion(DefaultInstalledJdk.current().getJavaVersion().getMajorVersion());

      JavaCompile javaCompile = getJavaCompileTask(project, sourceSet);
      if (javaCompile != null) {
        platform.setSourceCompatibility(javaCompile.getSourceCompatibility());
        platform.setTargetCompatibility(javaCompile.getTargetCompatibility());
      }
      
      return platform;
    }
  }
}
