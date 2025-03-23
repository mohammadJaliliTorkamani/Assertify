package org.example;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GradleBuildToolModel extends BuildToolModel {

    private final String repoPath;

    public GradleBuildToolModel(String repoPath) {
        this.repoPath = repoPath;
    }

    @Override
    public ComponentResponse compile() throws Exception {
        return this.compile(false, null);
    }

    @Override
    public ComponentResponse compile(boolean clean, String javaAddress) throws Exception {
        String[] commands;
        if (clean)
            commands = new String[]{
//                    "cmd.exe", "/c",
                    "./gradlew","clean",
                    "dependencies", "processResources", "processTestResources",
                    "build",
                    "-x", "test",
                    "-x", "check",    //new ( to fix the problem: Mqtt3SendMaximumIT > mqtt3_sendMaximum_applied() FAILED)
                    "-x", "javadoc"
            };
        else
            commands = new String[]{
//                    "cmd.exe", "/c",
                    "./gradlew",
                    "dependencies", "processResources", "processTestResources",
                    "build",
                    "-x", "test",
                    "-x", "check",    //new ( to fix the problem: Mqtt3SendMaximumIT > mqtt3_sendMaximum_applied() FAILED)
                    "-x", "javadoc"
            };
        System.out.printf("---> Compiling repository.... %n");
        return runShellCommand(commands, repoPath, javaAddress);
    }

    @Override
    public ComponentResponse runTestCase(String[] testFiles, String[] testCases, String javaAddress) throws Exception {
        List<String> params = new ArrayList<>();
//        params.add("cmd.exe");
//        params.add("/c");
        params.add("./gradlew");
        params.add("test");

        for (int i = 0; i < Math.min(testFiles.length, MAX_TEST_LENGTH); i++) {
            params.add("--tests");
            params.add(testFiles[i].replace(File.separator, ".").replace(".java", "") + "." + testCases[i]);
        }

        System.out.printf("---> Running unit test....%n");
        return BuildToolModel.runShellCommand(params.toArray(new String[0]), repoPath, javaAddress);
    }
}
