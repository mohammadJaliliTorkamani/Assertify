package org.example;

public class Main {
    final static String REPO_DIR_PATH = "ADDRESS GOES HERE";
    final static String DATA_FILE_PATH = REPO_DIR_PATH + "/" + "output.txt";
    final static String OUTPUT_FILE_PATH = REPO_DIR_PATH + "/" + "methods_corpus.json";

    public static void main(String[] args) {
        RepoWriter repoWriter = new RepoWriter(REPO_DIR_PATH, DATA_FILE_PATH, OUTPUT_FILE_PATH);
        repoWriter.saveDataset();
    }
}