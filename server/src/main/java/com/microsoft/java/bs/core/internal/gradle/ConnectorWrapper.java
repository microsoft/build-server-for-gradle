// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.gradle;

import java.io.Closeable;
import java.io.File;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;

import com.microsoft.java.bs.core.internal.model.Preferences;

/**
 * Wraps a {@link GradleConnector} to manage connector cache.
 */
public class ConnectorWrapper implements Closeable {

  private final GradleConnector connector;
  private final ConnectorCache connectorCache;

  ConnectorWrapper(GradleConnector connector, ConnectorCache connectorCache) {
    this.connector = connector;
    this.connectorCache = connectorCache;
  }

  ConnectorWrapper(File project, Preferences preferences, ConnectorCache connectorCache) {
    this(Utils.getProjectConnector(project, preferences), connectorCache);
  }

  public ProjectConnection connect() {
    return connector.connect();
  }

  void disconnect() {
    connector.disconnect();
  }

  @Override
  public void close() {
    connectorCache.returnConnector(connector);
  }
}
