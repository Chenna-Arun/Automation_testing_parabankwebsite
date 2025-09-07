package com.example.testframework.service;

import com.example.testframework.model.TestCase;
import com.example.testframework.model.TestSuite;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling test integration logic including test setup,
 * data validation, and integration with Selenium/REST-Assured executors.
 */
@Service
public class TestIntegrationService {

    private final TestService testService;
    private final ObjectMapper objectMapper;

    @Autowired
    public TestIntegrationService(TestService testService, ObjectMapper objectMapper) {
        this.testService = testService;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates and integrates a new test case with validation and setup
     */
    public TestCase integrateTestCase(TestCase testCase) {
        // Validate test case
        validateTestCase(testCase);
        
        // Set default values
        setDefaultValues(testCase);
        
        // Validate test data if provided
        if (testCase.getTestData() != null) {
            validateTestData(testCase.getTestData(), testCase.getType());
        }
        
        // Save the test case
        TestCase created = testService.createTestCase(testCase);
        
        // Update suite statistics if part of a suite
        if (testCase.getTestSuiteId() != null) {
            updateSuiteStatistics(testCase.getTestSuiteId());
        }
        
        return created;
    }

    /**
     * Batch integration of multiple test cases
     */
    public List<TestCase> integrateTestCases(List<TestCase> testCases) {
        List<TestCase> integratedCases = new ArrayList<>();
        
        for (TestCase testCase : testCases) {
            try {
                TestCase integrated = integrateTestCase(testCase);
                integratedCases.add(integrated);
            } catch (Exception e) {
                throw new RuntimeException("Failed to integrate test case: " + testCase.getName() + ". Error: " + e.getMessage());
            }
        }
        
        return integratedCases;
    }

    /**
     * Creates a test suite with associated test cases
     */
    public Map<String, Object> createTestSuiteWithTests(String suiteName, String description, 
                                                        List<TestCase> testCases, String environment) {
        String suiteId = generateSuiteId(suiteName);
        
        // Set suite ID for all test cases
        testCases.forEach(tc -> {
            tc.setTestSuiteId(suiteId);
            tc.setTestEnvironment(environment);
        });
        
        // Integrate all test cases
        List<TestCase> integratedCases = integrateTestCases(testCases);
        
        Map<String, Object> result = new HashMap<>();
        result.put("suiteId", suiteId);
        result.put("suiteName", suiteName);
        result.put("description", description);
        result.put("environment", environment);
        result.put("totalTests", integratedCases.size());
        result.put("testCases", integratedCases);
        result.put("createdAt", LocalDateTime.now());
        
        return result;
    }

    /**
     * Gets test cases by suite ID with filtering options
     */
    public List<TestCase> getTestCasesBySuite(String suiteId, String status, String type, String priority) {
        System.out.println("DEBUG: Getting test cases for suite: " + suiteId);
        
        List<TestCase> allTests = testService.getAllTestCases();
        System.out.println("DEBUG: Total tests in system: " + allTests.size());
        
        try {
            List<TestCase> filteredTests = allTests.stream()
                    .filter(tc -> {
                        if (tc == null) {
                            System.out.println("DEBUG: Found null test case");
                            return false;
                        }
                        boolean matches = suiteId.equals(tc.getTestSuiteId());
                        if (matches) {
                            System.out.println("DEBUG: Found matching test - ID: " + tc.getId() + 
                                ", Name: " + tc.getName() + 
                                ", Status: " + tc.getStatus() + 
                                ", Type: " + tc.getType() + 
                                ", Priority: " + tc.getPriority() + 
                                ", IsActive: " + tc.getIsActive());
                        }
                        return matches;
                    })
                    .filter(tc -> {
                        boolean statusMatch = status == null || (tc.getStatus() != null && status.equalsIgnoreCase(tc.getStatus().toString()));
                        if (!statusMatch) System.out.println("DEBUG: Status filter failed for test " + tc.getId());
                        return statusMatch;
                    })
                    .filter(tc -> {
                        boolean typeMatch = type == null || (tc.getType() != null && type.equalsIgnoreCase(tc.getType().toString()));
                        if (!typeMatch) System.out.println("DEBUG: Type filter failed for test " + tc.getId());
                        return typeMatch;
                    })
                    .filter(tc -> {
                        boolean priorityMatch = priority == null || (tc.getPriority() != null && priority.equalsIgnoreCase(tc.getPriority().toString()));
                        if (!priorityMatch) System.out.println("DEBUG: Priority filter failed for test " + tc.getId());
                        return priorityMatch;
                    })
                    .filter(tc -> {
                        boolean isActiveMatch = tc.getIsActive() != null && tc.getIsActive();
                        if (!isActiveMatch) System.out.println("DEBUG: IsActive filter failed for test " + tc.getId() + ", isActive: " + tc.getIsActive());
                        return isActiveMatch;
                    })
                    .collect(Collectors.toList());
            
            System.out.println("DEBUG: Final filtered test count: " + filteredTests.size());
            return filteredTests;
            
        } catch (Exception e) {
            System.err.println("DEBUG: Exception in filtering: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Validates test case data
     */
    private void validateTestCase(TestCase testCase) {
        if (testCase.getName() == null || testCase.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Test case name cannot be empty");
        }
        
        if (testCase.getType() == null) {
            throw new IllegalArgumentException("Test case type must be specified (UI or API)");
        }
        
        if (testCase.getFunctionality() == null || testCase.getFunctionality().trim().isEmpty()) {
            throw new IllegalArgumentException("Test case functionality cannot be empty");
        }
        
        // Validate timeout
        if (testCase.getTimeout() != null && testCase.getTimeout() <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        
        // Validate retry count
        if (testCase.getRetryCount() != null && testCase.getRetryCount() < 0) {
            throw new IllegalArgumentException("Retry count cannot be negative");
        }
    }

    /**
     * Sets default values for test case
     */
    private void setDefaultValues(TestCase testCase) {
        if (testCase.getPriority() == null) {
            testCase.setPriority(TestCase.Priority.MEDIUM);
        }
        
        if (testCase.getStatus() == null) {
            testCase.setStatus(TestCase.Status.READY);
        }
        
        if (testCase.getTimeout() == null) {
            testCase.setTimeout(300); // 5 minutes default
        }
        
        if (testCase.getRetryCount() == null) {
            testCase.setRetryCount(0);
        }
        
        if (testCase.getIsActive() == null) {
            testCase.setIsActive(true);
        }
        
        if (testCase.getExecutionMode() == null) {
            testCase.setExecutionMode("PARALLEL");
        }
        
        if (testCase.getCategory() == null) {
            testCase.setCategory(testCase.getType().toString());
        }
    }

    /**
     * Validates test data JSON format
     */
    private void validateTestData(String testData, TestCase.Type type) {
        try {
            Map<String, Object> data = objectMapper.readValue(testData, Map.class);
            
            if (type == TestCase.Type.API) {
                validateApiTestData(data);
            } else if (type == TestCase.Type.UI) {
                validateUiTestData(data);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid test data JSON format: " + e.getMessage());
        }
    }

    /**
     * Validates API test data
     */
    private void validateApiTestData(Map<String, Object> data) {
        // Add API-specific validation logic
        if (!data.containsKey("endpoint") && !data.containsKey("url")) {
            throw new IllegalArgumentException("API test data must contain 'endpoint' or 'url'");
        }
    }

    /**
     * Validates UI test data
     */
    private void validateUiTestData(Map<String, Object> data) {
        // Add UI-specific validation logic
        if (!data.containsKey("pageUrl") && !data.containsKey("elements")) {
            throw new IllegalArgumentException("UI test data should contain 'pageUrl' or 'elements'");
        }
    }

    /**
     * Generates unique suite ID
     */
    private String generateSuiteId(String suiteName) {
        String cleanName = suiteName.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        String timestamp = String.valueOf(System.currentTimeMillis());
        return "suite_" + cleanName + "_" + timestamp;
    }

    /**
     * Updates suite statistics (placeholder for future TestSuite entity integration)
     */
    private void updateSuiteStatistics(String suiteId) {
        // This will be implemented when TestSuite repository is created
        // For now, just log the update
        System.out.println("Suite statistics updated for suite: " + suiteId);
    }

    /**
     * Gets test integration summary
     */
    public Map<String, Object> getIntegrationSummary() {
        List<TestCase> allTests = testService.getAllTestCases();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalTests", allTests.size());
        
        // Add null checks to prevent NullPointerException
        summary.put("activeTests", allTests.stream()
            .filter(tc -> tc.getIsActive() != null && tc.getIsActive())
            .count());
        
        summary.put("uiTests", allTests.stream()
            .filter(tc -> tc.getType() != null && tc.getType() == TestCase.Type.UI)
            .count());
        
        summary.put("apiTests", allTests.stream()
            .filter(tc -> tc.getType() != null && tc.getType() == TestCase.Type.API)
            .count());
        
        summary.put("readyTests", allTests.stream()
            .filter(tc -> tc.getStatus() != null && tc.getStatus() == TestCase.Status.READY)
            .count());
        
        summary.put("completedTests", allTests.stream()
            .filter(tc -> tc.getStatus() != null && tc.getStatus() == TestCase.Status.COMPLETED)
            .count());
        
        // Group by suites - add null check for testSuiteId
        Map<String, Long> suiteStats = allTests.stream()
                .filter(tc -> tc.getTestSuiteId() != null && !tc.getTestSuiteId().trim().isEmpty())
                .collect(Collectors.groupingBy(TestCase::getTestSuiteId, Collectors.counting()));
        summary.put("suiteStats", suiteStats);
        
        return summary;
    }
}