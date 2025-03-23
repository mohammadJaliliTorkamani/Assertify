package org.example;

import java.util.List;

public class EvaluationResult {
    private boolean isCompiled;
    private String compieJavaVersion;
    private TestResult testResult;
    private String buildToolModelType;
    private List<TestDeclaration> testMethodDeclarations;
    private String errorMessage;
    private String log;
    private RougeAverage rouge;


    public RougeAverage getRouge() {
        return rouge;
    }

    public void setRouge(RougeAverage rouge) {
        this.rouge = rouge;
    }

    public void setFoundTestDeclarations(List<TestDeclaration> testMethodDeclarations) {
        this.testMethodDeclarations = testMethodDeclarations;
    }

    public boolean isCompiled() {
        return isCompiled;
    }

    public void setCompiled(boolean isCompiled) {
        this.isCompiled = isCompiled;
    }

    public List<TestDeclaration> getTestMethodDeclarations() {
        return testMethodDeclarations;
    }

    public String getBuildToolModelType() {
        return buildToolModelType;
    }

    public void setBuildToolModelType(String modelType) {
        this.buildToolModelType = modelType;
    }

    public TestResult getTestResult() {
        return testResult;
    }

    public void setTestResult(TestResult testResult) {
        this.testResult = testResult;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    public String getCompieJavaVersion() {
        return compieJavaVersion;
    }

    public void setCompieJavaVersion(String compieJavaVersion) {
        this.compieJavaVersion = compieJavaVersion;
    }
}
