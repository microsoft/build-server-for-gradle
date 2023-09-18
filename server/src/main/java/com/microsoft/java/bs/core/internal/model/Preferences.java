package com.microsoft.java.bs.core.internal.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The preferences sent from 'build/initialize' request.
 */
public class Preferences {

  /**
   * The location to the JVM used to run the Gradle daemon.
   */
  private String gradleJavaHome;

  /**
   * Whether to use Gradle from the 'gradle-wrapper.properties' file.
   */
  private boolean isWrapperEnabled;

  /**
   * Use Gradle from the specific version if the Gradle wrapper is missing or disabled.
   */
  private String gradleVersion;

  /**
   * Use Gradle from the specified local installation directory or GRADLE_HOME if the
   * Gradle wrapper is missing or disabled and no 'gradleVersion' is blank.
   */
  private String gradleHome;

  /**
   * Setting for GRADLE_USER_HOME.
   */
  private String gradleUserHome;

  /**
   * The arguments to pass to the Gradle daemon.
   */
  private List<String> gradleArguments;

  /**
   * The JVM arguments to pass to the Gradle daemon.
   */
  private List<String> gradleJvmArguments;

  /**
   * A map of the JDKs on the machine. The key is the major JDK version,
   * for example: "1.8", "17", etc. The value is the installation path of the
   * JDK. When this preference is available, the Build Server will find the
   * most matched JDK to launch the Gradle daemon according to the Gradle version.
   * See: https://docs.gradle.org/current/userguide/compatibility.html#java
   */
  private Map<String, String> jdks;

  /**
   * Initialize the preferences.
   */
  public Preferences() {
    isWrapperEnabled = true;
    gradleArguments = Collections.emptyList();
    gradleJvmArguments = Collections.emptyList();
    jdks = Collections.emptyMap();
  }

  public String getGradleJavaHome() {
    return gradleJavaHome;
  }

  public void setGradleJavaHome(String gradleJavaHome) {
    this.gradleJavaHome = gradleJavaHome;
  }

  public boolean isWrapperEnabled() {
    return isWrapperEnabled;
  }

  public void setWrapperEnabled(boolean isWrapperEnabled) {
    this.isWrapperEnabled = isWrapperEnabled;
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
