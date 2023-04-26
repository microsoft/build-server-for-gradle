package com.microsoft.java.bs.core.bsp;

import java.net.URI;

import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.slf4j.Logger;

import com.microsoft.java.bs.core.log.InjectLogger;

/**
 * Manage the status of the build server.
 */
public class BuildServerStatus {

  @InjectLogger
  Logger logger;

  private URI rootUri;

  private ServerLifetime lifetime = ServerLifetime.UNINITIALIZED;

  public URI getRootUri() {
    return rootUri;
  }

  public void setRootUri(URI rootUri) {
    this.rootUri = rootUri;
  }

  public ServerLifetime getLifetime() {
    return lifetime;
  }

  public void initialize() {
    this.lifetime = ServerLifetime.INITIALIZED;
  }

  public void shutdown() {
    this.lifetime = ServerLifetime.SHUTDOWN;
  }

  //TODO: check server status before handling requests
  /**
   * Check if the server is initialized.
   *
   * @param requestName the name of the request.
   */
  public void assureServerInitialized(String requestName) {
    if (lifetime == ServerLifetime.UNINITIALIZED) {
      logger.error(requestName + " is called before the server is initialized.");
      throw new ResponseErrorException(
          new ResponseError(
            ResponseErrorCode.ServerNotInitialized,
            "The server is not initialized yet.",
            null
          )
      );
    }
  }
}
