// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.android;

import static com.microsoft.java.bs.core.Launcher.LOGGER;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.java.bs.core.internal.context.BuildContextService.Option;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;

/**
 * Finds Android devices and runs application variants on them.
 */
public class AndroidExecutionService {
  private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(90);
  private static volatile List<Option> cachedDevices = List.of();
  private static volatile long deviceCacheTime;

  /** Return connected devices as execution destinations. */
  public static synchronized List<Option> devices() {
    if (System.nanoTime() - deviceCacheTime < Duration.ofSeconds(5).toNanos()) {
      return cachedDevices;
    }
    try {
      String output = execute(null, Duration.ofSeconds(10), "devices", "-l");
      List<Option> result = new ArrayList<>();
      for (String line : output.split("\\R")) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 2 || !"device".equals(parts[1])) {
          continue;
        }
        String label = parts[0];
        for (String part : parts) {
          if (part.startsWith("model:")) {
            label = part.substring("model:".length()).replace('_', ' ');
          }
        }
        boolean emulator = parts[0].startsWith("emulator-");
        if (emulator) {
          try {
            String avd = execute(parts[0], Duration.ofSeconds(3), "emu", "avd", "name")
                .lines().findFirst().orElse("").trim();
            if (!avd.isBlank()) {
              label = avd.replace('_', ' ');
            }
          } catch (IOException ignored) {
            // Keep the model name.
          }
        }
        result.add(new Option(parts[0], label, null, "execution-destination",
            java.util.Map.of(
                "kind", emulator ? "emulator" : "device",
                "platform", java.util.Map.of("id", "android", "label", "Android"),
                "state", "Online",
                "identifier", java.util.Map.of("label", "Serial", "value", parts[0]))));
      }
      cachedDevices = List.copyOf(result);
      deviceCacheTime = System.nanoTime();
      return cachedDevices;
    } catch (IOException e) {
      return List.of();
    }
  }

  /** Install and launch one application variant. */
  public void run(GradleSourceSet sourceSet, String serial) throws IOException {
    install(sourceSet, serial);
    String component = resolveLauncher(sourceSet.getAndroidApplicationId(), serial);
    LOGGER.info("Launching " + sourceSet.getDisplayName() + " on " + serial);
    command(serial, COMMAND_TIMEOUT, "shell", "am", "start", "-W", "-n", component);
  }

  /** Install the APK for one application variant. */
  public void install(GradleSourceSet sourceSet, String serial) throws IOException {
    Path apk = findApk(sourceSet);
    LOGGER.info("Installing " + apk.getFileName() + " on " + serial);
    command(serial, Duration.ofMinutes(3), "install", "-r", apk.toString());
  }

  /** Launch the app in debug mode and forward its JDWP port. */
  public int launchForDebug(GradleSourceSet sourceSet, String serial) throws IOException {
    install(sourceSet, serial);
    String applicationId = sourceSet.getAndroidApplicationId();
    String component = resolveLauncher(applicationId, serial);
    command(serial, COMMAND_TIMEOUT, "shell", "am", "force-stop", applicationId);
    LOGGER.info("Launching " + sourceSet.getDisplayName() + " for debugging on " + serial);
    command(serial, COMMAND_TIMEOUT, "shell", "am", "start", "-D", "-n", component);
    String pid = waitForPid(serial, applicationId);
    String port = command(serial, COMMAND_TIMEOUT, "forward", "tcp:0", "jdwp:" + pid).trim();
    try {
      return Integer.parseInt(port);
    } catch (NumberFormatException e) {
      throw new IOException("adb did not return a JDWP port: " + port, e);
    }
  }

  private String waitForPid(String serial, String applicationId) throws IOException {
    long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
    while (System.nanoTime() < deadline) {
      try {
        String pid = command(serial, Duration.ofSeconds(3), "shell", "pidof", "-s",
            applicationId).trim();
        if (pid.matches("[0-9]+")) {
          return pid;
        }
      } catch (IOException ignored) {
        // The process may still be starting.
      }
      try {
        Thread.sleep(250);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while waiting for the Android app process", e);
      }
    }
    throw new IOException("Cannot find the Android app process for " + applicationId);
  }

  /** Remove a JDWP port forward after the DAP session ends. */
  public void removeForward(String serial, int port) {
    try {
      command(serial, Duration.ofSeconds(10), "forward", "--remove", "tcp:" + port);
    } catch (IOException ignored) {
      // The device may already be disconnected.
    }
  }

  /** Find the launch activity of an installed application. */
  public String resolveLauncher(String applicationId, String serial) throws IOException {
    String output = command(serial, COMMAND_TIMEOUT, "shell", "cmd", "package",
        "resolve-activity", "--brief", "-a", "android.intent.action.MAIN",
        "-c", "android.intent.category.LAUNCHER", applicationId);
    for (String line : output.split("\\R")) {
      String value = line.trim();
      if (value.startsWith(applicationId + "/")) {
        return value;
      }
    }
    throw new IOException("No launch activity found for " + applicationId);
  }

  /** Run an adb command for one device. */
  public String command(String serial, Duration timeout, String... args) throws IOException {
    return execute(serial, timeout, args);
  }

  private static String execute(String serial, Duration timeout, String... args)
      throws IOException {
    List<String> command = new ArrayList<>();
    command.add(adbPath());
    if (serial != null) {
      command.add("-s");
      command.add(serial);
    }
    command.addAll(List.of(args));
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    try {
      boolean done = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
      if (!done) {
        process.destroyForcibly();
        throw new IOException("adb command timed out: " + String.join(" ", args));
      }
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      if (process.exitValue() != 0) {
        throw new IOException("adb " + String.join(" ", args) + " failed: " + output.trim());
      }
      return output;
    } catch (InterruptedException e) {
      process.destroyForcibly();
      Thread.currentThread().interrupt();
      throw new IOException("adb command interrupted", e);
    }
  }

  private static String adbPath() {
    String sdk = System.getenv("ANDROID_HOME");
    if (sdk == null || sdk.isBlank()) {
      sdk = System.getenv("ANDROID_SDK_ROOT");
    }
    if (sdk != null && !sdk.isBlank()) {
      File adb = new File(sdk, "platform-tools/adb");
      if (adb.canExecute()) {
        return adb.getAbsolutePath();
      }
    }
    return "adb";
  }

  /** Find the APK named by Gradle output metadata. */
  public Path findApk(GradleSourceSet sourceSet) throws IOException {
    File buildDir = sourceSet.getAndroidBuildDir();
    if (buildDir == null) {
      throw new IOException("Android build output directory is unknown");
    }
    Path apkDir = buildDir.toPath().resolve("outputs/apk");
    if (!Files.isDirectory(apkDir)) {
      throw new IOException("No APK output found for " + sourceSet.getDisplayName());
    }
    try (Stream<Path> files = Files.walk(apkDir, 5)) {
      List<Path> metadata = files.filter(path -> path.getFileName().toString()
          .equals("output-metadata.json")).sorted(Comparator.comparing(Path::toString)).toList();
      for (Path path : metadata) {
        Path result = apkFromMetadata(path, sourceSet);
        if (result != null) {
          return result;
        }
      }
    }
    throw new IOException("No APK output found for " + sourceSet.getDisplayName()
        + ". Build this variant first.");
  }

  private Path apkFromMetadata(Path metadata, GradleSourceSet sourceSet) throws IOException {
    JsonObject json = JsonParser.parseString(Files.readString(metadata)).getAsJsonObject();
    if (!sourceSet.getSourceSetName().equals(string(json, "variantName"))
        || !sourceSet.getAndroidApplicationId().equals(string(json, "applicationId"))) {
      return null;
    }
    JsonArray elements = json.getAsJsonArray("elements");
    if (elements == null) {
      return null;
    }
    for (JsonElement element : elements) {
      if (!element.isJsonObject()) {
        continue;
      }
      String outputFile = string(element.getAsJsonObject(), "outputFile");
      if (outputFile == null) {
        continue;
      }
      Path apk = metadata.getParent().resolve(outputFile).normalize();
      if (apk.startsWith(metadata.getParent()) && Files.isRegularFile(apk)) {
        return apk;
      }
    }
    return null;
  }

  private static String string(JsonObject object, String key) {
    JsonElement element = object.get(key);
    return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
  }
}
