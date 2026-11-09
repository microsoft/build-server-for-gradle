// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.context;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.core.internal.services.BuildTargetService;

/**
 * Resolves execution choices for a BSP connection.
 */
public class BuildContextService {
  private final BuildTargetService targets;
  private final Map<String, Resolution> resolved = new LinkedHashMap<>();
  private volatile boolean enabled;
  private volatile String workspaceUri;

  public BuildContextService(BuildTargetService targets) {
    this.targets = targets;
  }

  /** Set the negotiated extension state for this connection. */
  public synchronized void configure(boolean enabled, String workspaceUri) {
    this.enabled = enabled;
    this.workspaceUri = workspaceUri;
    resolved.clear();
  }

  /** Discard contexts when the workspace changes. */
  public synchronized void invalidate() {
    resolved.clear();
  }

  /** List targets and execution choices for a scope. */
  public Options options(OptionsParams params) {
    requireEnabled();
    Scope scope = params == null ? null : params.scope;
    validateScope(scope);
    Map<String, String> selection = params == null || params.selection == null
        ? Collections.emptyMap() : params.selection;
    List<Dimension> dimensions = new ArrayList<>();
    List<BuildTargetIdentifier> selectedTargets = scope == null ? null : scope.targets;
    if (selectedTargets == null) {
      List<Option> choices = new ArrayList<>();
      for (GradleBuildTarget target : targets.allBuildTargets()) {
        BuildTargetIdentifier id = target.getBuildTarget().getId();
        choices.add(new Option(id.getUri(), target.getBuildTarget().getDisplayName()));
      }
      choices.sort((a, b) -> a.label.compareToIgnoreCase(b.label));
      dimensions.add(new Dimension("target", "Target", true, choices));
      String targetUri = selection.get("target");
      if (targetUri != null) {
        selectedTargets = List.of(new BuildTargetIdentifier(targetUri));
        targetFor(selectedTargets.get(0));
      }
    }
    boolean android = selectedTargets != null && selectedTargets.size() == 1
        && targetFor(selectedTargets.get(0)).getSourceSet().getAndroidApplicationId() != null;
    if (android) {
      dimensions.add(new Dimension("device", "Android device", false, androidDevices()));
    }
    List<String> operations = new ArrayList<>();
    operations.add("compile");
    if (selectedTargets == null || selectedTargets.stream()
        .allMatch(id -> targetFor(id).getSourceSet().hasTests())) {
      operations.add("test");
    }
    if (android) {
      operations.add("run");
      operations.add("debug");
    }
    return new Options(dimensions, new HashMap<>(selection), operations);
  }

  /** Create a context for one operation and target set. */
  public synchronized Resolution resolve(ResolveParams params) {
    requireEnabled();
    if (params == null || !Arrays.asList("compile", "test", "run", "debug")
        .contains(params.operation)) {
      throw new IllegalArgumentException("Invalid build context operation");
    }
    validateScope(params.scope);
    String policy = params.buildPolicy == null ? "build" : params.buildPolicy;
    if (!"build".equals(policy) && !"reuse".equals(policy)) {
      throw new IllegalArgumentException("Invalid build policy");
    }
    Map<String, String> selection = params.selection == null
        ? new HashMap<>() : new HashMap<>(params.selection);
    List<BuildTargetIdentifier> selectedTargets = params.scope == null
        ? null : params.scope.targets;
    if (selectedTargets == null) {
      String targetUri = selection.get("target");
      if (targetUri == null) {
        throw new IllegalArgumentException("Select a target");
      }
      selectedTargets = List.of(new BuildTargetIdentifier(targetUri));
    }
    if (selectedTargets.isEmpty() || selectedTargets.stream().distinct().count()
        != selectedTargets.size()) {
      throw new IllegalArgumentException("Select distinct targets");
    }
    final boolean android = selectedTargets.size() == 1
        && targetFor(selectedTargets.get(0)).getSourceSet().getAndroidApplicationId() != null;
    for (BuildTargetIdentifier target : selectedTargets) {
      targetFor(target);
    }
    if ("test".equals(params.operation) && selectedTargets.stream()
        .anyMatch(id -> !targetFor(id).getSourceSet().hasTests())) {
      throw new IllegalArgumentException("Test requires a test target");
    }
    if ("compile".equals(params.operation) && !"build".equals(policy)) {
      throw new IllegalArgumentException("Compile requires the build policy");
    }
    if ("reuse".equals(policy) && (!android || "test".equals(params.operation))) {
      throw new IllegalArgumentException("This target does not support execution without building");
    }
    if (("run".equals(params.operation) || "debug".equals(params.operation)) && !android) {
      throw new IllegalArgumentException("Run and Debug require an Android application variant");
    }
    if (android && ("run".equals(params.operation) || "debug".equals(params.operation))) {
      String device = selection.get("device");
      List<Option> devices = androidDevices();
      if (device == null && devices.size() == 1) {
        device = devices.get(0).id;
        selection.put("device", device);
      }
      if (device == null || !devices.stream().map(option -> option.id)
          .toList().contains(device)) {
        throw new IllegalArgumentException("Select a connected Android device");
      }
    }
    if (selection.keySet().stream().anyMatch(key -> !key.equals("target")
        && !key.equals("device"))) {
      throw new IllegalArgumentException("Unknown build context selection");
    }
    String id = UUID.randomUUID().toString();
    Resolution result = new Resolution(id, params.operation, selectedTargets, selection, policy);
    if (resolved.size() >= 128) {
      resolved.remove(resolved.keySet().iterator().next());
    }
    resolved.put(id, result);
    return result;
  }

  /** Check that a request uses the resolved operation and targets. */
  public synchronized Resolution execution(Reference reference, String operation,
      List<BuildTargetIdentifier> requestedTargets) {
    if (!enabled) {
      if (reference == null) {
        return null;
      }
      throw new IllegalArgumentException("Build contexts were not negotiated");
    }
    if (reference == null || reference.id == null) {
      throw new IllegalArgumentException("Resolve a build context before " + operation);
    }
    Resolution result = resolved.get(reference.id);
    if (result == null || !Objects.equals(result.operation, operation)
        || requestedTargets == null || requestedTargets.isEmpty()
        || !result.targets.containsAll(requestedTargets)
        || !requestedTargets.containsAll(result.targets)
        || result.targets.size() != requestedTargets.size()) {
      throw new IllegalArgumentException("The build context does not match this request");
    }
    for (BuildTargetIdentifier target : result.targets) {
      targetFor(target);
    }
    if (result.selection.containsKey("device")
        && androidDevices().stream().noneMatch(option ->
            option.id.equals(result.selection.get("device")))) {
      throw new IllegalArgumentException("The selected Android device is no longer connected");
    }
    return result;
  }

  private void validateScope(Scope scope) {
    if (scope == null) {
      return;
    }
    if (scope.workspaceUri != null
        && !URI.create(scope.workspaceUri).normalize()
            .equals(URI.create(workspaceUri).normalize())) {
      throw new IllegalArgumentException("The workspace does not belong to this connection");
    }
    if (scope.targets != null) {
      if (scope.targets.isEmpty()) {
        throw new IllegalArgumentException("The scope has no targets");
      }
      for (BuildTargetIdentifier target : scope.targets) {
        targetFor(target);
      }
    }
  }

  private GradleBuildTarget targetFor(BuildTargetIdentifier id) {
    GradleBuildTarget target = targets.findBuildTarget(id);
    if (target == null) {
      throw new IllegalArgumentException("Unknown build target: " + id.getUri());
    }
    return target;
  }

  private void requireEnabled() {
    if (!enabled) {
      throw new IllegalArgumentException("Build contexts were not negotiated");
    }
  }

  private List<Option> androidDevices() {
    return com.microsoft.java.bs.core.internal.android.AndroidExecutionService.devices();
  }

  /** The workspace and optional target set. */
  public static final class Scope {
    public String workspaceUri;
    public List<BuildTargetIdentifier> targets;
  }

  /** Parameters for workspace/contextOptions. */
  public static final class OptionsParams {
    public Scope scope;
    public Map<String, String> selection;
  }

  /** Parameters for workspace/resolveContext. */
  public static final class ResolveParams {
    public Scope scope;
    public String operation;
    public Map<String, String> selection;
    public String buildPolicy;
  }

  /** A reference sent with an operation request. */
  public static final class Reference {
    public String id;
  }

  /** One value in a context dimension. */
  public static final class Option {
    public final String id;
    public final String label;
    public final String description;
    public final String dataKind;
    public final Object data;

    /** Create an option with a label. */
    public Option(String id, String label) {
      this(id, label, null);
    }

    /** Create an option with a description. */
    public Option(String id, String label, String description) {
      this(id, label, description, null, null);
    }

    /** Create an option with structured metadata. */
    public Option(String id, String label, String description, String dataKind, Object data) {
      this.id = id;
      this.label = label;
      this.description = description;
      this.dataKind = dataKind;
      this.data = data;
    }
  }

  /** A selectable execution setting. */
  public static final class Dimension {
    public final String id;
    public final String label;
    public final String kind = "enum";
    public final boolean required;
    public final List<Option> options;

    /** Create a dimension. */
    public Dimension(String id, String label, boolean required, List<Option> options) {
      this.id = id;
      this.label = label;
      this.required = required;
      this.options = options;
    }
  }

  /** The available settings for a scope. */
  public static final class Options {
    public final List<Dimension> dimensions;
    public final Map<String, String> selection;
    public final List<String> operations;

    /** Create a set of options. */
    public Options(List<Dimension> dimensions, Map<String, String> selection,
        List<String> operations) {
      this.dimensions = dimensions;
      this.selection = selection;
      this.operations = operations;
    }
  }

  /** A resolved execution choice. */
  public static final class Resolution {
    public final String id;
    public final String operation;
    public final List<BuildTargetIdentifier> targets;
    public final Map<String, String> selection;
    public final String buildPolicy;

    /** Create a resolved context. */
    public Resolution(String id, String operation, List<BuildTargetIdentifier> targets,
        Map<String, String> selection, String buildPolicy) {
      this.id = id;
      this.operation = operation;
      this.targets = List.copyOf(targets);
      this.selection = Map.copyOf(selection);
      this.buildPolicy = buildPolicy;
    }
  }
}
