package org.example;

import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.File;
import java.util.List;

import static org.example.Constants.BACKUP_DIR_NAME;

public class TestDeclaration {
    private MethodDeclaration methodDeclaration;
    private String path;

    public TestDeclaration(MethodDeclaration methodDeclaration, String path) {
        this.methodDeclaration = methodDeclaration;
        this.path = path;
    }

    public static boolean contains(List<TestDeclaration> list, MethodDeclaration methodDeclaration, String path) {
        return list.stream().anyMatch(testDeclaration -> testDeclaration.getPath().equals(path) &&
                testDeclaration.getMethodDeclaration().getDeclarationAsString().equals(methodDeclaration.getDeclarationAsString()) &&
                testDeclaration.getMethodDeclaration().getBody().get().equals(methodDeclaration.getBody().get()));
    }

    public static TestDeclaration from(MethodDeclaration methodDeclaration, TestInputRecord testInputRecord, String methodAlternativePath) {
        String testBackupVersionPath = extractTestBackupVersion(testInputRecord.getTestPath(),
                methodAlternativePath,
                testInputRecord.getRepoName(),
                testInputRecord.getRepoName() + "_" + testInputRecord.getMethodName());
        boolean fileExists = new File(testBackupVersionPath).exists();

        if (fileExists) {
            try (CompilationUnitWrapper testCUW = new CompilationUnitWrapper(testBackupVersionPath)) {
                MethodVisitor2 testVisitor = new MethodVisitor2(methodDeclaration, testInputRecord.getMethodClassName(),
                        null, testCUW);
                testVisitor.visit(testCUW.getCompilationUnit(), null);
                return new TestDeclaration(testVisitor.getEnclosingMethodsForMethodCallExpr().get(0),
                        testBackupVersionPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * hThis method creates the corresponding test path in the backup version
     */
    private static String extractTestBackupVersion(String testPath, String methodAlternativePath, String oldRepoName, String newRepoName) {
        String[] splittedAltPath = methodAlternativePath.split("/");
        for (int i = 0; i < splittedAltPath.length; i++) {
            if (splittedAltPath[i].equals(BACKUP_DIR_NAME)) {
                String recordDirName = splittedAltPath[i + 1];
                return testPath
                        .replace("Assertion_Repository_Extractor", "Pipeline_v2_0" +
                                File.separator + BACKUP_DIR_NAME + File.separator + recordDirName)
                        .replace(oldRepoName, newRepoName);
            }
        }
        return null;
    }

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    public void setMethodDeclaration(MethodDeclaration methodDeclaration) {
        this.methodDeclaration = methodDeclaration;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
