package com.example.testframework.controller;

import com.example.testframework.dto.TestCaseDTO;
import com.example.testframework.model.TestCase;
import com.example.testframework.model.TestSuite;
import com.example.testframework.service.TestService;
import com.example.testframework.service.TestIntegrationService;
import com.example.testframework.service.TestCaseValidator;
import com.example.testframework.service.ExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/suites")
public class TestSuiteController {

    private final TestService testService;
    private final TestIntegrationService testIntegrationService;
    private final TestCaseValidator testCaseValidator;
    private final ExecutionService executionService;

    @Autowired
    public TestSuiteController(TestService testService,
                              TestIntegrationService testIntegrationService,
                              TestCaseValidator testCaseValidator,
                              ExecutionService executionService) {
        this.testService = testService;
        this.testIntegrationService = testIntegrationService;
        this.testCaseValidator = testCaseValidator;
        this.executionService = executionService;
    }

    // ===========================
    // CREATE TEST SUITE
    // ===========================
    @PostMapping
    public ResponseEntity<?> createTestSuite(@RequestBody CreateSuiteRequest request) {
        try {
            // Validate request
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Suite name is required"));
            }

            // Create test suite with tests if provided
            if (request.getTestCases() != null && !request.getTestCases().isEmpty()) {
                List<TestCase> testCases = request.getTestCases().stream()
                    .map(this::createTestCaseFromRequest)
                    .collect(Collectors.toList());

                Map<String, Object> suiteResult = testIntegrationService.createTestSuiteWithTests(
                    request.getName(),
                    request.getDescription(),
                    testCases,
                    request.getEnvironment()
                );

                return ResponseEntity.ok(new ApiResponse("Test suite created successfully", suiteResult));
            } else {
                // Create empty suite
                String suiteId = generateSuiteId(request.getName());
                Map<String, Object> suiteInfo = createSuiteInfo(
                    suiteId, request.getName(), request.getDescription(), 
                    request.getEnvironment(), new ArrayList<>()
                );
                
                return ResponseEntity.ok(new ApiResponse("Empty test suite created successfully", suiteInfo));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to create test suite: " + e.getMessage()));
        }
    }

    // ===========================
    // GET ALL TEST SUITES
    // ===========================
    @GetMapping
    public ResponseEntity<?> getAllTestSuites() {
        try {
            List<TestCase> allTests = testService.getAllTestCases();
            
            // Group tests by suite ID
            Map<String, List<TestCase>> suiteGroups = allTests.stream()
                .filter(tc -> tc.getTestSuiteId() != null)
                .collect(Collectors.groupingBy(TestCase::getTestSuiteId));

            List<Map<String, Object>> suites = suiteGroups.entrySet().stream()
                .map(entry -> {
                    String suiteId = entry.getKey();
                    List<TestCase> tests = entry.getValue();
                    return createSuiteSummary(suiteId, tests);
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse("Test suites retrieved successfully", suites));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to retrieve test suites: " + e.getMessage()));
        }
    }

    // ===========================
    // GET TEST SUITE BY ID
    // ===========================
    @GetMapping("/{suiteId}")
    public ResponseEntity<?> getTestSuiteById(@PathVariable String suiteId) {
        try {
            List<TestCase> suiteTests = testIntegrationService.getTestCasesBySuite(suiteId, null, null, null);
            
            if (suiteTests.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(new ErrorResponse("Test suite with ID " + suiteId + " not found"));
            }

            Map<String, Object> suiteDetails = createDetailedSuiteInfo(suiteId, suiteTests);
            return ResponseEntity.ok(new ApiResponse("Test suite retrieved successfully", suiteDetails));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to retrieve test suite: " + e.getMessage()));
        }
    }

    // ===========================
    // GET SUITE TESTS WITH FILTERS
    // ===========================
    @GetMapping("/{suiteId}/tests")
    public ResponseEntity<?> getSuiteTests(@PathVariable String suiteId,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) String type,
                                          @RequestParam(required = false) String priority) {
        try {
            // Add debug logging
            System.out.println("Getting suite tests for suiteId: " + suiteId);
            
            List<TestCase> tests = testIntegrationService.getTestCasesBySuite(suiteId, status, type, priority);
            System.out.println("Found " + tests.size() + " tests for suite: " + suiteId);
            
            if (tests == null) {
                tests = new ArrayList<>();
            }
            
            List<TestCaseDTO> testDTOs = new ArrayList<>();
            for (TestCase tc : tests) {
                try {
                    if (tc != null) {
                        TestCaseDTO dto = new TestCaseDTO(tc);
                        if (dto != null) {
                            testDTOs.add(dto);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error creating DTO for test case ID: " + 
                        (tc != null ? tc.getId() : "null") + ", Error: " + e.getMessage());
                    e.printStackTrace();
                    // Continue with other test cases
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("suiteId", suiteId);
            response.put("totalTests", testDTOs.size());
            
            Map<String, Object> filters = new HashMap<>();
            filters.put("status", status);
            filters.put("type", type);
            filters.put("priority", priority);
            response.put("filters", filters);
            
            response.put("tests", testDTOs);

            return ResponseEntity.ok(new ApiResponse("Suite tests retrieved successfully", response));
        } catch (Exception e) {
            System.err.println("Error in getSuiteTests for suiteId: " + suiteId);
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to retrieve suite tests: " + 
                    (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
        }
    }

    // ===========================
    // ADD TESTS TO SUITE
    // ===========================
    @PostMapping("/{suiteId}/tests")
    public ResponseEntity<?> addTestsToSuite(@PathVariable String suiteId,
                                            @RequestBody AddTestsToSuiteRequest request) {
        try {
            List<TestCase> newTests = request.getTestCases().stream()
                .map(this::createTestCaseFromRequest)
                .peek(tc -> tc.setTestSuiteId(suiteId))
                .collect(Collectors.toList());

            // Validate all tests
            Map<Long, TestCaseValidator.ValidationResult> validations = 
                testCaseValidator.validateTestCases(newTests);
            
            List<String> allErrors = validations.values().stream()
                .flatMap(v -> v.getErrors().stream())
                .collect(Collectors.toList());
                
            if (!allErrors.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Validation failed: " + String.join("; ", allErrors)));
            }

            // Integrate tests
            List<TestCase> integratedTests = testIntegrationService.integrateTestCases(newTests);
            
            List<TestCaseDTO> testDTOs = integratedTests.stream()
                .map(TestCaseDTO::new)
                .collect(Collectors.toList());

            return ResponseEntity.ok(new ApiResponse("Tests added to suite successfully", 
                Map.of("suiteId", suiteId, "addedTests", testDTOs.size(), "tests", testDTOs)));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to add tests to suite: " + e.getMessage()));
        }
    }

    // ===========================
    // UPDATE SUITE INFORMATION
    // ===========================
    @PutMapping("/{suiteId}")
    public ResponseEntity<?> updateTestSuite(@PathVariable String suiteId,
                                            @RequestBody UpdateSuiteRequest request) {
        try {
            List<TestCase> suiteTests = testIntegrationService.getTestCasesBySuite(suiteId, null, null, null);
            
            if (suiteTests.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(new ErrorResponse("Test suite with ID " + suiteId + " not found"));
            }

            // Update suite properties on all tests
            for (TestCase testCase : suiteTests) {
                if (request.getEnvironment() != null) {
                    testCase.setTestEnvironment(request.getEnvironment());
                }
                if (request.getTags() != null && !request.getTags().isEmpty()) {
                    // Convert List<String> to comma-separated String
                    String tagsString = String.join(",", request.getTags());
                    testCase.setTags(tagsString);
                }
                testService.updateTestCase(testCase.getId(), testCase);
            }

            Map<String, Object> updatedSuite = createDetailedSuiteInfo(suiteId, suiteTests);
            return ResponseEntity.ok(new ApiResponse("Test suite updated successfully", updatedSuite));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to update test suite: " + e.getMessage()));
        }
    }

    // ===========================
    // DELETE SUITE
    // ===========================
    @DeleteMapping("/{suiteId}")
    public ResponseEntity<?> deleteTestSuite(@PathVariable String suiteId,
                                            @RequestParam(defaultValue = "false") boolean deleteTests) {
        try {
            List<TestCase> suiteTests = testIntegrationService.getTestCasesBySuite(suiteId, null, null, null);
            
            if (suiteTests.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(new ErrorResponse("Test suite with ID " + suiteId + " not found"));
            }

            if (deleteTests) {
                // Delete all tests in the suite
                for (TestCase testCase : suiteTests) {
                    testService.deleteTestCase(testCase.getId());
                }
                return ResponseEntity.ok(new ApiResponse("Test suite and all tests deleted successfully", 
                    Map.of("suiteId", suiteId, "deletedTests", suiteTests.size())));
            } else {
                // Just remove suite ID from tests
                for (TestCase testCase : suiteTests) {
                    testCase.setTestSuiteId(null);
                    testService.updateTestCase(testCase.getId(), testCase);
                }
                return ResponseEntity.ok(new ApiResponse("Test suite disbanded, tests moved to individual status", 
                    Map.of("suiteId", suiteId, "unlinkedTests", suiteTests.size())));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to delete test suite: " + e.getMessage()));
        }
    }

    // ===========================
    // GET SUITE ANALYTICS
    // ===========================
    @GetMapping("/{suiteId}/analytics")
    public ResponseEntity<?> getSuiteAnalytics(@PathVariable String suiteId) {
        try {
            List<TestCase> suiteTests = testIntegrationService.getTestCasesBySuite(suiteId, null, null, null);
            
            if (suiteTests.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(new ErrorResponse("Test suite with ID " + suiteId + " not found"));
            }

            Map<String, Object> analytics = createSuiteAnalytics(suiteId, suiteTests);
            return ResponseEntity.ok(new ApiResponse("Suite analytics retrieved successfully", analytics));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to retrieve suite analytics: " + e.getMessage()));
        }
    }

    // ===========================
    // EXECUTE TEST SUITE
    // ===========================
    @PostMapping("/execute")
    public ResponseEntity<?> executeTestSuite(@RequestBody ExecuteSuiteRequest request) {
        try {
            System.out.println("🚀 Suite execution request received for suiteId: " + request.getSuiteId());
            
            // Validate request
            if (request.getSuiteId() == null || request.getSuiteId().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Suite ID is required"));
            }

            // Get test cases for the suite
            System.out.println("🔍 Getting test cases for suite: " + request.getSuiteId());
            List<TestCase> suiteTests = testIntegrationService.getTestCasesBySuite(request.getSuiteId(), null, null, null);
            System.out.println("📋 Found " + suiteTests.size() + " test cases in suite");
            
            if (suiteTests.isEmpty()) {
                System.out.println("❌ No test cases found for suite: " + request.getSuiteId());
                return ResponseEntity.status(404)
                    .body(new ErrorResponse("Test suite with ID " + request.getSuiteId() + " not found or has no tests"));
            }

            // Filter only active tests
            List<TestCase> activeTests = suiteTests.stream()
                .filter(tc -> tc.getIsActive() != null && tc.getIsActive())
                .collect(Collectors.toList());
            System.out.println("✅ Found " + activeTests.size() + " active test cases");

            if (activeTests.isEmpty()) {
                System.out.println("❌ No active test cases found in suite: " + request.getSuiteId());
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("No active test cases found in suite " + request.getSuiteId()));
            }

            // Override environment if provided
            if (request.getEnvironment() != null) {
                activeTests.forEach(tc -> tc.setTestEnvironment(request.getEnvironment()));
            }

            // Override timeout if provided
            if (request.getTimeout() != null) {
                activeTests.forEach(tc -> tc.setTimeout(request.getTimeout()));
            }

            // Execute the test suite using ExecutionService
            System.out.println("🚀 Starting execution of " + activeTests.size() + " test cases");
            String runId = executionService.executeTests(
                activeTests,
                request.isParallel(),
                request.getThreadPoolSize() != null ? request.getThreadPoolSize() : 3
            );
            System.out.println("✅ Execution started with runId: " + runId);

            Map<String, Object> response = new HashMap<>();
            response.put("runId", runId);
            response.put("suiteId", request.getSuiteId());
            response.put("totalTests", activeTests.size());
            response.put("parallel", request.isParallel());
            response.put("threadPoolSize", request.getThreadPoolSize() != null ? request.getThreadPoolSize() : 3);
            response.put("environment", request.getEnvironment());

            return ResponseEntity.ok(new ApiResponse("Test suite execution started successfully", response));
        } catch (Exception e) {
            System.err.println("❌ Failed to execute test suite: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to execute test suite: " + e.getMessage()));
        }
    }

    // ===========================
    // HELPER METHODS
    // ===========================
    private TestCase createTestCaseFromRequest(TestCaseRequest request) {
        TestCase testCase = new TestCase();
        testCase.setName(request.getName());
        testCase.setType(TestCase.Type.valueOf(request.getType().toUpperCase()));
        testCase.setDescription(request.getDescription());
        testCase.setFunctionality(request.getFunctionality());
        testCase.setCategory(request.getCategory());
        testCase.setTestEnvironment(request.getEnvironment());
        testCase.setExpectedResult(request.getExpectedResult());
        testCase.setTags(request.getTags());
        testCase.setPrerequisites(request.getPrerequisites());
        testCase.setTestData(request.getTestData());
        
        if (request.getPriority() != null) {
            testCase.setPriority(TestCase.Priority.valueOf(request.getPriority().toUpperCase()));
        }
        
        testCase.setTimeout(request.getTimeout());
        testCase.setRetryCount(request.getRetryCount());
        testCase.setExecutionMode(request.getExecutionMode());
        
        return testCase;
    }

    private String generateSuiteId(String suiteName) {
        String cleanName = suiteName.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
        String timestamp = String.valueOf(System.currentTimeMillis());
        return "suite_" + cleanName + "_" + timestamp;
    }

    private Map<String, Object> createSuiteInfo(String suiteId, String name, String description, 
                                               String environment, List<TestCase> tests) {
        Map<String, Object> suiteInfo = new HashMap<>();
        suiteInfo.put("suiteId", suiteId);
        suiteInfo.put("name", name);
        suiteInfo.put("description", description);
        suiteInfo.put("environment", environment);
        suiteInfo.put("totalTests", tests.size());
        suiteInfo.put("createdAt", LocalDateTime.now());
        return suiteInfo;
    }

    private Map<String, Object> createSuiteSummary(String suiteId, List<TestCase> tests) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("suiteId", suiteId);
        summary.put("totalTests", tests.size());
        summary.put("activeTests", tests.stream().filter(TestCase::getIsActive).count());
        summary.put("readyTests", tests.stream().filter(tc -> tc.getStatus() == TestCase.Status.READY).count());
        summary.put("uiTests", tests.stream().filter(tc -> tc.getType() == TestCase.Type.UI).count());
        summary.put("apiTests", tests.stream().filter(tc -> tc.getType() == TestCase.Type.API).count());
        
        // Get common environment
        Set<String> environments = tests.stream()
            .map(TestCase::getTestEnvironment)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        summary.put("environments", environments);
        
        return summary;
    }

    private Map<String, Object> createDetailedSuiteInfo(String suiteId, List<TestCase> tests) {
        Map<String, Object> details = createSuiteSummary(suiteId, tests);
        
        // Add test list
        List<TestCaseDTO> testDTOs = tests.stream()
            .map(TestCaseDTO::new)
            .collect(Collectors.toList());
        details.put("tests", testDTOs);
        
        // Add validation summary
        Map<Long, TestCaseValidator.ValidationResult> validations = 
            testCaseValidator.validateTestCases(tests);
        
        long validTests = validations.values().stream()
            .filter(TestCaseValidator.ValidationResult::isValid)
            .count();
        
        details.put("validTests", validTests);
        details.put("invalidTests", tests.size() - validTests);
        
        return details;
    }

    private Map<String, Object> createSuiteAnalytics(String suiteId, List<TestCase> tests) {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("suiteId", suiteId);
        analytics.put("totalTests", tests.size());
        
        // Type distribution
        Map<String, Long> typeDistribution = tests.stream()
            .collect(Collectors.groupingBy(
                tc -> tc.getType().toString(),
                Collectors.counting()
            ));
        analytics.put("typeDistribution", typeDistribution);
        
        // Priority distribution
        Map<String, Long> priorityDistribution = tests.stream()
            .filter(tc -> tc.getPriority() != null)
            .collect(Collectors.groupingBy(
                tc -> tc.getPriority().toString(),
                Collectors.counting()
            ));
        analytics.put("priorityDistribution", priorityDistribution);
        
        // Status distribution
        Map<String, Long> statusDistribution = tests.stream()
            .collect(Collectors.groupingBy(
                tc -> tc.getStatus().toString(),
                Collectors.counting()
            ));
        analytics.put("statusDistribution", statusDistribution);
        
        // Functionality distribution
        Map<String, Long> functionalityDistribution = tests.stream()
            .collect(Collectors.groupingBy(
                TestCase::getFunctionality,
                Collectors.counting()
            ));
        analytics.put("functionalityDistribution", functionalityDistribution);
        
        return analytics;
    }

    // ===========================
    // REQUEST CLASSES
    // ===========================
    public static class CreateSuiteRequest {
        private String name;
        private String description;
        private String environment;
        private List<TestCaseRequest> testCases;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public List<TestCaseRequest> getTestCases() { return testCases; }
        public void setTestCases(List<TestCaseRequest> testCases) { this.testCases = testCases; }
    }

    public static class TestCaseRequest {
        private String name;
        private String type;
        private String description;
        private String functionality;
        private String category;
        private String environment;
        private String expectedResult;
        private String tags;
        private String prerequisites;
        private String priority;
        private Integer timeout;
        private Integer retryCount;
        private String executionMode;
        private String testData;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getFunctionality() { return functionality; }
        public void setFunctionality(String functionality) { this.functionality = functionality; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public String getExpectedResult() { return expectedResult; }
        public void setExpectedResult(String expectedResult) { this.expectedResult = expectedResult; }
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
        public String getPrerequisites() { return prerequisites; }
        public void setPrerequisites(String prerequisites) { this.prerequisites = prerequisites; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public Integer getTimeout() { return timeout; }
        public void setTimeout(Integer timeout) { this.timeout = timeout; }
        public Integer getRetryCount() { return retryCount; }
        public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
        public String getExecutionMode() { return executionMode; }
        public void setExecutionMode(String executionMode) { this.executionMode = executionMode; }
        public String getTestData() { return testData; }
        public void setTestData(String testData) { this.testData = testData; }
    }

    public static class AddTestsToSuiteRequest {
        private List<TestCaseRequest> testCases;

        public List<TestCaseRequest> getTestCases() { return testCases; }
        public void setTestCases(List<TestCaseRequest> testCases) { this.testCases = testCases; }
    }

    public static class UpdateSuiteRequest {
        private String environment;
        private List<String> tags; // Changed from String to List<String>

        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }

    public static class ExecuteSuiteRequest {
        private String suiteId;
        private String environment;
        private boolean parallel = true;
        private Integer threadPoolSize = 3;
        private Integer timeout;

        public String getSuiteId() { return suiteId; }
        public void setSuiteId(String suiteId) { this.suiteId = suiteId; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public boolean isParallel() { return parallel; }
        public void setParallel(boolean parallel) { this.parallel = parallel; }
        public Integer getThreadPoolSize() { return threadPoolSize; }
        public void setThreadPoolSize(Integer threadPoolSize) { this.threadPoolSize = threadPoolSize; }
        public Integer getTimeout() { return timeout; }
        public void setTimeout(Integer timeout) { this.timeout = timeout; }
    }

    // ===========================
    // RESPONSE CLASSES
    // ===========================
    static class ApiResponse {
        private String message;
        private Object data;

        public ApiResponse(String message, Object data) {
            this.message = message;
            this.data = data;
        }

        public String getMessage() { return message; }
        public Object getData() { return data; }
    }

    static class ErrorResponse {
        private String error;

        public ErrorResponse(String error) { this.error = error; }
        public String getError() { return error; }
    }
}