package com.example.testframework.dto;

import com.example.testframework.model.TestCase;

import java.time.LocalDateTime;

public class TestCaseDTO {

    private Long id;
    private String name;
    private String type;        // enum -> string
    private String description;
    private String status;      // enum -> string
    private LocalDateTime createdAt;
    private String functionality;
    private String priority;    // enum -> string
    private String category;
    
    // New fields for enhanced integration
    private String testSuiteId;
    private String testData;
    private String expectedResult;
    private String testEnvironment;
    private Integer timeout;
    private Integer retryCount;
    private Boolean isActive;
    private LocalDateTime lastExecuted;
    private String executionMode;
    private String prerequisites;
    private String tags;

    public TestCaseDTO(TestCase tc) {
        if (tc == null) return;
        this.id = tc.getId();
        this.name = tc.getName();
        this.type = (tc.getType() != null) ? tc.getType().name() : null;
        this.description = tc.getDescription();
        this.status = (tc.getStatus() != null) ? tc.getStatus().name() : null;
        this.createdAt = tc.getCreatedAt();
        this.functionality = tc.getFunctionality();
        this.priority = (tc.getPriority() != null) ? tc.getPriority().name() : null;
        this.category = tc.getCategory();
        this.testSuiteId = tc.getTestSuiteId();
        this.testData = tc.getTestData();
        this.expectedResult = tc.getExpectedResult();
        this.testEnvironment = tc.getTestEnvironment();
        this.timeout = tc.getTimeout();
        this.retryCount = tc.getRetryCount();
        this.isActive = tc.getIsActive();
        this.lastExecuted = tc.getLastExecuted();
        this.executionMode = tc.getExecutionMode();
        this.prerequisites = tc.getPrerequisites();
        this.tags = tc.getTags();
    }

    // ======================
    // Getters
    // ======================
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getFunctionality() { return functionality; }
    public String getPriority() { return priority; }
    public String getCategory() { return category; }
    public String getTestSuiteId() { return testSuiteId; }
    public String getTestData() { return testData; }
    public String getExpectedResult() { return expectedResult; }
    public String getTestEnvironment() { return testEnvironment; }
    public Integer getTimeout() { return timeout; }
    public Integer getRetryCount() { return retryCount; }
    public Boolean getIsActive() { return isActive; }
    public LocalDateTime getLastExecuted() { return lastExecuted; }
    public String getExecutionMode() { return executionMode; }
    public String getPrerequisites() { return prerequisites; }
    public String getTags() { return tags; }
}
