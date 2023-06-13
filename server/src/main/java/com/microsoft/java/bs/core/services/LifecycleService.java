package com.microsoft.java.bs.core.services;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.microsoft.java.bs.core.bsp.BuildServerStatus;
import com.microsoft.java.bs.core.bsp.ServerLifetime;
import com.microsoft.java.bs.core.log.InjectLogger;
import com.microsoft.java.bs.core.managers.BuildTargetsManager;
import com.microsoft.java.bs.core.managers.PreferencesManager;
import com.microsoft.java.bs.core.model.Preferences;
import com.microsoft.java.bs.core.utils.JsonUtils;

import ch.epfl.scala.bsp4j.BuildServerCapabilities;
import ch.epfl.scala.bsp4j.CompileProvider;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.InitializeBuildResult;

/**
 * Lifecycle service.
 */
public class LifecycleService {

  @InjectLogger
  Logger logger;

  @Inject
  BuildServerStatus buildServerStatus;

  @Inject
  BuildTargetsManager buildTargetsManager;

  @Inject
  PreferencesManager preferencesManager;

  /**
   * Initialize the build server.
   */
  public InitializeBuildResult buildInitialize(InitializeBuildParams params) {
    try {
      buildServerStatus.setRootUri(new URI(params.getRootUri()));
    } catch (URISyntaxException e) {
      String errorMessage = "Not a valid URI: " + params.getRootUri() + ".";
      logger.error(errorMessage, e);
      throw new ResponseErrorException(new ResponseError(
        ResponseErrorCode.InvalidParams, errorMessage, null));
    }
    Preferences preferences = JsonUtils.toModel(params.getData(), Preferences.class);
    preferencesManager.setPreferences(preferences);
    buildTargetsManager.initialize();
    BuildServerCapabilities capabilities = new BuildServerCapabilities();
    capabilities.setCanReload(true);
    capabilities.setCompileProvider(new CompileProvider(Arrays.asList("java")));
    capabilities.setDependencyModulesProvider(true);
    capabilities.setOutputPathsProvider(true);
    capabilities.setResourcesProvider(true);
    InitializeBuildResult result = new InitializeBuildResult(
        "java-bsp",
        "0.1.0",
        "2.1.0-M4",
        capabilities
    );
    buildServerStatus.initialize();
    return result;
  }

  public void initialized() {
    // do nothing.
  }

  /**
   * Shutdown the build server.
   */
  public Object buildShutdown() {
    buildServerStatus.shutdown();
    return null;
  }

  /**
   * Exit the build server.
   */
  public void exit() {
    if (buildServerStatus.getLifetime() == ServerLifetime.SHUTDOWN) {
      System.exit(0);
    }
    System.exit(1);
  }
  
}
