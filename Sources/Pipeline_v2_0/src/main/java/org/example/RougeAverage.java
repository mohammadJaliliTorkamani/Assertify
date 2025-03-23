package org.example;

public class RougeAverage {
    private double rouge_1_averageF;
    private double rouge_1_averageP;
    private double rouge_1_averageR;
    private double rouge_2_averageF;
    private double rouge_2_averageP;
    private double rouge_2_averageR;
    private double rouge_L_averageF;
    private double rouge_L_averageP;
    private double rouge_L_averageR;
    private String referenceSummary;
    private String candidateSummary;

    public RougeAverage(double rouge_1_averageF, double rouge_1_averageP, double rouge_1_averageR,
                        double rouge_2_averageF, double rouge_2_averageP, double rouge_2_averageR, double rouge_L_averageF, double rouge_L_averageP, double rouge_L_averageR, String referenceSummary, String candidateSummary) {
        this.rouge_1_averageF = rouge_1_averageF;
        this.rouge_1_averageP = rouge_1_averageP;
        this.rouge_1_averageR = rouge_1_averageR;
        this.rouge_2_averageF = rouge_2_averageF;
        this.rouge_2_averageP = rouge_2_averageP;
        this.rouge_2_averageR = rouge_2_averageR;
        this.rouge_L_averageF = rouge_L_averageF;
        this.rouge_L_averageP = rouge_L_averageP;
        this.rouge_L_averageR = rouge_L_averageR;
        this.referenceSummary = referenceSummary;
        this.candidateSummary = candidateSummary;
    }

    public RougeAverage() {
    }

    public double getRouge_1_averageF() {
        return rouge_1_averageF;
    }

    public void setRouge_1_averageF(double rouge_1_averageF) {
        this.rouge_1_averageF = rouge_1_averageF;
    }

    public double getRouge_1_averageP() {
        return rouge_1_averageP;
    }

    public void setRouge_1_averageP(double rouge_1_averageP) {
        this.rouge_1_averageP = rouge_1_averageP;
    }

    public double getRouge_1_averageR() {
        return rouge_1_averageR;
    }

    public void setRouge_1_averageR(double rouge_1_averageR) {
        this.rouge_1_averageR = rouge_1_averageR;
    }

    public double getRouge_2_averageF() {
        return rouge_2_averageF;
    }

    public void setRouge_2_averageF(double rouge_2_averageF) {
        this.rouge_2_averageF = rouge_2_averageF;
    }

    public double getRouge_2_averageP() {
        return rouge_2_averageP;
    }

    public void setRouge_2_averageP(double rouge_2_averageP) {
        this.rouge_2_averageP = rouge_2_averageP;
    }

    public double getRouge_2_averageR() {
        return rouge_2_averageR;
    }

    public void setRouge_2_averageR(double rouge_2_averageR) {
        this.rouge_2_averageR = rouge_2_averageR;
    }

    public double getRouge_L_averageF() {
        return rouge_L_averageF;
    }

    public void setRouge_L_averageF(double rouge_L_averageF) {
        this.rouge_L_averageF = rouge_L_averageF;
    }

    public double getRouge_L_averageP() {
        return rouge_L_averageP;
    }

    public void setRouge_L_averageP(double rouge_L_averageP) {
        this.rouge_L_averageP = rouge_L_averageP;
    }

    public double getRouge_L_averageR() {
        return rouge_L_averageR;
    }

    public void setRouge_L_averageR(double rouge_L_averageR) {
        this.rouge_L_averageR = rouge_L_averageR;
    }

    public String getReferenceSummary() {
        return referenceSummary;
    }

    public void setReferenceSummary(String referenceSummary) {
        this.referenceSummary = referenceSummary;
    }

    public String getCandidateSummary() {
        return candidateSummary;
    }

    public void setCandidateSummary(String candidateSummary) {
        this.candidateSummary = candidateSummary;
    }
}
