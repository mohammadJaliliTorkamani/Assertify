package org.example;

import org.apache.maven.model.Dependency;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class TestExecutor {
    private final String repositoriesDir;
    private final List<String> repositoriesPaths;
    private final List<String> testFilesPaths;
    private final List<String> unitTests;
    private final String srcChildDir;
    private final static String TEST_CHILD_DIR = "src" + File.separator + "test" + File.separator + "java";

    public TestExecutor(String repositoriesDir) {
        this(repositoriesDir, TEST_CHILD_DIR);
    }

    public TestExecutor(String repositoriesDir, String test_child_dir) {
        this.repositoriesDir = repositoriesDir;
        this.repositoriesPaths = new LinkedList<>();
        this.testFilesPaths = new LinkedList<>();
        this.unitTests = new LinkedList<>();
        this.srcChildDir = test_child_dir;
    }


    public String getRandomRepo() {
        if (repositoriesPaths.isEmpty())
            initializeRepositoriesPath(repositoriesDir);

        return getRandomElement(repositoriesPaths);
    }

    private String getRandomElement(List<String> list) {
        if (list.isEmpty())
            return null;
        Random random = new Random();
        int index = random.nextInt(list.size());
        return list.get(index);
    }

    private void initializeRepositoriesPath(String repositoriesPath) {
        List<String> repositoryPaths = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(repositoriesPath))) {
            for (Path path : stream) {
                if (Files.isDirectory(path)) {
                    repositoryPaths.add(path.toString());
                }
            }
        } catch (IOException e) {
            System.out.println("An error occurred while reading the repositories directory.");
            e.printStackTrace();
        }

        this.repositoriesPaths.addAll(repositoryPaths);
    }

    public List<String> getRepositoriesPaths() {
        return repositoriesPaths;
    }

    public List<String> getTestFilesPaths() {
        return testFilesPaths;
    }

    public List<String> getUnitTests() {
        return unitTests;
    }

    /**
     * Note: The real address should have TEST_CHILD_DIR_AT_THE_BEGINNING
     *
     * @param repoPath path of the repository
     * @return a randomly chosen test file
     */
    public String getRandomTestFile(String repoPath) {
        if (repoPath == null) {
            System.out.println("RepoPath should not be empty. Stopping...");
            return null;
        } else {
            if (this.testFilesPaths.isEmpty())
                initializeRandomTestFiles(repoPath);
            return getRandomElement(this.testFilesPaths);
        }
    }

    private void initializeRandomTestFiles(String randomRepoPath) {
        List<String> testFiles = new LinkedList<>();
        File testDirectory = new File(randomRepoPath, srcChildDir);
        if (!testDirectory.exists() || !testDirectory.isDirectory()) {
            System.out.println("Invalid test directory: " + testDirectory.getAbsolutePath());
            return;
        }

        try {
            Files.walkFileTree(testDirectory.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith("Test.java")) {
                        String relativePath = testDirectory.toPath().relativize(file).toString();
                        testFiles.add(relativePath);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.out.println("An error occurred while scanning the test directory for test files.");
            e.printStackTrace();
        }

        this.testFilesPaths.addAll(testFiles);
    }

    public String getRandomUnitTest(String repoPath, String testFilePath) {
        if (repoPath == null) {
            System.out.println("repoPath should not be empty. Stopping...");
            return null;
        } else if (testFilePath == null) {
            System.out.println("testFilePath should not be empty. Stopping...");
            return null;
        } else {
            if (unitTests.isEmpty())
                initializeUnitTests(repoPath, testFilePath);
            String output = getRandomElement(this.unitTests);
            if (output == null)
                return null;
            return output;
        }
    }

    private void initializeUnitTests(String randomRepoPath, String randomTestFile) {
        String fileName = randomRepoPath + File.separator + srcChildDir + File.separator + randomTestFile;
        try (CompilationUnitWrapper cuw = new CompilationUnitWrapper(fileName)) {
            TestFinderVisitor visitor = new TestFinderVisitor();
            visitor.visit(cuw.getCompilationUnit(), this.unitTests);
        } catch (Exception e) {
            System.err.println("File not found: " + fileName);
        }
    }

    public void runTestCase(String repoPath, String testFile, String testCase) {
        if (repoPath == null || testFile == null || testCase == null) {
            System.out.println("\nFatal Error! runTestCase inputs => " + repoPath + " # " + testFile + " # " + testCase);
            return;
        }
        BuildToolModel.Type buildTool = BuildToolModel.getProjectBuildTool(repoPath);
        System.out.println("\n" + buildTool + " project detected");
        switch (buildTool) {
            case MAVEN:
                List<Dependency> dependencies = MavenBuildToolModel.parseDependencyFromPOM(repoPath);
                System.out.println("\nFound " + dependencies.size() + " dependencies!");
                MavenBuildToolModel mavenBuildToolModel = new MavenBuildToolModel(repoPath);
                boolean isInstalledMavenDep = mavenBuildToolModel.installDependencies();
                if (isInstalledMavenDep) {
                    mavenBuildToolModel.runTestCase(testFile, testCase);
                } else
                    System.out.println("\nDependencies cannot be installed");
                break;
            case GRADLE:
                GradleBuildToolModel gradleBuildToolModel = new GradleBuildToolModel(repoPath);
                boolean isInstalledGradleDep = gradleBuildToolModel.installDependencies();
                if (isInstalledGradleDep)
                    gradleBuildToolModel.runTestCase(testFile, testCase);
                else
                    System.out.println("\nDependencies cannot be installed");
                break;
            case JAVA_STANDARD:
                System.out.println("\nJava Standard test running will be implemented in future");
                break;
            default:
                System.out.println("\nProject build tool cannot be detected!");
        }
    }
}
