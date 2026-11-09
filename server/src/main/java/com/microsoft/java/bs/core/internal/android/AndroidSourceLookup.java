// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.android;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.debug.core.JavaBreakpointLocation;
import com.microsoft.java.debug.core.adapter.ISourceLookUpProvider;
import com.microsoft.java.debug.core.adapter.Source;
import com.microsoft.java.debug.core.adapter.SourceType;
import com.microsoft.java.debug.core.protocol.Types.SourceBreakpoint;

/**
 * Maps Java source files in a Gradle variant to JDWP class names.
 */
final class AndroidSourceLookup implements ISourceLookUpProvider {
  private static final Pattern PACKAGE = Pattern.compile(
      "^\\s*package\\s+([a-zA-Z_$][\\w$]*(?:\\.[a-zA-Z_$][\\w$]*)*)\\s*;",
      Pattern.MULTILINE);
  private final List<Path> sourceDirectories;

  AndroidSourceLookup(GradleSourceSet sourceSet) {
    sourceDirectories = new ArrayList<>();
    sourceSet.getSourceDirs().forEach(file -> sourceDirectories.add(file.toPath()));
    sourceSet.getGeneratedSourceDirs().forEach(file -> sourceDirectories.add(file.toPath()));
  }

  @Override
  public boolean supportsRealtimeBreakpointVerification() {
    return false;
  }

  @Override
  public String[] getFullyQualifiedName(String uri, int[] lines, int[] columns) {
    String name = className(uri);
    String[] result = new String[lines.length];
    java.util.Arrays.fill(result, name);
    return result;
  }

  @Override
  public JavaBreakpointLocation[] getBreakpointLocations(String sourceUri,
      SourceBreakpoint[] breakpoints) {
    String name = className(sourceUri);
    JavaBreakpointLocation[] result = new JavaBreakpointLocation[breakpoints.length];
    for (int i = 0; i < breakpoints.length; i++) {
      SourceBreakpoint breakpoint = breakpoints[i];
      JavaBreakpointLocation location = new JavaBreakpointLocation(breakpoint.line,
          breakpoint.column);
      location.setClassName(name);
      result[i] = location;
    }
    return result;
  }

  @Override
  public String getSourceFileURI(String fullyQualifiedName, String sourcePath) {
    String path = sourcePath == null || sourcePath.isBlank()
        ? fullyQualifiedName.replace('.', '/') + ".java" : sourcePath;
    for (Path root : sourceDirectories) {
      Path candidate = root.resolve(path).normalize();
      if (candidate.startsWith(root) && Files.isRegularFile(candidate)) {
        return candidate.toUri().toString();
      }
    }
    return null;
  }

  @Override
  public Source getSource(String fullyQualifiedName, String sourcePath) {
    return new Source(getSourceFileURI(fullyQualifiedName, sourcePath), SourceType.LOCAL);
  }

  @Override
  public String getSourceContents(String uri) {
    try {
      return Files.readString(pathFrom(uri));
    } catch (IOException | IllegalArgumentException e) {
      return "";
    }
  }

  @Override
  public List<MethodInvocation> findMethodInvocations(String uri, int line) {
    return List.of();
  }

  private String className(String uri) {
    try {
      Path path = pathFrom(uri);
      if (!path.getFileName().toString().endsWith(".java")) {
        return null;
      }
      String contents = Files.readString(path);
      Matcher matcher = PACKAGE.matcher(contents);
      String base = path.getFileName().toString();
      base = base.substring(0, base.length() - ".java".length());
      return matcher.find() ? matcher.group(1) + "." + base : base;
    } catch (IOException | IllegalArgumentException e) {
      return null;
    }
  }

  private Path pathFrom(String value) {
    return value.startsWith("file:") ? Path.of(URI.create(value)) : Path.of(value);
  }
}
