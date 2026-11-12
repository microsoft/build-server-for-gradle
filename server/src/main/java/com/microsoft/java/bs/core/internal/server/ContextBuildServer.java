// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.server;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;

import ch.epfl.scala.bsp4j.CleanCacheParams;
import ch.epfl.scala.bsp4j.CleanCacheResult;
import ch.epfl.scala.bsp4j.CompileResult;
import ch.epfl.scala.bsp4j.DebugSessionAddress;
import ch.epfl.scala.bsp4j.DependencyModulesParams;
import ch.epfl.scala.bsp4j.DependencyModulesResult;
import ch.epfl.scala.bsp4j.DependencySourcesParams;
import ch.epfl.scala.bsp4j.DependencySourcesResult;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.InitializeBuildResult;
import ch.epfl.scala.bsp4j.InverseSourcesParams;
import ch.epfl.scala.bsp4j.InverseSourcesResult;
import ch.epfl.scala.bsp4j.OutputPathsParams;
import ch.epfl.scala.bsp4j.OutputPathsResult;
import ch.epfl.scala.bsp4j.ResourcesParams;
import ch.epfl.scala.bsp4j.ResourcesResult;
import ch.epfl.scala.bsp4j.RunResult;
import ch.epfl.scala.bsp4j.SourcesParams;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.TestResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import com.microsoft.java.bs.core.internal.context.BuildContextService;

/**
 * BSP methods with the optional execution context on operation requests.
 */
public interface ContextBuildServer {
  @JsonRequest("build/initialize")
  CompletableFuture<InitializeBuildResult> buildInitialize(InitializeBuildParams params);

  @JsonNotification("build/initialized")
  void onBuildInitialized();

  @JsonRequest("build/shutdown")
  CompletableFuture<Object> buildShutdown();

  @JsonNotification("build/exit")
  void onBuildExit();

  @JsonRequest("workspace/buildTargets")
  CompletableFuture<WorkspaceBuildTargetsResult> workspaceBuildTargets();

  @JsonRequest("workspace/reload")
  CompletableFuture<Object> workspaceReload();

  @JsonRequest("workspace/contextOptions")
  CompletableFuture<BuildContextService.Options> workspaceContextOptions(
      BuildContextService.OptionsParams params);

  @JsonRequest("workspace/resolveContext")
  CompletableFuture<BuildContextService.Resolution> workspaceResolveContext(
      BuildContextService.ResolveParams params);

  @JsonRequest("buildTarget/sources")
  CompletableFuture<SourcesResult> buildTargetSources(SourcesParams params);

  @JsonRequest("buildTarget/inverseSources")
  CompletableFuture<InverseSourcesResult> buildTargetInverseSources(InverseSourcesParams params);

  @JsonRequest("buildTarget/dependencySources")
  CompletableFuture<DependencySourcesResult> buildTargetDependencySources(
      DependencySourcesParams params);

  @JsonRequest("buildTarget/resources")
  CompletableFuture<ResourcesResult> buildTargetResources(ResourcesParams params);

  @JsonRequest("buildTarget/outputPaths")
  CompletableFuture<OutputPathsResult> buildTargetOutputPaths(OutputPathsParams params);

  @JsonRequest("buildTarget/compile")
  CompletableFuture<CompileResult> buildTargetCompile(ContextCompileParams params);

  @JsonRequest("buildTarget/test")
  CompletableFuture<TestResult> buildTargetTest(ContextTestParams params);

  @JsonRequest("buildTarget/run")
  CompletableFuture<RunResult> buildTargetRun(ContextRunParams params);

  @JsonRequest("debugSession/start")
  CompletableFuture<DebugSessionAddress> debugSessionStart(ContextDebugParams params);

  @JsonRequest("buildTarget/cleanCache")
  CompletableFuture<CleanCacheResult> buildTargetCleanCache(CleanCacheParams params);

  @JsonRequest("buildTarget/dependencyModules")
  CompletableFuture<DependencyModulesResult> buildTargetDependencyModules(
      DependencyModulesParams params);
}
