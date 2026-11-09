package com.microsoft.java.bs.core.internal.server;

import ch.epfl.scala.bsp4j.DebugSessionAddress;

/** A DAP address backed by Android JDWP. */
public class AndroidDebugAddress extends DebugSessionAddress {
  private final String dataKind = "java-debug";

  public AndroidDebugAddress(String uri) {
    super(uri);
  }

  public String getDataKind() {
    return dataKind;
  }
}
