package com.microsoft.java.bs.core.internal.model;

import java.util.Objects;

import ch.epfl.scala.bsp4j.JvmBuildTarget;

/**
 * Extended {@link JvmBuildTarget} with additional information.
 * See: https://github.com/build-server-protocol/build-server-protocol/issues/473
 */
public class JvmBuildTargetExt extends JvmBuildTarget {

  private String sourceCompatibility;

  private String targetCompatibility;

  public String getSourceCompatibility() {
    return sourceCompatibility;
  }

  public void setSourceCompatibility(String sourceCompatibility) {
    this.sourceCompatibility = sourceCompatibility;
  }

  public String getTargetCompatibility() {
    return targetCompatibility;
  }

  public void setTargetCompatibility(String targetCompatibility) {
    this.targetCompatibility = targetCompatibility;
  }

  public JvmBuildTargetExt(String javaHome, String javaVersion) {
    super(javaHome, javaVersion);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(sourceCompatibility, targetCompatibility);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    JvmBuildTargetExt other = (JvmBuildTargetExt) obj;
    return Objects.equals(sourceCompatibility, other.sourceCompatibility)
        && Objects.equals(targetCompatibility, other.targetCompatibility);
  }
}
