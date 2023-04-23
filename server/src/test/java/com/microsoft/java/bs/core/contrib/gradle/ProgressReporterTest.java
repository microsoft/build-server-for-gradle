package com.microsoft.java.bs.core.contrib.gradle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.java.bs.core.JavaBspLauncher;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.StatusCode;

@ExtendWith(MockitoExtension.class)
public class ProgressReporterTest {

    @Mock
    private BuildClient client;

    @BeforeEach
    void setUp() {
        JavaBspLauncher.client = client;
    }

    @Test
    void testTaskStarted() {
        doNothing().when(client).onBuildTaskStart(any());

        ProgressReporter reporter = new ProgressReporter();
        reporter.taskStarted("");

        verify(client, times(1)).onBuildTaskStart(any());
    }

    @Test
    void testTaskInProgress() {
        doNothing().when(client).onBuildTaskProgress(any());

        ProgressReporter reporter = new ProgressReporter();
        reporter.taskInProgress("");

        verify(client, times(1)).onBuildTaskProgress(any());
    }

    @Test
    void testTaskFinished() {
        doNothing().when(client).onBuildTaskFinish(any());

        ProgressReporter reporter = new ProgressReporter();
        reporter.taskFinished("", StatusCode.OK);

        verify(client, times(1)).onBuildTaskFinish(any());
    }
}
