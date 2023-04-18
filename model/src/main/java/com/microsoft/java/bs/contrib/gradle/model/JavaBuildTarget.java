package com.microsoft.java.bs.contrib.gradle.model;

import java.io.File;
import java.util.Set;

public interface JavaBuildTarget {
    public String getProjectName();

    public File getProjectDir();

    public Set<File> getSourceDirs();

    public File getSourceOutputDir();

    public Set<File> getResourceDirs();

    public File getResourceOutputDirs();

    public File getApGeneratedDir();

    public Set<ModuleDependency> getModuleDependencies();

    public Set<ProjectDependency> getProjectDependencies();

    public String getSourceSetName();

    public JdkPlatform getJdkPlatform();
}
