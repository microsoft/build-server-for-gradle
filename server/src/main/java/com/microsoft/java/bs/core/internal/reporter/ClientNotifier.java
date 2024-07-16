package com.microsoft.java.bs.core.internal.reporter;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.MessageType;
import ch.epfl.scala.bsp4j.ShowMessageParams;

/**
 * Utility class responsible for sending notifications to a client using a {@link BuildClient}.
 */
public class ClientNotifier {

  /**
   * Sends a notification to the client using the provided message type and content.
   *
   * @param client the {@link BuildClient} to be used for sending notifications
   * @param type the {@link MessageType} for the notification
   * @param message the message content to be sent
   */
  public static void sendNotification(BuildClient client, MessageType type, String message) {
    if (client == null) {
      throw new NullPointerException("BuildClient cannot be null");
    }
    ShowMessageParams messageParams = new ShowMessageParams(type, message);
    client.onBuildShowMessage(messageParams);
  }

}