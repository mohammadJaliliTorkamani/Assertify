package org.example;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import static org.example.Constants.BACKUP_REPOSITORIES_DIR_NAME;
import static org.example.Constants.PARSING_ERROR_MESSAGE;

public class TestInputRecord {
    private final String originalRepoPath;
    private final String testSignature;
    private final String methodSignature;
    private final String methodClassName;
    private String testName;
    private String methodName;
    private String testPath;
    private String methodPath;


    public TestInputRecord(String methodClassName, String originalRepoPath, String testName, String testSignature, String methodName, String methodSignature, String testPath, String methodPath) {
        this.methodClassName = methodClassName;
        this.originalRepoPath = originalRepoPath;
        this.testName = testName;
        this.testSignature = testSignature;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.testPath = testPath;
        this.methodPath = methodPath;
    }

    private static String findFirstDirectoryNameWithSpecialStringAndNoParsingError(File directory, String specialString, EvaluationOutputEntity evaluationOutputEntity) {
        File[] subDirectories = directory.listFiles(File::isDirectory);
        if (subDirectories != null) {
            for (File subDirectory : subDirectories) {
                if (subDirectory.getName().contains(specialString)) {
                    for (Map<String, Object> item : evaluationOutputEntity.getItems()) {
                        if (item.get("post_error_message") != null &&
                                !item.get("post_error_message").equals(PARSING_ERROR_MESSAGE) &&
                                (subDirectory.getName().equals(item.get("repository_name").toString() + "_" + item.get("method"))))
                            return subDirectory.getName();
                    }
                }
            }
        }

        return null; // No matching directory found
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getTestPath() {
        return testPath;
    }

    public String getMethodPath() {
        return methodPath;
    }


    public String getMethodClassName() {
        return methodClassName;
    }

    @Override
    public String toString() {
        return String.format("Test name: %s , Method name: %s , Test path: %s , Method path: %s , methodClassName : %s , methodSignature : %s", testName, methodName, testPath, methodPath, methodClassName, methodSignature);
    }

    public String getRepoPath() {
        File file = new File(methodPath);
        String parentPath = file.getParent();

        while (parentPath != null) {
            if (parentPath.endsWith("src")) {
                return file.getParentFile().getParent();
            }
            file = new File(parentPath);
            parentPath = file.getParent();
        }

        return null; // Repository address not found
    }

    public String getMethodSignature() {
        return methodSignature;
    }

    public String getTestSignature() {
        return testSignature;
    }

    public String getOriginalRepoPath() {
        return originalRepoPath;
    }

    public String getRepoName() {
        Path filePath = Path.of(methodPath);
        Path repositoryPath = filePath.getParent();
        while (repositoryPath != null) {
            String directoryName = repositoryPath.getFileName().toString();
            if (directoryName.contains("@")) {
                return directoryName;
            }
            repositoryPath = repositoryPath.getParent();
        }
        return "";
    }

    public boolean replaceMethodAndTestPathsToBackupVersionOfTheFirstCorrespondingLocalRepoWithNoParsingError(
            String recordsDirPath, EvaluationOutputEntity evaluationOutputEntity) {
        File repositoriesDir = Paths.get(Constants.PIPELINE_PROJECT_DIRECTORY + "\\", recordsDirPath, BACKUP_REPOSITORIES_DIR_NAME).toFile();
        String existingRepoName = findFirstDirectoryNameWithSpecialStringAndNoParsingError(repositoriesDir, getRepoName(), evaluationOutputEntity);
        if (existingRepoName != null) {
            testPath = testPath.replace("Assertion_Repository_Extractor", "Pipeline_v2_0" +
                    File.separator + recordsDirPath).replace(getRepoName(), existingRepoName);
            methodPath = methodPath.replace("Assertion_Repository_Extractor", "Pipeline_v2_0" +
                    "/" + recordsDirPath.replace(File.separator, "/")).replace(getRepoName(), existingRepoName);
            return true;
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestInputRecord that = (TestInputRecord) o;
        return
                Objects.equals(testName, that.testName) &&
                        Objects.equals(testSignature, that.testSignature) &&
                        Objects.equals(methodName, that.methodName) &&
                        Objects.equals(methodSignature, that.methodSignature) &&
                        Objects.equals(testPath, that.testPath) &&
                        Objects.equals(methodClassName, that.methodClassName) &&
                        Objects.equals(methodPath, that.methodPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(testName, testName, methodName, methodSignature, testPath, methodClassName, methodPath);
    }
}
