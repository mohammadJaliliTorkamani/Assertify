package org.example;

import org.example.Parser.Flag;

import static org.example.Constants.*;
import static org.example.InputGenerator.Model.GPT_4O;

public class Main {
    public static void main(String[] args) {
        InputGenerator.Model[] models = {
//                GPT_3_5_TURBO_16K,
//                GPT_4,
                GPT_4O
        };
        InputGenerator.Experiments[] experiments = {
//                InputGenerator.Experiments.A,
//                InputGenerator.Experiments.B,
//                InputGenerator.Experiments.C,
//                InputGenerator.Experiments.D,
                InputGenerator.Experiments.FINALIZED_MODE
        };
        int counter = 0;

        for (InputGenerator.Model model : models) {
            for (InputGenerator.Experiments exp : experiments) {
                double totalProgressPercentage = 1.0 * (++counter) / (models.length * experiments.length);

                Constants.SELECTED_COMPLETION_MODEL = model;
                Constants.SELECTED_EXPERIMENT = exp;

                System.out.println("Model: " + model.getModelName() + " Experiment: " + exp);

                Pipeline pipeline = new Pipeline(EVAL_OUTPUT_FILE_PATH);
                DataModulator dataModulator = new DataModulator(FILTERED_METHODS_CORPUS_PATH, EVAL_INPUT_FILE_PATH, FSL_DATABASE_INPUT_FILE_PATH,
                        METHODS_CORPUS_MINIMUM_REPO_ASSERTIONS_THRESHOLD, EVAL_SET_DATASET_POPULATION_RATIO);
                Parser parser = new Parser(Flag.NO_METHOD_COMMENTS, Flag.NO_METHOD_JAVADOCS, Flag.NO_METHOD_ASSERTIONS); //the last one added recently + change providing method without assertion in system field of LLM
                InputGenerator inputGenerator = new InputGenerator(API_KEY, SELECTED_COMPLETION_MODEL, SELECTED_EMBEDDING_MODEL, SELECTED_EXPERIMENT);
                Preprocessor preprocessor = new Preprocessor(BACKUP_REPOSITORIES_DIR_NAME);
                Postprocessor postprocessor = new Postprocessor();
                CodeReplacer replacer = new CodeReplacer();
                Evaluator evaluator = new Evaluator(PYTHON_HUGGINGFACE_FILE, ROUGE_REFERENCE_SUMMARY_PATH, ROUGE_CANDIDATE_SUMMARY_PATH);

                try {
                    pipeline
                            .setDataModulator(dataModulator)
                            .setPreprocessor(preprocessor)
                            .setParser(parser)
                            .setInputGenerator(inputGenerator)
                            .setPostprocessor(postprocessor)
                            .setCodeReplacer(replacer)
                            .setEvaluator(evaluator)
                            .execute(false, totalProgressPercentage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}