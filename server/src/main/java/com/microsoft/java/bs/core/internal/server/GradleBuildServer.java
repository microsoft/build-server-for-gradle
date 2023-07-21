package com.microsoft.java.bs.core.internal.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.java.bs.core.internal.services.BuildTargetService;
import com.microsoft.java.bs.core.internal.services.LifecycleService;

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
public class GradleBuildServer implements BuildServer {

  private static final Logger logger = LoggerFactory.getLogger(GradleBuildServer.class);

  private LifecycleService lifecycleService;

  private BuildTargetService buildTargetService;

  public GradleBuildServer(LifecycleService lifecycleService,
      BuildTargetService buildTargetService) {
    this.lifecycleService = lifecycleService;
    this.buildTargetService = buildTargetService;
  }

  @Override
  public CompletableFuture<InitializeBuildResult> buildInitialize(InitializeBuildParams params) {
    return handleRequest("build/initialize", cc -> lifecycleService.initializeServer(params));
  }

  @Override
  public void onBuildInitialized() {
    handleNotification("build/initialized", lifecycleService::onBuildInitialized);
  }

  @Override
  public CompletableFuture<Object> buildShutdown() {
    return handleRequest("buildTarget/shutdown", cc ->
        lifecycleService.shutdown());
  }

  @Override
  public void onBuildExit() {
    handleNotification("build/exit", lifecycleService::exit);
  }

  @Override
  public CompletableFuture<WorkspaceBuildTargetsResult> workspaceBuildTargets() {
    return handleRequest("workspace/buildTargets", cc ->
        buildTargetService.getWorkspaceBuildTargets());
  }

  @Override
  public CompletableFuture<Object> workspaceReload() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'workspaceReload'");
  }

  @Override
  public CompletableFuture<SourcesResult> buildTargetSources(SourcesParams params) {
    return handleRequest("buildTarget/sources", cc ->
        buildTargetService.getBuildTargetSources(params));
  }

  @Override
  public CompletableFuture<InverseSourcesResult> buildTargetInverseSources(
      InverseSourcesParams params) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'buildTargetInverseSources'");
  }

  @Override
  public CompletableFuture<DependencySourcesResult> buildTargetDependencySources(
      DependencySourcesParams params) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'buildTargetDependencySources'");
  }

  @Override
  public CompletableFuture<ResourcesResult> buildTargetResources(ResourcesParams params) {
    return handleRequest("buildTarget/resources", cc ->
        buildTargetService.getBuildTargetResources(params));
  }

  @Override
  public CompletableFuture<OutputPathsResult> buildTargetOutputPaths(OutputPathsParams params) {
    return handleRequest("buildTarget/outputPaths", cc ->
        buildTargetService.getBuildTargetOutputPaths(params));
  }

  @Override
  public CompletableFuture<CompileResult> buildTargetCompile(CompileParams params) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'buildTargetCompile'");
  }

  @Override
  public CompletableFuture<TestResult> buildTargetTest(TestParams params) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'buildTargetTest'");
  }

  @Override
  public CompletableFuture<RunResult> buildTargetRun(RunParams params) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'buildTargetRun'");
  }

  @Override
  public CompletableFuture<DebugSessionAddress> debugSessionStart(DebugSessionParams params) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'debugSessionStart'");
  }

  @Override
  public CompletableFuture<CleanCacheResult> buildTargetCleanCache(CleanCacheParams params) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'buildTargetCleanCache'");
  }

  @Override
  public CompletableFuture<DependencyModulesResult> buildTargetDependencyModules(
      DependencyModulesParams params) {
    return handleRequest("buildTarget/dependencyModules", cc ->
        buildTargetService.getBuildTargetDependencyModules(params));
  }

  private void handleNotification(String methodName, Runnable runnable) {
    logger.info(">> {} received.", methodName);
    runnable.run();
  }

  private <R> CompletableFuture<R> handleRequest(String methodName,
      Function<CancelChecker, R> supplier) {
    logger.info(">> {} starts.", methodName);
    return runAsync(methodName, supplier);
  }

  public <T, R> CompletableFuture<R> handleRequest(String methodName,
      BiFunction<CancelChecker, T, R> function, T arg) {
    logger.info(">> {} starts with arguments: {}", methodName, arg);
    return runAsync(methodName, cancelChecker -> function.apply(cancelChecker, arg));
  }

  private <T> CompletableFuture<T> runAsync(String methodName, Function<CancelChecker, T> request) {
    return CompletableFutures.computeAsync(request)
        .thenApply(Either::<Throwable, T>forRight)
        .exceptionally(Either::forLeft)
        .thenCompose(
            either ->
                either.isLeft()
                    ? failure(methodName, either.getLeft())
                    : success(methodName, either.getRight())
        );
  }

  private <T> CompletableFuture<T> success(String methodName, T response) {
    logger.info(">> {} finished.", methodName);
    return CompletableFuture.completedFuture(response);
  }

  private <T> CompletableFuture<T> failure(String methodName, Throwable throwable) {
    logger.error(">> {} failed: {}", methodName, throwable.getMessage());
    if (throwable instanceof ResponseErrorException) {
      return CompletableFuture.failedFuture(throwable);
    }
    return CompletableFuture.failedFuture(
        new ResponseErrorException(
            new ResponseError(ResponseErrorCode.InternalError, throwable.getMessage(), null)));
  }
}
