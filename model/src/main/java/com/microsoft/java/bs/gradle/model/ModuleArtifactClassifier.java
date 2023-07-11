package com.microsoft.java.bs.gradle.model;

/**
 * Represents a module artifact classifier.
 */
public enum ModuleArtifactClassifier {
  SOURCES("sources"),
  JAVADOC("javadoc");

  private final String classifier;

  ModuleArtifactClassifier(String classifier) {
    this.classifier = classifier;
  }

  public String getName() {
    return this.classifier;
  }
}
