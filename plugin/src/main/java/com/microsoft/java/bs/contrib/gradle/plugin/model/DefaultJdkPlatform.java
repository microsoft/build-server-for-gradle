package com.microsoft.java.bs.contrib.gradle.plugin.model;

import java.io.File;
import java.io.Serializable;

import com.microsoft.java.bs.contrib.gradle.model.JdkPlatform;

public class DefaultJdkPlatform implements JdkPlatform, Serializable {
    private static final long serialVersionUID = 1L;
    
    private File javaHome;

    private String javaVersion;

    private String sourceLanguageLevel;

    private String targetBytecodeVersion;

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

    public String getSourceLanguageLevel() {
        return sourceLanguageLevel;
    }

    public void setSourceLanguageLevel(String sourceLanguageLevel) {
        this.sourceLanguageLevel = sourceLanguageLevel;
    }

    public String getTargetBytecodeVersion() {
        return targetBytecodeVersion;
    }

    public void setTargetBytecodeVersion(String targetBytecodeVersion) {
        this.targetBytecodeVersion = targetBytecodeVersion;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((javaHome == null) ? 0 : javaHome.hashCode());
        result = prime * result + ((javaVersion == null) ? 0 : javaVersion.hashCode());
        result = prime * result + ((sourceLanguageLevel == null) ? 0 : sourceLanguageLevel.hashCode());
        result = prime * result + ((targetBytecodeVersion == null) ? 0 : targetBytecodeVersion.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultJdkPlatform other = (DefaultJdkPlatform) obj;
        if (javaHome == null) {
            if (other.javaHome != null)
                return false;
        } else if (!javaHome.equals(other.javaHome))
            return false;
        if (javaVersion == null) {
            if (other.javaVersion != null)
                return false;
        } else if (!javaVersion.equals(other.javaVersion))
            return false;
        if (sourceLanguageLevel == null) {
            if (other.sourceLanguageLevel != null)
                return false;
        } else if (!sourceLanguageLevel.equals(other.sourceLanguageLevel))
            return false;
        if (targetBytecodeVersion == null) {
            if (other.targetBytecodeVersion != null)
                return false;
        } else if (!targetBytecodeVersion.equals(other.targetBytecodeVersion))
            return false;
        return true;
    }
}
