package com.microsoft.java.bs.core.contrib;

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
public class DefaultProgressReporterTest {

    @Mock
    private BuildClient client;

    @BeforeEach
    void setUp() {
        JavaBspLauncher.client = client;
    }

    @Test
    void testTaskStarted() {
        doNothing().when(client).onBuildTaskStart(any());

        DefaultProgressReporter reporter = new DefaultProgressReporter();
        reporter.taskStarted("");

        verify(client, times(1)).onBuildTaskStart(any());
    }

    @Test
    void testTaskInProgress() {
        doNothing().when(client).onBuildTaskProgress(any());

        DefaultProgressReporter reporter = new DefaultProgressReporter();
        reporter.taskInProgress("");

        verify(client, times(1)).onBuildTaskProgress(any());
    }

    @Test
    void testTaskFinished() {
        doNothing().when(client).onBuildTaskFinish(any());

        DefaultProgressReporter reporter = new DefaultProgressReporter();
        reporter.taskFinished("", StatusCode.OK);

        verify(client, times(1)).onBuildTaskFinish(any());
    }
}
