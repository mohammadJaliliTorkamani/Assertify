package org.example;

import java.util.LinkedHashMap;
import java.util.Map;

public class Response {
    private static int counter = 0;
    private final int index;
    private int ceil;
//    private EvaluationResult preEvaluationResult;
    private EvaluationResult postEvaluationResult;
    private InputRecord record;
    private LLM_InputContent command;
    private String augmented_method;
    private String rawResponse;
    private String rawFilteredResponse;
    private String groundTruthAssertions;
    private Parser parser;
//    private String preLog;
    private String postLog;
//    private String preErrorMessage;
    private String postErrorMessage;
    private String compileCommand;
    private String runCommand;

    public static void resetCounter(){
        counter = 0;
    }

    public String getGroundTruthAssertions() {
        return groundTruthAssertions;
    }

    public void setGroundTruthAssertions(String groundTruthAssertions) {
        this.groundTruthAssertions = groundTruthAssertions;
    }

    /**
     * contains the time taken for creating prompt as well as getting the response from the LLM + preprocessing
     */
    private long promptCreationTime;
    /**
     * contains the cost spent for creating prompt as well as getting the response from the LLM (and one embedding vector computation)
     */
    private double promptCreationCost;

    public Response() {
        this.index = ++counter;
        this.promptCreationTime = -1;
    }


    public void setRawFilteredResponse(String rawFilteredResponse) {
        this.rawFilteredResponse = rawFilteredResponse;
    }

    public String getRawFilteredResponse() {
        return rawFilteredResponse;
    }

    public double getPromptCreationCost() {
        return promptCreationCost;
    }

    public void setPromptCreationCost(double promptCreationCost) {
        this.promptCreationCost = promptCreationCost;
    }

    @Override
    public String toString() {
        Map<String, Object> map = getAsMap();
        String output = "";
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            output = String.format("%s\n========================================\n%s: %s", output, key, value);
        }
        return output;
    }

    public LinkedHashMap<String, Object> getAsMap() {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("id", index);
        map.put("size", ceil);
        map.put("repository_name", record.getRepoName());
        map.put("method", record.getName());
        map.put("build_tool", postEvaluationResult == null || postEvaluationResult.getBuildToolModelType() == null ? null : postEvaluationResult.getBuildToolModelType());
        map.put("LLM_command", command);
        map.put("augmented_method", augmented_method);
        map.put("LLM_raw_response", rawResponse);
        map.put("LLM_filtered_raw_response", rawFilteredResponse);
        map.put("ground_truth_assertions", groundTruthAssertions);
        map.put("compileCommand", compileCommand);
        map.put("prompt_creation_time", promptCreationTime);
        map.put("prompt_creation_cost", promptCreationCost);
        map.put("runCommand", runCommand);
//        map.put("is_compiled_before", preEvaluationResult == null ? null : preEvaluationResult.isCompiled());
        map.put("is_compiled_after", postEvaluationResult == null ? null : postEvaluationResult.isCompiled());
        map.put("java_address", postEvaluationResult == null ? null : postEvaluationResult.getCompieJavaVersion());
//        map.put("tests_before", preEvaluationResult == null ? null : preEvaluationResult.getTestResult());
        map.put("tests_after", postEvaluationResult == null ? null : postEvaluationResult.getTestResult());
        map.put("rouge_scores", postEvaluationResult == null ? null : postEvaluationResult.getRouge());
//        map.put("pre_error_message", preErrorMessage);
        map.put("post_error_message", postErrorMessage);
//        map.put("pre_log", preLog);
        map.put("post_log", postLog);
        map.put("original_method", parser == null ? null : parser.getOriginalMethod());
        map.put("pruned_method", parser == null ? null : parser.getPrunedMethod());
        try {
            map.put("repository_original_path", record.getRepoPath(false));
            map.put("repository_alternative_path", record.getRepoPath(true));
        } catch (Exception e) {
            map.put("repository_original_path", null);
            map.put("repository_alternative_path", null);
        }
        try {
            map.put("method_original_path", record.getPath());
            map.put("method_alternative_path", record.getAlternativePath());
        } catch (Exception e) {
            map.put("method_original_path", null);
            map.put("method_alternative_path", null);
        }

        return map;
    }

    public int getIndex() {
        return index;
    }

    public void setPromptCreationTime(long promptCreationTime) {
        this.promptCreationTime = promptCreationTime;
    }

    public int getCeil() {
        return ceil;
    }

    public void setCeil(int ceil) {
        this.ceil = ceil;
    }

    public String getAugmented_method() {
        return augmented_method;
    }

    public void setAugmentedMethod(String augmented_method) {
        this.augmented_method = augmented_method;
    }

//    public EvaluationResult getPreEvaluationResult() {
//        return preEvaluationResult;
//    }

//    public void setPreEvaluationResult(EvaluationResult preEvaluationResult) {
//        this.preEvaluationResult = preEvaluationResult;
//    }

    public EvaluationResult getPostEvaluationResult() {
        return postEvaluationResult;
    }

    public void setPostEvaluationResult(EvaluationResult postEvaluationResult) {
        this.postEvaluationResult = postEvaluationResult;
    }

    public InputRecord getRecord() {
        return record;
    }

    public void setRecord(InputRecord record) {
        this.record = record;
    }

    public void setLLMCommand(LLM_InputContent command) {
        this.command = command;
    }

    public void setLLMRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public Parser getParser() {
        return parser;
    }

    public void setParser(Parser parser) {
        this.parser = parser;
    }

//    public String getPreLog() {
//        return preLog;
//    }

//    public void setPreLog(String preLog) {
//        this.preLog = preLog;
//    }

    public String getPostLog() {
        return postLog;
    }

    public void setPostLog(String postLog) {
        this.postLog = postLog;
    }

//    public String getPreErrorMessage() {
//        return preErrorMessage;
//    }

//    public void setPreErrorMessage(String preErrorMessage) {
//        this.preErrorMessage = preErrorMessage;
//    }

    public String getPostErrorMessage() {
        return postErrorMessage;
    }

    public void setPostErrorMessage(String postErrorMessage) {
        this.postErrorMessage = postErrorMessage;
    }

    public String getCompileCommand() {
        return compileCommand;
    }

    public void setCompileCommand(String compileCommand) {
        this.compileCommand = compileCommand;
    }

    public String getRunCommand() {
        return runCommand;
    }

    public void setRunCommand(String runCommand) {
        this.runCommand = runCommand;
    }
}
