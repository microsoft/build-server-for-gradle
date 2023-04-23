package com.microsoft.java.bs.core.contrib.gradle;

import java.util.UUID;

import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.ProgressListener;

import com.microsoft.java.bs.core.JavaBspLauncher;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TaskId;
import ch.epfl.scala.bsp4j.TaskProgressParams;
import ch.epfl.scala.bsp4j.TaskStartParams;

public class ProgressReporter implements ProgressListener {

    private final TaskId taskId;
    private BuildClient client;

    public ProgressReporter() {
        this.taskId = new TaskId(UUID.randomUUID().toString());
        client = JavaBspLauncher.client;
    }

    @Override
    public void statusChanged(ProgressEvent event) {
        taskInProgress(event.getDisplayName());
    }

    public void taskStarted(String message) {
        TaskStartParams startParam = new TaskStartParams(taskId);
        startParam.setMessage(message);
        if (client != null) {
            client.onBuildTaskStart(startParam);
        }
    }

    public void taskInProgress(String message) {
        TaskProgressParams progressParam = new TaskProgressParams(taskId);
        progressParam.setMessage(message);
        if (client != null) {
            client.onBuildTaskProgress(progressParam);
        }
    }

    public void taskFinished(String message, StatusCode statusCode) {
        TaskFinishParams endParam = new TaskFinishParams(taskId, statusCode);
        endParam.setMessage(message);
        if (client != null) {
            client.onBuildTaskFinish(endParam);
        }
        client = null;
    }
}
