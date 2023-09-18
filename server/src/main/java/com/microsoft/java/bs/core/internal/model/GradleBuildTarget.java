package com.microsoft.java.bs.core.internal.model;

import java.util.Objects;

import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSet;

import ch.epfl.scala.bsp4j.BuildTarget;

/**
 * Represents a Gradle build target.
 */
public class GradleBuildTarget {
  public GradleBuildTarget(BuildTarget buildTarget, GradleSourceSet sourceSet) {
    this.buildTarget = buildTarget;
    this.sourceSet = new DefaultGradleSourceSet(sourceSet);
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

  @Override
  public int hashCode() {
    return Objects.hash(buildTarget, sourceSet);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    GradleBuildTarget other = (GradleBuildTarget) obj;
    return Objects.equals(buildTarget, other.buildTarget)
        && Objects.equals(sourceSet, other.sourceSet);
  }
}
