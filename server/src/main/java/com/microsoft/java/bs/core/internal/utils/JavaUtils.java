package com.microsoft.java.bs.core.internal.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Runtime.Version;

/**
 * Utility class for Java.
 */
public class JavaUtils {

  public JavaUtils() {
  }

  /**
   * Reads the Java Version from the given JDK file.
   *
   * @param jdkFile JDK file to read the java version from.
   * @return Java Version of the given JDK or null upon failure.
   */
  public static String getJavaVersionFromFile(File jdkFile) throws IOException {

    ProcessBuilder processBuilder =
        new ProcessBuilder(jdkFile.getAbsolutePath() + "/bin/java", "-version");

    return getJavaVersionFromFile(processBuilder);

  }

  static String getJavaVersionFromFile(ProcessBuilder processBuilder) throws IOException {

    Process process = processBuilder
        .redirectErrorStream(true)
        .start();

    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

    String versionLine = reader.readLine();
    while (versionLine != null && !versionLine.contains("version")) {
      versionLine = reader.readLine();
    }

    process.destroy();

    if (versionLine != null) {
      versionLine = versionLine.split("\"")[1];
    }

    return versionLine;

  }

  /**
   * Checks if the given Java Version is compatible based on feature release.
   *
   * @param javaVersion Java version to compare.
   * @param minJavaVersion min Java version for compatibility check.
   * @param maxJavaVersion max Java version for compatibility check.
   * @return true if given javaVersion is less than or equal to the maxJavaVersion
   *        and greater than or equal to the minJavaVersion.
   */
  public static Boolean isCompatible(
      String javaVersion,
      String minJavaVersion,
      String maxJavaVersion
  ) {

    Version versionToCheck = Version.parse(javaVersion);
    Version minVersion = Version.parse(minJavaVersion);
    Version highestVersion = Version.parse(maxJavaVersion);

    return versionToCheck.feature() >= minVersion.feature()
        && versionToCheck.feature() <= highestVersion.feature();

  }

}