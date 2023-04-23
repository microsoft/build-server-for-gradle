package com.microsoft.java.bs.core.contrib.gradle;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
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

    private static final String PLUGIN_JAR_INTERNAL_LOCATION = "/plugin.jar";
    private static final String GRADLE_PLUGIN_JAR_TARGET_NAME = "gradle-plugin.jar";
    private static final String INIT_GRADLE_SCRIPT = "init.gradle";

    private static final String MESSAGE_DIGEST_ALGORITHM = "SHA-256";
    private static final String USER_HOME = "user.home";

    @InjectLogger
    Logger logger;

    @Inject
    BuildTargetsManager buildTargetsManager;

    public JavaBuildTargets getSourceSetEntries(URI projectUri) {
        File initScript = getInitScript();
        if (initScript == null) {
            logger.error("Failed to get init.gradle");
            return null;
        }

        ProgressListener listener = new ProgressListener() {
            @Override
            public void statusChanged(ProgressEvent event) {
                // TODO: report progress to client
            }
        };

        final ProjectConnection connection = GradleConnector.newConnector()
            .forProjectDirectory(new File(projectUri))
            .connect();
        try (connection) {
            ModelBuilder<JavaBuildTargets> customModelBuilder = connection
                .model(JavaBuildTargets.class).addProgressListener(listener, OperationType.FILE_DOWNLOAD, OperationType.PROJECT_CONFIGURATION);
            customModelBuilder.withArguments("--init-script", initScript.getAbsolutePath());
            JavaBuildTargets model = customModelBuilder.get();
            return model;
        } catch (GradleConnectionException | IllegalStateException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    public void build(List<BuildTargetIdentifier> targets) {
        ProgressListener listener = new ProgressListener() {
            @Override
            public void statusChanged(ProgressEvent event) {
                // TODO: report progress to client
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

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ProjectConnection connection = GradleConnector.newConnector()
            .forProjectDirectory(new File(projectUri))
            .connect();
        try (out;connection) {
            connection.newBuild()
                .addProgressListener(listener, OperationType.TASK)
                .setStandardError(out)
                .forTasks(getTaskName(isTestTarget(id)))
                .run();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
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
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Copy/update the required files to the target location.
     * And return the init.gradle file instance.
     * @throws Exception
     */
    private File getInitScript() {
        File pluginJarFile = Paths.get(System.getProperty(USER_HOME), ".bsp", GRADLE_PLUGIN_JAR_TARGET_NAME).toFile();
        // copy plugin jar to target location
        final InputStream input = GradleBuild.class.getResourceAsStream(PLUGIN_JAR_INTERNAL_LOCATION);
        try (input) {
            byte[] pluginJarBytes = input.readAllBytes();
            byte[] pluginJarDigest = getContentDigest(pluginJarBytes );
            if (needReplaceContent(pluginJarFile, pluginJarDigest)) {
                pluginJarFile.getParentFile().mkdirs();
                Files.write(pluginJarFile.toPath(), pluginJarBytes);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error(e.getMessage(), e);
        }

        if (!pluginJarFile.exists()) {
            logger.error("Failed to get plugin.jar");
            return null;
        }

        // copy init script to target location
        String pluginJarUnixPath = pluginJarFile.getAbsolutePath().replace("\\", "/");
        String initScriptContent = "initscript {\n" + "	dependencies {\n" + "		classpath files('"
                + pluginJarUnixPath + "')\n" + "	}\n" + "}\n" + "\n" + "allprojects {\n"
                + "	apply plugin: com.microsoft.java.bs.contrib.gradle.plugin.BspGradlePlugin\n" + "}\n";
        try {
            byte[] initScriptBytes = initScriptContent.getBytes();
            byte[] initScriptDigest = getContentDigest(initScriptBytes);
            File initScriptFile = Paths.get(System.getProperty(USER_HOME), ".bsp", INIT_GRADLE_SCRIPT).toFile();
            if (needReplaceContent(initScriptFile, initScriptDigest)) {
                initScriptFile.getParentFile().mkdirs();
                Files.write(initScriptFile.toPath(), initScriptBytes);
            }
            return initScriptFile;
        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public boolean needReplaceContent(File file, byte[] checksum) throws IOException, NoSuchAlgorithmException {
        if (!file.exists() || file.length() == 0) {
            return true;
        }

        byte[] digest = getContentDigest(Files.readAllBytes(file.toPath()));
        if (Arrays.equals(digest, checksum)) {
            return false;
        }
        return true;
    }

    private byte[] getContentDigest(byte[] contentBytes) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM);
        md.update(contentBytes);
        return md.digest();
    }
}
