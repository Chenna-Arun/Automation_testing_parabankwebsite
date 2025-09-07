package com.example.testframework.dto;

import com.example.testframework.model.TestResult;

public class TestResultDTO {
    private Long testCaseId;
    private String testCaseName;
    private String status;
    private String details;
    private String errorMessage;
    private String responseBody;
    private Integer statusCode;
    private String screenshotPath;

    public TestResultDTO(TestResult result) {
        this.testCaseId = result.getTestCase().getId();
        this.testCaseName = result.getTestCase().getName();
        this.status = result.getStatus();
        this.details = result.getDetails();
        this.errorMessage = result.getErrorMessage();
        this.responseBody = result.getResponseBody();
        this.statusCode = result.getStatusCode();
        this.screenshotPath = result.getScreenshotPath();
    }

    // getters
    public Long getTestCaseId() { return testCaseId; }
    public String getTestCaseName() { return testCaseName; }
    public String getStatus() { return status; }
    public String getDetails() { return details; }
    public String getErrorMessage() { return errorMessage; }
    public String getResponseBody() { return responseBody; }
    public Integer getStatusCode() { return statusCode; }
    public String getScreenshotPath() { return screenshotPath; }
}
