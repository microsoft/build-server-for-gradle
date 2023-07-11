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

import com.microsoft.java.bs.core.internal.managers.BuildTargetsManager;
import com.microsoft.java.bs.core.internal.services.LifecycleService;

import ch.epfl.scala.bsp4j.BuildClientCapabilities;
import ch.epfl.scala.bsp4j.BuildServerCapabilities;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.InitializeBuildResult;

class GradleBuildServerTest {
  @Test
  void testBuildInitialize() {
    GradleBuildServer server = new GradleBuildServer();
    BuildClientCapabilities capabilities = new BuildClientCapabilities(Arrays.asList("java"));
    InitializeBuildParams params = new InitializeBuildParams(
        "test-client",
        "0.1.0",
        "0.1.0",
        Paths.get(System.getProperty("java.io.tmpdir")).toUri().toString(),
        capabilities
    );

    LifecycleService lifecycleService = mock(LifecycleService.class);
    when(lifecycleService.buildInitialize(any(), any())).thenReturn(new InitializeBuildResult(
        "gradle-build-server",
        "0.1.0",
        "2.1.0-M4",
        new BuildServerCapabilities()
    ));
    server.setup(lifecycleService, mock(BuildTargetsManager.class));

    InitializeBuildResult response = server.buildInitialize(params).join();
    assertEquals("gradle-build-server", response.getDisplayName());
    assertEquals("0.1.0", response.getVersion());
    assertEquals("2.1.0-M4", response.getBspVersion());
  }

  @Test
  void testBuildInitializeInvalidInput() {
    GradleBuildServer server = new GradleBuildServer();
    BuildClientCapabilities capabilities = new BuildClientCapabilities(Arrays.asList("java"));
    InitializeBuildParams params = new InitializeBuildParams(
        "test-client",
        "0.1.0",
        "0.1.0",
        "!@#$%", // <-- invalid URI
        capabilities
    );

    server.setup(mock(LifecycleService.class), mock(BuildTargetsManager.class));

    CompletionException exception  = assertThrows(CompletionException.class, () -> {
      server.buildInitialize(params).join();
    });

    assertTrue(exception.getCause() instanceof ResponseErrorException);
  }
}
