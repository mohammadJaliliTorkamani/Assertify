package org.example;

public class FSL_Pair {
    private FSLRecord record;
    private double similarity;

    public FSL_Pair(FSLRecord record, double similarity) {
        this.record = record;
        this.similarity = similarity;
    }

    public FSLRecord getRecord() {
        return record;
    }

    public void setRecord(FSLRecord record) {
        this.record = record;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }
}
