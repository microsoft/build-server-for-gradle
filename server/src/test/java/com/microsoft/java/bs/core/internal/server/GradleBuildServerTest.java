package com.microsoft.java.bs.core.internal.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CompletionException;

import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.Constants;
import com.microsoft.java.bs.core.internal.managers.BuildTargetsManager;
import com.microsoft.java.bs.core.internal.model.GradleBuildTarget;
import com.microsoft.java.bs.core.internal.services.BuildTargetService;
import com.microsoft.java.bs.core.internal.services.LifecycleService;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;

import ch.epfl.scala.bsp4j.BuildClientCapabilities;
import ch.epfl.scala.bsp4j.BuildServerCapabilities;
import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.InitializeBuildResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;

class GradleBuildServerTest {
  @Test
  void testBuildInitialize() {
    BuildClientCapabilities capabilities = new BuildClientCapabilities(Arrays.asList("java"));
    InitializeBuildParams params = new InitializeBuildParams(
        "test-client",
        "0.1.0",
        "0.1.0",
        Paths.get(System.getProperty("java.io.tmpdir")).toUri().toString(),
        capabilities
    );

    LifecycleService lifecycleService = mock(LifecycleService.class);
    when(lifecycleService.initializeServer(any())).thenReturn(new InitializeBuildResult(
        Constants.SERVER_NAME,
        Constants.SERVER_VERSION,
        Constants.BSP_VERSION,
        new BuildServerCapabilities()
    ));
    BuildTargetService buildTargetService = mock(BuildTargetService.class);
    GradleBuildServer server = new GradleBuildServer(lifecycleService, buildTargetService);

    InitializeBuildResult response = server.buildInitialize(params).join();
    assertEquals(Constants.SERVER_NAME, response.getDisplayName());
    assertEquals(Constants.SERVER_VERSION, response.getVersion());
    assertEquals(Constants.BSP_VERSION, response.getBspVersion());
  }

  @Test
  void testBuildInitializeInvalidInput() {
    BuildClientCapabilities capabilities = new BuildClientCapabilities(Arrays.asList("java"));
    InitializeBuildParams params = new InitializeBuildParams(
        "test-client",
        "0.1.0",
        "0.1.0",
        "!@#$%", // <-- invalid URI
        capabilities
    );
    GradleBuildServer server = new GradleBuildServer(mock(LifecycleService.class),
        mock(BuildTargetService.class));

    CompletionException exception  = assertThrows(CompletionException.class, () -> {
      server.buildInitialize(params).join();
    });

    assertTrue(exception.getCause() instanceof ResponseErrorException);
  }

  @Test
  void testWorkspaceBuildTargets() {
    BuildTargetsManager manager = mock(BuildTargetsManager.class);
    BuildTarget target = mock(BuildTarget.class);
    when(target.getBaseDirectory()).thenReturn("foo/bar");
    GradleBuildTarget gradleBuildTarget = new GradleBuildTarget(target,
        mock(GradleSourceSet.class));
    when(manager.getAllGradleBuildTargets()).thenReturn(Arrays.asList(gradleBuildTarget));
    
    BuildTargetService buildTargetService = new BuildTargetService(manager);
    GradleBuildServer server = new GradleBuildServer(mock(LifecycleService.class),
        buildTargetService);

    WorkspaceBuildTargetsResult response = server.workspaceBuildTargets().join();

    assertEquals(1, response.getTargets().size());
    assertEquals("foo/bar", response.getTargets().get(0).getBaseDirectory());
  }
}
