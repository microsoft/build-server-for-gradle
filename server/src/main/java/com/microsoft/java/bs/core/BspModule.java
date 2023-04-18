package com.microsoft.java.bs.core;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.microsoft.java.bs.core.bsp.BspServer;
import com.microsoft.java.bs.core.bsp.BuildServerStatus;
import com.microsoft.java.bs.core.contrib.BuildSupport;
import com.microsoft.java.bs.core.contrib.gradle.GradleBuild;
import com.microsoft.java.bs.core.log.LogbackTypeListener;
import com.microsoft.java.bs.core.managers.BuildTargetsManager;
import com.microsoft.java.bs.core.services.BuildTargetsService;
import com.microsoft.java.bs.core.services.CompileService;
import com.microsoft.java.bs.core.services.LifecycleService;

public class BspModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(BuildTargetsManager.class).in(Singleton.class);
        bind(BspServer.class).in(Singleton.class);
        bind(BuildServerStatus.class).in(Singleton.class);
        bindListener(Matchers.any(), new LogbackTypeListener());

        // bind services
        bind(BuildTargetsService.class).in(Singleton.class);
        bind(LifecycleService.class).in(Singleton.class);
        bind(CompileService.class).in(Singleton.class);

        Multibinder<BuildSupport> buildSupportBinder = Multibinder.newSetBinder(binder(), BuildSupport.class);
        buildSupportBinder.addBinding().to(GradleBuild.class);
    }
}
