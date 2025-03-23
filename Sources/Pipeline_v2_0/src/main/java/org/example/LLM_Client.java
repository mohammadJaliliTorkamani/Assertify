package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javassist.NotFoundException;
import org.codehaus.plexus.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.example.Constants.MINIMUM_REQUEST_DELAY_SECONDS;

public class LLM_Client {
    private static Instant lastRequestTime = Instant.MIN;
    private final LLM_Config config;
    private final int trial_number;
    private final LLMCacheManager cacheManager;

    public LLM_Client(LLM_Config config, int trial_number) {
        this.config = config;
        this.trial_number = trial_number;
        this.cacheManager = new LLMCacheManager();
    }

    public LLM_Config getConfig() {
        return config;
    }

    public Pair<String, Double> askModel(LLM_InputContent command) throws Exception {
        Pair<String, Double> response = runScript(command, LLM_Operation.COMPLETION);
        if (response.getFirst().startsWith("Traceback") || response.getFirst().startsWith("Exceeded maximum trials"))
            System.err.printf("Error while asking Completion!%n===> Command:%n%s%n===> Response: %s%n", command, response);

        return response;
    }

    private String generatePythonModelRunnerCommand(LLM_Operation operation) throws NotFoundException {
        String path;
        String modelName;
        switch (operation) {
            case EMBEDDING:
                path = config.getEmbeddingsModel().getFileName();
                modelName = config.getEmbeddingsModel().getModelName();
                break;
            case COMPLETION:
                path = config.getCompletionModel().getFileName();
                modelName = config.getCompletionModel().getModelName();
                break;
            default:
                throw new NotFoundException("LLM Operation cannot be found");
        }

        double temperature = config.getTemperature();
        double topP = config.getTop_p();
        String apiKey = config.getApiKey();
        double frequencyPenalty = config.getFrequency_penalty();
        double presencePenalty = config.getPresence_penalty();
        return "source ./env/bin/activate && python3 " + path +
                " --model " + modelName +
                " --is_embedding " + operation.equals(LLM_Operation.EMBEDDING) +
                " --api_key " + apiKey +
                " --trial_number " + trial_number +
                " --mrds " + MINIMUM_REQUEST_DELAY_SECONDS +
                " --temperature " + temperature +
                " --top_p " + topP +
                " --frequency_penalty " + frequencyPenalty +
                " --presence_penalty " + presencePenalty +
                " --max_length " + Constants.MAX_LLM_RESPONSE_LENGTH;
    }

    private String generateBatchEmbeddingPythonModelRunnerCommand() {
        String path;
        String modelName;
        path = config.getEmbeddingsModel().getFileName();
        modelName = config.getEmbeddingsModel().getModelName();
        double temperature = config.getTemperature();
        double topP = config.getTop_p();
        String apiKey = config.getApiKey();
        double frequencyPenalty = config.getFrequency_penalty();
        double presencePenalty = config.getPresence_penalty();

        return String.format(
                "source ./env/bin/activate && python %s --model %s --is_embedding true --is_batch true --api_key %s --trial_number %d --mrds %d --temperature %f --top_p %f --frequency_penalty %f --presence_penalty %f --max_length %d",
                path, modelName, apiKey, trial_number, MINIMUM_REQUEST_DELAY_SECONDS, temperature, topP, frequencyPenalty, presencePenalty, Constants.MAX_LLM_RESPONSE_LENGTH
        );
    }


    private Pair<String, Double> runScript(LLM_InputContent input, LLM_Operation operation) throws Exception {
        Pair<String, Double> cachedResponse = cacheManager.retrieve(input, operation, config);
        if (cachedResponse != null) {
            System.out.printf("     (Using cache record for %s request)%n", operation.toString());
            return cachedResponse;
        } else {
            Instant currentTime = Instant.now();
            Duration timeSinceLastRequest = Duration.between(lastRequestTime, currentTime);
            long secondsSinceLastRequest = timeSinceLastRequest.getSeconds();

            double totalCost = 0;
            if (secondsSinceLastRequest >= MINIMUM_REQUEST_DELAY_SECONDS) {
                lastRequestTime = currentTime;
                String response = _runScript(input, operation);
                totalCost = Utils.computeCostOfTokens(input, response, operation, config.getCompletionModel(), config.getEmbeddingsModel());
                cacheManager.store(input, operation, response, config, totalCost);
                return Pair.of(response, totalCost);
            } else {
                long remainingSeconds = MINIMUM_REQUEST_DELAY_SECONDS - secondsSinceLastRequest;
                Thread.sleep(remainingSeconds * 1000);
                lastRequestTime = Instant.now();
                String response = _runScript(input, operation);
                totalCost = Utils.computeCostOfTokens(input, response, operation, config.getCompletionModel(), config.getEmbeddingsModel());
                cacheManager.store(input, operation, response, config, totalCost);
                return Pair.of(response, totalCost);
            }
        }
    }

    private String runBatchEmbeddingScript(List<LLM_InputContent> inputs) throws Exception {
        Instant currentTime = Instant.now();
        Duration timeSinceLastRequest = Duration.between(lastRequestTime, currentTime);
        long secondsSinceLastRequest = timeSinceLastRequest.getSeconds();

        if (secondsSinceLastRequest >= MINIMUM_REQUEST_DELAY_SECONDS) {
            lastRequestTime = currentTime;
        } else {
            long remainingSeconds = MINIMUM_REQUEST_DELAY_SECONDS - secondsSinceLastRequest;
            Thread.sleep(remainingSeconds * 1000);
            lastRequestTime = Instant.now();
        }
        return _runBatchEmbeddingScript(inputs);
    }

    private String _runScript(LLM_InputContent input, LLM_Operation operation) throws Exception {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        String userPromptFilePath = Constants.PYTHON_SCRIPT_MODELS_DIR + File.separator + Constants.USER_PROMPT_FILE;
        File userPromptFile = new File(userPromptFilePath);
        String systemPromptFilePath = Constants.PYTHON_SCRIPT_MODELS_DIR + File.separator + Constants.SYSTEM_PROMPT_FILE;
        File systemPromptFile = new File(systemPromptFilePath);
        String assistantPromptFilePath = Constants.PYTHON_SCRIPT_MODELS_DIR + File.separator + Constants.ASSISTANT_PROMPT_FILE;
        File assistantPromptFile = new File(assistantPromptFilePath);
        if (systemPromptFile.exists())
            systemPromptFile.delete();
        if (userPromptFile.exists())
            userPromptFile.delete();
        if (assistantPromptFile.exists())
            assistantPromptFile.delete();

        try {
            if (input.getUser() != null && !input.getUser().isEmpty()) {
                FileUtils.fileWrite(new File(userPromptFilePath), "UTF-8", gson.toJson(new TransitPrompt(gson.toJson(input.getUser()))));
            }

            if (input.getSystem() != null) {
                FileUtils.fileWrite(new File(systemPromptFilePath), "UTF-8", gson.toJson(new TransitPrompt(input.getSystem())));
            }

            if (input.getAssistant() != null && !input.getAssistant().isEmpty()) {
                FileUtils.fileWrite(new File(assistantPromptFilePath), "UTF-8", gson.toJson(new TransitPrompt(gson.toJson(input.getAssistant()))));
            }

            String[] terminalCommand = {"/bin/bash", "-c", generatePythonModelRunnerCommand(operation)};
            ProcessBuilder processBuilder = new ProcessBuilder(terminalCommand);
            processBuilder.directory(new File(Constants.PYTHON_SCRIPT_MODELS_DIR));
            processBuilder.environment().putAll(System.getenv());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null)
                response.append(line).append("\n");

            return response.toString().trim();
        } finally {
            if (systemPromptFile.exists())
                systemPromptFile.delete();
            if (userPromptFile.exists())
                userPromptFile.delete();
            if (assistantPromptFile.exists())
                assistantPromptFile.delete();
        }
    }

    private String _runBatchEmbeddingScript(List<LLM_InputContent> inputs) throws Exception {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        String systemPromptFilePath = Constants.PYTHON_SCRIPT_MODELS_DIR + File.separator + Constants.SYSTEM_PROMPT_FILE;
        File systemPromptFile = new File(systemPromptFilePath);
        try {
            FileUtils.fileWrite(new File(systemPromptFilePath), "UTF-8",
                    gson.toJson(new TransitPrompt(gson.toJson(
                            inputs.stream().map(LLM_InputContent::getSystem).collect(Collectors.toList())
                    )))
            );

            String[] terminalCommand = {"/bin/bash", "-c", generateBatchEmbeddingPythonModelRunnerCommand()};
            ProcessBuilder processBuilder = new ProcessBuilder(terminalCommand);
            processBuilder.directory(new File(Constants.PYTHON_SCRIPT_MODELS_DIR));
            processBuilder.environment().putAll(System.getenv());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null)
                response.append(line).append("\n");
            return response.toString().trim();
        } finally {
            if (systemPromptFile.exists())
                systemPromptFile.delete();
        }
    }

    public Pair<List<Double>, Double> calculateEmbeddings(String input) throws Exception {
        LLM_InputContent command = new LLM_InputContent(null, input, null);
        List<Double> list = new LinkedList<>();
        Pair<String, Double> cmdPair = runScript(command, LLM_Operation.EMBEDDING);
        list.addAll(new Gson().fromJson(cmdPair.getFirst(), List.class));
        return Pair.of(list, cmdPair.getSecond());
    }


    public Pair<List<List<Double>>, Double> calculateEmbeddingsForBatchInputs(List<LLM_InputContent> commands) throws Exception {
        List<List<Double>> list = new ArrayList<>();
        String response = runBatchEmbeddingScript(commands);
        list.addAll(new Gson().fromJson(response, List.class));

        double cost = 0;
        for (LLM_InputContent cmd : commands) {
            cost += Utils.computeCostOfTokens(cmd, null, LLM_Operation.EMBEDDING, null,
                    config.getEmbeddingsModel());
        }
        return Pair.of(list, cost);
    }

    public enum LLM_Operation {
        COMPLETION, EMBEDDING;
    }
}
