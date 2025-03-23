package org.example;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MavenBuildToolModel extends BuildToolModel {
    private final String repoPath;

    public MavenBuildToolModel(String repoPath) {
        this.repoPath = repoPath;
    }

    public static List<Dependency> parseDependencyFromPOM(String repoPath) throws Exception {
        File pomFile = new File(repoPath, "pom.xml");
        List<Dependency> dependencies = new ArrayList<>();

        try (FileReader reader = new FileReader(pomFile)) {
            MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
            Model model = xpp3Reader.read(reader);

            List<org.apache.maven.model.Dependency> pomDependencies = model.getDependencies();
            dependencies.addAll(pomDependencies);
        }

        return dependencies;
    }

    @Override
    public ComponentResponse compile() throws Exception {
        return this.compile(false,null);
    }

    @Override
    public ComponentResponse compile(boolean clean, String javaAddress) throws Exception {
        String[] commands;
        File mvnwFile=  new File(repoPath, "mvnw");
        boolean mvnwExists = mvnwFile.exists() && mvnwFile.isFile();
        if (!mvnwExists) {
            if (clean)
                commands = new String[]{
                        Constants.MVN_BINARY_PATH,
                        "clean", "package",
                        "dependency:resolve",
                        "process-resources",
                        "-DskipTests=true", "-Dcheckstyle.skip=true", "-Dspotbugs.skip=true", "-Dmaven.javadoc.skip=true"
                };
            else
                commands = new String[]{
//                    "cmd.exe", "/c",
                        Constants.MVN_BINARY_PATH, "package",
                        "-DskipTests=true", "-Dcheckstyle.skip=true", "-Dspotbugs.skip=true", "-Dmaven.javadoc.skip=true"
                };
        }else{
            if (clean)
                commands = new String[]{"./mvnw", "clean", "install","-DskipTests=true", "-Dcheckstyle.skip=true", "-Dspotbugs.skip=true", "-Dmaven.javadoc.skip=true"};
            else
                commands = new String[]{"./mvnw","install","-DskipTests=true", "-Dcheckstyle.skip=true", "-Dspotbugs.skip=true", "-Dmaven.javadoc.skip=true"};
        }

        System.out.printf("---> Compiling repository....%n");
        return BuildToolModel.runShellCommand(commands, repoPath, javaAddress);
    }

    @Override
    public ComponentResponse runTestCase(String[] testFiles, String[] testCases, String javaAddress) throws Exception {
        String[] params = new String[Math.min(testFiles.length, MAX_TEST_LENGTH)];
        for (int i = 0; i < Math.min(testFiles.length, MAX_TEST_LENGTH); i++)
            params[i] = testFiles[i].replace(File.separator, ".").replace(".java", "") + "#" + testCases[i];

        String[] commands = new String[]{
//                "cmd.exe", "/c",
                Constants.MVN_BINARY_PATH, "test", "-Dcheckstyle.skip=true", "-Dtest=\"" + String.join(",", params) + "\""};
        System.out.printf("---> Running unit test....%n");
        return BuildToolModel.runShellCommand(commands, repoPath, javaAddress);
    }


}
