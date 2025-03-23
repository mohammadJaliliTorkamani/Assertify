package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class BuildToolModel {

    public static enum Type {
        JAVA_STANDARD, MAVEN, GRADLE
    }

    public abstract boolean installDependencies();

    public abstract boolean runTestCase(String testFile, String testCase);

    public static Type getProjectBuildTool(String repositoryPath) {
        if (hasPomXml(repositoryPath))
            return Type.MAVEN;

        else if (hasBuildGradle(repositoryPath))
            return Type.GRADLE;

        return Type.JAVA_STANDARD;
    }

    public static boolean hasPomXml(String repositoryPath) {
        String pomXmlPath = repositoryPath + "/pom.xml";
        return Files.exists(Paths.get(pomXmlPath));
    }

    public static boolean hasBuildGradle(String repositoryPath) {
        String buildGradlePath = repositoryPath + "/build.gradle";
        String settingsGradlePath = repositoryPath + "/settings.gradle";
        return Files.exists(Paths.get(buildGradlePath)) || Files.exists(Paths.get(settingsGradlePath));
    }

    public static int runShellCommand(String[] commands, String dir) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            processBuilder.directory(new File(dir));
            processBuilder.redirectErrorStream(true);// Redirect the output to the Java process
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            return process.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return -1;
    }
}
