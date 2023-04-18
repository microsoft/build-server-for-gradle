package com.microsoft.java.bs.core.services;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.inject.Inject;
import com.microsoft.java.bs.contrib.gradle.model.ModuleDependency;
import com.microsoft.java.bs.core.managers.BuildTargetsManager;
import com.microsoft.java.bs.core.model.BuildTargetComponents;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.DependencyModule;
import ch.epfl.scala.bsp4j.DependencyModulesItem;
import ch.epfl.scala.bsp4j.DependencyModulesParams;
import ch.epfl.scala.bsp4j.DependencyModulesResult;
import ch.epfl.scala.bsp4j.MavenDependencyModule;
import ch.epfl.scala.bsp4j.MavenDependencyModuleArtifact;
import ch.epfl.scala.bsp4j.OutputPathItem;
import ch.epfl.scala.bsp4j.OutputPathItemKind;
import ch.epfl.scala.bsp4j.OutputPathsItem;
import ch.epfl.scala.bsp4j.OutputPathsParams;
import ch.epfl.scala.bsp4j.OutputPathsResult;
import ch.epfl.scala.bsp4j.ResourcesItem;
import ch.epfl.scala.bsp4j.ResourcesParams;
import ch.epfl.scala.bsp4j.ResourcesResult;
import ch.epfl.scala.bsp4j.SourceItem;
import ch.epfl.scala.bsp4j.SourceItemKind;
import ch.epfl.scala.bsp4j.SourcesItem;
import ch.epfl.scala.bsp4j.SourcesParams;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;

public class BuildTargetsService {

    private static final String MAVEN_DATA_KIND = "maven";

    @Inject
    BuildTargetsManager buildTargetsManager;

    public WorkspaceBuildTargetsResult workspaceBuildTargets() {
        return  new WorkspaceBuildTargetsResult(
            new ArrayList<>(buildTargetsManager.getBuildTargets())
        );
    }

    public DependencyModulesResult buildTargetDependencyModules(DependencyModulesParams params) {
        List<DependencyModulesItem> items = new ArrayList<>();
        for (BuildTargetIdentifier target : params.getTargets()) {
            BuildTargetComponents components = buildTargetsManager.getComponents(target);
            List<DependencyModule> modules = new ArrayList<>();
            for (ModuleDependency lib : components.getModuleDependencies()) {
                DependencyModule module = new DependencyModule(lib.getName(), lib.getVersion());
                module.setDataKind(MAVEN_DATA_KIND);
                List<MavenDependencyModuleArtifact> artifacts = lib.getArtifacts().stream().map(a -> {
                    MavenDependencyModuleArtifact artifact = new MavenDependencyModuleArtifact(a.getUri().toString());
                    artifact.setClassifier(a.getClassifier());
                    return artifact;
                }).collect(Collectors.toList());
                MavenDependencyModule mavenModule = new MavenDependencyModule(
                    lib.getOrganization(),
                    lib.getName(),
                    lib.getVersion(),
                    artifacts
                );
                module.setData(mavenModule);
                modules.add(module);
            }

            DependencyModulesItem item = new DependencyModulesItem(target, modules);
            items.add(item);
        }
        return new DependencyModulesResult(items);
    }

    public OutputPathsResult buildTargetOutputPaths(OutputPathsParams params) {
        List<OutputPathsItem> items = new ArrayList<>();
        for (BuildTargetIdentifier target : params.getTargets()) {
            BuildTargetComponents components = buildTargetsManager.getComponents(target);
            List<OutputPathItem> outputPaths = new ArrayList<>();
            // the first output path is source output
            outputPaths.add(new OutputPathItem(
                components.getSourceOutputDir().toURI().toString(),
                OutputPathItemKind.DIRECTORY
            ));
            // the second output path is resource output
            outputPaths.add(new OutputPathItem(
                components.getResourceOutputDir().toURI().toString(),
                OutputPathItemKind.DIRECTORY
            ));
            OutputPathsItem item = new OutputPathsItem(target, outputPaths);
            items.add(item);
        }
        return new OutputPathsResult(items);
    }

    public Object workspaceReload() {
        // TODO: return error when the build configuration has errors
        buildTargetsManager.reset();
        return null;
    }

    public ResourcesResult buildTargetResources(ResourcesParams params) {
        List<ResourcesItem> items = new ArrayList<>();
        for (BuildTargetIdentifier target : params.getTargets()) {
            BuildTargetComponents components = buildTargetsManager.getComponents(target);
            List<String> resources = new ArrayList<>();
            for (File resourceDir : components.getResourceDirs()) {
                resources.add(resourceDir.toURI().toString());
            }
            ResourcesItem item = new ResourcesItem(target, resources);
            items.add(item);
        }
        return new ResourcesResult(items);
    }

    public SourcesResult buildTargetSources(SourcesParams params) {
        List<SourcesItem> sourceItems = new ArrayList<>();
        for (BuildTargetIdentifier target : params.getTargets()) {
            BuildTargetComponents components = buildTargetsManager.getComponents(target);
            List<SourceItem> sources = new ArrayList<>();
            for (File sourceDir : components.getSourceDirs()) {
                sources.add(new SourceItem(sourceDir.toURI().toString(), SourceItemKind.DIRECTORY, false));
            }
            File apGeneratedDir = components.getApGeneratedDir();
            if (apGeneratedDir != null) {
                boolean updated = updateGeneratedFlag(sources, apGeneratedDir);;
                if (!updated) {
                    sources.add(new SourceItem(apGeneratedDir.toURI().toString(), SourceItemKind.DIRECTORY, true));
                }
            }
            SourcesItem item = new SourcesItem(target, sources);
            sourceItems.add(item);
        }
        return new SourcesResult(sourceItems);
    }

    private boolean updateGeneratedFlag(List<SourceItem> sources, File apGeneratedDir) {
        if (apGeneratedDir != null) {
            for (SourceItem item : sources) {
                if (item.getUri().equals(apGeneratedDir.toURI().toString())) {
                    item.setGenerated(true);
                    return true;
                }
            }
        }
        return false;
    }
}
