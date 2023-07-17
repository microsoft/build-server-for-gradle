package com.microsoft.java.bs.gradle.plugin.model;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

import com.microsoft.java.bs.gradle.model.JdkPlatform;

/**
 * Default implementation of {@link JdkPlatform}.
 */
public class DefaultJdkPlatform implements JdkPlatform, Serializable {
  private static final long serialVersionUID = 1L;

  private File javaHome;

  private String javaVersion;

  private String sourceCompatibility;

  private String targetCompatibility;

  public File getJavaHome() {
    return javaHome;
  }

  public void setJavaHome(File javaHome) {
    this.javaHome = javaHome;
  }

  public String getJavaVersion() {
    return javaVersion;
  }

  public void setJavaVersion(String javaVersion) {
    this.javaVersion = javaVersion;
  }

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

  @Override
  public int hashCode() {
    return Objects.hash(javaHome, javaVersion, sourceCompatibility, targetCompatibility);
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
    DefaultJdkPlatform other = (DefaultJdkPlatform) obj;
    return Objects.equals(javaHome, other.javaHome)
        && Objects.equals(javaVersion, other.javaVersion)
        && Objects.equals(sourceCompatibility, other.sourceCompatibility)
        && Objects.equals(targetCompatibility, other.targetCompatibility);
  }
}
