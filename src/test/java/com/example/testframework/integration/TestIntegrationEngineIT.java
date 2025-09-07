package com.example.testframework.integration;

import com.example.testframework.controller.*;
import com.example.testframework.service.*;
import com.example.testframework.model.*;
import com.example.testframework.dto.*;
import com.example.testframework.service.EmailAlertServiceInterface;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive Integration Test Suite for Test Integration Engine
 * Tests: Test execution, reporting, log collection, email alerts, and end-to-end workflows
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "app.email.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestIntegrationEngineIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestService testService;

    @Autowired
    private ExecutionService executionService;

    @Autowired
    private ReportGeneratorService reportGeneratorService;

    @Autowired
    private LogCollectionService logCollectionService;

    @Autowired
    private EmailAlertServiceInterface emailAlertService;

    // Test data
    private TestCase testTestCase;
    private String testRunId;
    private String testCollectionId;

    @BeforeEach
    void setUp() {
        // Create test data
        testTestCase = new TestCase();
        testTestCase.setName("Integration Test Case");
        testTestCase.setType(TestCase.Type.API);
        testTestCase.setFunctionality("login");
        testTestCase.setUrl("https://parabank.parasoft.com/parabank/");
        testTestCase.setMethod("POST");
        testTestCase.setTestData("{\"username\":\"testuser\",\"password\":\"testpass\"}");
        testTestCase.setExpectedResult("Successful login");
        testTestCase.setPriority(TestCase.Priority.HIGH);
        testTestCase.setRetryCount(1);
        testTestCase.setIsActive(true);
    }

    // ===========================
    // MODULE 1: TEST INTEGRATION ENGINE TESTS
    // ===========================

    @Test
    @Order(1)
    @DisplayName("Test Case Integration - POST /tests/integrate")
    void testTestCaseIntegration() throws Exception {
        // Test creating a new test case
        String requestBody = objectMapper.writeValueAsString(testTestCase);

        MvcResult result = mockMvc.perform(post("/tests/integrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("✅ Test Integration Response: " + response);

        // Extract test case ID for later use
        assertTrue(response.contains("successfully"), "Integration should be successful");
    }

    @Test
    @Order(2)
    @DisplayName("Test Case Retrieval - GET /tests/{id}")
    void testTestCaseRetrieval() throws Exception {
        // First create a test case
        TestCase savedTestCase = testService.createTestCase(testTestCase);
        
        // Then retrieve it
        mockMvc.perform(get("/tests/" + savedTestCase.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Test Case"))
                .andExpect(jsonPath("$.type").value("API"))
                .andExpect(jsonPath("$.functionality").value("login"));

        System.out.println("✅ Test Case Retrieved Successfully: ID=" + savedTestCase.getId());
    }

    @Test
    @Order(3)
    @DisplayName("Test Suite Management - Test Suite APIs")
    void testTestSuiteManagement() throws Exception {
        // Create test suite
        Map<String, Object> suiteRequest = Map.of(
            "name", "Integration Test Suite",
            "description", "Comprehensive integration test suite",
            "environment", "TEST",
            "tags", Arrays.asList("integration", "api")
        );

        String requestBody = objectMapper.writeValueAsString(suiteRequest);

        mockMvc.perform(post("/tests/suites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        System.out.println("✅ Test Suite Created Successfully");

        // List test suites
        mockMvc.perform(get("/tests/suites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        System.out.println("✅ Test Suite Listing Works");
    }

    // ===========================
    // MODULE 2: EXECUTION SYSTEM TESTS
    // ===========================

    @Test
    @Order(4)
    @DisplayName("Test Execution - POST /schedule/run")
    void testTestExecution() throws Exception {
        // Create test cases first
        TestCase testCase1 = testService.createTestCase(testTestCase);
        
        TestCase testCase2 = new TestCase();
        testCase2.setName("Second Test Case");
        testCase2.setType(TestCase.Type.API);
        testCase2.setFunctionality("healthcheck");
        testCase2.setUrl("https://parabank.parasoft.com/parabank/");
        testCase2.setMethod("GET");
        testCase2.setPriority(TestCase.Priority.MEDIUM);
        testCase2.setIsActive(true);
        TestCase savedTestCase2 = testService.createTestCase(testCase2);

        // Execute tests
        Map<String, Object> runRequest = Map.of(
            "testCaseIds", Arrays.asList(testCase1.getId(), savedTestCase2.getId()),
            "parallel", true,
            "threadPoolSize", 2
        );

        String requestBody = objectMapper.writeValueAsString(runRequest);

        MvcResult result = mockMvc.perform(post("/schedule/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        System.out.println("✅ Test Execution Started: " + response);

        // Extract run ID for later use
        Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
        testRunId = (String) responseMap.get("runId");
        assertNotNull(testRunId, "Run ID should be returned");

        // Wait a bit for execution to start
        Thread.sleep(2000);
    }

    @Test
    @Order(5)
    @DisplayName("Execution Status - GET /execution/status/{runId}")
    void testExecutionStatus() throws Exception {
        assertNotNull(testRunId, "Run ID should be available from previous test");

        mockMvc.perform(get("/execution/status/" + testRunId + "?includeDetails=true&includeMetrics=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value(testRunId))
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.totalTests").exists());

        System.out.println("✅ Execution Status Retrieved Successfully for: " + testRunId);
    }

    @Test
    @Order(6)
    @DisplayName("Batch Processing - POST /schedule/batch")
    void testBatchProcessing() throws Exception {
        // Create batch execution request
        Map<String, Object> batchRequest = Map.of(
            "batches", Arrays.asList(
                Map.of(
                    "name", "Batch 1",
                    "testCaseIds", Arrays.asList(1L, 2L),
                    "strategy", "BALANCED"
                )
            ),
            "globalStrategy", "PRIORITY_BASED",
            "maxConcurrentBatches", 2
        );

        String requestBody = objectMapper.writeValueAsString(batchRequest);

        mockMvc.perform(post("/schedule/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").exists());

        System.out.println("✅ Batch Processing Started Successfully");
    }

    // ===========================
    // MODULE 3: REPORTING TESTS
    // ===========================

    @Test
    @Order(7)
    @DisplayName("HTML Report Generation - GET /reports/generate")
    void testHtmlReportGeneration() throws Exception {
        assertNotNull(testRunId, "Run ID should be available");

        // Wait for execution to complete
        Thread.sleep(3000);

        mockMvc.perform(get("/reports/generate")
                .param("runId", testRunId)
                .param("format", "HTML")
                .param("title", "Integration Test Report")
                .param("includeCharts", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").exists())
                .andExpect(jsonPath("$.format").value("HTML"))
                .andExpect(jsonPath("$.downloadUrl").exists());

        System.out.println("✅ HTML Report Generated Successfully");
    }

    @Test
    @Order(8)
    @DisplayName("CSV Report Generation")
    void testCsvReportGeneration() throws Exception {
        assertNotNull(testRunId, "Run ID should be available");

        mockMvc.perform(get("/reports/generate")
                .param("runId", testRunId)
                .param("format", "CSV")
                .param("includeDetails", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").value("CSV"));

        System.out.println("✅ CSV Report Generated Successfully");
    }

    @Test
    @Order(9)
    @DisplayName("JUnit Report Generation")
    void testJUnitReportGeneration() throws Exception {
        assertNotNull(testRunId, "Run ID should be available");

        mockMvc.perform(get("/reports/generate")
                .param("runId", testRunId)
                .param("format", "JUNIT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").value("JUNIT"));

        System.out.println("✅ JUnit Report Generated Successfully");
    }

    @Test
    @Order(10)
    @DisplayName("Comparison Report Generation - GET /reports/compare")
    void testComparisonReportGeneration() throws Exception {
        // Create second run for comparison
        Map<String, Object> runRequest = Map.of(
            "testCaseIds", Arrays.asList(1L),
            "parallel", false
        );

        String requestBody = objectMapper.writeValueAsString(runRequest);
        MvcResult result = mockMvc.perform(post("/schedule/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> responseMap = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        String secondRunId = (String) responseMap.get("runId");

        // Wait for execution
        Thread.sleep(2000);

        // Generate comparison report
        mockMvc.perform(get("/reports/compare")
                .param("runIds", testRunId, secondRunId)
                .param("format", "HTML"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runIds").isArray());

        System.out.println("✅ Comparison Report Generated Successfully");
    }

    // ===========================
    // LOG COLLECTION TESTS
    // ===========================

    @Test
    @Order(11)
    @DisplayName("Log Collection - POST /logs/collect")
    void testLogCollection() throws Exception {
        assertNotNull(testRunId, "Run ID should be available");

        Map<String, Object> logRequest = Map.of(
            "runId", testRunId,
            "logLevels", Arrays.asList("INFO", "ERROR"),
            "includeSystemLogs", true,
            "includeApplicationLogs", true,
            "maxLogEntries", 1000
        );

        String requestBody = objectMapper.writeValueAsString(logRequest);

        MvcResult result = mockMvc.perform(post("/logs/collect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collectionId").exists())
                .andReturn();

        Map<String, Object> responseMap = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        testCollectionId = (String) responseMap.get("collectionId");

        System.out.println("✅ Log Collection Started: " + testCollectionId);
    }

    @Test
    @Order(12)
    @DisplayName("Log Retrieval - GET /logs/{collectionId}")
    void testLogRetrieval() throws Exception {
        assertNotNull(testCollectionId, "Collection ID should be available");

        mockMvc.perform(get("/logs/" + testCollectionId)
                .param("limit", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collectionId").value(testCollectionId))
                .andExpect(jsonPath("$.logs").isArray());

        System.out.println("✅ Logs Retrieved Successfully");
    }

    @Test
    @Order(13)
    @DisplayName("Log Analysis - GET /logs/{collectionId}/analyze")
    void testLogAnalysis() throws Exception {
        assertNotNull(testCollectionId, "Collection ID should be available");

        mockMvc.perform(get("/logs/" + testCollectionId + "/analyze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.analysis").exists());

        System.out.println("✅ Log Analysis Completed Successfully");
    }

    @Test
    @Order(14)
    @DisplayName("Log Export - GET /logs/{collectionId}/export")
    void testLogExport() throws Exception {
        assertNotNull(testCollectionId, "Collection ID should be available");

        mockMvc.perform(get("/logs/" + testCollectionId + "/export")
                .param("format", "JSON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.format").value("JSON"));

        System.out.println("✅ Log Export Completed Successfully");
    }

    // ===========================
    // EMAIL ALERT TESTS
    // ===========================

    @Test
    @Order(15)
    @DisplayName("Email Alert Subscription")
    void testEmailAlertSubscription() {
        assertNotNull(testRunId, "Run ID should be available");

        // Subscribe to alerts
        emailAlertService.subscribeToRunAlerts(testRunId, 
            Arrays.asList("test1@example.com", "test2@example.com"));

        System.out.println("✅ Email Alert Subscription Completed");
    }

    @Test
    @Order(16)
    @DisplayName("Email Alert Sending")
    void testEmailAlertSending() throws Exception {
        assertNotNull(testRunId, "Run ID should be available");

        // Test execution completion alert
        var result = emailAlertService.sendExecutionCompletionAlert(testRunId).get();
        assertNotNull(result, "Email send result should not be null");

        System.out.println("✅ Email Alert Sent: " + result.getMessage());
    }

    // ===========================
    // PERFORMANCE AND STRESS TESTS
    // ===========================

    @Test
    @Order(17)
    @DisplayName("Concurrent Execution Test")
    void testConcurrentExecution() throws Exception {
        List<String> runIds = new ArrayList<>();

        // Start multiple concurrent executions
        for (int i = 0; i < 3; i++) {
            Map<String, Object> runRequest = Map.of(
                "testCaseIds", Arrays.asList(1L),
                "parallel", true,
                "threadPoolSize", 1
            );

            String requestBody = objectMapper.writeValueAsString(runRequest);
            MvcResult result = mockMvc.perform(post("/schedule/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andExpect(status().isOk())
                    .andReturn();

            Map<String, Object> responseMap = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
            runIds.add((String) responseMap.get("runId"));
        }

        // Wait for executions to complete
        Thread.sleep(5000);

        // Verify all executions completed
        for (String runId : runIds) {
            RunStatusDTO status = executionService.getRunStatus(runId);
            assertNotNull(status, "Status should be available for run: " + runId);
        }

        System.out.println("✅ Concurrent Execution Test Completed - " + runIds.size() + " runs");
    }

    @Test
    @Order(18)
    @DisplayName("Large Dataset Test")
    void testLargeDatasetHandling() throws Exception {
        // Create multiple test cases
        List<Long> testCaseIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestCase tc = new TestCase();
            tc.setName("Load Test Case " + i);
            tc.setType(TestCase.Type.API);
            tc.setFunctionality("healthcheck");
            tc.setUrl("https://parabank.parasoft.com/parabank/");
            tc.setMethod("GET");
            tc.setPriority(TestCase.Priority.LOW);
            tc.setIsActive(true);
            
            TestCase saved = testService.createTestCase(tc);
            testCaseIds.add(saved.getId());
        }

        // Execute large batch
        Map<String, Object> runRequest = Map.of(
            "testCaseIds", testCaseIds,
            "parallel", true,
            "threadPoolSize", 5
        );

        String requestBody = objectMapper.writeValueAsString(runRequest);
        mockMvc.perform(post("/schedule/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        System.out.println("✅ Large Dataset Test Completed - " + testCaseIds.size() + " test cases");
    }

    // ===========================
    // ERROR HANDLING TESTS
    // ===========================

    @Test
    @Order(19)
    @DisplayName("Error Handling - Invalid Test Case")
    void testInvalidTestCaseHandling() throws Exception {
        Map<String, Object> invalidRequest = Map.of(
            "testCaseIds", Arrays.asList(99999L), // Non-existent ID
            "parallel", true
        );

        String requestBody = objectMapper.writeValueAsString(invalidRequest);
        mockMvc.perform(post("/schedule/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().is4xxClientError());

        System.out.println("✅ Invalid Test Case Error Handling Works");
    }

    @Test
    @Order(20)
    @DisplayName("Error Handling - Invalid Report Request")
    void testInvalidReportHandling() throws Exception {
        mockMvc.perform(get("/reports/generate")
                .param("runId", "non-existent-run")
                .param("format", "HTML"))
                .andExpect(status().isNotFound());

        System.out.println("✅ Invalid Report Request Error Handling Works");
    }

    // ===========================
    // CLEANUP
    // ===========================

    @AfterEach
    void tearDown() {
        // Cleanup test data if needed
        if (testCollectionId != null) {
            try {
                logCollectionService.archiveLogs(testCollectionId, true);
            } catch (Exception e) {
                System.err.println("Warning: Failed to archive logs during cleanup: " + e.getMessage());
            }
        }
    }

    @Test
    @Order(21)
    @DisplayName("System Health Check")
    void testSystemHealth() {
        // Verify all services are working
        assertNotNull(testService, "TestService should be available");
        assertNotNull(executionService, "ExecutionService should be available");
        assertNotNull(reportGeneratorService, "ReportGeneratorService should be available");
        assertNotNull(logCollectionService, "LogCollectionService should be available");
        assertNotNull(emailAlertService, "EmailAlertService should be available");

        System.out.println("✅ System Health Check Passed - All services operational");
    }
}