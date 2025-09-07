package com.example.testframework.controller;

import com.example.testframework.dto.TestCaseDTO;
import com.example.testframework.model.TestCase;
import com.example.testframework.model.TestResult;
import com.example.testframework.service.TestService;
import com.example.testframework.service.TestIntegrationService;
import com.example.testframework.service.TestCaseValidator;
import com.example.testframework.service.TestDataManager;
import com.example.testframework.service.TestEnvironmentManager;
import com.example.testframework.service.ExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tests")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    private final TestService testService;
    private final TestIntegrationService testIntegrationService;
    private final TestCaseValidator testCaseValidator;
    private final TestDataManager testDataManager;
    private final TestEnvironmentManager testEnvironmentManager;
    private final ExecutionService executionService;

    @Autowired
    public TestController(TestService testService, 
                         TestIntegrationService testIntegrationService,
                         TestCaseValidator testCaseValidator,
                         TestDataManager testDataManager,
                         TestEnvironmentManager testEnvironmentManager,
                         ExecutionService executionService) {
        this.testService = testService;
        this.testIntegrationService = testIntegrationService;
        this.testCaseValidator = testCaseValidator;
        this.testDataManager = testDataManager;
        this.testEnvironmentManager = testEnvironmentManager;
        this.executionService = executionService;
    }

    // ===========================
    // INTEGRATE TEST CASE (NEW)
    // ===========================
    @PostMapping("/integrate")
    public ResponseEntity<?> integrateTestCase(@RequestBody IntegrateTestRequest request) {
        try {
            // Create test case from request
            TestCase testCase = createTestCaseFromRequest(request);
            
            // Validate test case
            TestCaseValidator.ValidationResult validation = testCaseValidator.validateTestCase(testCase);
            if (!validation.isValid()) {
                return ResponseEntity.badRequest()
                    .body(new IntegrateResponse(false, "Validation failed: " + validation.getMessage(), 
                                              null, validation.getErrors(), validation.getWarnings()));
            }
            
            // Integrate test case
            TestCase integratedTestCase = testIntegrationService.integrateTestCase(testCase);
            
            // Return success response
            return ResponseEntity.ok(new IntegrateResponse(
                true, 
                "Test case integrated successfully", 
                new TestCaseDTO(integratedTestCase),
                List.of(),
                validation.getWarnings()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new IntegrateResponse(false, "Integration failed: " + e.getMessage(), 
                                          null, List.of(e.getMessage()), List.of()));
        }
    }

    // ===========================
    // INTEGRATE MULTIPLE TEST CASES
    // ===========================
    @PostMapping("/integrate/batch")
    public ResponseEntity<?> integrateTestCases(@RequestBody IntegrateBatchRequest request) {
        try {
            // Create test cases from request
            List<TestCase> testCases = request.getTestCases().stream()
                .map(this::createTestCaseFromRequest)
                .collect(Collectors.toList());
            
            // Validate all test cases
            Map<Long, TestCaseValidator.ValidationResult> validations = 
                testCaseValidator.validateTestCases(testCases);
            
            List<String> allErrors = validations.values().stream()
                .flatMap(v -> v.getErrors().stream())
                .collect(Collectors.toList());
                
            if (!allErrors.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new IntegrateBatchResponse(false, "Validation failed", null, allErrors));
            }
            
            // Integrate test cases
            List<TestCase> integratedTestCases = testIntegrationService.integrateTestCases(testCases);
            
            List<TestCaseDTO> dtoList = integratedTestCases.stream()
                .map(TestCaseDTO::new)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(new IntegrateBatchResponse(
                true, 
                "Test cases integrated successfully", 
                dtoList,
                List.of()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new IntegrateBatchResponse(false, "Batch integration failed: " + e.getMessage(), 
                                               null, List.of(e.getMessage())));
        }
    }

    // ===========================
    // CREATE TEST SUITE WITH TESTS
    // ===========================
    @PostMapping("/integrate/suite")
    public ResponseEntity<?> createTestSuiteWithTests(@RequestBody CreateSuiteRequest request) {
        try {
            // Create test cases
            List<TestCase> testCases = request.getTestCases().stream()
                .map(this::createTestCaseFromRequest)
                .collect(Collectors.toList());
            
            // Create suite with tests
            Map<String, Object> suiteResult = testIntegrationService.createTestSuiteWithTests(
                request.getSuiteName(),
                request.getDescription(),
                testCases,
                request.getEnvironment()
            );
            
            return ResponseEntity.ok(new ApiResponse("Test suite created successfully", suiteResult));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Suite creation failed: " + e.getMessage()));
        }
    }
    @PostMapping
    public ResponseEntity<TestCaseDTO> createTestCase(@RequestBody TestCase testCase) {
        TestCase created = testService.createTestCase(testCase);
        return ResponseEntity.ok(new TestCaseDTO(created));
    }

    // ===========================
    // CREATE MULTIPLE TEST CASES
    // ===========================
    @PostMapping("/batch")
    public ResponseEntity<List<TestCaseDTO>> createTestCases(@RequestBody List<TestCase> testCases) {
        List<TestCase> createdTests = testService.createTestCases(testCases);
        List<TestCaseDTO> dtoList = createdTests.stream()
                .map(TestCaseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    // ===========================
    // GET ALL TEST CASES
    // ===========================
    @GetMapping
    public ResponseEntity<List<TestCaseDTO>> getAllTestCases() {
        List<TestCase> tests = testService.getAllTestCases();
        List<TestCaseDTO> dtoList = tests.stream()
                .map(TestCaseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    // ===========================
    // GET TEST CASE BY ID (ENHANCED)
    // ===========================
    @GetMapping("/{id}")
    public ResponseEntity<?> getTestCaseById(@PathVariable Long id) {
        Optional<TestCase> testCaseOpt = testService.getTestCaseById(id);
        if (testCaseOpt.isPresent()) {
            TestCase testCase = testCaseOpt.get();
            return ResponseEntity.ok(createDetailedTestCaseResponse(testCase));
        } else {
            return ResponseEntity.status(404).body(new ErrorResponse("Test case with ID " + id + " not found"));
        }
    }

    // ===========================
    // GET TEST CASE WITH VALIDATION
    // ===========================
    @GetMapping("/{id}/detailed")
    public ResponseEntity<?> getDetailedTestCaseById(@PathVariable Long id,
                                                    @RequestParam(required = false) String environment) {
        Optional<TestCase> testCaseOpt = testService.getTestCaseById(id);
        if (testCaseOpt.isPresent()) {
            TestCase testCase = testCaseOpt.get();
            
            // Perform validation
            TestCaseValidator.ValidationResult validation;
            if (environment != null) {
                validation = testCaseValidator.validateForEnvironment(testCase, environment);
            } else {
                validation = testCaseValidator.validateTestCase(testCase);
            }
            
            DetailedTestCaseResponse response = createDetailedTestCaseResponse(testCase);
            response.setValidation(validation);
            response.setExecutionReady(testCaseValidator.isValidForExecution(testCase));
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(404).body(new ErrorResponse("Test case with ID " + id + " not found"));
        }
    }

    // ===========================
    // GET TEST CASE EXECUTION HISTORY
    // ===========================
    @GetMapping("/{id}/history")
    public ResponseEntity<?> getTestCaseHistory(@PathVariable Long id) {
        try {
            List<TestResult> results = testService.getResultsByTestCaseId(id);
            return ResponseEntity.ok(new TestHistoryResponse(id, results));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(404).body(new ErrorResponse(ex.getMessage()));
        }
    }

    // ===========================
    // GET TEST DATA TEMPLATES
    // ===========================
    @GetMapping("/{id}/data-templates")
    public ResponseEntity<?> getTestDataTemplates(@PathVariable Long id) {
        Optional<TestCase> testCaseOpt = testService.getTestCaseById(id);
        if (testCaseOpt.isPresent()) {
            TestCase testCase = testCaseOpt.get();
            
            // Get relevant templates
            String templateKey = testCase.getType().toString().toLowerCase() + "_" + 
                               testCase.getFunctionality().toLowerCase();
            
            Map<String, Object> templates = new HashMap<>();
            
            // Get specific template if exists
            if (testDataManager.getAvailableTemplates().contains(templateKey)) {
                templates.put("specific", testDataManager.getTemplate(templateKey));
            }
            
            // Get generated random data
            String randomData = testDataManager.generateRandomTestData(
                testCase.getFunctionality(), testCase.getType().toString());
            templates.put("random", randomData);
            
            // Get all available templates for this type
            List<String> availableTemplates = testDataManager.getAvailableTemplates().stream()
                .filter(template -> template.startsWith(testCase.getType().toString().toLowerCase()))
                .collect(Collectors.toList());
            templates.put("availableTemplates", availableTemplates);
            
            return ResponseEntity.ok(new ApiResponse("Test data templates retrieved", templates));
        } else {
            return ResponseEntity.status(404).body(new ErrorResponse("Test case with ID " + id + " not found"));
        }
    }

    // ===========================
    // UPDATE TEST CASE
    // ===========================
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTestCase(@PathVariable Long id, @RequestBody TestCase testCase) {
        try {
            TestCase updated = testService.updateTestCase(id, testCase);
            return ResponseEntity.ok(new TestCaseDTO(updated));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(404).body(new ErrorResponse(ex.getMessage()));
        }
    }

    // ===========================
    // DELETE TEST CASE
    // ===========================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTestCase(@PathVariable Long id) {
        try {
            testService.deleteTestCase(id);
            return ResponseEntity.ok(new ApiResponse("Test case deleted successfully", null));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(404).body(new ErrorResponse(ex.getMessage()));
        }
    }

    // ===========================
    // EXECUTE SINGLE TEST CASE
    // ===========================
    @PostMapping("/execute")
    public ResponseEntity<?> executeTestCase(@RequestBody ExecuteTestRequest request) {
        try {
            // Get test case by ID
            Optional<TestCase> testCaseOpt = testService.getTestCaseById(request.getTestCaseId());
            if (testCaseOpt.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(new ErrorResponse("Test case with ID " + request.getTestCaseId() + " not found"));
            }
            
            TestCase testCase = testCaseOpt.get();
            
            // Override timeout and retry count if provided in request
            if (request.getTimeout() != null) {
                testCase.setTimeout(request.getTimeout());
            }
            if (request.getRetryCount() != null) {
                testCase.setRetryCount(request.getRetryCount());
            }
            if (request.getEnvironment() != null) {
                testCase.setTestEnvironment(request.getEnvironment());
            }
            
            // Execute single test case
            String runId = executionService.executeTests(
                List.of(testCase), 
                false, // Single test, no parallel execution needed
                1      // Single thread
            );
            
            return ResponseEntity.ok(new ExecuteTestResponse(
                true,
                "Test case execution started successfully",
                runId,
                testCase.getId()
            ));
            
        } catch (Exception e) {
            logger.error("Failed to execute test case: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Execution failed: " + e.getMessage()));
        }
    }

    // ===========================
    // VALIDATE TEST CASE
    // ===========================
    @PostMapping("/validate")
    public ResponseEntity<?> validateTestCase(@RequestBody TestCase testCase) {
        try {
            TestCaseValidator.ValidationResult validation = testCaseValidator.validateTestCase(testCase);
            
            Map<String, Object> response = new HashMap<>();
            response.put("valid", validation.isValid());
            response.put("message", validation.getMessage());
            response.put("errors", validation.getErrors());
            response.put("warnings", validation.getWarnings());
            
            return ResponseEntity.ok(new ApiResponse("Validation completed", response));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Validation failed: " + e.getMessage()));
        }
    }

    // ===========================
    // GET TEST CASE STATUS
    // ===========================
    @GetMapping("/{id}/status")
    public ResponseEntity<?> getTestCaseStatus(@PathVariable Long id) {
        try {
            Optional<TestCase> testCaseOpt = testService.getTestCaseById(id);
            if (testCaseOpt.isEmpty()) {
                return ResponseEntity.status(404)
                    .body(new ErrorResponse("Test case with ID " + id + " not found"));
            }
            
            TestCase testCase = testCaseOpt.get();
            
            Map<String, Object> status = new HashMap<>();
            status.put("id", testCase.getId());
            status.put("name", testCase.getName());
            status.put("status", testCase.getStatus().toString());
            status.put("isActive", testCase.getIsActive());
            status.put("lastExecuted", testCase.getLastExecuted());
            status.put("type", testCase.getType().toString());
            status.put("functionality", testCase.getFunctionality());
            
            return ResponseEntity.ok(new ApiResponse("Test case status retrieved", status));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to get test case status: " + e.getMessage()));
        }
    }

    // ===========================
    // HELPER METHODS
    // ===========================
    private TestCase createTestCaseFromRequest(IntegrateTestRequest request) {
        TestCase testCase = new TestCase();
        testCase.setName(request.getName());
        testCase.setType(TestCase.Type.valueOf(request.getType().toUpperCase()));
        testCase.setDescription(request.getDescription());
        testCase.setFunctionality(request.getFunctionality());
        testCase.setCategory(request.getCategory());
        testCase.setTestSuiteId(request.getTestSuiteId());
        testCase.setTestEnvironment(request.getEnvironment());
        testCase.setExpectedResult(request.getExpectedResult());
        testCase.setTags(request.getTags());
        testCase.setPrerequisites(request.getPrerequisites());
        
        // Set priority
        if (request.getPriority() != null) {
            testCase.setPriority(TestCase.Priority.valueOf(request.getPriority().toUpperCase()));
        }
        
        // Set timeout and retry count
        testCase.setTimeout(request.getTimeout());
        testCase.setRetryCount(request.getRetryCount());
        testCase.setExecutionMode(request.getExecutionMode());
        
        // Generate or use provided test data
        if (request.getTestData() != null) {
            testCase.setTestData(request.getTestData());
        } else if (request.isGenerateTestData()) {
            String generatedData = testDataManager.generateRandomTestData(
                request.getFunctionality(), request.getType());
            testCase.setTestData(generatedData);
        }
        
        return testCase;
    }

    private DetailedTestCaseResponse createDetailedTestCaseResponse(TestCase testCase) {
        DetailedTestCaseResponse response = new DetailedTestCaseResponse();
        response.setTestCase(new TestCaseDTO(testCase));
        
        // Get execution history
        try {
            List<TestResult> results = testService.getResultsByTestCaseId(testCase.getId());
            response.setExecutionHistory(results.stream().limit(5).collect(Collectors.toList())); // Last 5 executions
            response.setTotalExecutions(results.size());
            
            // Calculate success rate
            long successCount = results.stream()
                .filter(r -> "PASSED".equalsIgnoreCase(r.getStatus()))
                .count();
            response.setSuccessRate(results.isEmpty() ? 0.0 : (double) successCount / results.size() * 100);
            
        } catch (Exception e) {
            response.setExecutionHistory(List.of());
            response.setTotalExecutions(0);
            response.setSuccessRate(0.0);
        }
        
        // Get integration summary - add try-catch to handle any null pointer issues
        try {
            response.setIntegrationSummary(testIntegrationService.getIntegrationSummary());
        } catch (Exception e) {
            logger.warn("Failed to get integration summary: {}", e.getMessage());
            // Set a default empty summary if there's an error
            Map<String, Object> defaultSummary = new HashMap<>();
            defaultSummary.put("totalTests", 0);
            defaultSummary.put("activeTests", 0);
            defaultSummary.put("uiTests", 0);
            defaultSummary.put("apiTests", 0);
            defaultSummary.put("readyTests", 0);
            defaultSummary.put("completedTests", 0);
            defaultSummary.put("suiteStats", new HashMap<>());
            response.setIntegrationSummary(defaultSummary);
        }
        
        // Check if test data template exists - add null checks
        try {
            if (testCase.getType() != null && testCase.getFunctionality() != null) {
                String templateKey = testCase.getType().toString().toLowerCase() + "_" + 
                                   testCase.getFunctionality().toLowerCase();
                response.setHasDataTemplate(testDataManager.getAvailableTemplates().contains(templateKey));
            } else {
                response.setHasDataTemplate(false);
            }
        } catch (Exception e) {
            response.setHasDataTemplate(false);
        }
        
        // Environment information - add null check
        try {
            if (testCase.getTestEnvironment() != null && !testCase.getTestEnvironment().trim().isEmpty()) {
                response.setEnvironmentValid(
                    testEnvironmentManager.validateEnvironment(testCase.getTestEnvironment()).isValid());
            } else {
                response.setEnvironmentValid(true); // Default to valid if no environment specified
            }
        } catch (Exception e) {
            response.setEnvironmentValid(false);
        }
        
        return response;
    }
    // ===========================
    // REQUEST CLASSES
    // ===========================
    public static class IntegrateTestRequest {
        private String name;
        private String type;
        private String description;
        private String functionality;
        private String category;
        private String testSuiteId;
        private String environment;
        private String expectedResult;
        private String tags;
        private String prerequisites;
        private String priority;
        private Integer timeout;
        private Integer retryCount;
        private String executionMode;
        private String testData;
        private boolean generateTestData = false;

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
        public String getTestSuiteId() { return testSuiteId; }
        public void setTestSuiteId(String testSuiteId) { this.testSuiteId = testSuiteId; }
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
        public boolean isGenerateTestData() { return generateTestData; }
        public void setGenerateTestData(boolean generateTestData) { this.generateTestData = generateTestData; }
    }

    public static class IntegrateBatchRequest {
        private List<IntegrateTestRequest> testCases;

        public List<IntegrateTestRequest> getTestCases() { return testCases; }
        public void setTestCases(List<IntegrateTestRequest> testCases) { this.testCases = testCases; }
    }

    public static class CreateSuiteRequest {
        private String suiteName;
        private String description;
        private String environment;
        private List<IntegrateTestRequest> testCases;

        public String getSuiteName() { return suiteName; }
        public void setSuiteName(String suiteName) { this.suiteName = suiteName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public List<IntegrateTestRequest> getTestCases() { return testCases; }
        public void setTestCases(List<IntegrateTestRequest> testCases) { this.testCases = testCases; }
    }

    public static class ExecuteTestRequest {
        private Long testCaseId;
        private String environment;
        private Integer timeout;
        private Integer retryCount;

        public Long getTestCaseId() { return testCaseId; }
        public void setTestCaseId(Long testCaseId) { this.testCaseId = testCaseId; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public Integer getTimeout() { return timeout; }
        public void setTimeout(Integer timeout) { this.timeout = timeout; }
        public Integer getRetryCount() { return retryCount; }
        public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    }

    // ===========================
    // RESPONSE CLASSES
    // ===========================
    public static class IntegrateResponse {
        private final boolean success;
        private final String message;
        private final TestCaseDTO testCase;
        private final List<String> errors;
        private final List<String> warnings;

        public IntegrateResponse(boolean success, String message, TestCaseDTO testCase, 
                               List<String> errors, List<String> warnings) {
            this.success = success;
            this.message = message;
            this.testCase = testCase;
            this.errors = errors;
            this.warnings = warnings;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public TestCaseDTO getTestCase() { return testCase; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
    }

    public static class IntegrateBatchResponse {
        private final boolean success;
        private final String message;
        private final List<TestCaseDTO> testCases;
        private final List<String> errors;

        public IntegrateBatchResponse(boolean success, String message, 
                                    List<TestCaseDTO> testCases, List<String> errors) {
            this.success = success;
            this.message = message;
            this.testCases = testCases;
            this.errors = errors;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public List<TestCaseDTO> getTestCases() { return testCases; }
        public List<String> getErrors() { return errors; }
    }

    public static class ExecuteTestResponse {
        private final boolean success;
        private final String message;
        private final String runId;
        private final Long testCaseId;

        public ExecuteTestResponse(boolean success, String message, String runId, Long testCaseId) {
            this.success = success;
            this.message = message;
            this.runId = runId;
            this.testCaseId = testCaseId;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getRunId() { return runId; }
        public Long getTestCaseId() { return testCaseId; }
    }

    public static class DetailedTestCaseResponse {
        private TestCaseDTO testCase;
        private List<TestResult> executionHistory;
        private int totalExecutions;
        private double successRate;
        private TestCaseValidator.ValidationResult validation;
        private boolean executionReady;
        private Map<String, Object> integrationSummary;
        private boolean hasDataTemplate;
        private boolean environmentValid;

        // Getters and setters
        public TestCaseDTO getTestCase() { return testCase; }
        public void setTestCase(TestCaseDTO testCase) { this.testCase = testCase; }
        public List<TestResult> getExecutionHistory() { return executionHistory; }
        public void setExecutionHistory(List<TestResult> executionHistory) { this.executionHistory = executionHistory; }
        public int getTotalExecutions() { return totalExecutions; }
        public void setTotalExecutions(int totalExecutions) { this.totalExecutions = totalExecutions; }
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        public TestCaseValidator.ValidationResult getValidation() { return validation; }
        public void setValidation(TestCaseValidator.ValidationResult validation) { this.validation = validation; }
        public boolean isExecutionReady() { return executionReady; }
        public void setExecutionReady(boolean executionReady) { this.executionReady = executionReady; }
        public Map<String, Object> getIntegrationSummary() { return integrationSummary; }
        public void setIntegrationSummary(Map<String, Object> integrationSummary) { this.integrationSummary = integrationSummary; }
        public boolean isHasDataTemplate() { return hasDataTemplate; }
        public void setHasDataTemplate(boolean hasDataTemplate) { this.hasDataTemplate = hasDataTemplate; }
        public boolean isEnvironmentValid() { return environmentValid; }
        public void setEnvironmentValid(boolean environmentValid) { this.environmentValid = environmentValid; }
    }

    public static class TestHistoryResponse {
        private final Long testCaseId;
        private final List<TestResult> executionHistory;
        private final int totalExecutions;
        private final long passedCount;
        private final long failedCount;
        private final double successRate;
        private final TestResult lastExecution;

        public TestHistoryResponse(Long testCaseId, List<TestResult> results) {
            this.testCaseId = testCaseId;
            this.executionHistory = results;
            this.totalExecutions = results.size();
            this.passedCount = results.stream().filter(r -> "PASSED".equalsIgnoreCase(r.getStatus())).count();
            this.failedCount = results.stream().filter(r -> "FAILED".equalsIgnoreCase(r.getStatus())).count();
            this.successRate = totalExecutions > 0 ? (double) passedCount / totalExecutions * 100 : 0.0;
            this.lastExecution = results.stream()
                .max((r1, r2) -> r1.getExecutedAt().compareTo(r2.getExecutedAt()))
                .orElse(null);
        }

        public Long getTestCaseId() { return testCaseId; }
        public List<TestResult> getExecutionHistory() { return executionHistory; }
        public int getTotalExecutions() { return totalExecutions; }
        public long getPassedCount() { return passedCount; }
        public long getFailedCount() { return failedCount; }
        public double getSuccessRate() { return successRate; }
        public TestResult getLastExecution() { return lastExecution; }
    }

    // ===========================
    // HELPER RESPONSE CLASSES
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
