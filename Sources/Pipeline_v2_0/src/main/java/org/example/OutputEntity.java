package org.example;

import com.google.gson.internal.LinkedTreeMap;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.example.Constants.COMPILATION_ERROR_MESSAGE;
import static org.example.Constants.PARSING_ERROR_MESSAGE;

public abstract class OutputEntity {

    /**
     * test results before replacing the code with LLM
     */
//    protected TestResult preTestResult;

    /**
     * test results after replacing the code with LLM
     */
    protected TestResult postTestResult;

    /**
     * number of compilation errors before replacing the code with LLM
     */
//    protected int numberOfPreCompilationErrors;

    /**
     * number of compilation errors after replacing the code with LLM
     */
    protected int numberOfPostCompilationErrors;

    /**
     * name of stage (either evaluation or testing)
     */
    protected String stageName;
    protected List<Map<String, Object>> items;
    protected RougeAverage rougeAverage;
    protected int numberOfParsingErrors;
    protected long totalPromptCreationTime;
    protected double totalPromptCreationCost;
    private LLM_Config llmConfig;

    public OutputEntity(List<Map<String, Object>> items, String stageName) {
        this.items = items;
        this.stageName = stageName;
//        this.preTestResult = new TestResult();
        this.postTestResult = new TestResult();
    }

    public OutputEntity() {
    }

    protected static RougeAverage createRougeAverage(List<Map<String, Object>> items) {
        double sumRouge1F = 0d;
        double sumRouge1P = 0d;
        double sumRouge1R = 0d;
        double sumRouge2F = 0d;
        double sumRouge2P = 0d;
        double sumRouge2R = 0d;
        double sumRougeLF = 0d;
        double sumRougeLP = 0d;
        double sumRougeLR = 0d;
        int counter = 0;

        for (Map<String, Object> item : items) {
            LinkedTreeMap<String, Object> score = (LinkedTreeMap<String, Object>) item.get("rouge_scores");
            counter++;
            if (score != null) {
                sumRouge1F += (Double) score.get("rouge_1_averageF");
                sumRouge1P += (Double) score.get("rouge_1_averageP");
                sumRouge1R += (Double) score.get("rouge_1_averageR");
                sumRouge2F += (Double) score.get("rouge_2_averageF");
                sumRouge2P += (Double) score.get("rouge_2_averageP");
                sumRouge2R += (Double) score.get("rouge_2_averageR");
                sumRougeLF += (Double) score.get("rouge_L_averageF");
                sumRougeLP += (Double) score.get("rouge_L_averageP");
                sumRougeLR += (Double) score.get("rouge_L_averageR");
            }
        }

        return counter == 0 ? null : new RougeAverage(
                sumRouge1F / counter,
                sumRouge1P / counter,
                sumRouge1R / counter,
                sumRouge2F / counter,
                sumRouge2P / counter,
                sumRouge2R / counter,
                sumRougeLF / counter,
                sumRougeLP / counter,
                sumRougeLR / counter,
                null,
                null);
    }

    protected String getStageName() {
        return stageName;
    }

    protected void setStageName(String stageName) {
        this.stageName = stageName;
    }

//    protected TestResult getPreTestResult() {
//        return preTestResult;
//    }

//    protected void setPreTestResult(TestResult preTestResult) {
//        this.preTestResult = preTestResult;
//    }

    public void adjustLlmConfig(LLM_Config llmConfig) {
        this.llmConfig = llmConfig;
    }

    public LLM_Config getLlmConfig() {
        return llmConfig;
    }

    protected TestResult getPostTestResult() {
        return postTestResult;
    }

    protected void setPostTestResult(TestResult postTestResult) {
        this.postTestResult = postTestResult;
    }

//    protected int getNumberOfPreCompilationErrors() {
//        return numberOfPreCompilationErrors;
//    }

//    protected void setNumberOfPreCompilationErrors(int numberOfPreCompilationErrors) {
//        this.numberOfPreCompilationErrors = numberOfPreCompilationErrors;
//    }

    protected int getNumberOfPostCompilationErrors() {
        return numberOfPostCompilationErrors;
    }

    protected void setNumberOfPostCompilationErrors(int numberOfPostCompilationErrors) {
        this.numberOfPostCompilationErrors = numberOfPostCompilationErrors;
    }

    protected List<Map<String, Object>> getItems() {
        return items;
    }

    protected void setItems(List<Map<String, Object>> items) {
        this.items = items;
    }

    protected void adjustNumberOfParsingErrors() {
        this.numberOfParsingErrors = (int) getItems()
                .stream()
                .filter(stringObjectMap -> stringObjectMap.get("post_error_message") != null)
                .map(stringObjectMap -> stringObjectMap.get("post_error_message"))
                .filter(error -> error.equals(PARSING_ERROR_MESSAGE))
                .count();
    }

    protected RougeAverage getRougeAverage() {
        return rougeAverage;
    }

    protected void setRougeAverage(RougeAverage rougeAverage) {
        this.rougeAverage = rougeAverage;
    }

    protected int getNumberOfParsingErrors() {
        return numberOfParsingErrors;
    }

//    protected void adjustNumberOfPreCompilationErrors() {
//        this.numberOfPreCompilationErrors = (int) items
//                .stream()
//                .filter(stringObjectMap -> stringObjectMap.get("pre_error_message") != null)
//                .map(stringObjectMap -> stringObjectMap.get("pre_error_message"))
//                .filter(error -> error.equals(COMPILATION_ERROR_MESSAGE))
//                .count();
//    }

    protected void adjustNumberOfPostCompilationErrors() {
        this.numberOfPostCompilationErrors = (int) items
                .stream()
                .filter(stringObjectMap -> stringObjectMap.get("post_error_message") != null)
                .map(stringObjectMap -> stringObjectMap.get("post_error_message"))
                .filter(error -> error.equals(COMPILATION_ERROR_MESSAGE))
                .count();
    }

//    protected void adjustNumberOfPreTests() {
//        this.preTestResult.setTestsRun(items
//                .stream()
//                .filter(stringObjectMap -> stringObjectMap.get("tests_before") != null)
//                .map(stringObjectMap -> stringObjectMap.get("tests_before"))
//                .mapToDouble(value -> (double) ((LinkedTreeMap<?, ?>) value).get("testsRun"))
//                .mapToInt(value -> (int) value)
//                .sum());
//    }

    protected void adjustNumberOfPostTests() {
        this.postTestResult.setTestsRun(items
                .stream()
                .filter(stringObjectMap -> stringObjectMap.get("tests_after") != null)
                .map(stringObjectMap -> stringObjectMap.get("tests_after"))
                .mapToDouble(value -> (double) ((LinkedTreeMap<?, ?>) value).get("testsRun"))
                .mapToInt(value -> (int) value)
                .sum());
    }

//    protected void adjustNumberOfPreFailedTests() {
//        this.preTestResult.setFailures(items
//                .stream()
//                .filter(stringObjectMap -> stringObjectMap.get("tests_before") != null)
//                .map(stringObjectMap -> stringObjectMap.get("tests_before"))
//                .mapToDouble(value -> (double) ((LinkedTreeMap<?, ?>) value).get("failures"))
//                .mapToInt(value -> (int) value)
//                .sum());
//    }

    protected void adjustNumberOfPostFailedTests() {
        this.postTestResult.setFailures(items
                .stream()
                .filter(stringObjectMap -> stringObjectMap.get("tests_after") != null)
                .map(stringObjectMap -> stringObjectMap.get("tests_after"))
                .mapToDouble(value -> (double) ((LinkedTreeMap<?, ?>) value).get("failures"))
                .mapToInt(value -> (int) value)
                .sum());
    }

//    protected void adjustNumberOfPreErrorTests() {
//        this.preTestResult.setErrors(items
//                .stream()
//                .filter(stringObjectMap -> stringObjectMap.get("tests_before") != null)
//                .map(stringObjectMap -> stringObjectMap.get("tests_before"))
//                .mapToDouble(value -> (double) ((LinkedTreeMap<?, ?>) value).get("errors"))
//                .mapToInt(value -> (int) value)
//                .sum());
//    }

    protected void adjustNumberOfPostErrorTests() {
        this.postTestResult.setErrors(items
                .stream()
                .filter(stringObjectMap -> stringObjectMap.get("tests_after") != null)
                .map(stringObjectMap -> stringObjectMap.get("tests_after"))
                .mapToDouble(value -> (double) ((LinkedTreeMap<?, ?>) value).get("errors"))
                .mapToInt(value -> (int) value)
                .sum());
    }

//    protected void adjustNumberOfPrePassedTests() {
//        this.preTestResult.setPassed(preTestResult.getTestsRun() - (preTestResult.getFailures() + preTestResult.getErrors()));
//    }

    protected void adjustNumberOfPostPassedTests() {
        this.postTestResult.setPassed(postTestResult.getTestsRun() - (postTestResult.getFailures() + postTestResult.getErrors()));
    }

//    protected void adjustNumberOfPreSkippedTests() {
//        this.preTestResult.setSkipped(items
//                .stream()
//                .filter(stringObjectMap -> stringObjectMap.get("tests_before") != null)
//                .map(stringObjectMap -> stringObjectMap.get("tests_before"))
//                .mapToDouble(value -> (double) ((LinkedTreeMap<?, ?>) value).get("skipped"))
//                .mapToInt(value -> (int) value)
//                .sum());
//    }

    protected void adjustNumberOfPostSkippedTests() {
        this.postTestResult.setSkipped(items
                .stream()
                .filter(stringObjectMap -> stringObjectMap.get("tests_after") != null)
                .map(stringObjectMap -> stringObjectMap.get("tests_after"))
                .mapToDouble(value -> (double) ((LinkedTreeMap<?, ?>) value).get("skipped"))
                .mapToInt(value -> (int) value)
                .sum());
    }

    public void adjustTotalPromptCreationTime() {
        this.totalPromptCreationTime = items
                .stream()
                .map(stringObjectMap -> stringObjectMap.get("prompt_creation_time"))
                .filter(Objects::nonNull)
                .mapToLong(value -> ((long) ((double) (Double) value)))
                .filter(value -> value > -1)
                .sum();
    }

    public void adjustTotalPromptCreationCost() {
        this.totalPromptCreationCost = items
                .stream()
                .map(stringObjectMap -> stringObjectMap.get("prompt_creation_cost"))
                .filter(Objects::nonNull)
                .mapToDouble(value -> ((double) (Double) value))
                .filter(value -> value >= 0d)
                .sum();
    }
}
