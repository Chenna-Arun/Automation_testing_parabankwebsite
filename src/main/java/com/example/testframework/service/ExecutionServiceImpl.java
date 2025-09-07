package com.example.testframework.service;

import com.example.testframework.dto.RunStatusDTO;
import com.example.testframework.dto.TestResultDTO;
import com.example.testframework.executor.ExecutionResult;
import com.example.testframework.executor.ParabankAPITestExecutor;
import com.example.testframework.executor.ParabankUITestExecutor;
import com.example.testframework.model.TestCase;
import com.example.testframework.model.TestResult;
import com.example.testframework.repository.TestResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class ExecutionServiceImpl implements ExecutionService {

    private final TestResultRepository testResultRepository;

    // Enhanced execution tracking
    private final Map<String, List<TestResult>> runResults = new ConcurrentHashMap<>();
    private final Map<String, Integer> runTotals = new ConcurrentHashMap<>();
    private final Map<String, String> runStatus = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> runStartTimes = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> runEndTimes = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> runProgress = new ConcurrentHashMap<>();
    private final Map<String, ExecutorService> activeExecutors = new ConcurrentHashMap<>();
    private final Map<String, Future<?>> runFutures = new ConcurrentHashMap<>();
    
    // Thread pool management
    private final Map<String, ThreadPoolExecutor> customThreadPools = new ConcurrentHashMap<>();
    private final AtomicLong executorCounter = new AtomicLong(0);

    private final ParabankUITestExecutor uiExecutor;
    private final ParabankAPITestExecutor apiExecutor;

    @Autowired
    public ExecutionServiceImpl(
            TestResultRepository testResultRepository,
            @Value("${parabank.ui.base-url}") String uiBaseUrl,
            @Value("${app.screenshots.path}") String screenshotPath,
            @Value("${parabank.ui.headless:true}") boolean headless,
            @Value("${parabank.api.base-url}") String apiBaseUrl
    ) {
        this.testResultRepository = testResultRepository;
        
        System.out.println("🌐 Initializing ExecutionServiceImpl with:");
        System.out.println("📱 UI Base URL: " + uiBaseUrl);
        System.out.println("📷 Screenshot Path: " + screenshotPath);
        System.out.println("🔮 Headless: " + headless);
        System.out.println("🌐 API Base URL: " + apiBaseUrl);
        
        try {
            this.uiExecutor = new ParabankUITestExecutor(uiBaseUrl, screenshotPath, headless);
            System.out.println("✅ UI Executor initialized successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize UI Executor: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        
        try {
            this.apiExecutor = new ParabankAPITestExecutor(apiBaseUrl);
            System.out.println("✅ API Executor initialized successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize API Executor: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public String executeTests(List<TestCase> testCases, boolean parallel, int threadPoolSize) {
        String runId = "run-" + System.currentTimeMillis();
        return executeTestsAdvanced(runId, testCases, parallel, threadPoolSize, ExecutionStrategy.BALANCED);
    }

    /**
     * Advanced test execution with enhanced multithreading capabilities
     */
    public String executeTestsAdvanced(String runId, List<TestCase> testCases, boolean parallel, 
                                      int threadPoolSize, ExecutionStrategy strategy) {
        System.out.println("🚀 Starting executeTestsAdvanced with runId: " + runId);
        System.out.println("📋 Test cases count: " + testCases.size());
        System.out.println("📋 Parallel: " + parallel + ", ThreadPoolSize: " + threadPoolSize);
        
        List<TestResult> results = Collections.synchronizedList(new ArrayList<>());
        runResults.put(runId, results);
        runTotals.put(runId, testCases.size());
        runStatus.put(runId, "RUNNING");
        runStartTimes.put(runId, LocalDateTime.now());
        runProgress.put(runId, new AtomicInteger(0));

        ExecutorService executor = createOptimizedExecutor(runId, parallel, threadPoolSize, strategy);
        activeExecutors.put(runId, executor);
        System.out.println("📋 Executor created: " + executor.getClass().getSimpleName());

        // Group tests by type for optimized execution
        Map<TestCase.Type, List<TestCase>> testsByType = testCases.stream()
            .collect(Collectors.groupingBy(TestCase::getType));
        System.out.println("📋 Tests by type: " + testsByType.keySet());

        List<Callable<Void>> tasks = new ArrayList<>();
        
        // Create tasks based on execution strategy
        switch (strategy) {
            case BALANCED -> {
                System.out.println("📋 Using BALANCED strategy");
                tasks.addAll(createBalancedTasks(runId, testsByType, results));
            }
            case TYPE_GROUPED -> {
                System.out.println("📋 Using TYPE_GROUPED strategy");
                tasks.addAll(createTypeGroupedTasks(runId, testsByType, results));
            }
            case PRIORITY_BASED -> {
                System.out.println("📋 Using PRIORITY_BASED strategy");
                tasks.addAll(createPriorityBasedTasks(runId, testCases, results));
            }
            case SEQUENTIAL -> {
                System.out.println("📋 Using SEQUENTIAL strategy");
                tasks.addAll(createSequentialTasks(runId, testCases, results));
            }
            default -> {
                System.out.println("📋 Using default BALANCED strategy");
                tasks.addAll(createBalancedTasks(runId, testsByType, results));
            }
        }

        System.out.println("📋 Created " + tasks.size() + " tasks for execution");

        // Execute tasks asynchronously
        Future<?> executionFuture = CompletableFuture.runAsync(() -> {
            try {
                System.out.println("📋 Starting task execution with " + tasks.size() + " tasks");
                System.out.println("🔧 Executor status: " + executor.getClass().getSimpleName());
                
                // Additional validation
                if (tasks.isEmpty()) {
                    System.out.println("❌ No tasks to execute!");
                    runStatus.put(runId, "COMPLETED");
                    runEndTimes.put(runId, LocalDateTime.now());
                    return;
                }
                
                System.out.println("🚀 About to call executor.invokeAll() with " + tasks.size() + " tasks");
                List<Future<Void>> futures = executor.invokeAll(tasks);
                System.out.println("✅ invokeAll() completed. Futures count: " + futures.size());
                
                // Check if all futures completed
                for (int i = 0; i < futures.size(); i++) {
                    Future<Void> future = futures.get(i);
                    try {
                        future.get(); // This will throw if the task failed
                        System.out.println("✅ Task " + (i+1) + " completed successfully");
                    } catch (Exception e) {
                        System.err.println("❌ Task " + (i+1) + " failed: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                
                runStatus.put(runId, "COMPLETED");
                runEndTimes.put(runId, LocalDateTime.now());
                System.out.println("✅ Execution completed for run: " + runId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                runStatus.put(runId, "INTERRUPTED");
                runEndTimes.put(runId, LocalDateTime.now());
                System.err.println("❌ Execution interrupted for run: " + runId);
                throw new RuntimeException("Test execution interrupted", e);
            } catch (Exception e) {
                runStatus.put(runId, "FAILED");
                runEndTimes.put(runId, LocalDateTime.now());
                System.err.println("❌ Execution failed for run: " + runId + " - " + e.getMessage());
                e.printStackTrace();
            } finally {
                shutdownExecutor(runId);
            }
        });
        
        runFutures.put(runId, executionFuture);
        System.out.println("🚀 Execution started asynchronously for run: " + runId);
        return runId;
    }

    @Override
    public RunStatusDTO getRunStatus(String runId) {
        List<TestResult> results = runResults.get(runId);
        if (results == null) {
            return new RunStatusDTO(runId, "NOT_FOUND", 0, 0, new ArrayList<>());
        }

        int completed = runProgress.getOrDefault(runId, new AtomicInteger(0)).get();
        int total = runTotals.getOrDefault(runId, completed);
        List<TestResultDTO> dtoResults = results.stream()
                .map(TestResultDTO::new)
                .collect(Collectors.toList());

        String status = runStatus.getOrDefault(runId, "UNKNOWN");
        
        // Enhanced status with execution details
        RunStatusDTO statusDTO = new RunStatusDTO(runId, status, total, completed, dtoResults);
        
        // Add timing information if available
        LocalDateTime startTime = runStartTimes.get(runId);
        LocalDateTime endTime = runEndTimes.get(runId);
        
        return statusDTO;
    }

    /**
     * Get enhanced run status with detailed metrics
     */
    public EnhancedRunStatusDTO getEnhancedRunStatus(String runId) {
        RunStatusDTO basicStatus = getRunStatus(runId);
        
        EnhancedRunStatusDTO enhanced = new EnhancedRunStatusDTO();
        enhanced.setRunId(runId);
        enhanced.setStatus(basicStatus.getStatus());
        enhanced.setTotalTests(basicStatus.getTotalTests());
        enhanced.setCompletedTests(basicStatus.getCompletedTests());
        enhanced.setResults(basicStatus.getResults());
        
        // Add enhanced metrics
        enhanced.setStartTime(runStartTimes.get(runId));
        enhanced.setEndTime(runEndTimes.get(runId));
        enhanced.setProgress(calculateProgress(runId));
        enhanced.setIsActive(isRunActive(runId));
        enhanced.setExecutionMetrics(calculateExecutionMetrics(runId));
        enhanced.setThreadPoolInfo(getThreadPoolInfo(runId));
        
        return enhanced;
    }

    /**
     * Cancel a running execution
     */
    public boolean cancelExecution(String runId) {
        try {
            Future<?> future = runFutures.get(runId);
            if (future != null && !future.isDone()) {
                future.cancel(true);
                runStatus.put(runId, "CANCELLED");
                runEndTimes.put(runId, LocalDateTime.now());
                shutdownExecutor(runId);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println("Error cancelling execution: " + e.getMessage());
            return false;
        }
    }

    // ===========================
    // ADVANCED EXECUTION METHODS
    // ===========================

    /**
     * Create optimized executor based on strategy
     */
    private ExecutorService createOptimizedExecutor(String runId, boolean parallel, 
                                                   int threadPoolSize, ExecutionStrategy strategy) {
        if (!parallel) {
            return Executors.newSingleThreadExecutor(createNamedThreadFactory(runId + "-sequential"));
        }

        // Calculate optimal thread pool size
        int optimalSize = calculateOptimalThreadPoolSize(threadPoolSize, strategy);
        
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            Math.max(1, optimalSize / 2), // Core pool size
            optimalSize, // Maximum pool size
            60L, TimeUnit.SECONDS, // Keep alive time
            new LinkedBlockingQueue<>(100), // Work queue
            createNamedThreadFactory(runId + "-worker"),
            new ThreadPoolExecutor.CallerRunsPolicy() // Rejection policy
        );
        
        customThreadPools.put(runId, executor);
        return executor;
    }

    /**
     * Create named thread factory for better monitoring
     */
    private ThreadFactory createNamedThreadFactory(String namePrefix) {
        return new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, namePrefix + "-" + threadNumber.getAndIncrement());
                t.setDaemon(false);
                t.setPriority(Thread.NORM_PRIORITY);
                return t;
            }
        };
    }

    /**
     * Calculate optimal thread pool size based on strategy
     */
    private int calculateOptimalThreadPoolSize(int requestedSize, ExecutionStrategy strategy) {
        int processors = Runtime.getRuntime().availableProcessors();
        
        return switch (strategy) {
            case BALANCED -> Math.min(requestedSize, processors * 2);
            case TYPE_GROUPED -> Math.min(requestedSize, processors);
            case PRIORITY_BASED -> Math.min(requestedSize, processors * 3);
            case SEQUENTIAL -> 1;
            default -> Math.max(1, Math.min(requestedSize, processors * 2));
        };
    }

    /**
     * Create balanced execution tasks
     */
    private List<Callable<Void>> createBalancedTasks(String runId, Map<TestCase.Type, List<TestCase>> testsByType, 
                                                    List<TestResult> results) {
        List<Callable<Void>> tasks = new ArrayList<>();
        
        // Mix UI and API tests for balanced load
        List<TestCase> uiTests = testsByType.getOrDefault(TestCase.Type.UI, new ArrayList<>());
        List<TestCase> apiTests = testsByType.getOrDefault(TestCase.Type.API, new ArrayList<>());
        
        int maxSize = Math.max(uiTests.size(), apiTests.size());
        
        for (int i = 0; i < maxSize; i++) {
            if (i < uiTests.size()) {
                tasks.add(createTestTask(runId, uiTests.get(i), results));
            }
            if (i < apiTests.size()) {
                tasks.add(createTestTask(runId, apiTests.get(i), results));
            }
        }
        
        return tasks;
    }

    /**
     * Create type-grouped execution tasks
     */
    private List<Callable<Void>> createTypeGroupedTasks(String runId, Map<TestCase.Type, List<TestCase>> testsByType, 
                                                        List<TestResult> results) {
        List<Callable<Void>> tasks = new ArrayList<>();
        
        // Execute all API tests first, then UI tests
        testsByType.getOrDefault(TestCase.Type.API, new ArrayList<>())
            .forEach(test -> tasks.add(createTestTask(runId, test, results)));
            
        testsByType.getOrDefault(TestCase.Type.UI, new ArrayList<>())
            .forEach(test -> tasks.add(createTestTask(runId, test, results)));
        
        return tasks;
    }

    /**
     * Create priority-based execution tasks
     */
    private List<Callable<Void>> createPriorityBasedTasks(String runId, List<TestCase> testCases, 
                                                          List<TestResult> results) {
        List<Callable<Void>> tasks = new ArrayList<>();
        
        // Sort by priority: HIGH -> MEDIUM -> LOW
        testCases.stream()
            .sorted((t1, t2) -> {
                TestCase.Priority p1 = t1.getPriority() != null ? t1.getPriority() : TestCase.Priority.MEDIUM;
                TestCase.Priority p2 = t2.getPriority() != null ? t2.getPriority() : TestCase.Priority.MEDIUM;
                return Integer.compare(p2.ordinal(), p1.ordinal()); // Reverse order for HIGH first
            })
            .forEach(test -> tasks.add(createTestTask(runId, test, results)));
        
        return tasks;
    }

    /**
     * Create sequential execution tasks
     */
    private List<Callable<Void>> createSequentialTasks(String runId, List<TestCase> testCases, 
                                                       List<TestResult> results) {
        List<Callable<Void>> tasks = new ArrayList<>();
        testCases.forEach(test -> tasks.add(createTestTask(runId, test, results)));
        return tasks;
    }

    /**
     * Create individual test execution task with enhanced monitoring
     */
    private Callable<Void> createTestTask(String runId, TestCase testCase, List<TestResult> results) {
        System.out.println("🎯 Creating task for: " + testCase.getName() + " [" + testCase.getType() + ":" + testCase.getFunctionality() + "]");
        
        return () -> {
            System.out.println("🔥 Task Starting: " + testCase.getName() + " [Thread: " + Thread.currentThread().getName() + "]");
            
            TestResult dbResult = new TestResult();
            dbResult.setTestCase(testCase);
            dbResult.setExecutedAt(LocalDateTime.now());

            try {
                System.out.printf("🚀 [%s] Executing: %s [%s:%s]%n",
                    Thread.currentThread().getName(),
                    testCase.getName(), 
                    testCase.getType(), 
                    testCase.getFunctionality());

                // Execute test with enhanced error handling
                System.out.println("🔄 About to call executeTestWithRetry...");
                ExecutionResult execResult = executeTestWithRetry(testCase);
                System.out.println("🔄 executeTestWithRetry completed. Result: " + (execResult != null ? execResult.isSuccess() : "NULL"));

                // Map ExecutionResult -> TestResult
                dbResult.setStatus(execResult.isSuccess() ? "PASSED" : "FAILED");
                dbResult.setDetails(execResult.getDetails());
                dbResult.setErrorMessage(execResult.getErrorMessage());
                dbResult.setResponseBody(execResult.getResponseBody());
                dbResult.setStatusCode(execResult.getStatusCode());
                dbResult.setScreenshotPath(execResult.getScreenshotPath());

                System.out.printf("✅ [%s] Completed: %s = %s%n", 
                    Thread.currentThread().getName(), testCase.getName(), dbResult.getStatus());

            } catch (Exception e) {
                System.out.printf("❌ [%s] Exception in task: %s -> %s%n", 
                    Thread.currentThread().getName(), testCase.getName(), e.getMessage());
                e.printStackTrace();
                
                dbResult.setStatus("FAILED");
                dbResult.setDetails("Execution error: " + e.getMessage());
                dbResult.setErrorMessage(e.getMessage());
                System.err.printf("❌ [%s] Error: %s -> %s%n", 
                    Thread.currentThread().getName(), testCase.getName(), e.getMessage());
            }

            // Save to DB and memory
            try {
                System.out.println("💾 Saving test result to database...");
                testResultRepository.save(dbResult);
                results.add(dbResult);
                System.out.println("💾 Test result saved successfully");
            } catch (Exception e) {
                System.err.println("❌ Failed to save test result: " + e.getMessage());
                e.printStackTrace();
            }
            
            // Update progress
            runProgress.get(runId).incrementAndGet();
            System.out.println("📊 Progress updated for run: " + runId);
            
            return null;
        };
    }

    /**
     * Execute test with retry logic
     */
    private ExecutionResult executeTestWithRetry(TestCase testCase) {
        int maxRetries = testCase.getRetryCount() != null ? testCase.getRetryCount() : 0;
        ExecutionResult lastResult = null;
        
        System.out.println("🔄 executeTestWithRetry called for: " + testCase.getName() + " with maxRetries: " + maxRetries);
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    Thread.sleep(1000); // Wait before retry
                    System.out.printf("🔄 Retry attempt %d for: %s%n", attempt, testCase.getName());
                }
                
                System.out.println("🔄 About to call executeTestCore (attempt " + (attempt + 1) + ")");
                lastResult = executeTestCore(testCase);
                System.out.println("🔄 executeTestCore returned: " + (lastResult != null ? "SUCCESS=" + lastResult.isSuccess() : "NULL"));
                
                if (lastResult != null && lastResult.isSuccess()) {
                    System.out.println("✅ Test execution successful on attempt " + (attempt + 1));
                    return lastResult;
                } else {
                    System.out.println("⚠️ Test execution failed on attempt " + (attempt + 1) + ": " + 
                        (lastResult != null ? lastResult.getErrorMessage() : "NULL result"));
                }
            } catch (Exception e) {
                System.err.println("❌ Exception in executeTestWithRetry (attempt " + (attempt + 1) + "): " + e.getMessage());
                e.printStackTrace();
                lastResult = ExecutionResult.failure("Execution error on attempt " + (attempt + 1) + ": " + e.getMessage());
            }
        }
        
        System.out.println("❌ All retry attempts failed for: " + testCase.getName());
        return lastResult != null ? lastResult : ExecutionResult.failure("Test execution failed after all retries");
    }

    /**
     * Core test execution logic
     */
    private ExecutionResult executeTestCore(TestCase testCase) {
        System.out.println("🚀 ExecuteTestCore called for: " + testCase.getName() + " [" + testCase.getType() + ":" + testCase.getFunctionality() + "]");
        
        if (testCase.getType() == TestCase.Type.UI) {
            System.out.println("📱 Executing UI test with uiExecutor");
            System.out.println("🔧 UI BaseURL: " + (uiExecutor != null ? "initialized" : "NULL"));
            ExecutionResult result = uiExecutor.execute(testCase.getFunctionality(), testCase.getTestData());
            System.out.println("📱 UI Test Result: " + (result != null ? result.isSuccess() : "NULL"));
            return result;
        } else if (testCase.getType() == TestCase.Type.API) {
            System.out.println("🌐 Executing API test with apiExecutor");
            System.out.println("🔧 API BaseURL: " + (apiExecutor != null ? "initialized" : "NULL"));
            Object data = testCase.getTestData() != null ? 
                parseTestData(testCase.getTestData()) : 
                getDefaultApiData(testCase.getFunctionality());
            System.out.println("📋 Test Data: " + data);
            ExecutionResult result = apiExecutor.execute(testCase.getFunctionality(), data);
            System.out.println("🌐 API Test Result: " + (result != null ? result.isSuccess() : "NULL"));
            return result;
        } else {
            System.out.println("❌ Unsupported test type: " + testCase.getType());
            return ExecutionResult.failure("Unsupported test type: " + testCase.getType());
        }
    }

    // ===========================
    // UTILITY METHODS
    // ===========================

    /**
     * Parse test data from JSON string
     */
    private Object parseTestData(String testData) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(testData, Map.class);
        } catch (Exception e) {
            System.err.println("Failed to parse test data: " + e.getMessage());
            return null;
        }
    }

    /**
     * Shutdown executor gracefully
     */
    private void shutdownExecutor(String runId) {
        ExecutorService executor = activeExecutors.remove(runId);
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        customThreadPools.remove(runId);
        runFutures.remove(runId);
    }

    /**
     * Calculate execution progress
     */
    private double calculateProgress(String runId) {
        int total = runTotals.getOrDefault(runId, 0);
        int completed = runProgress.getOrDefault(runId, new AtomicInteger(0)).get();
        return total > 0 ? (double) completed / total * 100.0 : 0.0;
    }

    /**
     * Check if run is currently active
     */
    private boolean isRunActive(String runId) {
        String status = runStatus.get(runId);
        return "RUNNING".equals(status);
    }

    /**
     * Calculate execution metrics
     */
    private Map<String, Object> calculateExecutionMetrics(String runId) {
        Map<String, Object> metrics = new HashMap<>();
        List<TestResult> results = runResults.get(runId);
        
        if (results != null) {
            long passed = results.stream().filter(r -> "PASSED".equals(r.getStatus())).count();
            long failed = results.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
            
            metrics.put("passedCount", passed);
            metrics.put("failedCount", failed);
            metrics.put("successRate", results.size() > 0 ? (double) passed / results.size() * 100.0 : 0.0);
        }
        
        LocalDateTime startTime = runStartTimes.get(runId);
        LocalDateTime endTime = runEndTimes.get(runId);
        if (startTime != null) {
            LocalDateTime compareTime = endTime != null ? endTime : LocalDateTime.now();
            long durationSeconds = java.time.Duration.between(startTime, compareTime).getSeconds();
            metrics.put("durationSeconds", durationSeconds);
        }
        
        return metrics;
    }

    /**
     * Get thread pool information
     */
    private Map<String, Object> getThreadPoolInfo(String runId) {
        Map<String, Object> info = new HashMap<>();
        ThreadPoolExecutor executor = customThreadPools.get(runId);
        
        if (executor != null) {
            info.put("corePoolSize", executor.getCorePoolSize());
            info.put("maximumPoolSize", executor.getMaximumPoolSize());
            info.put("activeCount", executor.getActiveCount());
            info.put("completedTaskCount", executor.getCompletedTaskCount());
            info.put("taskCount", executor.getTaskCount());
            info.put("queueSize", executor.getQueue().size());
        }
        
        return info;
    }

    @SuppressWarnings("unchecked")
    private Object getDefaultApiData(String functionality) {
        return switch (functionality.toLowerCase()) {
            case "login" -> Map.of("username", "testuser", "password", "testpass");
            case "createcustomer" -> Map.of(
                    "firstName", "John",
                    "lastName", "Doe",
                    "address", "123 Main St",
                    "city", "NY",
                    "state", "NY",
                    "zipCode", "10001",
                    "phone", "1234567890",
                    "ssn", "123-45-6789"
            );
            case "updatecustomer" -> Map.of("id", "1", "address", "456 Elm St");
            case "deletecustomer", "getcustomerdetails", "getaccounts", "gettransactionhistory" -> 1;
            case "transferfunds" -> Map.of("fromAccountId", 123, "toAccountId", 456, "amount", 100.0);
            case "paybills" -> Map.of("accountId", 123, "payeeName", "Electric Company", "amount", 50.0);
            case "requestloan" -> Map.of("customerId", 1, "amount", 5000.0, "term", 12);
            default -> null;
        };
    }

    // ===========================
    // ENUMS AND CLASSES
    // ===========================

    /**
     * Execution strategies for different test execution approaches
     */
    public enum ExecutionStrategy {
        BALANCED,      // Mix UI and API tests for balanced load
        TYPE_GROUPED,  // Group by test type (API first, then UI)
        PRIORITY_BASED, // Execute by priority (HIGH -> MEDIUM -> LOW)
        SEQUENTIAL     // Execute one by one
    }

    /**
     * Enhanced run status with detailed metrics
     */
    public static class EnhancedRunStatusDTO {
        private String runId;
        private String status;
        private int totalTests;
        private int completedTests;
        private List<TestResultDTO> results;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private double progress;
        private boolean isActive;
        private Map<String, Object> executionMetrics;
        private Map<String, Object> threadPoolInfo;

        // Getters and setters
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }
        public int getCompletedTests() { return completedTests; }
        public void setCompletedTests(int completedTests) { this.completedTests = completedTests; }
        public List<TestResultDTO> getResults() { return results; }
        public void setResults(List<TestResultDTO> results) { this.results = results; }
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public double getProgress() { return progress; }
        public void setProgress(double progress) { this.progress = progress; }
        public boolean isActive() { return isActive; }
        public void setIsActive(boolean isActive) { this.isActive = isActive; }
        public Map<String, Object> getExecutionMetrics() { return executionMetrics; }
        public void setExecutionMetrics(Map<String, Object> executionMetrics) { this.executionMetrics = executionMetrics; }
        public Map<String, Object> getThreadPoolInfo() { return threadPoolInfo; }
        public void setThreadPoolInfo(Map<String, Object> threadPoolInfo) { this.threadPoolInfo = threadPoolInfo; }
    }
}
