package org.example;

import java.io.File;

public class GradleBuildToolModel extends BuildToolModel {

    private final String repoPath;

    public GradleBuildToolModel(String repoPath) {
        this.repoPath = repoPath;
    }

    @Override
    public boolean installDependencies() {
        String[] commands = new String[]{"cmd.exe", "/c", "gradlew", "build"};
        System.out.println("Installing dependencies....\n");
        int code = runShellCommand(commands, repoPath);
        if (code == 0) {
            System.out.println("\nDependencies resolved successfully.");
            return true;
        } else {
            System.out.println("\nFailed to resolve dependencies.");
            return false;
        }
    }

    @Override
    public boolean runTestCase(String testFile, String testCase) {
        String[] commands = new String[]{"cmd.exe", "/c", "gradlew", "test", "--tests", testFile.replace(File.separator, ".").replace(".java", "") + "." + testCase};
        System.out.println("\nRunning unit test.... (" + String.join(" ", commands) + ")\n");
        int code = BuildToolModel.runShellCommand(commands, repoPath);
        if (code == 0) {
            System.out.println("\n*** TEST PASSED!");
            return true;
        } else {
            System.out.println("\n*** TEST FAILED!");
            return false;
        }
    }
}
