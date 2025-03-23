package org.example;

import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestAnalyzer {
    private static final String TEST_SUFFIX = "Test.java";
    private static final String TEST_PREFIX = "Test";

    public static List<String> getTestJavaPaths(InputRecord record, boolean backupVersion, MethodDeclaration methodDeclaration) throws NullPointerException {
        List<String> testFiles = new ArrayList<>();
        Path repoDir = Paths.get(record.getRepoPath(backupVersion));

        try (Stream<Path> paths = Files.walk(repoDir)) {
            testFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> p.toString().endsWith(TEST_SUFFIX) || p.toString().startsWith(TEST_PREFIX))
                    .filter(p -> p.toString().contains(File.separator + "test" + File.separator))
                    .filter(p -> {
                        try (BufferedReader reader = Files.newBufferedReader(p)) {
                            return reader.lines().anyMatch(line -> line.contains(methodDeclaration.getNameAsString() + "("));
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    })
                    .map(Path::toString)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return testFiles;
    }

    public List<TestDeclaration> inspect(InputRecord record, boolean backupVersion, MethodDeclaration methodDeclaration) {
        MethodVisitor2 testVisitor = null;
//        ResolvedMethodDeclaration resolvedMethodDeclaration = null;

        List<TestDeclaration> result = new ArrayList<>();
        List<String> javaTestFilePaths = TestAnalyzer.getTestJavaPaths(record, backupVersion, methodDeclaration);
        for (String testFilePath : javaTestFilePaths) {
            try (CompilationUnitWrapper testCUW = new CompilationUnitWrapper(testFilePath)) {
//                            if (resolvedMethodDeclaration == null) {
//                                try {
//                                   resolvedMethodDeclaration = methodDeclaration.resolve();
//                                } catch (Exception e) {
//                                }
//                            }

//                           if (resolvedMethodDeclaration != null) {
//                                testVisitor = new MethodVisitor2(methodDeclaration, resolvedMethodDeclaration, testCUW);
                testVisitor = new MethodVisitor2(methodDeclaration, record.getClassName(), null, testCUW);
                testVisitor.visit(testCUW.getCompilationUnit(), null);
                for (MethodDeclaration tmd : testVisitor.getEnclosingMethodsForMethodCallExpr()) {
                    if (!TestDeclaration.contains(result, tmd, testFilePath)) {
                        result.add(new TestDeclaration(tmd, testFilePath));
                    }
                }
//                            }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private void printDetails(Repository[] repositories) {
        int repos_sum = repositories.length;
        int repoFiles_sum = 0;
        int methods_sum = 0;
        int assertions_sum = 0;
        for (Repository repository : repositories) {
            repoFiles_sum += repository.getFiles().size();
            for (RepoFile repoFile : repository.getFiles()) {
                methods_sum += repoFile.getMethods().size();
                for (Method method : repoFile.getMethods())
                    assertions_sum += method.getAssertions().size();
            }
        }

        System.out.println("\nJSON Summary: \n");
        System.out.println("#Repositories: " + repos_sum);
        System.out.println("#Files having assertions: " + repoFiles_sum);
        System.out.println("#Methods: " + methods_sum);
        System.out.println("#Assertions: " + assertions_sum);
        System.out.println("==========================\n");
    }
}
