package org.example;

import java.util.HashMap;
import java.util.Map;

import static org.example.InputGenerator.Model.GPT_4;
import static org.example.InputGenerator.Model.TEXT_EMBEDDINGS_3_SMALL;

public class Constants {
    public static final String PIPELINE_PROJECT_DIRECTORY = "ADDRESS GOES HERE";
    public static final String REPO_EXTRACTOR_PROJECT_DIRECTORY = "ADDRESS GOES HERE/Sources" + "/ADDRESS GOES HERE";
    public static final String PARSING_ERROR_MESSAGE = "Error while parsing the LLM output";
    public static final String COMPILATION_ERROR_MESSAGE = "Compilation Failed";
    public static final String PIPELINE_ERROR_MESSAGE = "Pipeline iteration Failed";
    public static final String TESTING_ERROR_MESSAGE = "Testing Failed";
    public static final String ROUGE_COMPUTATION_ERROR_MESSAGE = "ROUGE Scores Calculation Failed";
    public static final String TEST_NOT_FOUND_ERROR_MESSAGE = "Finding Test Failed";
    public static final String UNDETECTABLE_PROJECT_BUILD_TOOL_ERROR_MESSAGE = "Project build tool cannot be detected";
    public static final int LLM_REQUEST_TRIAL_NUMBER = 10;
    public static final int API_KEY_ASTERISK_MARGIN = 7;
    public static final String BACKUP_DIR_NAME = "data";
    public static final String BACKUP_REPOSITORIES_DIR_NAME = "repositories";
    public static final String DATE_TIME_FORMAT = "yyyy_MM_dd_HH_mm";
    public static final String API_KEY = "API GOES HERE"; // research organization
    public static final String PYTHON_HUGGINGFACE_FILE = "huggingface_rouge.py";
    public static final String PYTHON_TOKENIZER_FILE = "tokenizer.py";
    public static final String ROUGE_REFERENCE_SUMMARY_PATH = PIPELINE_PROJECT_DIRECTORY + "/data/reference_summary.txt";
    public static final String ROUGE_CANDIDATE_SUMMARY_PATH = PIPELINE_PROJECT_DIRECTORY + "/data/candidate_summary.txt";
    public static final int MAX_NUMBER_OF_SIMILAR_MINED_EXAMPLES = 3;
    public static final double MINIMUM_ACCEPTABLE_COSINE_VALUE = 5e-1;
    public static final String METHODS_CORPUS_PATH = REPO_EXTRACTOR_PROJECT_DIRECTORY + "/ADDRESS GOES HERE/methods_corpus.json";// "/repositories/methods_corpus.json";
    public static final String FILTERED_METHODS_CORPUS_PATH = REPO_EXTRACTOR_PROJECT_DIRECTORY + "/repositories/filtered_methods_corpus.json";
    public static final String FSL_DATABASE_INPUT_FILE_PATH = PIPELINE_PROJECT_DIRECTORY + "/fsl_db.json";
    public static final String EVAL_INPUT_FILE_PATH = PIPELINE_PROJECT_DIRECTORY + "/eval_input.json";
    public static final String SMALL_EVAL_INPUT_FILE_PATH = PIPELINE_PROJECT_DIRECTORY + "/small_eval_input.json";
    public static final String TEST_INPUT_FILE_PATH = PIPELINE_PROJECT_DIRECTORY + "/test_input.json";
    public static final String EVAL_OUTPUT_FILE_PATH = "eval_output.json";
    public static final String PYTHON_SCRIPTS_DIR = PIPELINE_PROJECT_DIRECTORY + "/python_scripts";
    public static final String PYTHON_SCRIPT_MODELS_DIR = PYTHON_SCRIPTS_DIR + "/models";
    public static final String USER_PROMPT_FILE = "user.txt";
    public static final String SYSTEM_PROMPT_FILE = "system.txt";
    public static final String ASSISTANT_PROMPT_FILE = "assistant.txt";
    public static final String LLM_CACHE_FILE = PIPELINE_PROJECT_DIRECTORY + "/llm_cache.json";
    public static final String MVN_BINARY_PATH = "mvn";
    /**
     * It will and should not be applied in openAI script because openAI can specify the remained number of allowed
     * tokens as maximum response length
     */
    public static final int MAX_LLM_RESPONSE_LENGTH = 4096;
    public static final int EMBEDDING_BATCH_SIZE = 50;
    public static final int MAX_CACHE_SIZE = 0;
    public static final String PYTHON_TOKENIZER_ENCODER = "cl100k_base";
    public static final String PYTHON_TOKENIZER_TEMP_FILE = "tokenizer_tmp.txt";
    /**
     * It contains start tag and end tag
     */
    public static final String[] LLM_ASSISTANT_DELIMITER = {"<JAVA>", "</JAVA>"};
    public static final String TOGA_FILE_NAME_INPUT = PIPELINE_PROJECT_DIRECTORY + "/data/" + "my_input.csv";
    public static final String TOGA_FILE_NAME_META = PIPELINE_PROJECT_DIRECTORY + "/data/" + "my_meta.csv";
    public static final String[] TOGA_COLUMNS_INPUT = new String[]{"focal_method", "test_prefix"};
    public static final String[] TOGA_COLUMNS_META = new String[]{"project", "bug_num", "test_name", "exception_bug", "assertion_bug", "exception_lbl", "assertion_lbl", "assert_err"};
    /**
     * Make sure you also apply corresponding string modification in 'LLM_SYSTEM_BASE_MESSAGE'
     */
    public static final String LLM_USER_INPUT_METHOD_DELIMITER = "\"\"\"";
    public static final String LLM_SYSTEM_MESSAGE =
          String.format("You are an expert in generating Java standard assertions. Your task is to insert assertions into a given method, ensuring the method’s correct behavior while keeping it compilable. Follow these instructions carefully:\n" +
                  "Input Method: A Java method will be provided, delimited by triple quotes (%s), where each line is numbered.\n" +
                  "Task: Insert Java standard assertions before specified lines without using JUnit assertions. The remaining method lines will shift down by one to accommodate the assertion.\n" +
                  "Constraints:\n" +
                  "Generate only Java standard assertions. Avoid using any undefined methods or symbols in the project.\n" +
                  "The assertions must use only variables defined before the predicted line, ensuring the code remains compilable.\n" +
                  "Do not generate the new method, only focus on generating assertion and line number pairs.\n" +
                  "Do not generate assertions that require importing additional classes.\n" +
                  "Assertions should not alter the behavior of the method but validate specific conditions.\n" +
                  "Output Format:\n" +
                  "Provide output in pairs of assertions and line numbers.\n" +
                  "Each pair must be encapsulated within %s and %s tags, formatted as (line_number, assertion). For instance: %s(3, assert a < 3;)%s, %s(5, assert a.getAge() == 4;)%s.\n" +
                  "Exclude all descriptions, explanations, or any additional code (e.g., method structure or import statements). Only return the assertion and line number pairs.",
                  LLM_USER_INPUT_METHOD_DELIMITER, LLM_ASSISTANT_DELIMITER[0], LLM_ASSISTANT_DELIMITER[1],
                    LLM_ASSISTANT_DELIMITER[0], LLM_ASSISTANT_DELIMITER[1], LLM_ASSISTANT_DELIMITER[0], LLM_ASSISTANT_DELIMITER[1]);

    public static final boolean READ_PIPELINE_EVALUATION_OFFLINE = false;
    public static final String JAVA_HOME_VERSION_8 = "ADDRESS GOES HERE";
    public static final String JAVA_HOME_VERSION_11 = "ADDRESS GOES HERE";
    public static final String JAVA_HOME_VERSION_17 = "ADDRESS GOES HERE";
    public static final String JAVA_HOME_VERSION_21 = "ADDRESS GOES HERE";
    public static final String JAVA_HOME_VERSION_23 = "ADDRESS GOES HERE";
    public static final String[] JAVA_HOME_VERSIONS =
            {JAVA_HOME_VERSION_11, JAVA_HOME_VERSION_23, JAVA_HOME_VERSION_17, JAVA_HOME_VERSION_8, JAVA_HOME_VERSION_21};

    public static final double MODEL_GPT4O_INPUT_PRICE_PER_K = 0.005;
    public static final double MODEL_GPT4O_OUTPUT_PRICE_PER_K = 0.015;


    public static final double MODEL_GPT4_8K_COMPLETION_INPUT_PRICE_PER_K = 0.03;
    public static final double MODEL_GPT4_8K_COMPLETION_OUTPUT_PRICE_PER_K = 0.06;
    public static final double MODEL_GPT4_32K_COMPLETION_INPUT_PRICE_PER_K = 0.06;
    public static final double MODEL_GPT4_32K_COMPLETION_OUTPUT_PRICE_PER_K = 0.12;
    public static final double MODEL_GPT3_5_COMPLETION_INPUT_PRICE_PER_K = 0.0015;
    public static final double MODEL_GPT3_5_COMPLETION_OUTPUT_PRICE_PER_K = 0.002;
    public static final double MODEL_GPT3_5_16K_COMPLETION_INPUT_PRICE_PER_K = 0.003;
    public static final double MODEL_GPT3_5_16K_COMPLETION_OUTPUT_PRICE_PER_K = 0.004;
    public static final double TEXT_EMBEDDINGS_3_SMALL_PRICE_PER_K = 0.00002;
    public static final boolean GENERATE_TOGA_DATASET = false;

    /**
     * This preecnts memory overflow when analyzing methods corpus
     */
    public static final int DATASET_STATISTICS_MAX_NON_TEST_JAVA_FILES = 6000;
    /**
     * In fact, MAX_FSL_DB_SIZE is an integer but due to the need for positive infinity, we have to consider as double
     */
    public static final double FSL_DB_SIZE = 6242;
    public static final boolean COMPUTE_DATASET_STATISTICS = true;
    ////////////////////////////   start changes   ////////////////////////////
    public static final int MINIMUM_REQUEST_DELAY_SECONDS = 3;
    public static final int EVAL_SET_NUMBER_OF_METHOD = 3;//must be 2000 for experiments
    public static final int TEST_SET_NUMBER_OF_METHOD = 0;
    public static final InputGenerator.Model SELECTED_EMBEDDING_MODEL = TEXT_EMBEDDINGS_3_SMALL;
    public static final String PIPELINE_LLM_PROMPT_AND_RESPONSE_READ_OFFLINE_OUTPUT_PATH = "JSON ADDRESS GOES HERE";
    public static final int METHODS_CORPUS_MINIMUM_REPO_ASSERTIONS_THRESHOLD = 50;
    public static InputGenerator.Model SELECTED_COMPLETION_MODEL = GPT_4;
    public static InputGenerator.Experiments SELECTED_EXPERIMENT = InputGenerator.Experiments.A;
    public static String DATASET_STATISTICS_OUTPUT_PATH = PIPELINE_PROJECT_DIRECTORY + "/dataset_statistics.json";
    public static final double EVAL_SET_DATASET_POPULATION_RATIO = 0.35;
    public static final Map<String, String> REPO_JAVA_ADDRESS_MAP= new HashMap<>();

}