package org.example;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MavenBuildToolModel extends BuildToolModel {
    private String repoPath;

    public MavenBuildToolModel(String repoPath) {
        this.repoPath = repoPath;
    }

    @Override
    public boolean installDependencies() {
        String[] commands = new String[]{"cmd.exe", "/c",
                "mvn", "clean",
                "dependency:resolve",
                "process-resources",
                "compile",
                "process-test-resources",
                "test-compile"
        };
        System.out.println("\nInstalling dependencies.... (" + String.join(" ", commands) + ")\n");
        int code = BuildToolModel.runShellCommand(commands, repoPath);
        if (code == 0) {
            System.out.println("\nDependencies resolved successfully.");
            return true;
        } else {
            System.out.println("Failed to resolve dependencies.");
            return false;
        }
    }

    @Override
    public boolean runTestCase(String testFile, String testCase) {
        String[] commands = new String[]{"cmd.exe", "/c", "mvn", "test", "-Dtest=" + testFile.replace(File.separator, ".").replace(".java", "") + "#" + testCase};
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

    public static List<Dependency> parseDependencyFromPOM(String repoPath) {
        File pomFile = new File(repoPath, "pom.xml");
        List<Dependency> dependencies = new ArrayList<>();

        try (FileReader reader = new FileReader(pomFile)) {
            MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
            Model model = xpp3Reader.read(reader);

            List<org.apache.maven.model.Dependency> pomDependencies = model.getDependencies();
            dependencies.addAll(pomDependencies);
        } catch (IOException e) {
            System.out.println("An error occurred while reading the POM file.");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("An error occurred while parsing the POM file.");
            e.printStackTrace();
        }

        return dependencies;
    }
}
