package com.microsoft.java.bs.core.internal.utils;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Runtime.Version;

/**
 * Utility class for Java.
 */
public class JavaUtils {

  private JavaUtils() {}

  /**
   * Reads the Java Version from the given JDK file.
   *
   * @param jdkFile JDK file to read the java version from.
   * @return Java Version of the given JDK or null upon failure.
   * @throws IOException If an I/O exception occurs while executing the process
   *        for extracting the java version.
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
   * @param javaVersion Java version to check for compatibility.
   * @param oldestJavaVersion oldest compatible Java version.
   * @param latestJavaVersion latest compatible Java version.
   * @return true if given javaVersion is less than or equal to the latestJavaVersion
   *        and greater than or equal to the oldestJavaVersion.
   * @throws IllegalArgumentException If any of the given version strings cannot be
   *        parsed as a valid version.
   */
  public static boolean isCompatible(
      @Nonnull String javaVersion,
      @Nonnull String oldestJavaVersion,
      @Nonnull String latestJavaVersion
  ) throws IllegalArgumentException {

    Version versionToCheck = Version.parse(javaVersion);
    Version oldestVersion = Version.parse(oldestJavaVersion);
    Version latestVersion = Version.parse(latestJavaVersion);

    return versionToCheck.feature() >= oldestVersion.feature()
        && versionToCheck.feature() <= latestVersion.feature();

  }

}