package com.example.testframework.dto;

import java.util.List;

public class RunStatusDTO {
    private String runId;
    private String status;
    private int totalTests;
    private int completedTests;
    private List<TestResultDTO> results;

    public RunStatusDTO(String runId, String status, int totalTests, int completedTests, List<TestResultDTO> results) {
        this.runId = runId;
        this.status = status;
        this.totalTests = totalTests;
        this.completedTests = completedTests;
        this.results = results;
    }

    // getters
    public String getRunId() { return runId; }
    public String getStatus() { return status; }
    public int getTotalTests() { return totalTests; }
    public int getCompletedTests() { return completedTests; }
    public List<TestResultDTO> getResults() { return results; }
}
