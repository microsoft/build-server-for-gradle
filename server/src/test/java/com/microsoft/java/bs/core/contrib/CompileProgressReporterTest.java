package com.microsoft.java.bs.core.contrib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.java.bs.core.JavaBspLauncher;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TaskDataKind;
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TaskStartParams;

/**
 * Tests for {@link CompileProgressReporter}.
 */
@ExtendWith(MockitoExtension.class)
public class CompileProgressReporterTest {

  @Mock
  private BuildClient client;

  @BeforeEach
  void setUp() {
    JavaBspLauncher.client = client;
  }

  @Test
  void testTaskStarted() {
    doNothing().when(client).onBuildTaskStart(any());

    BuildTargetIdentifier id = new BuildTargetIdentifier("id");
    CompileProgressReporter reporter = new CompileProgressReporter(id);
    reporter.taskStarted("");

    final ArgumentCaptor<TaskStartParams> captor = ArgumentCaptor.forClass(TaskStartParams.class);
    verify(client, times(1)).onBuildTaskStart(captor.capture());

    assertEquals(TaskDataKind.COMPILE_TASK, captor.getValue().getDataKind()); 
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

    BuildTargetIdentifier id = new BuildTargetIdentifier("id");
    CompileProgressReporter reporter = new CompileProgressReporter(id);
    reporter.taskFinished("", StatusCode.OK);

    final ArgumentCaptor<TaskFinishParams> captor = ArgumentCaptor.forClass(TaskFinishParams.class);
    verify(client, times(1)).onBuildTaskFinish(captor.capture());

    assertEquals(TaskDataKind.COMPILE_REPORT, captor.getValue().getDataKind()); 
  }
}
