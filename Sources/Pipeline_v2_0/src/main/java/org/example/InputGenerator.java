package org.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.Type;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.example.Constants.*;

public class InputGenerator {
    private final String apiKey;
    private final Model LLM_completionModel;
    private final Model LLM_embeddingModel;
    private final Experiments experiment;

    public InputGenerator(String apiKey, Model LLM_completionModel,
                          Model LLM_embeddingModel, Experiments experiment) {
        this.apiKey = apiKey;
        this.LLM_completionModel = LLM_completionModel;
        this.LLM_embeddingModel = LLM_embeddingModel;
        this.experiment = experiment;
    }

    public static String describeInputOutputDescription(MethodDeclaration methodDeclaration) {
        Type returnType = methodDeclaration.getType();
        String returnTypeString = returnType.toString();

        NodeList<Parameter> parameters = methodDeclaration.getParameters();
        List<String> parameterList = new ArrayList<>();
        for (Parameter parameter : parameters) {
            Type parameterType = parameter.getType();
            String parameterName = parameter.getNameAsString();
            String parameterString = parameterType + " " + parameterName;
            parameterList.add(parameterString);
        }

        return String.format("The method returns %s and takes %s as its argument%s",
                returnTypeString,
                parameterList.isEmpty() ? "nothing" : String.format("(%s)", String.join(",",
                        parameterList)), parameterList.size() > 1 ? "s" : "");
    }

    private static String addLineNumbersToMethod(String method) {
        String withCurlyBracesMethod = Postprocessor.addCurlyBraces(method);
        String[] lines = withCurlyBracesMethod.split("\n");
        StringBuilder numberedMethod = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            numberedMethod.append(i + 1).append(". ").append(lines[i]).append("\n");
        }
        return numberedMethod.toString();
    }

    public static int getLastStatementLineNumber(String method){
        String withCurlyBracesMethod = Postprocessor.addCurlyBraces(method);
        String[] lines = withCurlyBracesMethod.split("\n");
        return lines.length-1;
    }

    public static String extractAssertionsLineNumbers(String originalMethod) {
        String originalMethodWithCurlyBraces = Postprocessor.addCurlyBraces(originalMethod);

        Set<Integer> lineNumbers = new HashSet<>();
        String[] lines = originalMethodWithCurlyBraces.split("\n");
        Pattern assertionPattern = Pattern.compile("\\s*assert\\s+.*;");
        int lineNumberCounter = 1;

        // Create an array to store line numbers
        int[] assignedLineNumbers = new int[lines.length];

        // First pass: Assign line numbers to non-assert lines
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = assertionPattern.matcher(lines[i]);
            if (!matcher.find()) {
                assignedLineNumbers[i] = lineNumberCounter++;
            }
        }

        // Second pass: Assign line numbers to assert lines
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = assertionPattern.matcher(lines[i]);
            if (matcher.find()) {
                // Find the next non-zero line number
                int nextLineNumber = 0;
                for (int j = i; j < lines.length; j++) {
                    if (assignedLineNumbers[j] != 0) {
                        nextLineNumber = assignedLineNumbers[j];
                        break;
                    }
                }
                lineNumbers.add(nextLineNumber);
            }
        }

        // Convert the list of line numbers to a comma-separated string
        return String.join(",", lineNumbers.stream().map(String::valueOf).toArray(String[]::new));
    }

    public static List<Pair<Integer,String>> extractAssertionsWithLineNumbers_NonConsecutive(String originalMethod) {
        List<Pair<Integer, String>> assertionsListWithNumber = new ArrayList<>();

        String originalMethodWithCurlyBraces = Postprocessor.addCurlyBraces(originalMethod);

        String[] lines = originalMethodWithCurlyBraces.split("\n");
        Pattern assertionPattern = Pattern.compile("\\s*assert\\s+.*;");

        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = assertionPattern.matcher(lines[i]);
            if (matcher.find())
                assertionsListWithNumber.add(Pair.of(i+1, lines[i]));
        }

        return assertionsListWithNumber;
    }

    /**
     * Note: This method considers the same line number for consecutive assertions
     * @param originalMethod
     * @return
     */
    public static List<Pair<Integer,String>> extractAssertionsWithLineNumbers(String originalMethod) {
        List<Pair<Integer, String>> assertionsListWithNumber = new ArrayList<>();

        String originalMethodWithCurlyBraces = Postprocessor.addCurlyBraces(originalMethod);

        String[] lines = originalMethodWithCurlyBraces.split("\n");
        Pattern assertionPattern = Pattern.compile("\\s*assert\\s+.*;");
        int lineNumberCounter = 1;

        // Create an array to store line numbers
        int[] assignedLineNumbers = new int[lines.length];

        // First pass: Assign line numbers to non-assert lines
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = assertionPattern.matcher(lines[i]);
            if (!matcher.find()) {
                assignedLineNumbers[i] = lineNumberCounter++;
            }
        }

        // Second pass: Assign line numbers to assert lines
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = assertionPattern.matcher(lines[i]);
            if (matcher.find()) {
                // Find the next non-zero line number
                int nextLineNumber = 0;
                for (int j = i; j < lines.length; j++) {
                    if (assignedLineNumbers[j] != 0) {
                        nextLineNumber = assignedLineNumbers[j];
                        break;
                    }
                }
                assertionsListWithNumber.add(Pair.of(nextLineNumber, lines[i]));
            }
        }

        // Convert the list of line numbers to a comma-separated string
        return assertionsListWithNumber;
    }

    public static int getFirstStatementLineNumber(String method) {
        String withCurlyBracesMethod = Postprocessor.addCurlyBraces(method);
        String[] lines = withCurlyBracesMethod.split("\n");
        for (int i = 0; i < lines.length; i++) {
            for(int j = 0; j < lines[i].length(); j++) {
                if(lines[i].charAt(j) == '{')
                    return i+1;
            }
        }
        return -1;
    }

    public String getApiKey() {
        return apiKey;
    }

    public Model getLLM_completionModel() {
        return LLM_completionModel;
    }

    public Model getLLM_embeddingModel() {
        return LLM_embeddingModel;
    }

    public Pair<LLM_InputContent, Double> generateInputCommand(String originalMethod, String withoutAssertionMethod, String functionDescription, String inputOutputDescription,
                                                               Map<String, String> dependenciesDescriptionMap,
                                                               Parser parser, LLM_Client client, List<FSLRecord> fslRecords) throws Exception {
        StringBuilder invokedMethodsDescriptionStr = null;
        if (!experiment.equals(Experiments.A) && !experiment.equals(Experiments.B) && !experiment.equals(Experiments.C)) {
            if (dependenciesDescriptionMap != null) {
                invokedMethodsDescriptionStr = new StringBuilder();
                for (Map.Entry<String, String> entry : dependenciesDescriptionMap.entrySet()) {
                    if (entry.getValue() != null)
                        invokedMethodsDescriptionStr.append(" - ").append("' ").append(entry.getKey()).append(" '").append(" method: ")
                                .append(entry.getValue()).append(entry.getValue().endsWith(".") ? " " : ". ").append("\n");
                }
            }
        }
        return generateInputCommand(originalMethod, withoutAssertionMethod, functionDescription, inputOutputDescription, invokedMethodsDescriptionStr,
                parser, client, fslRecords);
    }

    public Experiments getExperiment() {
        return experiment;
    }

    private Pair<LLM_InputContent, Double> generateInputCommand(String originalMethod, String withoutAssertionMethod, String functionDescription,
                                                                String inputOutputDescription, StringBuilder invokedMethodsDescriptionStr,
                                                                Parser parser, LLM_Client client, List<FSLRecord> fslRecords) throws Exception {
        MethodDeclaration methodDeclaration = StaticJavaParser.parseMethodDeclaration(withoutAssertionMethod);
        switch (experiment) {
            case A:
                return generateCommand_Experiment_A(originalMethod, withoutAssertionMethod, methodDeclaration);
            case B:
                return generateCommand_Experiment_B(originalMethod, withoutAssertionMethod, methodDeclaration, functionDescription);
            case C:
                return generateCommand_Experiment_C(originalMethod, withoutAssertionMethod, methodDeclaration, functionDescription, inputOutputDescription);
            case D:
                return generateCommand_Experiment_D(originalMethod, withoutAssertionMethod, methodDeclaration, functionDescription, inputOutputDescription,
                        invokedMethodsDescriptionStr);
            default:
                return generateCommand_Experiment_Finalized(originalMethod, withoutAssertionMethod, methodDeclaration, functionDescription, inputOutputDescription,
                        invokedMethodsDescriptionStr, parser, client, fslRecords);
        }
    }

    private Pair<LLM_InputContent, Double> generateCommand_Experiment_A(String originalMethod, String withoutAssertionMethod, MethodDeclaration methodDeclaration) {
        List<String> usersSet = new LinkedList<>();
        usersSet.add(String.format("The method for which you will generate assertions has the following characteristic(s):%n" +
                        "* Name: \"%s\",%n" +
                        "* Signature: \"%s\"%n" +
                        "* Method declaration: %n%n%s%n%s%s",
                methodDeclaration.getNameAsString(),
                methodDeclaration.getDeclarationAsString(),
                LLM_USER_INPUT_METHOD_DELIMITER,
                addLineNumbersToMethod(Postprocessor.addCurlyBraces(withoutAssertionMethod)),
                LLM_USER_INPUT_METHOD_DELIMITER));
        return Pair.of(new LLM_InputContent(usersSet, LLM_SYSTEM_MESSAGE), 0d);
    }

    private Pair<LLM_InputContent, Double> generateCommand_Experiment_B(String originalMethod, String withoutAssertionMethod, MethodDeclaration methodDeclaration, String functionDescription) {
        List<String> usersSet = new LinkedList<>();
        usersSet.add(String.format("The method for which you will generate assertions has the following characteristic(s):%n" +
                        "* Name: \"%s\",%n" +
                        "* Signature: \"%s\"%n" +
                        "* Purpose: \"%s\"%n" +
                        "* Method declaration: %n%n%s%n%s%s",
                methodDeclaration.getNameAsString(),
                methodDeclaration.getDeclarationAsString(),
                functionDescription,
                LLM_USER_INPUT_METHOD_DELIMITER,
                addLineNumbersToMethod(Postprocessor.addCurlyBraces(withoutAssertionMethod)),
                LLM_USER_INPUT_METHOD_DELIMITER));
        return Pair.of(new LLM_InputContent(usersSet, LLM_SYSTEM_MESSAGE), 0d);
    }

    private Pair<LLM_InputContent, Double> generateCommand_Experiment_C(String originalMethod, String withoutAssertionMethod, MethodDeclaration methodDeclaration, String functionDescription, String inputOutputDescription) {
        List<String> usersSet = new LinkedList<>();
        usersSet.add(String.format("The method for which you will generate assertions has the following characteristic(s):%n" +
                        "* Name: \"%s\",%n" +
                        "* Signature: \"%s\"%n" +
                        "* Purpose: \"%s\"%n" +
                        "* Input and output: %s%n" +
                        "* Method declaration: %n%n%s%n%s%s",
                methodDeclaration.getNameAsString(),
                methodDeclaration.getDeclarationAsString(),
                functionDescription,
                inputOutputDescription,
                LLM_USER_INPUT_METHOD_DELIMITER,
                addLineNumbersToMethod(Postprocessor.addCurlyBraces(withoutAssertionMethod)),
                LLM_USER_INPUT_METHOD_DELIMITER));
        return Pair.of(new LLM_InputContent(usersSet, LLM_SYSTEM_MESSAGE), 0d);
    }

    private Pair<LLM_InputContent, Double> generateCommand_Experiment_D(String originalMethod, String withoutAssertionMethod, MethodDeclaration methodDeclaration, String functionDescription, String inputOutputDescription, StringBuilder invokedMethodsDescriptionStr) {
        List<String> usersSet = new LinkedList<>();
        usersSet.add(String.format("The method for which you will generate assertions has the following characteristic(s):%n" +
                        "* Name: \"%s\",%n" +
                        "* Signature: \"%s\"%n" +
                        "* Purpose: \"%s\"%n" +
                        "* Input and output: %s%n" +
                        "* External dependencies: %n%s%n" +
                        "* Method declaration: %n%n%s%n%s%s",
                methodDeclaration.getNameAsString(),
                methodDeclaration.getDeclarationAsString(),
                functionDescription,
                inputOutputDescription,
                invokedMethodsDescriptionStr.toString().isBlank() ? "The method does not have any external dependencies." : invokedMethodsDescriptionStr.toString(),
                LLM_USER_INPUT_METHOD_DELIMITER,
                addLineNumbersToMethod(Postprocessor.addCurlyBraces(withoutAssertionMethod)),
                LLM_USER_INPUT_METHOD_DELIMITER));
        return Pair.of(new LLM_InputContent(usersSet, LLM_SYSTEM_MESSAGE), 0d);
    }

    private Pair<LLM_InputContent, Double> generateCommand_Experiment_Finalized(String originalMethod, String withoutAssertionMethod, MethodDeclaration methodDeclaration, String functionDescription,
                                                                                String inputOutputDescription,
                                                                                StringBuilder invokedMethodsDescriptionStr,
                                                                                Parser parser, LLM_Client client, List<FSLRecord> fslRecords) throws Exception {

        int degree = MAX_NUMBER_OF_SIMILAR_MINED_EXAMPLES;
        String system = LLM_SYSTEM_MESSAGE;
        Map<String, String> userAssistantPairs = new LinkedHashMap<>();
        double totalEmbeddingCost = 0;
        while (degree >= 0) {
            userAssistantPairs.clear();

            System.out.println("     Similar Examples (using Cosine)");
            Pair<Map<String, String>, Double> similarMethodsPair = EmbeddingsMiner.mineSimilarMethods(client, parser, degree,
                    parser.getCommentLessAndJavadocLessMethodDeclaration(StaticJavaParser.parseMethodDeclaration(parser.getPrunedMethod())), fslRecords,
                    MINIMUM_ACCEPTABLE_COSINE_VALUE);
            totalEmbeddingCost += similarMethodsPair.getSecond();
            if (!similarMethodsPair.getFirst().isEmpty()) {
                for (Map.Entry<String, String> entry : similarMethodsPair.getFirst().entrySet()) {
                    String methodWithoutAssertion = entry.getKey();
                    String methodWithAssertion = entry.getValue();
//                    String delimitedMethodWithoutAssertion = LLM_USER_INPUT_METHOD_DELIMITER + "\n" + methodWithoutAssertion + "\n" + LLM_USER_INPUT_METHOD_DELIMITER;
//                    String delimitedMethodWithAssertion = LLM_ASSISTANT_DELIMITER[0] + "\n" + methodWithAssertion + "\n" + LLM_ASSISTANT_DELIMITER[1];

                    String userItem = addLineNumbersToMethod(methodWithoutAssertion);
                    List<Pair<Integer,String>> assistantItemsWithLines = extractAssertionsWithLineNumbers(methodWithAssertion);
                    StringBuilder assistantItem = new StringBuilder();
                    for(Pair<Integer,String> pair : assistantItemsWithLines) {
                        assistantItem.append(String.format("%s(%d, %s)%s", LLM_ASSISTANT_DELIMITER[0], pair.getFirst(), pair.getSecond().trim(), LLM_ASSISTANT_DELIMITER[1]));
                    }

                    userAssistantPairs.put(userItem, assistantItem.toString().trim());
                }
            }

            Pair<LLM_InputContent, Double> lastPair = generateCommand_Experiment_D(originalMethod, Postprocessor.addCurlyBraces(withoutAssertionMethod), methodDeclaration, functionDescription, inputOutputDescription, invokedMethodsDescriptionStr);
            userAssistantPairs.put(lastPair.getFirst().getUser().get(0), null);

            int systemLength = Utils.computeNumberOfTokens(system);
            int userLength = 0;
            int assistantLength = 0;
            for (Map.Entry<String, String> entry : userAssistantPairs.entrySet()) {
                userLength += Utils.computeNumberOfTokens(entry.getKey());
                if (entry.getValue() != null)
                    assistantLength += Utils.computeNumberOfTokens(entry.getValue());
            }

            int totalLength = systemLength + userLength + assistantLength;
            System.out.printf("     FSL-Degree: %d  |  (Total,System,User,Assistant): (%d,%d,%d,%d)%n",
                    degree, totalLength, systemLength, userLength, assistantLength);

            if (totalLength < Utils.getMaxTPMLengthOfModel(client.getConfig().getCompletionModel())) {
                List<String> usersSet = new LinkedList<>(userAssistantPairs.keySet());
                List<String> assistantSet = getTreeSetWithoutNotNullElements(userAssistantPairs.values());

                return Pair.of(new LLM_InputContent(usersSet, system, assistantSet), totalEmbeddingCost);
            }
            degree--;
        }

        Pair<LLM_InputContent, Double> otherwiseInputPrompt = generateCommand_Experiment_D(originalMethod, Postprocessor.addCurlyBraces(withoutAssertionMethod), methodDeclaration,
                functionDescription, inputOutputDescription, invokedMethodsDescriptionStr);
        return Pair.of(otherwiseInputPrompt.getFirst(), totalEmbeddingCost + otherwiseInputPrompt.getSecond());
    }

    private List<String> getTreeSetWithoutNotNullElements(Collection<String> values) {
        List<String> set = new LinkedList<>();
        for (String str : values)
            if (str != null)
                set.add(str);
        return set;
    }

    public LLM_InputContent generateMethodDescriberCommand(String originalMethod) {
        List<String> user = new LinkedList<>();
        user.add("Summarize what this java method is doing by describing the objective preferably in one paragraph:\n\n" + originalMethod);
        return new LLM_InputContent(user, null);
    }

    public enum Experiments {
        A, B, C, D, FINALIZED_MODE
    }

    public enum Model {
        //OPENAI
        GPT_4O("openai_script.py", "gpt-4o", MODEL_GPT4O_INPUT_PRICE_PER_K, MODEL_GPT4O_OUTPUT_PRICE_PER_K, 0),
        GPT_4("openai_script.py", "gpt-4", MODEL_GPT4_8K_COMPLETION_INPUT_PRICE_PER_K, MODEL_GPT4_8K_COMPLETION_OUTPUT_PRICE_PER_K, 0),
        GPT_4_0613("openai_script.py", "gpt-4-0613", MODEL_GPT4_8K_COMPLETION_INPUT_PRICE_PER_K, MODEL_GPT4_8K_COMPLETION_OUTPUT_PRICE_PER_K, 0),
        GPT_4_32K("openai_script.py", "gpt-4-32k", MODEL_GPT4_32K_COMPLETION_INPUT_PRICE_PER_K, MODEL_GPT4_32K_COMPLETION_OUTPUT_PRICE_PER_K, 0),
        GPT_4_32K_0613("openai_script.py", "gpt-4-32k-0613", MODEL_GPT4_32K_COMPLETION_INPUT_PRICE_PER_K, MODEL_GPT4_32K_COMPLETION_OUTPUT_PRICE_PER_K, 0),
        GPT_3_5_TURBO_0613("openai_script.py", "gpt-3.5-turbo-0613", MODEL_GPT3_5_COMPLETION_INPUT_PRICE_PER_K, MODEL_GPT3_5_COMPLETION_OUTPUT_PRICE_PER_K, 0),
        GPT_3_5_TURBO_16K("openai_script.py", "gpt-3.5-turbo-16k", MODEL_GPT3_5_16K_COMPLETION_INPUT_PRICE_PER_K, MODEL_GPT3_5_16K_COMPLETION_OUTPUT_PRICE_PER_K, 0),
        GPT_3_5_TURBO_16K_0613("openai_script.py", "gpt-3.5-turbo-16k-0613", MODEL_GPT3_5_16K_COMPLETION_INPUT_PRICE_PER_K, MODEL_GPT3_5_16K_COMPLETION_OUTPUT_PRICE_PER_K, 0),
        TEXT_EMBEDDINGS_3_SMALL("openai_script.py", "text-embedding-3-small", 0, 0, TEXT_EMBEDDINGS_3_SMALL_PRICE_PER_K),
        //Microsoft
        PHI("microsoft_script.py", "microsoft/phi-1_5", 0, 0, 0),
        PHI_EMBEDDINGS("microsoft_script.py", "microsoft/phi-1_5", 0, 0, 0);
        private final String fileName;
        private final String modelName;
        private final double completionInputCost;
        private final double completionOutputCost;
        private final double embeddingCost;

        Model(String value, String modelName, double completionInputCost, double completionOutputCost, double embeddingCost) {
            this.fileName = value;
            this.modelName = modelName;
            this.completionInputCost = completionInputCost;
            this.completionOutputCost = completionOutputCost;
            this.embeddingCost = embeddingCost;
        }

        public String getFileName() {
            return fileName;
        }

        public String getModelName() {
            return modelName;
        }

        public double getCompletionInputCost() {
            return completionInputCost;
        }

        public double getCompletionOutputCost() {
            return completionOutputCost;
        }

        public double getEmbeddingCost() {
            return embeddingCost;
        }

        @Override
        public String toString() {
            return String.format("File name: %s , Model name: %s , Model completion input price (perK): %f , Model completion output price (perK): %f , Model embedding price (perK): %f", fileName, modelName, completionInputCost, completionOutputCost, embeddingCost);
        }
    }
}
