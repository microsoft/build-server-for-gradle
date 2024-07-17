// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model;

import java.io.Serializable;
import java.util.List;

/**
 * List of all Gradle source set instances.
 */
public interface GradleSourceSets extends Serializable {
  List<GradleSourceSet> getGradleSourceSets();
}
