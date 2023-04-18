package com.microsoft.java.bs.core.log;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.MembersInjector;

public class LoggerInjector<T> implements MembersInjector<T> {
    private final Field targetField;

    private final Class<?> declaringClass;

    private Logger logger;

    public LoggerInjector(Field targetField) {
        this.targetField = targetField;
        declaringClass = targetField.getDeclaringClass();

        initLogger();
    }

    private void initLogger() {
        logger = LoggerFactory.getLogger(declaringClass);
    }

    @Override
    public void injectMembers(T instance) {
        try {
            targetField.setAccessible(true);
            targetField.set(instance, logger);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
