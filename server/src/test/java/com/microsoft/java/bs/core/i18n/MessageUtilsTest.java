package com.microsoft.java.bs.core.i18n;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Locale;

import org.junit.jupiter.api.Test;

class MessageUtilsTest {
  @Test
  void testGet() {
    assertNotNull(MessageUtils.get("error.serverStorageMissing"));
  }

  @Test
  void testSetLocale() {
    MessageUtils.setLocale(new Locale("zh", "CN"));
    assertNotNull(MessageUtils.get("error.serverStorageMissing"));
  }

}
