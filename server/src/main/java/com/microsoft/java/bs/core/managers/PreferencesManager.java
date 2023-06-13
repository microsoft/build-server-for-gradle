package com.microsoft.java.bs.core.managers;

import com.microsoft.java.bs.core.model.Preferences;

/**
 * Manage the preferences of the build server.
 */
public class PreferencesManager {
  private Preferences preferences;

  public void setPreferences(Preferences preferences) {
    this.preferences = preferences;
  }

  public Preferences getPreferences() {
    return preferences;
  }
}
