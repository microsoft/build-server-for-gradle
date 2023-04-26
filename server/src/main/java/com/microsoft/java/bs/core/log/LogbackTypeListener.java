package com.microsoft.java.bs.core.log;

import java.lang.reflect.Field;

import org.slf4j.Logger;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

/**
 * Type listener.
 */
public class LogbackTypeListener implements TypeListener {

  @Override
  public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
    Class<?> clazz = type.getRawType();
    while (clazz != null) {
      for (Field field : clazz.getDeclaredFields()) {
        if (field.getType() == Logger.class && field.isAnnotationPresent(InjectLogger.class)) {
          encounter.register(new LoggerInjector<>(field));
        }
      }
      clazz = clazz.getSuperclass();
    }
  }
}
