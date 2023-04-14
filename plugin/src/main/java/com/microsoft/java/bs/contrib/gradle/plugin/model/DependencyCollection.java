package com.microsoft.java.bs.contrib.gradle.plugin.model;

import java.util.Set;

import com.microsoft.java.bs.contrib.gradle.model.ModuleDependency;
import com.microsoft.java.bs.contrib.gradle.model.ProjectDependency;

public class DependencyCollection {
    private Set<ModuleDependency> moduleDependencies;

    private Set<ProjectDependency> projectDependencies;

    public DependencyCollection(Set<ModuleDependency> moduleDependencies, Set<ProjectDependency> projectDependencies) {
        this.moduleDependencies = moduleDependencies;
        this.projectDependencies = projectDependencies;
    }

    public Set<ModuleDependency> getModuleDependencies() {
        return moduleDependencies;
    }

    public Set<ProjectDependency> getProjectDependencies() {
        return projectDependencies;
    }
}
