package com.microsoft.java.bs.core.internal.model;

import java.util.List;
import java.util.Map;

/**
 * The preferences sent from 'build/initialize' request.
 */
public class Preferences {
  private String gradleJavaHome;
  private String gradleVersion;
  private String gradleHome;
  private String gradleUserHome;
  private List<String> gradleArguments;
  private List<String> gradleJvmArguments;
  private Map<String, String> jdks;

  public String getGradleJavaHome() {
    return gradleJavaHome;
  }

  public void setGradleJavaHome(String gradleJavaHome) {
    this.gradleJavaHome = gradleJavaHome;
  }

  public String getGradleVersion() {
    return gradleVersion;
  }

  public void setGradleVersion(String gradleVersion) {
    this.gradleVersion = gradleVersion;
  }

  public String getGradleHome() {
    return gradleHome;
  }

  public void setGradleHome(String gradleHome) {
    this.gradleHome = gradleHome;
  }

  public String getGradleUserHome() {
    return gradleUserHome;
  }

  public void setGradleUserHome(String gradleUserHome) {
    this.gradleUserHome = gradleUserHome;
  }

  public List<String> getGradleArguments() {
    return gradleArguments;
  }

  public void setGradleArguments(List<String> gradleArguments) {
    this.gradleArguments = gradleArguments;
  }

  public List<String> getGradleJvmArguments() {
    return gradleJvmArguments;
  }

  public void setGradleJvmArguments(List<String> gradleJvmArguments) {
    this.gradleJvmArguments = gradleJvmArguments;
  }

  public Map<String, String> getJdks() {
    return jdks;
  }

  public void setJdks(Map<String, String> jdks) {
    this.jdks = jdks;
  }
}
