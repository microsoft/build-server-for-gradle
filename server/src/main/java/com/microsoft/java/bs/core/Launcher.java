package com.microsoft.java.bs.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the BSP server.
 */
public class Launcher {
  
  /**
   * The property name for the build server storage location.
   */
  private static final String PROP_BUILD_SERVER_STORAGE = "buildServerStorage";

  private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

  /**
   * Main entry point.
   */
  public static void main(String[] args) {
    checkRequiredProperties();
    logSessionStart();
  }

  private static void checkRequiredProperties() {
    if (System.getProperty(PROP_BUILD_SERVER_STORAGE) == null) {
      throw new IllegalStateException("The property 'buildServerStorage' is not set");
    }
  }

  private static void logSessionStart() {
    LocalDateTime currentTime = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    String formattedTimestamp = currentTime.format(formatter);
    logger.info("!SESSION {}\n---------------------------------------------------\n",
        formattedTimestamp);
  }
}
