package com.microsoft.java.bs.core.contrib.gradle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.gradle.tooling.BuildException;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.events.OperationType;
import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.ProgressListener;
import org.slf4j.Logger;

import com.google.inject.Inject;
import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTargets;
import com.microsoft.java.bs.core.JavaBspLauncher;
import com.microsoft.java.bs.core.bsp.BuildServerStatus;
import com.microsoft.java.bs.core.contrib.BuildSupport;
import com.microsoft.java.bs.core.contrib.javac.JavacOutputParser;
import com.microsoft.java.bs.core.log.InjectLogger;
import com.microsoft.java.bs.core.managers.BuildTargetsManager;
import com.microsoft.java.bs.core.model.BuildTargetComponents;
import com.microsoft.java.bs.core.utils.UriUtils;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.BuildTargetTag;
import ch.epfl.scala.bsp4j.Diagnostic;
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams;
import ch.epfl.scala.bsp4j.TextDocumentIdentifier;

public class GradleBuild implements BuildSupport {

    @InjectLogger
    Logger logger;

    @Inject
    BuildServerStatus buildServerStatus;

    @Inject
    BuildTargetsManager buildTargetsManager;

    public JavaBuildTargets getSourceSetEntries() {

        ProgressListener listener = new ProgressListener() {
            @Override
            public void statusChanged(ProgressEvent event) {
                event.getDisplayName();
            }
        };

        ProjectConnection connection = null;

        try {
            connection = GradleConnector.newConnector()
                .forProjectDirectory(new File(buildServerStatus.getRootUri()))
                .connect();
            ModelBuilder<JavaBuildTargets> customModelBuilder = connection
                .model(JavaBuildTargets.class).addProgressListener(listener, OperationType.FILE_DOWNLOAD);
            // TODO: copy init.gradle to a temp file and use that path
            customModelBuilder.withArguments(/*"-Dorg.gradle.debug=true", */"--init-script", "<PATH_TO_INIT_SCRIPT>");
            JavaBuildTargets model = customModelBuilder.get();
            return model;
        } catch (GradleConnectionException | IllegalStateException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }

        return null;
    }

    public void build(List<BuildTargetIdentifier> targets) {
        ProgressListener listener = new ProgressListener() {
            @Override
            public void statusChanged(ProgressEvent event) {
                event.getDisplayName();
            }
        };
        for (BuildTargetIdentifier target : targets) {
            runGradleBuildTask(listener, target);
        }
    }

    private void runGradleBuildTask(ProgressListener listener, BuildTargetIdentifier id) {
        URI projectUri = getProjectUri(id);
        if (projectUri == null) {
            return;
        }

        ProjectConnection connection = GradleConnector.newConnector()
            .forProjectDirectory(new File(projectUri))
            .connect();

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        boolean isTest = isTestTarget(id);
        try {
            connection.newBuild()
                .addProgressListener(listener, OperationType.TASK)
                .setStandardError(out)
                .forTasks(getTaskName(isTest))
                .run();
        } catch (BuildException e) {
            JavacOutputParser parser = new JavacOutputParser();
            Map<TextDocumentIdentifier, List<Diagnostic>> diagnostics = parser.parse(out.toString());
            // TODO: same diagnostic may be published twice, because testClasses includes classes.
            for (Map.Entry<TextDocumentIdentifier, List<Diagnostic>> entry : diagnostics.entrySet()) {
                PublishDiagnosticsParams params = new PublishDiagnosticsParams(
                    entry.getKey(),
                    id,
                    entry.getValue(),
                    true
                );
                JavaBspLauncher.client.onBuildPublishDiagnostics(params);
            }
        } finally {
            connection.close();
            try {
                out.close();
            } catch (IOException e) {
                // TODO log
            }
        }
    }

    private boolean isTestTarget(BuildTargetIdentifier btId) {
        BuildTargetComponents components = buildTargetsManager.getComponents(btId);
        BuildTarget buildTarget = components.getBuildTarget();
        return buildTarget.getTags().contains(BuildTargetTag.TEST);
    }

    private String getTaskName(boolean isTest) {
        return isTest ? "testClasses" : "classes";
    }

    private URI getProjectUri(BuildTargetIdentifier btId) {
        BuildTargetComponents components = buildTargetsManager.getComponents(btId);
        BuildTarget buildTarget = components.getBuildTarget();
        try {
            if (buildTarget.getBaseDirectory() != null) {
                return new URI(buildTarget.getBaseDirectory());
            }

            return UriUtils.uriWithoutQuery(btId.getUri());
        } catch (URISyntaxException e) {
            // TODO Auto-generated catch block
            return null;
        }
    }
}
