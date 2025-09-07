package com.example.testframework.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "test_suite")
public class TestSuite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String suiteId; // Unique identifier for the suite

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(length = 100)
    private String environment; // Target environment

    @Column(length = 500)
    private String tags; // Comma-separated tags

    @Column
    private Integer totalTests = 0;

    @Column
    private Integer activeTests = 0;

    @Column(length = 100)
    private String createdBy; // User who created the suite

    @OneToMany(mappedBy = "testSuiteId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestCase> testCases;

    // ======================
    // Lifecycle Callbacks
    // ======================
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ======================
    // Constructors
    // ======================
    public TestSuite() {}

    public TestSuite(String suiteId, String name, String description) {
        this.suiteId = suiteId;
        this.name = name;
        this.description = description;
    }

    // ======================
    // Getters and Setters
    // ======================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSuiteId() { return suiteId; }
    public void setSuiteId(String suiteId) { this.suiteId = suiteId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public Integer getTotalTests() { return totalTests; }
    public void setTotalTests(Integer totalTests) { this.totalTests = totalTests; }

    public Integer getActiveTests() { return activeTests; }
    public void setActiveTests(Integer activeTests) { this.activeTests = activeTests; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public List<TestCase> getTestCases() { return testCases; }
    public void setTestCases(List<TestCase> testCases) { this.testCases = testCases; }

    // ======================
    // Enums
    // ======================
    public enum Status {
        ACTIVE, INACTIVE, ARCHIVED
    }

    // ======================
    // Utility Methods
    // ======================
    @Override
    public String toString() {
        return "TestSuite{" +
                "id=" + id +
                ", suiteId='" + suiteId + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", totalTests=" + totalTests +
                ", activeTests=" + activeTests +
                '}';
    }
}