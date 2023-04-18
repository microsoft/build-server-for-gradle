package com.microsoft.java.bs.contrib.gradle.model;

import java.io.File;

public interface JdkPlatform {
    public File getJavaHome();

    public String getJavaVersion();

    public String getSourceLanguageLevel();

    public String getTargetBytecodeVersion();
}
