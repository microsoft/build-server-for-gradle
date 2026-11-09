// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.server;

import static com.microsoft.java.bs.core.Launcher.LOGGER;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;

import com.microsoft.java.bs.core.internal.log.BspTraceEntity;
import com.microsoft.java.bs.core.internal.services.BuildTargetService;
import com.microsoft.java.bs.core.internal.services.LifecycleService;
import com.microsoft.java.bs.core.internal.context.BuildContextService;
import com.microsoft.java.bs.core.internal.android.AndroidExecutionService;
import com.microsoft.java.bs.core.internal.android.AndroidDebugServer;
import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;

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
import ch.epfl.scala.bsp4j.JavaBuildServer;
import ch.epfl.scala.bsp4j.JavacOptionsParams;
import ch.epfl.scala.bsp4j.JavacOptionsResult;
import ch.epfl.scala.bsp4j.OutputPathsParams;
import ch.epfl.scala.bsp4j.OutputPathsResult;
import ch.epfl.scala.bsp4j.ResourcesParams;
import ch.epfl.scala.bsp4j.ResourcesResult;
import ch.epfl.scala.bsp4j.RunParams;
import ch.epfl.scala.bsp4j.RunResult;
import ch.epfl.scala.bsp4j.ScalaBuildServer;
import ch.epfl.scala.bsp4j.ScalaMainClassesParams;
import ch.epfl.scala.bsp4j.ScalaMainClassesResult;
import ch.epfl.scala.bsp4j.ScalaTestClassesParams;
import ch.epfl.scala.bsp4j.ScalaTestClassesResult;
import ch.epfl.scala.bsp4j.ScalacOptionsParams;
import ch.epfl.scala.bsp4j.ScalacOptionsResult;
import ch.epfl.scala.bsp4j.SourcesParams;
import ch.epfl.scala.bsp4j.SourcesResult;
import ch.epfl.scala.bsp4j.TestParams;
import ch.epfl.scala.bsp4j.TestResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;

/**
 * The implementation of the Build Server Protocol.
 */
public class GradleBuildServer implements ContextBuildServer, JavaBuildServer, ScalaBuildServer {

  private LifecycleService lifecycleService;

  private BuildTargetService buildTargetService;

  private final BuildContextService contexts;

  private final AndroidExecutionService android = new AndroidExecutionService();

  /** Create a build server with Gradle and context services. */
  public GradleBuildServer(LifecycleService lifecycleService,
      BuildTargetService buildTargetService) {
    this.lifecycleService = lifecycleService;
    this.buildTargetService = buildTargetService;
    this.contexts = new BuildContextService(buildTargetService);
  }

  @Override
  public CompletableFuture<InitializeBuildResult> buildInitialize(InitializeBuildParams params) {
    return handleRequest("build/initialize", cc -> {
      InitializeBuildResult result = lifecycleService.initializeServer(params);
      contexts.configure(lifecycleService.isBuildContextEnabled(), params.getRootUri());
      return result;
    });
  }

  @Override
  public void onBuildInitialized() {
    handleNotification("build/initialized", lifecycleService::onBuildInitialized, true /*async*/);
  }

  @Override
  public CompletableFuture<Object> buildShutdown() {
    return handleRequest("build/shutdown", cc ->
        lifecycleService.shutdown());
  }

  @Override
  public void onBuildExit() {
    handleNotification("build/exit", lifecycleService::exit, false /*async*/);
  }

  @Override
  public CompletableFuture<WorkspaceBuildTargetsResult> workspaceBuildTargets() {
    return handleRequest("workspace/buildTargets", cc ->
        buildTargetService.getWorkspaceBuildTargets());
  }

  @Override
  public CompletableFuture<Object> workspaceReload() {
    return handleRequest("workspace/reload", cc -> {
      buildTargetService.reloadWorkspace();
      contexts.invalidate();
      return null;
    });
  }

  @Override
  public CompletableFuture<BuildContextService.Options> workspaceContextOptions(
      BuildContextService.OptionsParams params) {
    return handleRequest("workspace/contextOptions", cc -> contexts.options(params));
  }

  @Override
  public CompletableFuture<BuildContextService.Resolution> workspaceResolveContext(
      BuildContextService.ResolveParams params) {
    return handleRequest("workspace/resolveContext", cc -> contexts.resolve(params));
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
    return handleRequest("buildTarget/dependencySources", cc ->
            buildTargetService.getBuildTargetDependencySources(params));
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

  public CompletableFuture<CompileResult> buildTargetCompile(CompileParams params) {
    return handleRequest("buildTarget/compile", cc -> buildTargetService.compile(params));
  }

  @Override
  public CompletableFuture<CompileResult> buildTargetCompile(ContextCompileParams params) {
    return handleRequest("buildTarget/compile", cc -> {
      contexts.execution(params.getExecutionContext(), "compile", params.getTargets());
      return buildTargetService.compile(params);
    });
  }

  public CompletableFuture<TestResult> buildTargetTest(TestParams params) {
    return handleRequest("buildTarget/test", cc -> buildTargetService.buildTargetTest(params));
  }

  @Override
  public CompletableFuture<TestResult> buildTargetTest(ContextTestParams params) {
    return handleRequest("buildTarget/test", cc -> {
      contexts.execution(params.getExecutionContext(), "test", params.getTargets());
      return buildTargetService.buildTargetTest(params);
    });
  }

  @Override
  public CompletableFuture<RunResult> buildTargetRun(ContextRunParams params) {
    return handleRequest("buildTarget/run", cc -> {
      BuildContextService.Resolution context = contexts.execution(params.getExecutionContext(),
          "run", java.util.List.of(params.getTarget()));
      GradleBuildTarget target = buildTargetService.findBuildTarget(params.getTarget());
      if (target == null || target.getSourceSet().getAndroidApplicationId() == null) {
        throw new IllegalArgumentException("Run requires an Android application variant");
      }
      if (context == null || "build".equals(context.buildPolicy)) {
        CompileParams compile = new CompileParams(java.util.List.of(params.getTarget()));
        compile.setOriginId(params.getOriginId());
        if (buildTargetService.compile(compile).getStatusCode()
            != ch.epfl.scala.bsp4j.StatusCode.OK) {
          RunResult failure = new RunResult(ch.epfl.scala.bsp4j.StatusCode.ERROR);
          failure.setOriginId(params.getOriginId());
          return failure;
        }
      }
      try {
        android.run(target.getSourceSet(), context == null
            ? soleAndroidDevice() : context.selection.get("device"));
        RunResult success = new RunResult(ch.epfl.scala.bsp4j.StatusCode.OK);
        success.setOriginId(params.getOriginId());
        return success;
      } catch (java.io.IOException e) {
        throw new IllegalArgumentException("Failed to run Android app: " + e.getMessage(), e);
      }
    });
  }

  @Override
  public CompletableFuture<DebugSessionAddress> debugSessionStart(ContextDebugParams params) {
    return handleRequest("debugSession/start", cc -> {
      if (params.getTargets() == null || params.getTargets().size() != 1) {
        throw new IllegalArgumentException("Android Debug requires one target");
      }
      BuildContextService.Resolution context = contexts.execution(params.getExecutionContext(),
          "debug", params.getTargets());
      GradleBuildTarget target = buildTargetService.findBuildTarget(params.getTargets().get(0));
      if (target == null || target.getSourceSet().getAndroidApplicationId() == null) {
        throw new IllegalArgumentException("Debug requires an Android application variant");
      }
      if (context == null || "build".equals(context.buildPolicy)) {
        CompileParams compile = new CompileParams(params.getTargets());
        if (buildTargetService.compile(compile).getStatusCode()
            != ch.epfl.scala.bsp4j.StatusCode.OK) {
          throw new IllegalStateException("The Android Debug build failed");
        }
      }
      String device = context == null ? soleAndroidDevice() : context.selection.get("device");
      try {
        int forwardedPort = android.launchForDebug(target.getSourceSet(), device);
        try {
          String address = new AndroidDebugServer(forwardedPort, target.getSourceSet(),
              device, android).start();
          return new AndroidDebugAddress(address);
        } catch (java.io.IOException e) {
          android.removeForward(device, forwardedPort);
          throw e;
        }
      } catch (java.io.IOException e) {
        throw new IllegalArgumentException("Failed to debug Android app: " + e.getMessage(), e);
      }
    });
  }

  private String soleAndroidDevice() {
    java.util.List<BuildContextService.Option> devices = AndroidExecutionService.devices();
    if (devices.size() != 1) {
      throw new IllegalArgumentException(
          "Select one Android device with build contexts or connect exactly one device");
    }
    return devices.get(0).id;
  }

  @Override
  public CompletableFuture<CleanCacheResult> buildTargetCleanCache(CleanCacheParams params) {
    return handleRequest("buildTarget/cleanCache", cc -> buildTargetService.cleanCache(params));
  }

  @Override
  public CompletableFuture<DependencyModulesResult> buildTargetDependencyModules(
      DependencyModulesParams params) {
    return handleRequest("buildTarget/dependencyModules", cc ->
        buildTargetService.getBuildTargetDependencyModules(params));
  }

  @Override
  public CompletableFuture<JavacOptionsResult> buildTargetJavacOptions(JavacOptionsParams params) {
    return handleRequest("buildTarget/javacOptions", cc ->
        buildTargetService.getBuildTargetJavacOptions(params));
  }

  @Override
  public CompletableFuture<ScalacOptionsResult> buildTargetScalacOptions(
      ScalacOptionsParams params) {
    return handleRequest("buildTarget/scalacOptions", cc ->
        buildTargetService.getBuildTargetScalacOptions(params));
  }

  @Override
  public CompletableFuture<ScalaTestClassesResult> buildTargetScalaTestClasses(
      ScalaTestClassesParams params) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'buildTargetScalaTestClasses'");
  }

  @Override
  public CompletableFuture<ScalaMainClassesResult> buildTargetScalaMainClasses(
      ScalaMainClassesParams params) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'buildTargetScalaMainClasses'");
  }

  private void handleNotification(String methodName, Runnable runnable, boolean async) {
    BspTraceEntity entity = new BspTraceEntity.Builder()
        .operationName(escapeMethodName(methodName))
        .build();
    LOGGER.log(Level.INFO, "Received notification '" + methodName + "'.", entity);
    if (async) {
      CompletableFuture.runAsync(runnable);
    } else {
      runnable.run();
    }
  }

  private <R> CompletableFuture<R> handleRequest(String methodName,
      Function<CancelChecker, R> supplier) {
    return runAsync(methodName, supplier);
  }

  public <T, R> CompletableFuture<R> handleRequest(String methodName,
      BiFunction<CancelChecker, T, R> function, T arg) {
    LOGGER.info("Received request '" + methodName + "'.");
    return runAsync(methodName, cancelChecker -> function.apply(cancelChecker, arg));
  }

  private <T> CompletableFuture<T> runAsync(String methodName, Function<CancelChecker, T> request) {
    long startTime = System.nanoTime();
    return CompletableFutures.computeAsync(request)
        .thenApply(Either::<Throwable, T>forRight)
        .exceptionally(Either::forLeft)
        .thenCompose(either -> {
          long elapsedTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
          return either.isLeft()
              ? failure(methodName, either.getLeft())
              : success(methodName, either.getRight(), elapsedTime);
        });
  }

  private <T> CompletableFuture<T> success(String methodName, T response, long elapsedTime) {
    BspTraceEntity entity = new BspTraceEntity.Builder()
        .operationName(escapeMethodName(methodName))
        .duration(String.valueOf(elapsedTime))
        .build();
    String message = String.format("Sending response '%s'. Processing request took %d ms.",
        methodName, elapsedTime);
    LOGGER.log(Level.INFO, message, entity);
    return CompletableFuture.completedFuture(response);
  }

  private <T> CompletableFuture<T> failure(String methodName, Throwable throwable) {
    String stackTrace = ExceptionUtils.getStackTrace(throwable);
    Throwable rootCause = ExceptionUtils.getRootCause(throwable);
    String rootCauseMessage = rootCause != null ? rootCause.getMessage() : null;
    BspTraceEntity entity = new BspTraceEntity.Builder()
        .operationName(escapeMethodName(methodName))
        .trace(stackTrace)
        .rootCauseMessage(rootCauseMessage)
        .build();
    String message = String.format("Failed to process '%s': %s", methodName, stackTrace);
    LOGGER.log(Level.SEVERE, message, entity);
    if (throwable instanceof ResponseErrorException) {
      return CompletableFuture.failedFuture(throwable);
    }
    return CompletableFuture.failedFuture(
        new ResponseErrorException(
            new ResponseError(ResponseErrorCode.InternalError,
            rootCauseMessage == null ? throwable.getMessage() : rootCauseMessage, null)));
  }

  private String escapeMethodName(String name) {
    return name.replace('/', '-');
  }
}
