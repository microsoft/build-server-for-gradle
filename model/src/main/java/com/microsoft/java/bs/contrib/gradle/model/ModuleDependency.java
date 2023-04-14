package com.microsoft.java.bs.contrib.gradle.model;

import java.util.List;

public interface ModuleDependency {
    public String getOrganization();

    public String getName();

    public String getVersion();

    public List<ModuleArtifact> getArtifacts();
}
