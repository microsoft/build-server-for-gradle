// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.gradle;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.gradle.tooling.GradleConnector;

import com.microsoft.java.bs.core.internal.managers.PreferenceManager;

/**
 * Hold a cache of {@link GradleConnector} for performance reasons.
 */
public class ConnectorCache {

  private final Map<File, ConnectorWrapper> connectors;
  private final PreferenceManager preferenceManager;
  
  public ConnectorCache(PreferenceManager preferenceManager, boolean useCache) {
    this.preferenceManager = preferenceManager;
    connectors = useCache ? new HashMap<>() : null;
  }

  /**
   * Disconnect all {@link GradleConnector}.
   */
  public void shutdown() {
    if (connectors != null) {
      connectors.values().forEach(ConnectorWrapper::disconnect);
    }
  }

  /**
   * Get a {@link GradleConnector} to the project.
   *
   * @param project Directory of project.
   * @return a wrapper to the {@link GradleConnector}.
   */
  public ConnectorWrapper getGradleConnector(File project) {
    if (connectors != null) {
      return connectors.computeIfAbsent(project,
        p -> new ConnectorWrapper(p, preferenceManager.getPreferences(), ConnectorCache.this));
    } else {
      return new ConnectorWrapper(project, preferenceManager.getPreferences(), ConnectorCache.this);
    }
  }

  void returnConnector(GradleConnector connector) {
    if (connectors == null) {
      connector.disconnect();
    }
  }
}
