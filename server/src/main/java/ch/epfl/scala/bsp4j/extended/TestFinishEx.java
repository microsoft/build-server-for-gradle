// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package ch.epfl.scala.bsp4j.extended;

import java.util.Objects;

import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
import org.eclipse.xtext.xbase.lib.Pure;
import org.eclipse.xtext.xbase.lib.util.ToStringBuilder;

import ch.epfl.scala.bsp4j.TestFinish;
import ch.epfl.scala.bsp4j.TestStatus;

/**
 * Extended {@link TestFinish}, which contains the Suite, class, method.
 * {@link TestFinish} only contains file location which Gradle doesn't have.
 */
public class TestFinishEx extends TestFinish {
  
  private String suiteName;

  private String className;

  private String methodName;

  private String stackTrace;

  /**
   * Create a new instance of {@link TestFinishEx}.
   */
  public TestFinishEx(@NonNull String displayName, @NonNull TestStatus status, String suiteName,
      String className, String methodName) {
    super(displayName, status);
    this.suiteName = suiteName;
    this.className = className;
    this.methodName = methodName;
  }

  public String getSuiteName() {
    return suiteName;
  }

  public void setSuiteName(String suiteName) {
    this.suiteName = suiteName;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getMethodName() {
    return methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public String getStackTrace() {
    return stackTrace;
  }

  public void setStackTrace(String stackTrace) {
    this.stackTrace = stackTrace;
  }

  @Override
  @Pure
  public String toString() {
    ToStringBuilder b = new ToStringBuilder(this);
    b.add(super.toString());
    b.add("suiteName", this.suiteName);
    b.add("className", this.className);
    b.add("methodName", this.methodName);
    return b.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hash(suiteName, className, methodName, stackTrace);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    TestFinishEx other = (TestFinishEx) obj;
    return Objects.equals(suiteName, other.suiteName)
        && Objects.equals(className, other.className)
        && Objects.equals(methodName, other.methodName)
        && Objects.equals(stackTrace, other.stackTrace);
  }
}