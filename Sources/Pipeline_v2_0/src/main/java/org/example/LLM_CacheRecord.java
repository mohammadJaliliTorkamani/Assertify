package org.example;

public class LLM_CacheRecord {
    private LLM_InputContent content;
    private String operation;
    private String model;
    private String response;
    private double temperature;
    private double top_p;
    private double presence_penalty;
    private double frequency_penalty;
    private double cost;

    public LLM_CacheRecord(LLM_InputContent content, String operation, String model, String response,
                           double temperature, double top_p, double presence_penalty, double frequency_penalty,
                           double cost) {
        this.content = content;
        this.operation = operation;
        this.model = model;
        this.response = response;
        this.temperature = temperature;
        this.top_p = top_p;
        this.presence_penalty = presence_penalty;
        this.frequency_penalty = frequency_penalty;
        this.cost = cost;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public LLM_InputContent getContent() {
        return content;
    }

    public void setContent(LLM_InputContent content) {
        this.content = content;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getTop_p() {
        return top_p;
    }

    public void setTop_p(double top_p) {
        this.top_p = top_p;
    }

    public double getPresence_penalty() {
        return presence_penalty;
    }

    public void setPresence_penalty(double presence_penalty) {
        this.presence_penalty = presence_penalty;
    }

    public double getFrequency_penalty() {
        return frequency_penalty;
    }

    public void setFrequency_penalty(double frequency_penalty) {
        this.frequency_penalty = frequency_penalty;
    }
}
