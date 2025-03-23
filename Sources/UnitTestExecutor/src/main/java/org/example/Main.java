package org.example;

public class Main {
    private static final String REPOSITORIES_PATH = "E:\\Ph.D - UNL\\Research - Assertion Generation using LLMs\\Sources\\Assertion_Repository_Extractor\\repositories";

    public static void main(String[] args) {
        TestExecutor testExecutor = new TestExecutor(REPOSITORIES_PATH);

        String randomRepoPath = testExecutor.getRandomRepo();
        System.out.println("#Repo Path: " + randomRepoPath);

        String randomTestFile = testExecutor.getRandomTestFile(randomRepoPath);
        System.out.println("#Test Path: " + randomTestFile);

        String randomTestCase = testExecutor.getRandomUnitTest(randomRepoPath, randomTestFile);
        final String testRepo="E:\\Ph.D - UNL\\Research - Assertion Generation using LLMs\\Sources\\Assertion_Repository_Extractor\\repositories\\castorini@anserini";
        final String testUnitTestFile = "io\\anserini\\search\\GeoSearchExplorationTest.java";
        final String testUnitTestMethod = "testGetLine";
        System.out.println("#Test case: " + randomTestCase + "\n");

        System.out.println("Unit tests: " + "\n" + "-----------------------");
        for (String str : testExecutor.getUnitTests())
            System.out.println(str);

        testExecutor.runTestCase(testRepo,testUnitTestFile,testUnitTestMethod);
//        testExecutor.runTestCase(randomRepoPath,randomTestFile,randomTestCase);
    }
}