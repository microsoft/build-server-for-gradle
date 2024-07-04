package com.microsoft.java.bs.core.internal.reporter;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.MessageType;
import ch.epfl.scala.bsp4j.ShowMessageParams;
import ch.epfl.scala.bsp4j.TaskId;

import java.util.Objects;

/**
 * Utility class responsible for sending notifications to a client using a {@link BuildClient}.
 */
public class ClientNotifier {

  private final BuildClient client;
  private String originId;
  private TaskId taskId;

  /**
   * Constructs a new ClientNotifier instance with the provided BuildClient.
   *
   * @param client the {@link BuildClient} to be used for sending notifications
   * @throws NullPointerException if the client is null
   */
  public ClientNotifier(BuildClient client) {
    this.client = Objects.requireNonNull(client, "BuildClient cannot be null");
  }

  /**
   * Sets the origin identifier for the next notification.
   *
   * <p>
   * <b>Note:</b> This origin identifier gets consumed after being used in
   * {@link ClientNotifier#sendNotification} method.
   * </p>
   *
   * @param originId the origin identifier for the notification
   * @return this ClientNotifier instance to allow for method chaining
   */
  public ClientNotifier withOriginId(String originId) {
    this.originId = originId;
    return this;
  }

  /**
   * Sets the task identifier for the next notification.
   *
   * <p>
   * <b>Note:</b> This task identifier gets consumed after being used in
   * {@link ClientNotifier#sendNotification} method.
   * </p>
   *
   * @param taskId the {@link TaskId} for the notification
   * @return this ClientNotifier instance to allow for method chaining
   */
  public ClientNotifier withTask(TaskId taskId) {
    this.taskId = taskId;
    return this;
  }

  /**
   * Sends a notification to the client using the provided message type and content.
   *
   * <p>
   * <b>Note:</b> The originId and taskId properties are set to null after sending the
   * notification to ensure they are not sent for subsequent notifications.
   * </p>
   *
   * @param type the {@link MessageType} for the notification
   * @param message the message content to be sent
   */
  public void sendNotification(MessageType type, String message) {
    ShowMessageParams messageParams = new ShowMessageParams(type, message);
    if (taskId != null) {
      messageParams.setTask(taskId);
      taskId = null;
    }
    if (originId != null) {
      messageParams.setOriginId(originId);
      originId = null;
    }
    client.onBuildShowMessage(messageParams);
  }

}