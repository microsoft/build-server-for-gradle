package com.microsoft.java.bs.gradle.plugin.model;

import java.io.File;
import java.util.List;
import java.util.Set;

public class AndroidSourceSet {

  private String name;
  private Set<File> aidlDirectories;
  private Set<File> assetsDirectories;
  private Set<File> cDirectories;
  private Set<File> cppDirectories;
  private List<File> customDirectories;
  private Set<File> javaDirectories;
  private Set<File> kotlinDirectories;
  private File manifestFile;
  private Set<File> mlModelDirectories;
  private Set<File> renderScriptDirectories;
  private Set<File> resDirectories;
  private Set<File> resourceDirectories;
  private Set<File> shaderDirectories;
  private Set<File> compileClasspath;

  public AndroidSourceSet() {
  }

  public AndroidSourceSet(
      String name,
      Set<File> aidlDirectories,
      Set<File> assetsDirectories,
      Set<File> cDirectories,
      Set<File> cppDirectories,
      List<File> customDirectories,
      Set<File> javaDirectories,
      Set<File> kotlinDirectories,
      File manifestFile,
      Set<File> mlModelDirectories,
      Set<File> renderScriptDirectories,
      Set<File> resDirectories,
      Set<File> resourceDirectories,
      Set<File> shaderDirectories,
      Set<File> compileClasspath
  ) {
    this.name = name;
    this.aidlDirectories = aidlDirectories;
    this.assetsDirectories = assetsDirectories;
    this.cDirectories = cDirectories;
    this.cppDirectories = cppDirectories;
    this.customDirectories = customDirectories;
    this.javaDirectories = javaDirectories;
    this.kotlinDirectories = kotlinDirectories;
    this.manifestFile = manifestFile;
    this.mlModelDirectories = mlModelDirectories;
    this.renderScriptDirectories = renderScriptDirectories;
    this.resDirectories = resDirectories;
    this.resourceDirectories = resourceDirectories;
    this.shaderDirectories = shaderDirectories;
    this.compileClasspath = compileClasspath;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<File> getAidlDirectories() {
    return aidlDirectories;
  }

  public void setAidlDirectories(Set<File> aidlDirectories) {
    this.aidlDirectories = aidlDirectories;
  }

  public Set<File> getAssetsDirectories() {
    return assetsDirectories;
  }

  public void setAssetsDirectories(Set<File> assetsDirectories) {
    this.assetsDirectories = assetsDirectories;
  }

  public Set<File> getCDirectories() {
    return cDirectories;
  }

  public void setCDirectories(Set<File> cDirectories) {
    this.cDirectories = cDirectories;
  }

  public Set<File> getCppDirectories() {
    return cppDirectories;
  }

  public void setCppDirectories(Set<File> cppDirectories) {
    this.cppDirectories = cppDirectories;
  }

  public List<File> getCustomDirectories() {
    return customDirectories;
  }

  public void setCustomDirectories(List<File> customDirectories) {
    this.customDirectories = customDirectories;
  }

  public Set<File> getJavaDirectories() {
    return javaDirectories;
  }

  public void setJavaDirectories(Set<File> javaDirectories) {
    this.javaDirectories = javaDirectories;
  }

  public Set<File> getKotlinDirectories() {
    return kotlinDirectories;
  }

  public void setKotlinDirectories(Set<File> kotlinDirectories) {
    this.kotlinDirectories = kotlinDirectories;
  }

  public File getManifestFile() {
    return manifestFile;
  }

  public void setManifestFile(File manifestFile) {
    this.manifestFile = manifestFile;
  }

  public Set<File> getMlModelDirectories() {
    return mlModelDirectories;
  }

  public void setMlModelDirectories(Set<File> mlModelDirectories) {
    this.mlModelDirectories = mlModelDirectories;
  }

  public Set<File> getRenderScriptDirectories() {
    return renderScriptDirectories;
  }

  public void setRenderScriptDirectories(Set<File> renderScriptDirectories) {
    this.renderScriptDirectories = renderScriptDirectories;
  }

  public Set<File> getResDirectories() {
    return resDirectories;
  }

  public void setResDirectories(Set<File> resDirectories) {
    this.resDirectories = resDirectories;
  }

  public Set<File> getResourceDirectories() {
    return resourceDirectories;
  }

  public void setResourceDirectories(Set<File> resourceDirectories) {
    this.resourceDirectories = resourceDirectories;
  }

  public Set<File> getShaderDirectories() {
    return shaderDirectories;
  }

  public void setShaderDirectories(Set<File> shaderDirectories) {
    this.shaderDirectories = shaderDirectories;
  }

  public Set<File> getCompileClasspath() {
    return compileClasspath;
  }

  public void setCompileClasspath(Set<File> compileClasspath) {
    this.compileClasspath = compileClasspath;
  }

}
