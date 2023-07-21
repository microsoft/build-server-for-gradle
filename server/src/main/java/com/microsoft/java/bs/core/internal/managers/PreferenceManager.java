package com.microsoft.java.bs.core.internal.managers;

import java.net.URI;

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
}
