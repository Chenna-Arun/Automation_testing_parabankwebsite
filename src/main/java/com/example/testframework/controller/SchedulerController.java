package com.example.testframework.controller;

import com.example.testframework.dto.RunStatusDTO;
import com.example.testframework.model.TestCase;
import com.example.testframework.model.TestSuite;
import com.example.testframework.service.ExecutionService;
import com.example.testframework.service.ExecutionServiceImpl;
import com.example.testframework.service.TestService;
import com.example.testframework.service.TestIntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/schedule")
public class SchedulerController {

    private final ExecutionService executionService;
    private final ExecutionServiceImpl executionServiceImpl;
    private final TestService testService;
    private final TestIntegrationService testIntegrationService;
    
    // Batch processing tracking
    private final Map<String, BatchExecutionContext> batchContexts = new HashMap<>();
    private final Map<String, CompletableFuture<BatchExecutionResult>> batchFutures = new HashMap<>();

    @Autowired
    public SchedulerController(ExecutionService executionService, 
                              ExecutionServiceImpl executionServiceImpl,
                              TestService testService,
                              TestIntegrationService testIntegrationService) {
        this.executionService = executionService;
        this.executionServiceImpl = executionServiceImpl;
        this.testService = testService;
        this.testIntegrationService = testIntegrationService;
    }

    // ===========================
    // POST /schedule/run
    // Trigger execution of multiple test cases
    // ===========================
    @PostMapping("/run")
    public ResponseEntity<?> runTests(@RequestBody RunRequest request) {
        try {
            if (request.getTestCaseIds() == null || request.getTestCaseIds().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("No test case IDs provided"));
            }

            List<TestCase> testCases = testService.getAllTestCases()
                    .stream()
                    .filter(tc -> request.getTestCaseIds().contains(tc.getId()))
                    .collect(Collectors.toList());

            if (testCases.isEmpty()) {
                return ResponseEntity.status(404)
                        .body(new ErrorResponse("No test cases found for provided IDs"));
            }

            String runId = executionService.executeTests(
                    testCases,
                    request.isParallel(),
                    request.getThreadPoolSize()
            );

            return ResponseEntity.ok(new RunResponse("Tests scheduled successfully", runId));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ErrorResponse("Execution error: " + e.getMessage()));
        }
    }

    // ===========================
    // GET /schedule/status?runId=...
    // Get execution status of a run
    // ===========================
    @GetMapping("/status")
    public ResponseEntity<?> getRunStatus(@RequestParam String runId) {
        RunStatusDTO status = executionService.getRunStatus(runId);
        return ResponseEntity.ok(status);
    }

    // ===========================
    // POST /schedule/batch
    // Advanced batch processing with multiple execution strategies
    // ===========================
    @PostMapping("/batch")
    public ResponseEntity<?> executeBatch(@RequestBody BatchRequest request) {
        try {
            String batchId = "batch-" + System.currentTimeMillis();
            
            // Validate request
            if (request.getBatches() == null || request.getBatches().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("No batch configurations provided"));
            }
            
            // Create batch execution context
            BatchExecutionContext context = new BatchExecutionContext();
            context.setBatchId(batchId);
            context.setTotalBatches(request.getBatches().size());
            context.setStartTime(LocalDateTime.now());
            context.setStatus("RUNNING");
            context.setStrategy(request.getStrategy());
            batchContexts.put(batchId, context);
            
            // Execute batch asynchronously
            CompletableFuture<BatchExecutionResult> future = executeBatchAsync(batchId, request);
            batchFutures.put(batchId, future);
            
            return ResponseEntity.ok(new BatchResponse(
                "Batch execution started successfully", 
                batchId, 
                request.getBatches().size()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Batch execution error: " + e.getMessage()));
        }
    }
    
    // ===========================
    // POST /schedule/bulk
    // Bulk test execution with advanced scheduling
    // ===========================
    @PostMapping("/bulk")
    public ResponseEntity<?> executeBulk(@RequestBody BulkRequest request) {
        try {
            String bulkId = "bulk-" + System.currentTimeMillis();
            
            List<TestCase> allTestCases = new ArrayList<>();
            
            // Collect test cases from different sources
            if (request.getTestCaseIds() != null && !request.getTestCaseIds().isEmpty()) {
                List<TestCase> testCases = testService.getAllTestCases()
                    .stream()
                    .filter(tc -> request.getTestCaseIds().contains(tc.getId()))
                    .collect(Collectors.toList());
                allTestCases.addAll(testCases);
            }
            
            if (request.getTestSuiteIds() != null && !request.getTestSuiteIds().isEmpty()) {
                for (Long suiteId : request.getTestSuiteIds()) {
                    // Convert Long ID to String for the integration service
                    String suiteIdStr = String.valueOf(suiteId);
                    List<TestCase> suiteTests = testIntegrationService.getTestCasesBySuite(suiteIdStr, null, null, null);
                    allTestCases.addAll(suiteTests);
                }
            }
            
            if (allTestCases.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("No valid test cases found for bulk execution"));
            }
            
            // Remove duplicates
            allTestCases = allTestCases.stream()
                .distinct()
                .collect(Collectors.toList());
            
            // Apply filters if specified
            if (request.getFilters() != null) {
                allTestCases = applyFilters(allTestCases, request.getFilters());
            }
            
            // Execute with advanced strategy
            String runId = executionServiceImpl.executeTestsAdvanced(
                bulkId, 
                allTestCases, 
                request.isParallel(), 
                request.getThreadPoolSize(),
                request.getExecutionStrategy()
            );
            
            return ResponseEntity.ok(new BulkResponse(
                "Bulk execution started successfully", 
                runId, 
                allTestCases.size(),
                request.getExecutionStrategy().name()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Bulk execution error: " + e.getMessage()));
        }
    }
    
    // ===========================
    // POST /schedule/priority
    // Priority-based execution scheduling
    // ===========================
    @PostMapping("/priority")
    public ResponseEntity<?> executePriority(@RequestBody PriorityRequest request) {
        try {
            String priorityId = "priority-" + System.currentTimeMillis();
            
            // Group test cases by priority
            Map<TestCase.Priority, List<TestCase>> priorityGroups = new HashMap<>();
            
            for (PriorityGroup group : request.getPriorityGroups()) {
                List<TestCase> testCases = testService.getAllTestCases()
                    .stream()
                    .filter(tc -> group.getTestCaseIds().contains(tc.getId()))
                    .collect(Collectors.toList());
                    
                priorityGroups.put(group.getPriority(), testCases);
            }
            
            if (priorityGroups.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("No valid priority groups found"));
            }
            
            // Execute in priority order: HIGH -> MEDIUM -> LOW
            CompletableFuture<List<String>> future = executePriorityAsync(priorityId, priorityGroups, request);
            
            return ResponseEntity.ok(new PriorityResponse(
                "Priority execution started successfully", 
                priorityId,
                priorityGroups.size(),
                priorityGroups.values().stream().mapToInt(List::size).sum()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Priority execution error: " + e.getMessage()));
        }
    }
    
    // ===========================
    // GET /schedule/batch/status/{batchId}
    // Get batch execution status
    // ===========================
    @GetMapping("/batch/status/{batchId}")
    public ResponseEntity<?> getBatchStatus(@PathVariable String batchId) {
        try {
            BatchExecutionContext context = batchContexts.get(batchId);
            if (context == null) {
                return ResponseEntity.status(404)
                    .body(new ErrorResponse("Batch not found: " + batchId));
            }
            
            CompletableFuture<BatchExecutionResult> future = batchFutures.get(batchId);
            boolean isComplete = future != null && future.isDone();
            
            BatchStatusResponse response = new BatchStatusResponse();
            response.setBatchId(batchId);
            response.setStatus(context.getStatus());
            response.setTotalBatches(context.getTotalBatches());
            response.setCompletedBatches(context.getCompletedBatches());
            response.setStartTime(context.getStartTime());
            response.setEndTime(context.getEndTime());
            response.setProgress(calculateBatchProgress(context));
            response.setResults(context.getResults());
            response.setIsComplete(isComplete);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Error retrieving batch status: " + e.getMessage()));
        }
    }
    
    // ===========================
    // POST /schedule/cancel/{runId}
    // Cancel running execution
    // ===========================
    @PostMapping("/cancel/{runId}")
    public ResponseEntity<?> cancelExecution(@PathVariable String runId) {
        try {
            boolean cancelled = executionServiceImpl.cancelExecution(runId);
            
            if (cancelled) {
                return ResponseEntity.ok(new CancelResponse(
                    "Execution cancelled successfully", 
                    runId, 
                    true
                ));
            } else {
                return ResponseEntity.status(404)
                    .body(new CancelResponse(
                        "Execution not found or already completed", 
                        runId, 
                        false
                    ));
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Cancel operation error: " + e.getMessage()));
        }
    }
    
    // ===========================
    // GET /schedule/active
    // Get all active executions
    // ===========================
    @GetMapping("/active")
    public ResponseEntity<?> getActiveExecutions() {
        try {
            List<ActiveExecutionInfo> activeExecutions = new ArrayList<>();
            
            // Add regular executions
            // Note: This would require extending ExecutionServiceImpl to track active runs
            
            // Add batch executions
            for (Map.Entry<String, BatchExecutionContext> entry : batchContexts.entrySet()) {
                if ("RUNNING".equals(entry.getValue().getStatus())) {
                    ActiveExecutionInfo info = new ActiveExecutionInfo();
                    info.setExecutionId(entry.getKey());
                    info.setType("BATCH");
                    info.setStartTime(entry.getValue().getStartTime());
                    info.setProgress(calculateBatchProgress(entry.getValue()));
                    activeExecutions.add(info);
                }
            }
            
            return ResponseEntity.ok(new ActiveExecutionsResponse(activeExecutions));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Error retrieving active executions: " + e.getMessage()));
        }
    }

    // ===========================
    // HELPER METHODS
    // ===========================
    
    @Async
    private CompletableFuture<BatchExecutionResult> executeBatchAsync(String batchId, BatchRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                BatchExecutionContext context = batchContexts.get(batchId);
                List<String> runIds = new ArrayList<>();
                Map<String, Exception> errors = new HashMap<>();
                
                for (BatchConfig batch : request.getBatches()) {
                    try {
                        List<TestCase> testCases = testService.getAllTestCases()
                            .stream()
                            .filter(tc -> batch.getTestCaseIds().contains(tc.getId()))
                            .collect(Collectors.toList());
                        
                        if (!testCases.isEmpty()) {
                            String runId = executionServiceImpl.executeTestsAdvanced(
                                batchId + "-sub-" + System.currentTimeMillis(),
                                testCases,
                                batch.isParallel(),
                                batch.getThreadPoolSize(),
                                batch.getExecutionStrategy()
                            );
                            runIds.add(runId);
                        }
                        
                        context.incrementCompleted();
                        
                        // Add delay between batches if specified
                        if (batch.getDelayBetweenBatches() > 0) {
                            Thread.sleep(batch.getDelayBetweenBatches() * 1000L);
                        }
                        
                    } catch (Exception e) {
                        errors.put("batch-" + batch.hashCode(), e);
                    }
                }
                
                context.setStatus("COMPLETED");
                context.setEndTime(LocalDateTime.now());
                
                BatchExecutionResult result = new BatchExecutionResult();
                result.setBatchId(batchId);
                result.setRunIds(runIds);
                result.setSuccessfulBatches(runIds.size());
                result.setFailedBatches(errors.size());
                result.setErrors(errors);
                
                return result;
                
            } catch (Exception e) {
                BatchExecutionContext context = batchContexts.get(batchId);
                context.setStatus("FAILED");
                context.setEndTime(LocalDateTime.now());
                throw new RuntimeException("Batch execution failed", e);
            }
        });
    }
    
    @Async
    private CompletableFuture<List<String>> executePriorityAsync(String priorityId, 
                                                               Map<TestCase.Priority, List<TestCase>> priorityGroups,
                                                               PriorityRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> runIds = new ArrayList<>();
            
            try {
                // Execute in priority order: HIGH -> MEDIUM -> LOW
                for (TestCase.Priority priority : Arrays.asList(
                    TestCase.Priority.HIGH, 
                    TestCase.Priority.MEDIUM, 
                    TestCase.Priority.LOW
                )) {
                    List<TestCase> testCases = priorityGroups.get(priority);
                    if (testCases != null && !testCases.isEmpty()) {
                        String runId = executionServiceImpl.executeTestsAdvanced(
                            priorityId + "-" + priority.name().toLowerCase(),
                            testCases,
                            request.isParallel(),
                            request.getThreadPoolSize(),
                            ExecutionServiceImpl.ExecutionStrategy.PRIORITY_BASED
                        );
                        runIds.add(runId);
                        
                        // Wait for completion before starting next priority if sequential
                        if (request.isWaitForCompletion()) {
                            // Poll for completion
                            while ("RUNNING".equals(executionService.getRunStatus(runId).getStatus())) {
                                Thread.sleep(1000);
                            }
                        }
                    }
                }
                
                return runIds;
                
            } catch (Exception e) {
                throw new RuntimeException("Priority execution failed", e);
            }
        });
    }
    
    private List<TestCase> applyFilters(List<TestCase> testCases, ExecutionFilters filters) {
        return testCases.stream()
            .filter(tc -> {
                // Filter by type
                if (filters.getTypes() != null && !filters.getTypes().isEmpty()) {
                    if (!filters.getTypes().contains(tc.getType())) {
                        return false;
                    }
                }
                
                // Filter by priority
                if (filters.getPriorities() != null && !filters.getPriorities().isEmpty()) {
                    TestCase.Priority priority = tc.getPriority() != null ? tc.getPriority() : TestCase.Priority.MEDIUM;
                    if (!filters.getPriorities().contains(priority)) {
                        return false;
                    }
                }
                
                // Filter by status
                if (filters.getStatuses() != null && !filters.getStatuses().isEmpty()) {
                    if (!filters.getStatuses().contains(tc.getStatus())) {
                        return false;
                    }
                }
                
                // Filter by environment
                if (filters.getEnvironments() != null && !filters.getEnvironments().isEmpty()) {
                    String environment = tc.getTestEnvironment() != null ? tc.getTestEnvironment() : "default";
                    if (!filters.getEnvironments().contains(environment)) {
                        return false;
                    }
                }
                
                // Filter by tags
                if (filters.getTags() != null && !filters.getTags().isEmpty()) {
                    String tags = tc.getTags() != null ? tc.getTags() : "";
                    boolean hasRequiredTag = filters.getTags().stream()
                        .anyMatch(tag -> tags.contains(tag));
                    if (!hasRequiredTag) {
                        return false;
                    }
                }
                
                return true;
            })
            .collect(Collectors.toList());
    }
    
    private double calculateBatchProgress(BatchExecutionContext context) {
        if (context.getTotalBatches() == 0) {
            return 0.0;
        }
        return (double) context.getCompletedBatches() / context.getTotalBatches() * 100.0;
    }

    // ===========================
    // REQUEST & RESPONSE CLASSES
    // ===========================
    
    // Enhanced RunRequest with execution strategy
    public static class RunRequest {
        private List<Long> testCaseIds;
        private boolean parallel = true;
        private int threadPoolSize = 5;
        private ExecutionServiceImpl.ExecutionStrategy executionStrategy = ExecutionServiceImpl.ExecutionStrategy.BALANCED;

        public List<Long> getTestCaseIds() { return testCaseIds; }
        public void setTestCaseIds(List<Long> testCaseIds) { this.testCaseIds = testCaseIds; }
        public boolean isParallel() { return parallel; }
        public void setParallel(boolean parallel) { this.parallel = parallel; }
        public int getThreadPoolSize() { return threadPoolSize; }
        public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }
        public ExecutionServiceImpl.ExecutionStrategy getExecutionStrategy() { return executionStrategy; }
        public void setExecutionStrategy(ExecutionServiceImpl.ExecutionStrategy executionStrategy) { this.executionStrategy = executionStrategy; }
    }
    
    // Batch processing request
    public static class BatchRequest {
        private List<BatchConfig> batches;
        private String strategy = "SEQUENTIAL"; // SEQUENTIAL, PARALLEL
        private int maxConcurrentBatches = 3;
        private boolean continueOnError = true;
        
        public List<BatchConfig> getBatches() { return batches; }
        public void setBatches(List<BatchConfig> batches) { this.batches = batches; }
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        public int getMaxConcurrentBatches() { return maxConcurrentBatches; }
        public void setMaxConcurrentBatches(int maxConcurrentBatches) { this.maxConcurrentBatches = maxConcurrentBatches; }
        public boolean isContinueOnError() { return continueOnError; }
        public void setContinueOnError(boolean continueOnError) { this.continueOnError = continueOnError; }
    }
    
    public static class BatchConfig {
        private String name;
        private List<Long> testCaseIds;
        private boolean parallel = true;
        private int threadPoolSize = 5;
        private ExecutionServiceImpl.ExecutionStrategy executionStrategy = ExecutionServiceImpl.ExecutionStrategy.BALANCED;
        private int delayBetweenBatches = 0; // seconds
        private int timeoutMinutes = 30;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<Long> getTestCaseIds() { return testCaseIds; }
        public void setTestCaseIds(List<Long> testCaseIds) { this.testCaseIds = testCaseIds; }
        public boolean isParallel() { return parallel; }
        public void setParallel(boolean parallel) { this.parallel = parallel; }
        public int getThreadPoolSize() { return threadPoolSize; }
        public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }
        public ExecutionServiceImpl.ExecutionStrategy getExecutionStrategy() { return executionStrategy; }
        public void setExecutionStrategy(ExecutionServiceImpl.ExecutionStrategy executionStrategy) { this.executionStrategy = executionStrategy; }
        public int getDelayBetweenBatches() { return delayBetweenBatches; }
        public void setDelayBetweenBatches(int delayBetweenBatches) { this.delayBetweenBatches = delayBetweenBatches; }
        public int getTimeoutMinutes() { return timeoutMinutes; }
        public void setTimeoutMinutes(int timeoutMinutes) { this.timeoutMinutes = timeoutMinutes; }
    }
    
    // Bulk execution request
    public static class BulkRequest {
        private List<Long> testCaseIds;
        private List<Long> testSuiteIds;
        private boolean parallel = true;
        private int threadPoolSize = 10;
        private ExecutionServiceImpl.ExecutionStrategy executionStrategy = ExecutionServiceImpl.ExecutionStrategy.BALANCED;
        private ExecutionFilters filters;
        private boolean includeInactive = false;
        
        public List<Long> getTestCaseIds() { return testCaseIds; }
        public void setTestCaseIds(List<Long> testCaseIds) { this.testCaseIds = testCaseIds; }
        public List<Long> getTestSuiteIds() { return testSuiteIds; }
        public void setTestSuiteIds(List<Long> testSuiteIds) { this.testSuiteIds = testSuiteIds; }
        public boolean isParallel() { return parallel; }
        public void setParallel(boolean parallel) { this.parallel = parallel; }
        public int getThreadPoolSize() { return threadPoolSize; }
        public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }
        public ExecutionServiceImpl.ExecutionStrategy getExecutionStrategy() { return executionStrategy; }
        public void setExecutionStrategy(ExecutionServiceImpl.ExecutionStrategy executionStrategy) { this.executionStrategy = executionStrategy; }
        public ExecutionFilters getFilters() { return filters; }
        public void setFilters(ExecutionFilters filters) { this.filters = filters; }
        public boolean isIncludeInactive() { return includeInactive; }
        public void setIncludeInactive(boolean includeInactive) { this.includeInactive = includeInactive; }
    }
    
    // Priority execution request
    public static class PriorityRequest {
        private List<PriorityGroup> priorityGroups;
        private boolean parallel = true;
        private int threadPoolSize = 5;
        private boolean waitForCompletion = false; // Wait for each priority to complete before starting next
        
        public List<PriorityGroup> getPriorityGroups() { return priorityGroups; }
        public void setPriorityGroups(List<PriorityGroup> priorityGroups) { this.priorityGroups = priorityGroups; }
        public boolean isParallel() { return parallel; }
        public void setParallel(boolean parallel) { this.parallel = parallel; }
        public int getThreadPoolSize() { return threadPoolSize; }
        public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }
        public boolean isWaitForCompletion() { return waitForCompletion; }
        public void setWaitForCompletion(boolean waitForCompletion) { this.waitForCompletion = waitForCompletion; }
    }
    
    public static class PriorityGroup {
        private TestCase.Priority priority;
        private List<Long> testCaseIds;
        
        public TestCase.Priority getPriority() { return priority; }
        public void setPriority(TestCase.Priority priority) { this.priority = priority; }
        public List<Long> getTestCaseIds() { return testCaseIds; }
        public void setTestCaseIds(List<Long> testCaseIds) { this.testCaseIds = testCaseIds; }
    }
    
    // Execution filters
    public static class ExecutionFilters {
        private List<TestCase.Type> types;
        private List<TestCase.Priority> priorities;
        private List<String> statuses;
        private List<String> environments;
        private List<String> tags;
        
        public List<TestCase.Type> getTypes() { return types; }
        public void setTypes(List<TestCase.Type> types) { this.types = types; }
        public List<TestCase.Priority> getPriorities() { return priorities; }
        public void setPriorities(List<TestCase.Priority> priorities) { this.priorities = priorities; }
        public List<String> getStatuses() { return statuses; }
        public void setStatuses(List<String> statuses) { this.statuses = statuses; }
        public List<String> getEnvironments() { return environments; }
        public void setEnvironments(List<String> environments) { this.environments = environments; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
    }
    
    // Context classes
    public static class BatchExecutionContext {
        private String batchId;
        private int totalBatches;
        private int completedBatches = 0;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private String strategy;
        private List<String> results = new ArrayList<>();
        
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public int getTotalBatches() { return totalBatches; }
        public void setTotalBatches(int totalBatches) { this.totalBatches = totalBatches; }
        public int getCompletedBatches() { return completedBatches; }
        public void setCompletedBatches(int completedBatches) { this.completedBatches = completedBatches; }
        public void incrementCompleted() { this.completedBatches++; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getStrategy() { return strategy; }
        public void setStrategy(String strategy) { this.strategy = strategy; }
        public List<String> getResults() { return results; }
        public void setResults(List<String> results) { this.results = results; }
    }
    
    // Result classes
    public static class BatchExecutionResult {
        private String batchId;
        private List<String> runIds;
        private int successfulBatches;
        private int failedBatches;
        private Map<String, Exception> errors;
        
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public List<String> getRunIds() { return runIds; }
        public void setRunIds(List<String> runIds) { this.runIds = runIds; }
        public int getSuccessfulBatches() { return successfulBatches; }
        public void setSuccessfulBatches(int successfulBatches) { this.successfulBatches = successfulBatches; }
        public int getFailedBatches() { return failedBatches; }
        public void setFailedBatches(int failedBatches) { this.failedBatches = failedBatches; }
        public Map<String, Exception> getErrors() { return errors; }
        public void setErrors(Map<String, Exception> errors) { this.errors = errors; }
    }
    
    // Response classes
    public static class BatchResponse {
        private final String message;
        private final String batchId;
        private final int totalBatches;
        
        public BatchResponse(String message, String batchId, int totalBatches) {
            this.message = message;
            this.batchId = batchId;
            this.totalBatches = totalBatches;
        }
        
        public String getMessage() { return message; }
        public String getBatchId() { return batchId; }
        public int getTotalBatches() { return totalBatches; }
    }
    
    public static class BulkResponse {
        private final String message;
        private final String runId;
        private final int totalTests;
        private final String executionStrategy;
        
        public BulkResponse(String message, String runId, int totalTests, String executionStrategy) {
            this.message = message;
            this.runId = runId;
            this.totalTests = totalTests;
            this.executionStrategy = executionStrategy;
        }
        
        public String getMessage() { return message; }
        public String getRunId() { return runId; }
        public int getTotalTests() { return totalTests; }
        public String getExecutionStrategy() { return executionStrategy; }
    }
    
    public static class PriorityResponse {
        private final String message;
        private final String priorityId;
        private final int totalPriorityGroups;
        private final int totalTests;
        
        public PriorityResponse(String message, String priorityId, int totalPriorityGroups, int totalTests) {
            this.message = message;
            this.priorityId = priorityId;
            this.totalPriorityGroups = totalPriorityGroups;
            this.totalTests = totalTests;
        }
        
        public String getMessage() { return message; }
        public String getPriorityId() { return priorityId; }
        public int getTotalPriorityGroups() { return totalPriorityGroups; }
        public int getTotalTests() { return totalTests; }
    }
    
    public static class BatchStatusResponse {
        private String batchId;
        private String status;
        private int totalBatches;
        private int completedBatches;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private double progress;
        private List<String> results;
        private boolean isComplete;
        
        public String getBatchId() { return batchId; }
        public void setBatchId(String batchId) { this.batchId = batchId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getTotalBatches() { return totalBatches; }
        public void setTotalBatches(int totalBatches) { this.totalBatches = totalBatches; }
        public int getCompletedBatches() { return completedBatches; }
        public void setCompletedBatches(int completedBatches) { this.completedBatches = completedBatches; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public double getProgress() { return progress; }
        public void setProgress(double progress) { this.progress = progress; }
        public List<String> getResults() { return results; }
        public void setResults(List<String> results) { this.results = results; }
        public boolean isComplete() { return isComplete; }
        public void setIsComplete(boolean isComplete) { this.isComplete = isComplete; }
    }
    
    public static class CancelResponse {
        private final String message;
        private final String runId;
        private final boolean cancelled;
        
        public CancelResponse(String message, String runId, boolean cancelled) {
            this.message = message;
            this.runId = runId;
            this.cancelled = cancelled;
        }
        
        public String getMessage() { return message; }
        public String getRunId() { return runId; }
        public boolean isCancelled() { return cancelled; }
    }
    
    public static class ActiveExecutionInfo {
        private String executionId;
        private String type;
        private LocalDateTime startTime;
        private double progress;
        
        public String getExecutionId() { return executionId; }
        public void setExecutionId(String executionId) { this.executionId = executionId; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public double getProgress() { return progress; }
        public void setProgress(double progress) { this.progress = progress; }
    }
    
    public static class ActiveExecutionsResponse {
        private final List<ActiveExecutionInfo> activeExecutions;
        
        public ActiveExecutionsResponse(List<ActiveExecutionInfo> activeExecutions) {
            this.activeExecutions = activeExecutions;
        }
        
        public List<ActiveExecutionInfo> getActiveExecutions() { return activeExecutions; }
        public int getCount() { return activeExecutions.size(); }
    }

    public static class RunResponse {
        private final String message;
        private final String runId;

        public RunResponse(String message, String runId) {
            this.message = message;
            this.runId = runId;
        }

        public String getMessage() { return message; }
        public String getRunId() { return runId; }
    }

    public static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) { this.error = error; }
        public String getError() { return error; }
    }
}
