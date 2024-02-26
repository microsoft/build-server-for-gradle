// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.managers;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import com.microsoft.java.bs.core.internal.model.Preferences;

/**
 * Manage the preferences of the build server.
 */
public class PreferenceManager {
  private Preferences preferences;

  /**
  * The root URI of the workspace.
  */
  private URI rootUri;

  private List<String> clientSupportedLanguages;

  public PreferenceManager() {
    this.clientSupportedLanguages = new LinkedList<>();
  }

  public void setPreferences(Preferences preferences) {
    this.preferences = preferences;
  }

  public Preferences getPreferences() {
    return preferences;
  }

  public URI getRootUri() {
    return rootUri;
  }

  public void setRootUri(URI rootUri) {
    this.rootUri = rootUri;
  }

  public List<String> getClientSupportedLanguages() {
    return clientSupportedLanguages;
  }

  public void setClientSupportedLanguages(List<String> clientSupportedLanguages) {
    this.clientSupportedLanguages = clientSupportedLanguages;
  }
}
