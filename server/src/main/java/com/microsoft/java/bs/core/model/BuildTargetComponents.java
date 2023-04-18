package com.microsoft.java.bs.core.model;

import java.io.File;
import java.util.Set;

import com.microsoft.java.bs.contrib.gradle.model.ModuleDependency;

import ch.epfl.scala.bsp4j.BuildTarget;

public class BuildTargetComponents {
    private BuildTarget buildTarget;

    private Set<File> sourceDirs;

    private File sourceOutputDir;

    private Set<File> resourceDirs;

    private File resourceOutputDir;

    private File apGeneratedDir;

    private Set<ModuleDependency> moduleDependencies;

    public BuildTarget getBuildTarget() {
        return buildTarget;
    }

    public void setBuildTarget(BuildTarget buildTarget) {
        this.buildTarget = buildTarget;
    }

    public Set<File> getSourceDirs() {
        return sourceDirs;
    }

    public void setSourceDirs(Set<File> sourceDirs) {
        this.sourceDirs = sourceDirs;
    }

    public File getSourceOutputDir() {
        return sourceOutputDir;
    }

    public void setSourceOutputDir(File sourceOutputDir) {
        this.sourceOutputDir = sourceOutputDir;
    }

    public Set<File> getResourceDirs() {
        return resourceDirs;
    }

    public void setResourceDirs(Set<File> resourceDirs) {
        this.resourceDirs = resourceDirs;
    }

    public File getResourceOutputDir() {
        return resourceOutputDir;
    }

    public void setResourceOutputDir(File resourceOutputDirs) {
        this.resourceOutputDir = resourceOutputDirs;
    }

    public File getApGeneratedDir() {
        return apGeneratedDir;
    }

    public void setApGeneratedDir(File apGeneratedDir) {
        this.apGeneratedDir = apGeneratedDir;
    }

    public Set<ModuleDependency> getModuleDependencies() {
        return moduleDependencies;
    }

    public void setModuleDependencies(Set<ModuleDependency> modules) {
        this.moduleDependencies = modules;
    }
}
