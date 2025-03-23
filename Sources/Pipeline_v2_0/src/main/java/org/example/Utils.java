package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.opencsv.CSVWriter;
import org.codehaus.plexus.util.FileUtils;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.example.Constants.*;

public class Utils {
    public static String backupRepository(InputRecord record, String repoPath) {
        System.out.printf("\n################ REPOSITORY (%s - %s) FETCH ################%n", record.getRepoName(), record.getName());
        File sourceDirectory = new File(record.getRepoPath(false));
        if (!new File(repoPath).exists()) {
            if (createDirIfNotExists(repoPath)) {
                System.out.println("---> Back up in progress...");
                try {
                    copyDirectory(sourceDirectory, new File(repoPath));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return record.getRelativeFilePath();
    }

    public static void copyDirectory(File sourceDir, File destDir) throws IOException {
        // Create the destination directory if it doesn't exist
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        // Iterate over all files and directories in the source directory
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                File destFile = new File(destDir, file.getName());
                if (file.isDirectory()) {
                    // Recursively copy subdirectories
                    copyDirectory(file, destFile);
                } else {
                    // Copy individual files
                    Path sourcePath = file.toPath();
                    Path destPath = destFile.toPath();
                    try {
                        Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception e) {
                        System.err.printf("Access denied while copying %s to %s .%n", sourcePath.getFileName(), destPath);
                    }
                }
            }
        }
    }

    public static boolean createDirIfNotExists(String dirName) {
        File file = new File(dirName);
        if (!file.exists()) {
            return file.mkdir();
        }
        return true;
    }

    public static String getDateTime(String format) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return now.format(formatter);
    }

    public static void addKeyValuePairsToJsonFile(String className, LinkedHashMap<String, Object> keyValuePairs,
                                                  String dirPath, String fileName) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();
        String jsonPath = dirPath + File.separator + fileName;
        File file = new File(jsonPath);
        if (file.exists()) {
            String jsonContent = new String(Files.readAllBytes(Paths.get(jsonPath)));
            Type type = className.equals("EvaluationOutputEntity")
                    ? new TypeToken<EvaluationOutputEntity>() {
            }.getType()
                    : new TypeToken<TestOutputEntity>() {
            }.getType();

            OutputEntity outputEntity = gson.fromJson(jsonContent, type);
            outputEntity.getItems().add(keyValuePairs);
            FileWriter fileWriter = new FileWriter(jsonPath);
            gson.toJson(outputEntity, fileWriter);
            fileWriter.close();
        } else {
            List<Map<String, Object>> records = new ArrayList<>();
            records.add(keyValuePairs);
            OutputEntity outputEntity = className.equals("EvaluationOutputEntity") ? new EvaluationOutputEntity(records) : new TestOutputEntity(records);
            FileWriter fileWriter = new FileWriter(jsonPath);
            gson.toJson(outputEntity, fileWriter);
            fileWriter.close();
        }
    }

    public static void addStatistics(Class entityClass, LLM_Config llm_config, String dirPath, String fileName) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();

        String jsonPath = dirPath + File.separator + fileName;
        try (BufferedReader reader = new BufferedReader(new FileReader(jsonPath))) {
            OutputEntity entity = null;
            if (entityClass.getSimpleName().equals("EvaluationOutputEntity")) {
                entity = gson.fromJson(reader, EvaluationOutputEntity.class);
                List<Map<String, Object>> items = entity.getItems();
                ((EvaluationOutputEntity) entity).setRougeAverage(EvaluationOutputEntity.createRougeAverage(items));
                ((EvaluationOutputEntity) entity).adjustNumberOfParsingErrors();
            } else {
                entity = gson.fromJson(reader, TestOutputEntity.class);
                List<Map<String, Object>> items = entity.getItems();
                ((TestOutputEntity) entity).setRougeAverage(TestOutputEntity.createRougeAverage(items));
                ((TestOutputEntity) entity).adjustNumberOfParsingErrors();
            }
            entity.adjustTotalPromptCreationTime();
            entity.adjustTotalPromptCreationCost();
//            entity.adjustNumberOfPreCompilationErrors();
            entity.adjustNumberOfPostCompilationErrors();
//            entity.adjustNumberOfPreTests();
            entity.adjustNumberOfPostTests();
//            entity.adjustNumberOfPreFailedTests();
            entity.adjustNumberOfPostFailedTests();
//            entity.adjustNumberOfPreErrorTests();
            entity.adjustNumberOfPostErrorTests();
//            entity.adjustNumberOfPrePassedTests();
            entity.adjustNumberOfPostPassedTests();
            entity.adjustLlmConfig(llm_config);

            FileWriter fileWriter = new FileWriter(jsonPath);
            gson.toJson(entity, fileWriter);
            fileWriter.close();
        }
    }

    public static String createRecordsDirectory(String container_dir, String repositories_dir, String date_time_format) {
        boolean dataDirCreated = Utils.createDirIfNotExists(container_dir);
        if (dataDirCreated) {
            String dateTime = Utils.getDateTime(date_time_format);
            String recordsDirPath = container_dir + File.separator + dateTime + "_Experiment_" + SELECTED_EXPERIMENT + "_Model_" + Constants.SELECTED_COMPLETION_MODEL.getModelName();
            boolean recordsDirCreated = Utils.createDirIfNotExists(recordsDirPath);
            if (recordsDirCreated) {
                String recordRepositoriesDirPath = recordsDirPath + File.separator + repositories_dir;
                boolean recordsRepositoriesDirCreated = Utils.createDirIfNotExists(recordRepositoriesDirPath);
                if (recordsRepositoriesDirCreated) return recordsDirPath;
            }
        }
        return null;
    }


    /**
     * This method is used for test set
     *
     * @param testSetNumberOfMethod number of methods that we aim to find (with their corresponding test cases)
     * @param methodsCorpusPath     methods corpus
     * @return map of methods with their unit tests
     */
    public static Map<MethodDeclaration, Set<TestDeclaration>> findUnittestsForMethods(int testSetNumberOfMethod, String methodsCorpusPath) {
        Map<MethodDeclaration, Set<TestDeclaration>> results = new HashMap<>();
        TestAnalyzer testAnalyzer = new TestAnalyzer();
        Parser parser = new Parser(Parser.Flag.NO_METHOD_COMMENTS, Parser.Flag.NO_METHOD_JAVADOCS);
        int counter = 0;
        try {
            Gson gson = new Gson();
            BufferedReader reader = new BufferedReader(new FileReader(methodsCorpusPath));
            Repository[] repositories = gson.fromJson(reader, Repository[].class);

            for (Repository repository : repositories)
                for (RepoFile repoFile : repository.getFiles()) {
                    for (Method method : repoFile.getMethods()) {
                        MethodDeclaration unresolvableMethodDeclaration = new JavaParser().parseMethodDeclaration(method.getOriginalContent()).getResult().orElse(null);
                        if (unresolvableMethodDeclaration != null) {
                            InputRecord record = new InputRecord(method.getClassName(), unresolvableMethodDeclaration.getDeclarationAsString(), unresolvableMethodDeclaration.getNameAsString(), repoFile.getPath());
                            parser.initialize(record, false);
                            MethodDeclaration methodDeclaration = parser.getOriginalMethodDeclaration();

                            if (methodDeclaration != null) {
                                List<TestDeclaration> testDeclarations = testAnalyzer.inspect(record, false, methodDeclaration);
                                if (!testDeclarations.isEmpty()) {
                                    results.put(methodDeclaration, new HashSet<>(testDeclarations));
                                    System.out.println(results.size() + " " + counter);
                                    counter += testDeclarations.size();
                                    if (results.size() == 12) {
                                        System.out.println("TOTAL SIZE : " + counter);
                                        return results.entrySet().stream()
                                                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                                                            Collections.shuffle(list);
                                                            return list.stream()
                                                                    .limit(testSetNumberOfMethod)
                                                                    .collect(Collectors.toMap(
                                                                            Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new
                                                                    ));
                                                        }
                                                ));
                                    }
                                }
                            } else
                                System.out.println("Error occurred during mining similar methods...");
                        }
                    }
                }

            System.out.println("TOTAL SIZE : " + counter);
            return results.entrySet().stream()
                    .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                                Collections.shuffle(list);
                                return list.stream()
                                        .limit(testSetNumberOfMethod)
                                        .collect(Collectors.toMap(
                                                Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new
                                        ));
                            }
                    ));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    public static void saveFSLRecordToFile(List<FSLRecord> myList, String fileName) throws Exception {
        if (new File(fileName).exists())
            new File(fileName).delete();

        try (FileWriter writer = new FileWriter(fileName)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<FSLRecord>>() {
            }.getType();
            gson.toJson(myList, listType, writer);
            System.out.printf("List of fsl records has been saved to %s%n", fileName);
        }
    }

    public static void saveDatasetStatistics(DatasetStatistics datasetStatistics, String path) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        FileWriter fileWriter = new FileWriter(path);
        gson.toJson(datasetStatistics, fileWriter);
        fileWriter.close();
    }

    public static <T> void saveInputToFile(Set<T> list, String path) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        FileWriter fileWriter = new FileWriter(path);
        gson.toJson(list, fileWriter);
        fileWriter.close();
    }

    public static List<InputRecord> readEvalInputRecords(String path) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        Gson gson = new Gson();
        return Arrays.asList(gson.fromJson(reader, InputRecord[].class));
    }

    public static List<TestInputRecord> readTestInputRecords(String path) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        Gson gson = new Gson();
        return Arrays.asList(gson.fromJson(reader, TestInputRecord[].class));
    }

    public static List<FSLRecord> readFSLRecords(String path) throws Exception {
        List<FSLRecord> myList = new LinkedList<>();
        try (FileReader reader = new FileReader(path)) {
            Gson gson = new Gson();
//            Type listType = new TypeToken<List<FSLRecord>>() {
//            }.getType();
            myList.addAll(Arrays.asList(gson.fromJson(reader, FSLRecord[].class)));
        }
//        //because of character encoding problems on behalf of OpenAI, we have saved null when calculating embeddings. so we discard them when reading
//        return myList.stream().filter(record -> record.getEmbeddingVector() != null).collect(Collectors.toList());
        return myList;//new
    }

    public static int countOccurrences(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find())
            count++;

        return count;
    }

    public static EvaluationOutputEntity readEvalOutputRecords(String recordsDirPath, String evalOutputFilePath) {
        if (!FileUtils.fileExists(Paths.get(recordsDirPath, evalOutputFilePath).toString()))
            return null;

        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(Paths.get(recordsDirPath, evalOutputFilePath).toString()));
            Gson gson = new Gson();
            return gson.fromJson(reader, EvaluationOutputEntity.class);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean containsCoveo(String repoFilePath) {
        Path pomFilePath = Paths.get(repoFilePath, "pom.xml");

        try {
            List<String> lines = Files.readAllLines(pomFilePath);

            for (String line : lines) {
                if (line.contains("<groupId>com.coveo</groupId>")) {
                    return true; // Found coveo group ID
                }
            }
        } catch (IOException e) {
        }

        return false; // coveo not found
    }

    public static String[] concatenateArrays(String[] array1, String[] array2) {
        String[] result = new String[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);

        return result;
    }

    public static int computeNumberOfTokens(String input) throws Exception {
        if (input == null || input.isEmpty())
            return 0;
        String path = Paths.get(Constants.PYTHON_SCRIPTS_DIR, Constants.PYTHON_TOKENIZER_TEMP_FILE).toString();
        File file = new File(path);
        if (file.exists())
            file.delete();
        try {
            FileUtils.fileWrite(path, input);
            String[] terminalCommand = {"/bin/bash", "-c", generateTokenComputingCommand()};
            ProcessBuilder processBuilder = new ProcessBuilder(terminalCommand);
            processBuilder.directory(new File(Constants.PYTHON_SCRIPTS_DIR));
            processBuilder.environment().putAll(System.getenv());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null)
                response.append(line).append("\n");

            return Integer.parseInt(response.toString().trim());
        } finally {
            if (file.exists())
                file.delete();
        }
    }

    private static String generateTokenComputingCommand() {
        return "source ./models/env/bin/activate && python3 " +
                Constants.PYTHON_TOKENIZER_FILE +
                " --encoder " + Constants.PYTHON_TOKENIZER_ENCODER;
    }

    public static int getMaxTPMLengthOfModel(InputGenerator.Model completionModel) throws Exception {
        switch (completionModel) {
            case GPT_4O:
                return 30000;
            case GPT_4:
            case GPT_4_0613:
                return 10000;
            case GPT_3_5_TURBO_0613:
            case GPT_3_5_TURBO_16K:
            case GPT_3_5_TURBO_16K_0613:
                return 200000;
            default:
                throw new Exception("Model not found when extracting maximum TPM length. Returning 0 as max TPM");
        }
    }

    /**
     * Note that the headers and some values of the generated files are not exactly the same as the file TOGA needs,
     * since there are some double quotation marks and strings that should be edited to match the input of the TOGA
     */
    public static boolean generateToGAInput(Parser parser, Set<InputRecord> evalRecords) throws Exception {
        List<String[]> list = new ArrayList<>();
        try (CSVWriter writer = new CSVWriter(new FileWriter(TOGA_FILE_NAME_INPUT), ',',
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.NO_ESCAPE_CHARACTER,
                System.getProperty("line.separator"))) {
            writer.writeNext(TOGA_COLUMNS_INPUT);
            writer.flush();
            for (InputRecord record : evalRecords) {
                try {
                    parser.initialize(record, false);
                    String prunedMethod = parser.getOriginalMethod();
                    writer.writeNext(new String[]{"", prunedMethod.replace("\"", "\"\"")});
                    writer.flush();
                } catch (Exception e) {
                    System.err.println("An error occurred while initializing the parser in pipeline. skipping this case");
                }
            }
        } catch (Exception e) {
            return false;
        }

        try (CSVWriter writer = new CSVWriter(new FileWriter(TOGA_FILE_NAME_META), ',',
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.NO_ESCAPE_CHARACTER,
                System.getProperty("line.separator"))) {
            writer.writeNext(TOGA_COLUMNS_META);
            writer.flush();
            for (InputRecord record : evalRecords) {
                try {
                    parser.initialize(record, false);
                    String prunedMethod = parser.getPrunedMethod();
                    writer.writeNext(new String[]{"", "", "", "", "", "", "", ""});
                    writer.flush();
                } catch (Exception e) {
                    System.err.println("An error occurred while initializing the parser in pipeline. skipping this case");
                }
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * Gets an address as string and goes upward (parent-side) until meeting the @name parameter
     *
     * @param pathStr  address to go upward
     * @param name     name (or keyword) to search (Depending on @contains)
     * @param contains if true, checks if the name of the folders contains @name. otherwise, one foldr name must be equal to @name
     * @return new path in a sense that the latest level is the one we are looking for.
     */
    public static Path getFirstAddressUpwardHaving(String pathStr, String name, boolean contains) {
        if (contains) {
            if (!pathStr.contains(name))
                return null;
        } else {
            if (!pathStr.contains("/" + name + "/") && !pathStr.contains("\\" + name + "\\"))
                return null;
        }
        Path path = Path.of(pathStr);
        do {
            if (contains && path.getFileName().toString().contains(name))
                return path;
            else if (!contains && path.getFileName().toString().equals(name))
                return path;
            path = path.getParent();
        } while (true);
    }

    public static LinkedHashMap<String, Object> extractOfflineRecord(List<Map<String, Object>> items, int index, String repoName, String name) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (Map<String, Object> item : items) {
            double id = (int) ((double) ((Double) item.get("id")));
            if (id == index && item.get("repository_name").toString().equals(repoName) &&
                    item.get("method").toString().equals(name)) {
                map.put("id", item.get("id"));
                map.put("size", item.get("size"));
                map.put("repository_name", item.get("repository_name"));
                map.put("method", item.get("method"));
                map.put("build_tool", item.get("build_tool"));
                map.put("LLM_command", item.get("LLM_command"));
                map.put("LLM_response", item.get("LLM_response"));
                map.put("LLM_raw_response", item.get("LLM_raw_response"));
                map.put("compileCommand", item.get("compileCommand"));
                map.put("prompt_creation_time", item.get("prompt_creation_time"));
                map.put("prompt_creation_cost", item.get("prompt_creation_cost"));
                map.put("runCommand", item.get("runCommand"));
                map.put("is_compiled_before", item.get("is_compiled_before"));
                map.put("is_compiled_after", item.get("is_compiled_after"));
                map.put("tests_before", item.get("tests_before"));
                map.put("tests_after", item.get("tests_after"));
                map.put("rouge_scores", item.get("rouge_scores"));
                map.put("method_original_path", item.get("method_original_path"));
                map.put("method_alternative_path", item.get("method_alternative_path"));
                map.put("repository_original_path", item.get("repository_original_path"));
                map.put("repository_alternative_path", item.get("repository_alternative_path"));
                map.put("original_method", item.get("original_method"));
                map.put("pruned_method", item.get("pruned_method"));
                map.put("pre_log", item.get("pre_log"));
                map.put("post_log", item.get("post_log"));
                map.put("pre_error_message", item.get("pre_error_message"));
                map.put("post_error_message", item.get("post_error_message"));
            }
        }
        return map;
    }

    public static double computeCostOfTokens(LLM_InputContent input, String response, LLM_Client.LLM_Operation operation,
                                             InputGenerator.Model completionModel, InputGenerator.Model embeddingModel) throws Exception {
        switch (operation) {
            case COMPLETION:
                double inputCost = 0;
                double outputCost = 0;

                int systemTokens = computeNumberOfTokens(input.getSystem());
                int userTokens = 0;
                int assistantTokens = 0;

                if (input.getUser() != null)
                    for (String userItem : input.getUser())
                        userTokens += computeNumberOfTokens(userItem);

                if (input.getAssistant() != null)
                    for (String assistantItem : input.getAssistant())
                        assistantTokens += computeNumberOfTokens(assistantItem);

                inputCost = systemTokens * completionModel.getCompletionInputCost()
                        + userTokens * completionModel.getCompletionInputCost()
                        + assistantTokens * completionModel.getCompletionInputCost();
                if (response != null && !response.startsWith("Traceback") && !response.startsWith("Exceeded maximum trials")) {
                    outputCost = computeNumberOfTokens(response) * completionModel.getCompletionOutputCost();
                }

                return (inputCost + outputCost) / 1000.0d;
            case EMBEDDING:
                int embeddingTokens = computeNumberOfTokens(input.getSystem());
                return embeddingTokens * embeddingModel.getEmbeddingCost() / 1000.0d;
            default:
                throw new Exception("No operation found for computing cost for input and output tokens");
        }
    }

    public static boolean createCorruptRecordsEvalInput(String evalOutputPath, String evalInputPath) {
        if (!new File(evalOutputPath).exists())
            return false;

        try (BufferedReader reader = new BufferedReader(new FileReader(evalOutputPath))) {
            EvaluationOutputEntity entity = new Gson().fromJson(reader, EvaluationOutputEntity.class);
            List<Map<String, Object>> corruptItems = new LinkedList<>();
            for (Map<String, Object> item : entity.getItems()) {
                if (item.get("post_error_message") != null) {
                    String postProcessorMessage = item.get("post_error_message").toString();
                    if (postProcessorMessage.equals(PARSING_ERROR_MESSAGE))
                        corruptItems.add(item);
                    continue;
                }
                if (item.get("LLM_raw_response") != null) {
                    String LLMRawResponse = item.get("LLM_raw_response").toString();
                    if (isCorruptString(LLMRawResponse)) {
                        corruptItems.add(item);
                        continue;
                    }
                    StringBuilder usersStringBuilder = new StringBuilder();

                    LinkedTreeMap ltm = (LinkedTreeMap) item.get("LLM_command");
                    if (ltm != null) {
                        if (ltm.get("user") != null) {
                            ArrayList<Object> users = (ArrayList) ltm.get("user");
                            for (Object strObject : users)
                                usersStringBuilder.append(strObject.toString());
                        }
                    } else {
                        corruptItems.add(item);
                        continue;
                    }
                    if (isCorruptString(usersStringBuilder.toString())) {
                        corruptItems.add(item);
                    }
                } else
                    corruptItems.add(item);
            }

            List<InputRecord> inputRecords = readEvalInputRecords(evalInputPath);
            List<InputRecord> toReCompileInputRecords = new LinkedList<>();

            for (Map<String, Object> corruptItem : corruptItems) {
                String name = corruptItem.get("method").toString().trim();
                String path = corruptItem.get("method_original_path").toString().trim();
                if (corruptItem.get("original_method") != null) {
                    try {
                        String signature = StaticJavaParser.parseMethodDeclaration(corruptItem.get("original_method").toString()).getDeclarationAsString(false, false, true).trim();
                        InputRecord corruptInputRecord = getCorruptInputRecord(inputRecords, name, path, signature);
                        if (corruptInputRecord != null) {
                            toReCompileInputRecords.add(corruptInputRecord);
                        }
                    } catch (Exception e) {
                    }
                }
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().disableHtmlEscaping().create();
            FileUtils.fileWrite(EVAL_INPUT_FILE_PATH, gson.toJson(toReCompileInputRecords));
            System.out.println(toReCompileInputRecords.size() + "/" + corruptItems.size() + " were stored in eval_input.json");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static InputRecord getCorruptInputRecord(List<InputRecord> inputRecords, String name, String path, String signature) {
        for (InputRecord record : inputRecords) {
            if (record.getName().equals(name) && record.getPath().equals(path) &&
                    record.getSignature().contains(signature))
                return record;
        }
        return null;
    }

    private static boolean isCorruptString(String str) {
        return str.contains("Traceback (") || str.contains("Bad gateway") || str.contains("Exceeded maximum trials");
    }

    public static boolean deleteDirectory(String directoryPath) {
        try {
            FileUtils.deleteDirectory(new File(directoryPath));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static String getCompatibleJavaVersionOf(String repoName) {
        return REPO_JAVA_ADDRESS_MAP.get(repoName);
    }
}
