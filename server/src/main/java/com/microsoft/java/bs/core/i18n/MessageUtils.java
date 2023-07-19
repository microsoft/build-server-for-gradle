package com.microsoft.java.bs.core.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Utils to get the messages from properties file.
 */
public class MessageUtils {
  private static ResourceBundle messages;

  static {
    // By default, use the system default locale
    messages = ResourceBundle.getBundle("messages");
  }

  public static void setLocale(Locale locale) {
    messages = ResourceBundle.getBundle("messages", locale);
  }

  public static String get(String key) {
    return messages.getString(key);
  }

  private MessageUtils() {}
}
