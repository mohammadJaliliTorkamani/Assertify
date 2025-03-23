package org.example;

import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.example.Constants.*;

/**
 * Note that while executing unit tests, the statistics could be more than the number of tests we execute, because
 * sometimes there are some configurations in the source code itself which allows a unit test run multiple times with
 * different parameters. Overall, the number of executed tests could be more than total number of tests in dataset.
 * (like 2000>1196)
 */
public class Evaluator {
    private final TestAnalyzer testAnalyzer;
    private final String rougeReferenceSummaryPath;
    private final String rougeCandidateSummaryPath;
    private final String rougePythonFile;

    public Evaluator(String rougePythonFile, String rougeReferenceSummaryPath, String rougeCandidateSummaryPath) {
        this.rougeReferenceSummaryPath = rougeReferenceSummaryPath;
        this.rougeCandidateSummaryPath = rougeCandidateSummaryPath;
        this.testAnalyzer = new TestAnalyzer();
        this.rougePythonFile = rougePythonFile;
    }

//    public EvaluationResult preEvaluate(Response response, InputRecord record, Parser parser) throws Exception {
//        MethodDeclaration methodDeclaration = parser.getOriginalMethodDeclaration();
//        EvaluationResult evaluationResult = new EvaluationResult();
//
//        BuildToolModel buildToolModel = BuildToolModel.getProjectBuildToolModel(record.getRepoPath(false));
//        if (buildToolModel != null) {
//            evaluationResult.setBuildToolModelType(buildToolModel.getClass().getSimpleName());
//            System.out.println("---> Evaluation - Code Compilation | Before Replacement");
//            ComponentResponse compilationResponse = buildToolModel.compile(true, null);
//            response.setCompileCommand(compilationResponse.getCommand());
//            evaluationResult.setCompiled(compilationResponse.isOK());
//            evaluationResult.setCompieJavaVersion(compilationResponse.getJavaAddress());
//            System.out.println("AQA:" +compilationResponse.isOK());
//            if (compilationResponse.isOK()) {
//                System.out.println("     Finding tests for the method: " + methodDeclaration.getDeclarationAsString());
//                List<TestDeclaration> testMethodDeclarations = findTests(record, parser.getOriginalMethodDeclaration());
//                evaluationResult.setFoundTestDeclarations(testMethodDeclarations);
//                System.out.printf("          Found %d tests%n", testMethodDeclarations.size());
//                if (!testMethodDeclarations.isEmpty()) {
//                    ComponentResponse runTestsResponse = runTests(buildToolModel, testMethodDeclarations, compilationResponse.getJavaAddress());
//                    response.setRunCommand(runTestsResponse.getCommand());
//                    TestResult testResult = buildToolModel.extractTestExecutionStatistic(runTestsResponse.getMessage(), runTestsResponse.getCommand());
//                    evaluationResult.setTestResult(testResult);
//                    evaluationResult.setLog(runTestsResponse.getMessage());
//                    if (!runTestsResponse.isOK())
//                        evaluationResult.setErrorMessage(TESTING_ERROR_MESSAGE);
//                }
//            } else {
//                evaluationResult.setErrorMessage(COMPILATION_ERROR_MESSAGE);
//                evaluationResult.setLog(compilationResponse.getMessage());
//            }
//        } else
//            evaluationResult.setErrorMessage(UNDETECTABLE_PROJECT_BUILD_TOOL_ERROR_MESSAGE);
//
//
//        return evaluationResult;
//    }

    public EvaluationResult postEvaluate(Response response, InputRecord record, Parser parser, MethodDeclaration methodDeclaration, String compileJavaVersion) throws Exception {
        System.out.println("---> Evaluation - Code Compilation | After Replacement");
        EvaluationResult evaluationResult = new EvaluationResult();
        try {
            System.out.println("---> Evaluation - ROUGE scores");
            if (prepareRouges(record, parser, rougeReferenceSummaryPath, rougeCandidateSummaryPath)) {
                RougeAverage rouge = evaluateRouges(rougeReferenceSummaryPath, rougeCandidateSummaryPath);
                evaluationResult.setRouge(rouge);

                BuildToolModel buildToolModel = BuildToolModel.getProjectBuildToolModel(record.getRepoPath(true));
                if (buildToolModel != null) {
                    evaluationResult.setBuildToolModelType(buildToolModel.getClass().getSimpleName());
                    ComponentResponse compilationResponse = buildToolModel.compile(true, compileJavaVersion);
                    response.setCompileCommand(compilationResponse.getCommand());
                    evaluationResult.setCompiled(compilationResponse.isOK());
                    evaluationResult.setCompieJavaVersion(compilationResponse.getJavaAddress());
                    if (compilationResponse.isOK()) {
                        System.out.printf("     Finding tests for the method: %s%n", methodDeclaration.getDeclarationAsString());
                        List<TestDeclaration> testMethodDeclarations = findTests(record, parser.getOriginalMethodDeclaration());
                        evaluationResult.setFoundTestDeclarations(testMethodDeclarations);
                        System.out.printf("          Found %d tests%n", testMethodDeclarations.size());
                        if (!testMethodDeclarations.isEmpty()) {
                            ComponentResponse runTestsResponse = runTests(buildToolModel, testMethodDeclarations, compileJavaVersion);
                            TestResult testResult = buildToolModel.extractTestExecutionStatistic(runTestsResponse.getMessage(), runTestsResponse.getCommand());
                            evaluationResult.setTestResult(testResult);
                            evaluationResult.setLog(runTestsResponse.getMessage());
                            if (!runTestsResponse.isOK())
                                evaluationResult.setErrorMessage(TESTING_ERROR_MESSAGE);
                        }
                    } else {
                        evaluationResult.setErrorMessage(COMPILATION_ERROR_MESSAGE);
                        evaluationResult.setLog(compilationResponse.getMessage());
                    }
                } else
                    evaluationResult.setErrorMessage(UNDETECTABLE_PROJECT_BUILD_TOOL_ERROR_MESSAGE);

            } else
                evaluationResult.setErrorMessage(ROUGE_COMPUTATION_ERROR_MESSAGE);
            return evaluationResult;
        } finally {
            File candidateSummaryFile = new File(rougeCandidateSummaryPath);
            File referenceSummaryFile = new File(rougeReferenceSummaryPath);
            if (candidateSummaryFile.exists()) candidateSummaryFile.delete();
            if (referenceSummaryFile.exists()) referenceSummaryFile.delete();
        }
    }

    private boolean prepareRouges(InputRecord record, Parser parser, String rougeReferenceSummaryPath, String rougeCandidateSummaryPath) throws Exception {
        String methodName = record.getName();
        Set<String> originalAssertions = new Extractor(record.getPath()).extractAssertions(record.getClassName(), methodName, record.getSignature());
        List<String> newMethodAssertions = parser.extractAssertions(record.getClassName(), methodName, record.getSignature());
        StringBuilder originalAssertionsStr = new StringBuilder();
        StringBuilder newMethodAssertionsStr = new StringBuilder();

        originalAssertions.forEach(assertion -> originalAssertionsStr.append(assertion).append("\n"));
        newMethodAssertions.forEach(assertion -> newMethodAssertionsStr.append(assertion).append("\n"));

        return createFile(rougeReferenceSummaryPath, originalAssertionsStr.toString()) &&
                createFile(rougeCandidateSummaryPath, newMethodAssertionsStr.toString());
    }

    private boolean createFile(String path, String content) throws Exception {
        File file = new File(path);
        if (file.exists()) {
            if (!file.delete())
                return false;
        }
        if (file.createNewFile()) {
            FileWriter fileWriter = new FileWriter(path, false);
            fileWriter.write(content.trim());
            fileWriter.close();
            return true;
        }

        return false;
    }

    private RougeAverage evaluateRouges(String rougeReferenceSummaryPath, String rougeCandidateSummaryPath) throws Exception {
        RougeAverage rouge = new RougeAverage();
        List<String> originalAssertions = Files.readAllLines(Path.of(rougeReferenceSummaryPath));
        List<String> newMethodAssertions = Files.readAllLines(Path.of(rougeCandidateSummaryPath));
        StringBuilder originalAssertionsStr = new StringBuilder();
        StringBuilder newMethodAssertionsStr = new StringBuilder();

        originalAssertions.forEach(assertion -> originalAssertionsStr.append(assertion).append("\n"));
        newMethodAssertions.forEach(assertion -> newMethodAssertionsStr.append(assertion).append("\n"));

        String command = "source ./models/env/bin/activate && python3" + " " + rougePythonFile + " " + rougeReferenceSummaryPath + " " + rougeCandidateSummaryPath;
        String[] terminalCommand = {"/bin/bash", "-c", command};
        ProcessBuilder processBuilder = new ProcessBuilder(terminalCommand);
        processBuilder.directory(new File(PYTHON_SCRIPTS_DIR));
        processBuilder.environment().putAll(System.getenv());
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        InputStream inputStream = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            Pattern pattern = Pattern.compile("\\{'rouge1':+\\s\\{'F':\\s(\\d*\\.\\d*),\\s'P':\\s(\\d*\\.\\d*),\\s'R':\\s(\\d*\\.\\d*)\\},\\s'rouge2':\\s\\{'F':\\s(\\d*\\.\\d*),\\s'P':\\s(\\d*\\.\\d*),\\s'R':\\s(\\d*\\.\\d*)\\},\\s'rougeL':\\s\\{'F':\\s(\\d*\\.\\d*),\\s'P':\\s(\\d*\\.\\d*),\\s'R':\\s(\\d*\\.\\d*)\\}\\}");
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                rouge.setRouge_1_averageF(Double.parseDouble(matcher.group(1)));
                rouge.setRouge_1_averageP(Double.parseDouble(matcher.group(2)));
                rouge.setRouge_1_averageR(Double.parseDouble(matcher.group(3)));
                rouge.setRouge_2_averageF(Double.parseDouble(matcher.group(4)));
                rouge.setRouge_2_averageP(Double.parseDouble(matcher.group(5)));
                rouge.setRouge_2_averageR(Double.parseDouble(matcher.group(6)));
                rouge.setRouge_L_averageF(Double.parseDouble(matcher.group(7)));
                rouge.setRouge_L_averageP(Double.parseDouble(matcher.group(8)));
                rouge.setRouge_L_averageR(Double.parseDouble(matcher.group(9)));
                break;
            }
        }

        rouge.setCandidateSummary(newMethodAssertionsStr.toString().trim());
        rouge.setReferenceSummary(originalAssertionsStr.toString().trim());
        int exitCode = process.waitFor();
        return rouge;
    }

    private ComponentResponse runTests(BuildToolModel buildToolModel, List<TestDeclaration> testMethodDeclarations, String javaAddress) throws Exception {
        String[] testFiles = new String[testMethodDeclarations.size()];
        String[] testCases = new String[testMethodDeclarations.size()];
        for (int i = 0; i < testMethodDeclarations.size(); i++) {
            TestDeclaration td = testMethodDeclarations.get(i);
            testFiles[i] = Paths.get(Utils.getFirstAddressUpwardHaving(td.getPath(), "src", false).toString(),
                    "test", "java").relativize(Paths.get(td.getPath())).toString();
            testCases[i] = td.getMethodDeclaration().getNameAsString();
        }
        return buildToolModel.runTestCase(testFiles, testCases, javaAddress);
    }

    private List<TestDeclaration> findTests(InputRecord record, MethodDeclaration methodDeclaration) {
        return testAnalyzer.inspect(record, true, methodDeclaration);
    }
}
