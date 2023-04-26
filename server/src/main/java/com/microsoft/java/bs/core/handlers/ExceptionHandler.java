package com.microsoft.java.bs.core.handlers;

import java.util.function.Function;

import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception handler set to the launcher.
 */
public class ExceptionHandler implements Function<Throwable, ResponseError> {

  private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

  @Override
  public ResponseError apply(final Throwable t) {
    logger.error(t.getMessage(), t);
    if (t instanceof ResponseErrorException) {
      return ((ResponseErrorException) t).getResponseError();
    }
    return new ResponseError(ResponseErrorCode.InternalError, t.getMessage(), t);
  }
}
