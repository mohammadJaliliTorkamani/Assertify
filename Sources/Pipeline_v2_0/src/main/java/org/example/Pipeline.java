package org.example;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

import static org.example.Constants.*;

public class Pipeline {
    private final int trial_number;
    private final String evalOutputFilePath;
    private Preprocessor preprocessor;
    private Parser parser;
    private InputGenerator inputGenerator;
    private CodeReplacer replacer;
    private Evaluator evaluator;
    private LLM_Client client;
    private Postprocessor postProcessor;
    private DataModulator dataModulator;

    public Pipeline(String evalOutputFilePath) {
        this(evalOutputFilePath, LLM_REQUEST_TRIAL_NUMBER);
    }

    public Pipeline(String evalOutputFilePath, int trial_number) {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
        this.evalOutputFilePath = evalOutputFilePath;
        this.trial_number = trial_number;
    }

    public Pipeline setPreprocessor(Preprocessor preprocessor) {
        this.preprocessor = preprocessor;
        return this;
    }

    public Pipeline setParser(Parser parser) {
        this.parser = parser;
        return this;
    }

    public Pipeline setInputGenerator(InputGenerator inputGenerator) {
        this.inputGenerator = inputGenerator;
        this.client = new LLM_Client(new LLM_Config(inputGenerator.getApiKey(),
                inputGenerator.getLLM_completionModel(),
                inputGenerator.getLLM_embeddingModel()), trial_number);
        return this;
    }

    public Pipeline setCodeReplacer(CodeReplacer replacer) {
        this.replacer = replacer;
        return this;
    }

    public Pipeline setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
        return this;
    }

    public Pipeline setPostprocessor(Postprocessor postprocessor) {
        this.postProcessor = postprocessor;
        return this;
    }

    public Pipeline setDataModulator(DataModulator dataModulator) {
        this.dataModulator = dataModulator;
        return this;
    }

    public void execute(boolean testPipeline, double totalProgressPercentage) throws Exception {
        execute(testPipeline, null, totalProgressPercentage);
    }

    public void execute(boolean testPipeline, Runnable<Response> runnable, double totalProgressPercentage) throws Exception {
        preInitialize();
        int numberOfStages = testPipeline ? 2 : 1;
        int step = 0;

        System.out.printf("%n========> stage %d of %d: %s%n", ++step, numberOfStages, "Evaluation");
        runPipeline(runnable, totalProgressPercentage);
        System.out.printf("%n~~~ Finished ~~~");
    }

    private void preInitialize() {
        System.out.println("Pre initializing...");
        //
        //Anything can be added here later
        REPO_JAVA_ADDRESS_MAP.clear();
        REPO_JAVA_ADDRESS_MAP.put("scenebuilder", JAVA_HOME_VERSION_23);
        REPO_JAVA_ADDRESS_MAP.put("elki", JAVA_HOME_VERSION_11);
        REPO_JAVA_ADDRESS_MAP.put("ratel-core", JAVA_HOME_VERSION_11);
        REPO_JAVA_ADDRESS_MAP.put("jbox2d", JAVA_HOME_VERSION_23);
        REPO_JAVA_ADDRESS_MAP.put("procyon", JAVA_HOME_VERSION_11);
        REPO_JAVA_ADDRESS_MAP.put("rocketmq", JAVA_HOME_VERSION_11);
        REPO_JAVA_ADDRESS_MAP.put("evosuite", JAVA_HOME_VERSION_21);
        REPO_JAVA_ADDRESS_MAP.put("flexmark-java", JAVA_HOME_VERSION_11);
        REPO_JAVA_ADDRESS_MAP.put("pmd", JAVA_HOME_VERSION_11);
        REPO_JAVA_ADDRESS_MAP.put("jruby", JAVA_HOME_VERSION_8);
        REPO_JAVA_ADDRESS_MAP.put("jmonkeyengine", JAVA_HOME_VERSION_8);
        REPO_JAVA_ADDRESS_MAP.put("docx4j", JAVA_HOME_VERSION_11);
        REPO_JAVA_ADDRESS_MAP.put("rapidoid", JAVA_HOME_VERSION_11);
        REPO_JAVA_ADDRESS_MAP.put("hive", JAVA_HOME_VERSION_8);
        REPO_JAVA_ADDRESS_MAP.put("cassandra-java-driver", JAVA_HOME_VERSION_8);
        REPO_JAVA_ADDRESS_MAP.put("tugraph-analytics", JAVA_HOME_VERSION_8);
        REPO_JAVA_ADDRESS_MAP.put("pravega", JAVA_HOME_VERSION_11);
        REPO_JAVA_ADDRESS_MAP.put("graphicsfuzz", JAVA_HOME_VERSION_11);
        REPO_JAVA_ADDRESS_MAP.put("pkl", JAVA_HOME_VERSION_17);
        REPO_JAVA_ADDRESS_MAP.put("runelite", JAVA_HOME_VERSION_17);
        REPO_JAVA_ADDRESS_MAP.put("spark", JAVA_HOME_VERSION_17);
        REPO_JAVA_ADDRESS_MAP.put("jeromq", JAVA_HOME_VERSION_17);
        REPO_JAVA_ADDRESS_MAP.put("unidbg", JAVA_HOME_VERSION_21);
        REPO_JAVA_ADDRESS_MAP.put("quasar", JAVA_HOME_VERSION_11);
        //
        System.out.println("Pipeline initialized successfully!");
    }

    private void runPipeline(Runnable<Response> runnable, double totalProgressPercentage) throws Exception {
        if (COMPUTE_DATASET_STATISTICS) {
            System.out.println("Computing Dataset Statistics...");
            DatasetStatistics datasetStatistics = dataModulator.computeDBStatistics();
            Utils.saveDatasetStatistics(datasetStatistics, DATASET_STATISTICS_OUTPUT_PATH);
            System.out.printf("%nStatistics were successfully computed and saved in '%s'%n", DATASET_STATISTICS_OUTPUT_PATH);
            System.exit(0);
        }

        double embeddingCost = dataModulator.populate(client, -1, -1);
        Response.resetCounter();
        for (InputRecord record : dataModulator.getEvalRecords()) {
            Response response = new Response();
            response.setRecord(record);
            response.setCeil(dataModulator.getEvalRecords().size());

            Pair<LLM_InputContent, Double> LLMCommandPair = null;
            Pair<String, Double> LLMRawResponsePair = null;
            Pair<String, Double> LLMFilteredawResponsePair = null;
            String LLMResponse;

            try {
                if (preprocessor.apply(record)) {
                    parser.initialize(record, true);
                    response.setParser(parser);
                    response.setGroundTruthAssertions(createLineNumberAssertionsPack(parser.getCommentLessAndJavadocLessMethodDeclaration(parser.getOriginalMethodDeclaration()).toString()));

                    long startTimer = System.currentTimeMillis();
                    LLMCommandPair = createPrompt(record);
                    LLMRawResponsePair = client.askModel(LLMCommandPair.getFirst());
                    long endTimer = System.currentTimeMillis();
                    response.setLLMCommand(LLMCommandPair.getFirst());
                    response.setPromptCreationTime(endTimer - startTimer);
                    response.setPromptCreationCost(LLMCommandPair.getSecond() + LLMRawResponsePair.getSecond() + embeddingCost);

                    response.setLLMRawResponse(LLMRawResponsePair.getFirst());
                    LLMFilteredawResponsePair = filterInvalidAssertions(LLMRawResponsePair, parser.getPrunedMethod());
                    response.setRawFilteredResponse(LLMFilteredawResponsePair.getFirst());

                    if (LLMFilteredawResponsePair.getFirst() == null) {
                        System.out.println("No valid filtered assertions were found!");
                        response.setPostErrorMessage(PARSING_ERROR_MESSAGE);
                        response.setPostLog("No valid filtered assertions were found!");
                    } else {
                        Pair<String, Exception> postProcessedResponse = postProcessor.apply(parser.getPrunedMethod(),
                                LLMRawResponsePair.getFirst(), parser.getOriginalMethodDeclaration().getAnnotations(),
                                parser.getOriginalMethodDeclaration().getJavadocComment().isPresent() ? parser.getOriginalMethodDeclaration().getJavadocComment().get() : null);

                        if (postProcessedResponse.getSecond() == null) {
                            LLMResponse = postProcessedResponse.getFirst();
                            response.setAugmentedMethod(LLMResponse);

                            ComponentResponse replaceResponse = replacer.replace(record.getClassName(), record.getAlternativePath(), LLMResponse);
                            if (replaceResponse.isOK()) {
                                System.out.println("---> Code replacement done");
                                MethodDeclaration methodDeclaration = StaticJavaParser.parseMethodDeclaration(LLMResponse);

                                EvaluationResult evaluationResult = evaluator.postEvaluate(response, record, parser, methodDeclaration, Utils.getCompatibleJavaVersionOf(record.getRepoName().split("@")[1]));
                                response.setPostErrorMessage(evaluationResult.getErrorMessage());
                                response.setPostLog(evaluationResult.getLog());
                                response.setPostEvaluationResult(evaluationResult);

                            } else {
                                System.out.println("Code replacement failed");
                                response.setPostErrorMessage(PARSING_ERROR_MESSAGE);
                                response.setPostLog(replaceResponse.getMessage());
                            }
                        } else {
                            response.setPostEvaluationResult(null);
                            response.setPostErrorMessage(PARSING_ERROR_MESSAGE);
                            response.setPostLog(postProcessedResponse.getSecond().getMessage());
                        }
                    }
                } else {
                    response.setPostErrorMessage(String.format("Preprocess failed for record '%s'", record));
                }

                Utils.addKeyValuePairsToJsonFile("EvaluationOutputEntity",
                        response.getAsMap(),
                        preprocessor.getRecordsDirPath(), evalOutputFilePath);

                if (response.getPostErrorMessage() != null)
                    System.err.printf("Failed! %s%n", response.getPostErrorMessage());
                else
                    System.out.printf("Done!%n");

                if (runnable != null)
                    runnable.run(response);
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Error in pipeline iteration. skipping the current case in loop");
                response.setLLMCommand(LLMCommandPair != null ? LLMCommandPair.getFirst() : null);
                response.setLLMRawResponse(LLMFilteredawResponsePair != null ? LLMFilteredawResponsePair.getFirst() : null);
                response.setPostErrorMessage(Constants.PIPELINE_ERROR_MESSAGE);
                response.setPostLog(e.getMessage());
                Utils.addKeyValuePairsToJsonFile("EvaluationOutputEntity",
                        response.getAsMap(), preprocessor.getRecordsDirPath(), evalOutputFilePath);

                e.printStackTrace();
            }


            System.out.println("Removing alternative path...");
            try {
                Utils.deleteDirectory(record.getRepoPath(true));
                System.out.println("Garbage Collection....");
                System.gc();
                System.out.println("Local Progress: " + (1.0 * response.getIndex()) / dataModulator.getEvalRecords().size() * 100 + "%");
                System.out.println("Total Progress: " + (totalProgressPercentage * 100.0 + "%"));
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        try {
            Utils.addStatistics(EvaluationOutputEntity.class, client.getConfig(), preprocessor.getRecordsDirPath(), evalOutputFilePath);
        }catch (Exception e){
            System.err.println("Error while adding statistics.");
            e.printStackTrace();
        }
    }

    private String createLineNumberAssertionsPack(String originalMethod) {
        List<Pair<Integer, String>> lineAssertionPack = InputGenerator.extractAssertionsWithLineNumbers(originalMethod);
        StringBuilder pack = new StringBuilder();
        for (Pair<Integer, String> pair : lineAssertionPack) {
            pack
                    .append(Constants.LLM_ASSISTANT_DELIMITER[0])
                    .append("(")
                    .append(pair.getFirst())
                    .append(", ")
                    .append(pair.getSecond())
                    .append(")")
                    .append(Constants.LLM_ASSISTANT_DELIMITER[1])
                    .append("\n");
        }
        return pack.toString();
    }

    private Pair<String, Double> filterInvalidAssertions(Pair<String, Double> llmResponsePair, String prunedMethod) {
        int maxStatementLineNumber = InputGenerator.getLastStatementLineNumber(prunedMethod);
        int minStatementLineNumber = InputGenerator.getFirstStatementLineNumber(prunedMethod);
        Map<Integer, List<String>> assertionsMap = Postprocessor.parseAssertions(llmResponsePair.getFirst());
        StringBuilder newResponsePairStr = new StringBuilder();
        for (Map.Entry<Integer, List<String>> entry : assertionsMap.entrySet()) {
            if (entry.getKey() <= maxStatementLineNumber + 1 && entry.getKey() > minStatementLineNumber) {
                for (String assertion : entry.getValue())
                    newResponsePairStr
                            .append(Constants.LLM_ASSISTANT_DELIMITER[0])
                            .append("(")
                            .append(entry.getKey())
                            .append(", ")
                            .append(assertion)
                            .append(")")
                            .append(Constants.LLM_ASSISTANT_DELIMITER[1])
                            .append("\n");

            }
        }

        llmResponsePair.setFirst(newResponsePairStr.toString().isBlank() ? null : newResponsePairStr.toString().strip());
        return llmResponsePair;
    }

    private Pair<Pair<String, Double>, Pair<Long, Double>> extractResponse(InputRecord record) throws Exception {
        if (!new File(PIPELINE_LLM_PROMPT_AND_RESPONSE_READ_OFFLINE_OUTPUT_PATH).exists())
            throw new Exception("Record file does not exist to read offline");

        try (BufferedReader reader = new BufferedReader(new FileReader(PIPELINE_LLM_PROMPT_AND_RESPONSE_READ_OFFLINE_OUTPUT_PATH))) {
            EvaluationOutputEntity entity = new Gson().fromJson(reader, EvaluationOutputEntity.class);
            for (Map<String, Object> item : entity.getItems()) {
                if (item.get("method").toString().equals(record.getName()) &&
                        item.get("repository_name").toString().equals(record.getRepoName()) &&
                        item.get("method_original_path").toString().equals(record.getPath()) &&
                        item.get("repository_original_path").toString().equals(record.getRepoPath(false)) &&
                        StaticJavaParser.parseMethodDeclaration(item.get("original_method").toString()).toString().equals(StaticJavaParser.parseMethodDeclaration(parser.getOriginalMethod()).toString()) &&
                        StaticJavaParser.parseMethodDeclaration(item.get("pruned_method").toString()).toString().equals(StaticJavaParser.parseMethodDeclaration(parser.getPrunedMethod()).toString())
                ) {
                    String LLMRawResponse = null;
                    double doubleTime = -1;
                    long longTime = -1;
                    double cost = -1;
                    if (item.get("LLM_raw_response") != null)
                        LLMRawResponse = item.get("LLM_raw_response").toString();
                    if (item.get("prompt_creation_time") != null)
                        doubleTime = (double) item.get("prompt_creation_time");
                    if (item.get("prompt_creation_time") != null)
                        longTime = (long) doubleTime;
                    if (item.get("prompt_creation_cost") != null)
                        cost = (double) item.get("prompt_creation_cost");

                    return Pair.of(Pair.of(LLMRawResponse, -1d), Pair.of(longTime, cost));
                }
            }
        }
        throw new Exception("offline Record not found in the associated file");
    }

    private Pair<LLM_InputContent, Double> createPrompt(InputRecord record, boolean readOffline) throws Exception {
        if (!readOffline)
            return createPrompt(record);
        else {
            if (!new File(PIPELINE_LLM_PROMPT_AND_RESPONSE_READ_OFFLINE_OUTPUT_PATH).exists())
                throw new Exception("Record file does not exist to read offline");

            try (BufferedReader reader = new BufferedReader(new FileReader(PIPELINE_LLM_PROMPT_AND_RESPONSE_READ_OFFLINE_OUTPUT_PATH))) {
                EvaluationOutputEntity entity = new Gson().fromJson(reader, EvaluationOutputEntity.class);
                for (Map<String, Object> item : entity.getItems()) {
                    if (item.get("method").toString().equals(record.getName()) &&
                            item.get("repository_name").toString().equals(record.getRepoName()) &&
                            item.get("method_original_path").toString().equals(record.getPath()) &&
                            item.get("repository_original_path").toString().equals(record.getRepoPath(false)) &&
                            StaticJavaParser.parseMethodDeclaration(item.get("original_method").toString()).toString().equals(StaticJavaParser.parseMethodDeclaration(parser.getOriginalMethod()).toString()) &&
                            StaticJavaParser.parseMethodDeclaration(item.get("pruned_method").toString()).toString().equals(StaticJavaParser.parseMethodDeclaration(parser.getPrunedMethod()).toString())
                    ) {

                        List<String> users = new LinkedList<>();
                        List<String> assistants = new LinkedList<>();
                        String system = null;
                        LinkedTreeMap ltm = (LinkedTreeMap) item.get("LLM_command");
                        if (ltm != null) {
                            if (ltm.get("user") != null) {
                                ArrayList<Object> usersArrayList = (ArrayList) ltm.get("user");
                                for (Object strObject : usersArrayList)
                                    users.add(strObject.toString());
                            }
                            if (ltm.get("assistant") != null) {
                                ArrayList<Object> assistantsArrayList = (ArrayList) ltm.get("assistant");
                                for (Object strObject : assistantsArrayList)
                                    assistants.add(strObject.toString());
                            }
                            if (ltm.get("system") != null)
                                system = ltm.get("system").toString();

                        }

                        return Pair.of(new LLM_InputContent(users, system, assistants), -1d);
                    }
                }
            }
            throw new Exception("offline Record not found in the associated file");
        }
    }


    private Pair<LLM_InputContent, Double> createPrompt(InputRecord record) throws Exception {
        System.out.printf("---> Creating prompt (%s)....%n", client.getConfig().getCompletionModel().getModelName());
        double totalCost = 0;
        String functionDescription = null;
        if (!inputGenerator.getExperiment().equals(InputGenerator.Experiments.A)) {
            System.out.println("     Function Description (using LLM)");
            Pair<String, Double> functionDescriptionPair = client.askModel(inputGenerator.generateMethodDescriberCommand(parser.getPrunedMethod()));
            functionDescription = functionDescriptionPair.getFirst();
            totalCost += functionDescriptionPair.getSecond();
        }

        String inputOutputDescription = null;
        if (!inputGenerator.getExperiment().equals(InputGenerator.Experiments.A) &&
                !inputGenerator.getExperiment().equals(InputGenerator.Experiments.B)) {
            System.out.println("     Input Output Description (using SPA)");
            inputOutputDescription = InputGenerator.describeInputOutputDescription(StaticJavaParser.parseMethodDeclaration(parser.getOriginalMethod()));
        }

        Map<String, String> dependenciesDescriptionMap = null;
        if (inputGenerator.getExperiment().equals(InputGenerator.Experiments.D) ||
                inputGenerator.getExperiment().equals(InputGenerator.Experiments.FINALIZED_MODE)) {
            System.out.println("     Invoked Dependencies' Description (using SPA+LLM)");
            dependenciesDescriptionMap = new HashMap<>();

            Map<MethodCallExpr, MethodDeclaration> invokedDependencySourceCodes = parser.extractInvokedSourceCodes(record);
            for (Map.Entry<MethodCallExpr, MethodDeclaration> entry : invokedDependencySourceCodes.entrySet()) {
                MethodCallExpr methodCallExpr = entry.getKey();
                MethodDeclaration methodDeclaration = entry.getValue();
                if (methodDeclaration != null) { //yet we want to just generate descriptions for the methods with declaration (if we wanted to generate description for all the invoked methods, then no need to have "if")
                    Pair<String, Double> dependencyResponsePair = client.askModel(inputGenerator.generateMethodDescriberCommand(methodDeclaration.toString()));
                    dependenciesDescriptionMap.put(methodDeclaration.getDeclarationAsString(), dependencyResponsePair.getFirst()); //passing the method declaration and its body to the LLM and storing the result in map of callExpr and received description
                    totalCost += dependencyResponsePair.getSecond();
                } else
                    dependenciesDescriptionMap.put(methodCallExpr.toString(), null);
            }
        }

        String originalMethod = new Extractor(record.getAlternativePath()).printMethod(parser.getOriginalMethodDeclaration(), true, false, true);
        Pair<LLM_InputContent, Double> tempResponse = inputGenerator.generateInputCommand(originalMethod,
                new Extractor(record.getAlternativePath()).printMethod(parser.getOriginalMethodDeclaration(), true, true, true),
                functionDescription,
                inputOutputDescription,
                dependenciesDescriptionMap,
                parser, client, dataModulator.getFslRecords()
        );
        tempResponse.setSecond(tempResponse.getSecond() + totalCost);
        return tempResponse;
    }
}
