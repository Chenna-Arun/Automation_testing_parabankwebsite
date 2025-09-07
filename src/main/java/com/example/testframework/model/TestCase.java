package com.example.testframework.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "test_case")
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Type type; // UI or API

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING; // default

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String functionality; // e.g., "Login", "TransferFunds"

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Priority priority; // HIGH, MEDIUM, LOW

    @Column(length = 100)
    private String category; // <-- Added field for grouping test cases

    // New fields for enhanced integration
    @Column(name = "test_suite_id")
    private String testSuiteId; // For grouping tests in suites

    @Column(columnDefinition = "TEXT")
    private String testData; // JSON string for dynamic test data

    @Column(length = 500)
    private String expectedResult; // Expected outcome description

    @Column(length = 200)
    private String testEnvironment; // Environment where test should run

    @Column
    private Integer timeout; // Test timeout in seconds

    @Column
    private Integer retryCount = 0; // Number of retries on failure

    @Column
    private Boolean isActive = true; // Whether test is active

    @Column(name = "last_executed")
    private LocalDateTime lastExecuted; // Last execution time

    @Column(length = 50)
    private String executionMode; // SEQUENTIAL, PARALLEL

    @Column(columnDefinition = "TEXT")
    private String prerequisites; // Test prerequisites description

    @Column(columnDefinition = "TEXT")
    private String tags; // Comma-separated tags for filtering

    // API-specific fields
    @Column(length = 2000)
    private String url; // API endpoint URL for API tests

    @Column(length = 10)
    private String method; // HTTP method for API tests (GET, POST, PUT, DELETE)

    @OneToMany(mappedBy = "testCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestResult> results;

    // ======================
    // Lifecycle Callbacks
    // ======================
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ======================
    // Getters and Setters
    // ======================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getFunctionality() { return functionality; }
    public void setFunctionality(String functionality) { this.functionality = functionality; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTestSuiteId() { return testSuiteId; }
    public void setTestSuiteId(String testSuiteId) { this.testSuiteId = testSuiteId; }

    public String getTestData() { return testData; }
    public void setTestData(String testData) { this.testData = testData; }

    public String getExpectedResult() { return expectedResult; }
    public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }

    public String getTestEnvironment() { return testEnvironment; }
    public void setTestEnvironment(String testEnvironment) { this.testEnvironment = testEnvironment; }

    public Integer getTimeout() { return timeout; }
    public void setTimeout(Integer timeout) { this.timeout = timeout; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getLastExecuted() { return lastExecuted; }
    public void setLastExecuted(LocalDateTime lastExecuted) { this.lastExecuted = lastExecuted; }

    public String getExecutionMode() { return executionMode; }
    public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }

    public String getPrerequisites() { return prerequisites; }
    public void setPrerequisites(String prerequisites) { this.prerequisites = prerequisites; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public List<TestResult> getResults() { return results; }
    public void setResults(List<TestResult> results) { this.results = results; }

    // ======================
    // Enums
    // ======================
    public enum Type {
        UI, API
    }

    public enum Status {
        COMPLETED, FAILED, PENDING, READY, RUNNING

    }

    public enum Priority {
        LOW, MEDIUM, HIGH
    }
}
