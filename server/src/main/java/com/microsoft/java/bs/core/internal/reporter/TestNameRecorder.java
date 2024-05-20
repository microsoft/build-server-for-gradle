// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.reporter;

import java.util.HashSet;
import java.util.Set;

import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.ProgressListener;
import org.gradle.tooling.events.test.JvmTestOperationDescriptor;

/**
 * Implements {@link ProgressListener} that listens to the progress of gradle test tasks,
 * and records the names of the tests.
 */
public class TestNameRecorder implements ProgressListener {

  private final Set<String> mainClasses;

  public TestNameRecorder() {
    mainClasses = new HashSet<>();
  }

  @Override
  public void statusChanged(ProgressEvent event) {
    if (event.getDescriptor() instanceof JvmTestOperationDescriptor) {
      JvmTestOperationDescriptor descriptor = (JvmTestOperationDescriptor) event.getDescriptor();
      if (descriptor.getClassName() != null && descriptor.getMethodName() == null) {
        mainClasses.add(descriptor.getClassName());
      }
    }
  }

  public Set<String> getMainClasses() {
    return mainClasses;
  }
}
