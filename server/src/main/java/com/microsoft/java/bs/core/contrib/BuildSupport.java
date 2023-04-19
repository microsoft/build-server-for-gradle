package com.microsoft.java.bs.core.contrib;

import java.net.URI;
import java.util.List;

import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTargets;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;

public interface BuildSupport {
    default boolean applies() {
        return true;
    }

    JavaBuildTargets getSourceSetEntries(URI projectUri);

    void build(List<BuildTargetIdentifier> targets);
}
