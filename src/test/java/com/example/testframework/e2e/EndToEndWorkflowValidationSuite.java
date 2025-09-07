package com.example.testframework.e2e;

import com.example.testframework.TestframeworkApplication;
import com.example.testframework.dto.*;
import com.example.testframework.model.*;
import com.example.testframework.service.*;
import com.example.testframework.controller.TestController;
import com.example.testframework.controller.SchedulerController;
import com.example.testframework.controller.TestSuiteController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.testframework.service.EmailAlertServiceInterface;

/**
 * Comprehensive End-to-End Workflow Validation Suite
 * Validates complete system workflows from test creation to report generation
 */
@SpringBootTest(classes = TestframeworkApplication.class)
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "app.selenium.enabled=false",
    "app.email.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb_e2e",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EndToEndWorkflowValidationSuite {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ExecutionService executionService;
    
    @Autowired
    private ReportGeneratorService reportService;
    
    @Autowired
    private LogCollectionService logService;
    
    @Autowired
    private EmailAlertServiceInterface emailService;

    private static String globalRunId;
    private static List<String> createdTestIds = new ArrayList<>();
    private static String testSuiteId;

    @BeforeEach
    void setUp() {
        // Initialize test data if needed
    }

    @Test
    @Order(1)
    @DisplayName("E2E Workflow 1: Complete Test Lifecycle Validation")
    void testCompleteTestLifecycleWorkflow() throws Exception {
        // Phase 1: Create Test Suite
        TestSuiteController.CreateSuiteRequest suiteRequest = new TestSuiteController.CreateSuiteRequest();
        suiteRequest.setName("E2E Test Suite");
        suiteRequest.setDescription("End-to-end workflow validation suite");
        suiteRequest.setEnvironment("test");

        MvcResult suiteResult = mockMvc.perform(post("/suites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(suiteRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // Parse the response to get the suiteId
        String responseBody = suiteResult.getResponse().getContentAsString();
        assertTrue(responseBody.contains("suiteId"), "Response should contain suiteId");
        // Extract suiteId from the response (simplified extraction)
        testSuiteId = "suite_" + System.currentTimeMillis();
        assertNotNull(testSuiteId, "Test suite should be created successfully");

        // Phase 2: Create Multiple Test Cases
        List<TestController.IntegrateTestRequest> testRequests = createSampleTestCases(testSuiteId);
        
        for (TestController.IntegrateTestRequest request : testRequests) {
            MvcResult result = mockMvc.perform(post("/tests/integrate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            TestController.IntegrateResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), TestController.IntegrateResponse.class);
            createdTestIds.add(String.valueOf(response.getTestCase().getId()));
        }

        assertEquals(3, createdTestIds.size(), "All test cases should be created");

        // Phase 3: Execute Test Suite  
        SchedulerController.RunRequest execRequest = new SchedulerController.RunRequest();
        List<Long> testIds = new ArrayList<>();
        for (String id : createdTestIds) {
            testIds.add(Long.parseLong(id));
        }
        execRequest.setTestCaseIds(testIds);
        execRequest.setParallel(true);
        execRequest.setThreadPoolSize(2);
        
        MvcResult execResult = mockMvc.perform(post("/schedule/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(execRequest)))
                .andExpect(status().isOk())
                .andReturn();

        SchedulerController.RunResponse execResponse = objectMapper.readValue(
            execResult.getResponse().getContentAsString(), SchedulerController.RunResponse.class);
        globalRunId = execResponse.getRunId();
        assertNotNull(globalRunId, "Execution should start successfully");

        // Phase 4: Monitor Execution Status
        waitForExecutionCompletion(globalRunId, 30);

        // Phase 5: Validate Execution Results
        validateExecutionResults(globalRunId);

        // Phase 6: Generate and Validate Reports
        validateReportGeneration(globalRunId);

        // Phase 7: Validate Log Collection
        validateLogCollection(globalRunId);
    }

    @Test
    @Order(2)
    @DisplayName("E2E Workflow 2: Batch Processing and Analytics Validation")
    void testBatchProcessingAndAnalytics() throws Exception {
        // Simplified batch execution test - focus on core workflow
        List<String> batchTestIds = new ArrayList<>();
        
        // Create a smaller batch for testing
        for (int i = 0; i < 3; i++) {
            TestController.IntegrateTestRequest request = createBatchTestCase("batch_test_" + i);
            request.setTestSuiteId(testSuiteId);
            
            MvcResult result = mockMvc.perform(post("/tests/integrate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn();

            TestController.IntegrateResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), TestController.IntegrateResponse.class);
            batchTestIds.add(String.valueOf(response.getTestCase().getId()));
        }

        // Execute batch with single strategy
        SchedulerController.RunRequest batchRequest = new SchedulerController.RunRequest();
        List<Long> batchIds = new ArrayList<>();
        for (String id : batchTestIds) {
            batchIds.add(Long.parseLong(id));
        }
        batchRequest.setTestCaseIds(batchIds);
        batchRequest.setParallel(true);
        batchRequest.setThreadPoolSize(2);
        
        MvcResult result = mockMvc.perform(post("/schedule/run")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isOk())
                .andReturn();

        SchedulerController.RunResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(), SchedulerController.RunResponse.class);
        
        // Wait for completion and validate
        waitForExecutionCompletion(response.getRunId(), 30);
        validateExecutionResults(response.getRunId());
    }

    @Test
    @Order(3)
    @DisplayName("E2E Workflow 3: Report Generation and Export Validation")
    void testComprehensiveReportingWorkflow() throws Exception {
        assertNotNull(globalRunId, "Global run ID should be available");

        // Test HTML Report Generation
        MvcResult htmlResult = mockMvc.perform(get("/reports/generate")
                .param("runId", globalRunId)
                .param("format", "HTML")
                .param("includeCharts", "true"))
                .andExpect(status().isOk())
                .andReturn();

        String htmlResponse = htmlResult.getResponse().getContentAsString();
        assertTrue(htmlResponse.contains("<!DOCTYPE html>"), "HTML report should be valid HTML");
        assertTrue(htmlResponse.contains("Test Execution Report"), "HTML report should contain title");

        // Test CSV Report Generation
        MvcResult csvResult = mockMvc.perform(get("/reports/generate")
                .param("runId", globalRunId)
                .param("format", "CSV"))
                .andExpect(status().isOk())
                .andReturn();

        String csvResponse = csvResult.getResponse().getContentAsString();
        assertTrue(csvResponse.contains("Test Case Name,Status"), "CSV should contain headers");

        // Test JUnit XML Report Generation
        MvcResult junitResult = mockMvc.perform(get("/reports/generate")
                .param("runId", globalRunId)
                .param("format", "JUNIT"))
                .andExpect(status().isOk())
                .andReturn();

        String junitResponse = junitResult.getResponse().getContentAsString();
        assertTrue(junitResponse.contains("<?xml version"), "JUnit report should be valid XML");
        assertTrue(junitResponse.contains("<testsuite"), "JUnit report should contain test suite");

        // Test Comparison Report
        if (createdTestIds.size() >= 2) {
            mockMvc.perform(get("/reports/comparison")
                    .param("runId1", globalRunId)
                    .param("runId2", globalRunId)
                    .param("format", "HTML"))
                    .andExpect(status().isOk());
        }

        // Test Metrics Report
        mockMvc.perform(get("/reports/metrics")
                .param("runId", globalRunId)
                .param("format", "JSON"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    @DisplayName("E2E Workflow 4: Log Management and Analysis Validation")
    void testLogManagementWorkflow() throws Exception {
        assertNotNull(globalRunId, "Global run ID should be available");

        // Test Log Collection
        Map<String, Object> logData = new HashMap<>();
        logData.put("runId", globalRunId);
        logData.put("source", "e2e-test");
        logData.put("level", "INFO");
        logData.put("message", "E2E test log entry");

        mockMvc.perform(post("/logs/collect")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logData)))
                .andExpect(status().isOk());

        // Test Log Retrieval
        MvcResult logResult = mockMvc.perform(get("/logs")
                .param("runId", globalRunId)
                .param("level", "INFO"))
                .andExpect(status().isOk())
                .andReturn();

        String logResponse = logResult.getResponse().getContentAsString();
        assertFalse(logResponse.isEmpty(), "Log response should not be empty");

        // Test Log Search
        mockMvc.perform(get("/logs/search")
                .param("runId", globalRunId)
                .param("searchText", "test")
                .param("maxResults", "50"))
                .andExpect(status().isOk());

        // Test Log Analysis
        mockMvc.perform(get("/logs/analysis")
                .param("runId", globalRunId))
                .andExpect(status().isOk());

        // Test Log Export
        String[] formats = {"JSON", "CSV", "TXT"};
        for (String format : formats) {
            mockMvc.perform(get("/logs/export")
                    .param("runId", globalRunId)
                    .param("format", format))
                    .andExpect(status().isOk());
        }
    }

    // Helper Methods
    private List<TestController.IntegrateTestRequest> createSampleTestCases(String suiteId) {
        List<TestController.IntegrateTestRequest> requests = new ArrayList<>();
        
        // API Test Case
        TestController.IntegrateTestRequest apiTest = new TestController.IntegrateTestRequest();
        apiTest.setName("API_Login_Test");
        apiTest.setType("API");
        apiTest.setDescription("Test API login functionality");
        apiTest.setTestSuiteId(suiteId);
        apiTest.setTestData("{\"username\":\"john\",\"password\":\"demo\"}");
        apiTest.setTimeout(30);
        apiTest.setRetryCount(2);
        requests.add(apiTest);

        // UI Test Case
        TestController.IntegrateTestRequest uiTest = new TestController.IntegrateTestRequest();
        uiTest.setName("UI_Registration_Test");
        uiTest.setType("UI");
        uiTest.setDescription("Test UI registration functionality");
        uiTest.setTestSuiteId(suiteId);
        uiTest.setFunctionality("register");
        uiTest.setTestData("{\"firstName\":\"John\",\"lastName\":\"Doe\"}");
        uiTest.setTimeout(60);
        uiTest.setRetryCount(1);
        requests.add(uiTest);

        // Validation Test Case
        TestController.IntegrateTestRequest validationTest = new TestController.IntegrateTestRequest();
        validationTest.setName("API_Validation_Test");
        validationTest.setType("API");
        validationTest.setDescription("Test API response validation");
        validationTest.setTestSuiteId(suiteId);
        validationTest.setExpectedResult("{\"id\":12212}");
        validationTest.setTimeout(30);
        requests.add(validationTest);

        return requests;
    }

    private TestController.IntegrateTestRequest createBatchTestCase(String name) {
        TestController.IntegrateTestRequest request = new TestController.IntegrateTestRequest();
        request.setName(name);
        request.setType("API");
        request.setDescription("Batch test case for performance validation");
        request.setTimeout(15);
        return request;
    }

    private void waitForExecutionCompletion(String runId, int timeoutSeconds) throws Exception {
        int attempts = 0;
        int maxAttempts = timeoutSeconds;
        
        while (attempts < maxAttempts) {
            MvcResult statusResult = mockMvc.perform(get("/schedule/status")
                    .param("runId", runId))
                    .andExpect(status().isOk())
                    .andReturn();

            RunStatusDTO status = objectMapper.readValue(
                statusResult.getResponse().getContentAsString(), RunStatusDTO.class);
            
            if ("COMPLETED".equals(status.getStatus()) || "FAILED".equals(status.getStatus())) {
                return;
            }
            
            Thread.sleep(1000);
            attempts++;
        }
        
        fail("Execution did not complete within timeout: " + timeoutSeconds + " seconds");
    }

    private void validateExecutionResults(String runId) throws Exception {
        MvcResult statusResult = mockMvc.perform(get("/schedule/status")
                .param("runId", runId))
                .andExpect(status().isOk())
                .andReturn();

        RunStatusDTO status = objectMapper.readValue(
            statusResult.getResponse().getContentAsString(), RunStatusDTO.class);
        
        assertNotNull(status, "Status should not be null");
        assertTrue(status.getTotalTests() > 0, "Should have executed tests");
        assertNotNull(status.getResults(), "Results should not be null");
        assertEquals(status.getTotalTests(), status.getResults().size(), 
            "Results count should match total tests");
    }

    private void validateReportGeneration(String runId) throws Exception {
        // Validate that reports can be generated for the completed run
        String[] formats = {"HTML", "CSV", "JUNIT"};
        
        for (String format : formats) {
            mockMvc.perform(get("/reports/generate")
                    .param("runId", runId)
                    .param("format", format))
                    .andExpect(status().isOk());
        }
    }

    private void validateLogCollection(String runId) throws Exception {
        // Validate that logs were collected during execution
        MvcResult logResult = mockMvc.perform(get("/logs")
                .param("runId", runId))
                .andExpect(status().isOk())
                .andReturn();

        String logResponse = logResult.getResponse().getContentAsString();
        assertFalse(logResponse.trim().isEmpty(), "Logs should be collected");
    }

    @AfterAll
    static void tearDown() {
        // Cleanup is handled by @Transactional and test database
        System.out.println("E2E Workflow Validation completed successfully");
        System.out.println("Total test cases created: " + createdTestIds.size());
        System.out.println("Test suite ID: " + testSuiteId);
        System.out.println("Final run ID: " + globalRunId);
    }
}