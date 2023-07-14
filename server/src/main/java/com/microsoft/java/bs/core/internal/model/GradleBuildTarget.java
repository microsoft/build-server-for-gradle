package com.microsoft.java.bs.core.internal.model;

import com.microsoft.java.bs.gradle.model.GradleSourceSet;

import ch.epfl.scala.bsp4j.BuildTarget;

/**
 * Represents a Gradle build target.
 */
public class GradleBuildTarget {
  public GradleBuildTarget(BuildTarget buildTarget, GradleSourceSet sourceSet) {
    this.buildTarget = buildTarget;
    this.sourceSet = sourceSet;
  }

  private BuildTarget buildTarget;

  private GradleSourceSet sourceSet;

  public BuildTarget getBuildTarget() {
    return buildTarget;
  }

  public void setBuildTarget(BuildTarget buildTarget) {
    this.buildTarget = buildTarget;
  }

  public GradleSourceSet getSourceSet() {
    return sourceSet;
  }

  public void setSourceSet(GradleSourceSet sourceSet) {
    this.sourceSet = sourceSet;
  }
}
