package ch.epfl.scala.bsp4j.extended;

import java.util.List;
import java.util.Objects;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.JvmBuildTarget;

/**
 * Should be possible to remove this when Kotling is finalised in BSP spec.
 * See https://github.com/build-server-protocol/build-server-protocol/issues/520
 */
public class KotlinBuildTarget {
  private String languageVersion;
  private String apiVersion;
  private List<String> kotlincOptions;
  private List<BuildTargetIdentifier> associates;
  private JvmBuildTarget jvmBuildTarget;
  
  /**
   * Create a new instance of {@link KotlinBuildTarget}.
   */
  public KotlinBuildTarget(String languageVersion, String apiVersion,
      List<String> kotlincOptions, List<BuildTargetIdentifier> associates,
      JvmBuildTarget jvmBuildTarget) {
    this.languageVersion = languageVersion;
    this.apiVersion = apiVersion;
    this.kotlincOptions = kotlincOptions;
    this.associates = associates;
    this.jvmBuildTarget = jvmBuildTarget;
  }

  public String getLanguageVersion() {
    return languageVersion;
  }

  public void setLanguageVersion(String languageVersion) {
    this.languageVersion = languageVersion;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public void setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
  }

  public List<String> getKotlincOptions() {
    return kotlincOptions;
  }

  public void setKotlincOptions(List<String> kotlincOptions) {
    this.kotlincOptions = kotlincOptions;
  }

  public List<BuildTargetIdentifier> getAssociates() {
    return associates;
  }

  public void setAssociates(List<BuildTargetIdentifier> associates) {
    this.associates = associates;
  }

  public JvmBuildTarget getJvmBuildTarget() {
    return jvmBuildTarget;
  }

  public void setJvmBuildTarget(JvmBuildTarget jvmBuildTarget) {
    this.jvmBuildTarget = jvmBuildTarget;
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(languageVersion, apiVersion,
      kotlincOptions, associates, jvmBuildTarget);
    
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
    KotlinBuildTarget other = (KotlinBuildTarget) obj;
    return Objects.equals(languageVersion, other.languageVersion)
        && Objects.equals(apiVersion, other.apiVersion)
        && Objects.equals(kotlincOptions, other.kotlincOptions)
        && Objects.equals(associates, other.associates)
        && Objects.equals(jvmBuildTarget, other.jvmBuildTarget);
  }
}
