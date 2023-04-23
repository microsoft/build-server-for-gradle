package com.microsoft.java.bs.core.contrib.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTarget;
import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTargets;

public class GradleBuildTest {
    @Test
    void testGetSourceSetEntries() throws Exception {
        File projectDir = Paths.get(
            System.getProperty("user.dir"),
            "..",
            "testProjects",
            "junit5-jupiter-starter-gradle"
        ).toFile();
        GradleBuild gradleBuild = new GradleBuild();
        JavaBuildTargets sourceSetEntries = gradleBuild.getSourceSetEntries(projectDir.toURI());
        List<JavaBuildTarget> javaBuildTargets = sourceSetEntries.getJavaBuildTargets();

        assertEquals(2, javaBuildTargets.size());
        for (JavaBuildTarget javaBuildTarget : javaBuildTargets) {
            assertEquals(1, javaBuildTarget.getSourceDirs().size());
            if (Objects.equals(javaBuildTarget.getSourceSetName(), "test")) {
                assertEquals(6, javaBuildTarget.getModuleDependencies().size());
                assertEquals(0, javaBuildTarget.getProjectDependencies().size());
            }
        }
    }
}
