// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import com.microsoft.java.bs.core.internal.context.BuildContextService.Reference;
import com.microsoft.java.bs.core.internal.context.BuildContextService.ResolveParams;
import com.microsoft.java.bs.core.internal.context.BuildContextService.Scope;
import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.core.internal.services.BuildTargetService;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;

class BuildContextServiceTest {
  @Test
  void plainGradleTargetCanCompileWithoutAndroid() {
    BuildTargetIdentifier id = new BuildTargetIdentifier("file:///project?sourceSet=main");
    BuildTargetService targets = Mockito.mock(BuildTargetService.class);
    GradleBuildTarget target = Mockito.mock(GradleBuildTarget.class);
    GradleSourceSet sourceSet = Mockito.mock(GradleSourceSet.class);
    BuildTarget buildTarget = Mockito.mock(BuildTarget.class);
    when(targets.findBuildTarget(id)).thenReturn(target);
    when(target.getSourceSet()).thenReturn(sourceSet);
    when(sourceSet.hasTests()).thenReturn(true);
    when(target.getBuildTarget()).thenReturn(buildTarget);
    when(buildTarget.getId()).thenReturn(id);
    when(buildTarget.getDisplayName()).thenReturn("main");
    when(targets.allBuildTargets()).thenReturn(List.of(target));

    BuildContextService contexts = new BuildContextService(targets);
    contexts.configure(true, "file:///project");
    BuildContextService.OptionsParams optionsParams = new BuildContextService.OptionsParams();
    optionsParams.scope = new Scope();
    optionsParams.scope.targets = List.of(id);
    assertEquals(List.of("compile", "test"), contexts.options(optionsParams).operations);
    assertEquals(List.of(), contexts.options(optionsParams).dimensions);

    ResolveParams params = new ResolveParams();
    params.scope = optionsParams.scope;
    params.operation = "compile";
    params.selection = Map.of();
    BuildContextService.Resolution resolution = contexts.resolve(params);
    Reference reference = new Reference();
    reference.id = resolution.id;
    assertEquals(resolution, contexts.execution(reference, "compile", List.of(id)));
    assertThrows(IllegalArgumentException.class,
        () -> contexts.execution(reference, "test", List.of(id)));
    contexts.invalidate();
    assertThrows(IllegalArgumentException.class,
        () -> contexts.execution(reference, "compile", List.of(id)));
  }
}
