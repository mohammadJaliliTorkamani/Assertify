package org.example;

public class TestResult {
    private int testsRun;
    private int failures;
    private int errors;
    private int skipped;
    private int passed;

    public TestResult(int testsRun, int failures, int errors, int skipped) {
        this.passed = testsRun - errors - failures - skipped;
        this.testsRun = testsRun;
        this.failures = failures;
        this.errors = errors;
        this.skipped = skipped;
    }

    public TestResult() {
    }

    public int getTestsRun() {
        return testsRun;
    }

    public void setTestsRun(int testsRun) {
        this.testsRun = testsRun;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public int getSkipped() {
        return skipped;
    }

    public void setSkipped(int skipped) {
        this.skipped = skipped;
    }

    public int getPassed() {
        return passed;
    }

    public void setPassed(int passed) {
        this.passed = passed;
    }
}