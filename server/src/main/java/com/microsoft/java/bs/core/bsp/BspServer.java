package com.microsoft.java.bs.core.bsp;

import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.microsoft.java.bs.core.log.InjectLogger;
import com.microsoft.java.bs.core.services.BuildTargetsService;
import com.microsoft.java.bs.core.services.CompileService;
import com.microsoft.java.bs.core.services.LifecycleService;

import ch.epfl.scala.bsp4j.BuildServer;
import ch.epfl.scala.bsp4j.CleanCacheParams;
import ch.epfl.scala.bsp4j.CleanCacheResult;
import ch.epfl.scala.bsp4j.CompileParams;
import ch.epfl.scala.bsp4j.CompileResult;
import ch.epfl.scala.bsp4j.DebugSessionAddress;
import ch.epfl.scala.bsp4j.DebugSessionParams;
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
import ch.epfl.scala.bsp4j.RunParams;
import ch.epfl.scala.bsp4j.RunResult;
import ch.epfl.scala.bsp4j.SourcesParams;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.TestParams;
import ch.epfl.scala.bsp4j.TestResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;

/**
 * The implementation of the Build Server Protocol.
 */
public class BspServer implements BuildServer {

  @InjectLogger
  Logger logger;

  @Inject
  BuildTargetsService buildTargetsService;

  @Inject
  LifecycleService lifecycleService;

  @Inject
  CompileService compileService;

  @Override
  public CompletableFuture<InitializeBuildResult> buildInitialize(InitializeBuildParams params) {
    logger.info(">> build/initialize");
    return CompletableFuture.completedFuture(lifecycleService.buildInitialize(params));
  }

  @Override
  public void onBuildInitialized() {
    logger.info(">> build/initialized");
    lifecycleService.initialized();
  }

  @Override
  public CompletableFuture<Object> buildShutdown() {
    logger.info(">> build/shutdown");
    return CompletableFuture.completedFuture(lifecycleService.buildShutdown());
  }

  @Override
  public void onBuildExit() {
    logger.info(">> build/exit");
    lifecycleService.exit();
  }

  @Override
  public CompletableFuture<WorkspaceBuildTargetsResult> workspaceBuildTargets() {
    logger.info(">> workspace/buildTargets");
    return CompletableFuture.completedFuture(buildTargetsService.workspaceBuildTargets());
  }

  @Override
  public CompletableFuture<Object> workspaceReload() {
    logger.info(">> workspace/reload");
    return CompletableFuture.completedFuture(buildTargetsService.workspaceReload());
  }

  @Override
  public CompletableFuture<SourcesResult> buildTargetSources(SourcesParams params) {
    logger.info(">> buildTarget/sources");
    return CompletableFuture.completedFuture(buildTargetsService.buildTargetSources(params));
  }

  @Override
  public CompletableFuture<InverseSourcesResult> buildTargetInverseSources(
      InverseSourcesParams params) {
    throw new UnsupportedOperationException("Unimplemented method 'buildTargetInverseSources'");
  }

  @Override
  public CompletableFuture<DependencySourcesResult> buildTargetDependencySources(
      DependencySourcesParams params) {
    throw new UnsupportedOperationException("Unimplemented method 'buildTargetDependencySources'");
  }

  @Override
  public CompletableFuture<ResourcesResult> buildTargetResources(ResourcesParams params) {
    logger.info(">> buildTarget/resources");
    return CompletableFuture.completedFuture(buildTargetsService.buildTargetResources(params));
  }

  @Override
  public CompletableFuture<OutputPathsResult> buildTargetOutputPaths(OutputPathsParams params) {
    logger.info(">> buildTarget/outputPaths");
    return CompletableFuture.completedFuture(buildTargetsService.buildTargetOutputPaths(params));
  }

  @Override
  public CompletableFuture<CompileResult> buildTargetCompile(CompileParams params) {
    logger.info(">> buildTarget/compile");
    return CompletableFuture.completedFuture(compileService.buildTargetCompile(params));
  }

  @Override
  public CompletableFuture<TestResult> buildTargetTest(TestParams params) {
    throw new UnsupportedOperationException("Unimplemented method 'buildTargetTest'");
  }

  @Override
  public CompletableFuture<RunResult> buildTargetRun(RunParams params) {
    throw new UnsupportedOperationException("Unimplemented method 'buildTargetRun'");
  }

  @Override
  public CompletableFuture<DebugSessionAddress> debugSessionStart(DebugSessionParams params) {
    throw new UnsupportedOperationException("Unimplemented method 'debugSessionStart'");
  }

  @Override
  public CompletableFuture<CleanCacheResult> buildTargetCleanCache(CleanCacheParams params) {
    throw new UnsupportedOperationException("Unimplemented method 'buildTargetCleanCache'");
  }

  @Override
  public CompletableFuture<DependencyModulesResult> buildTargetDependencyModules(
      DependencyModulesParams params) {
    logger.info(">> buildTarget/dependencyModules");
    return CompletableFuture.completedFuture(
      buildTargetsService.buildTargetDependencyModules(params));
  }
}
