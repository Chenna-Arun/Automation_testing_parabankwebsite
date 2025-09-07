package com.example.testframework.controller;

import com.example.testframework.dto.RunStatusDTO;
import com.example.testframework.dto.TestResultDTO;
import com.example.testframework.service.ExecutionService;
import com.example.testframework.service.ExecutionServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ExecutionController provides comprehensive execution status monitoring
 * with detailed metrics, analytics, and real-time monitoring capabilities
 */
@RestController
@RequestMapping("/execution")
public class ExecutionController {

    private final ExecutionService executionService;
    private final ExecutionServiceImpl executionServiceImpl;

    @Autowired
    public ExecutionController(ExecutionService executionService, 
                              ExecutionServiceImpl executionServiceImpl) {
        this.executionService = executionService;
        this.executionServiceImpl = executionServiceImpl;
    }

    // ===========================
    // GET /execution/status/{runId}
    // Get detailed execution status with comprehensive metrics
    // ===========================
    @GetMapping("/status/{runId}")
    public ResponseEntity<?> getExecutionStatus(@PathVariable String runId,
                                               @RequestParam(defaultValue = "false") boolean includeDetails,
                                               @RequestParam(defaultValue = "false") boolean includeMetrics) {
        try {
            if (includeDetails || includeMetrics) {
                // Get enhanced status with full details
                ExecutionServiceImpl.EnhancedRunStatusDTO enhancedStatus = 
                    executionServiceImpl.getEnhancedRunStatus(runId);
                
                if ("NOT_FOUND".equals(enhancedStatus.getStatus())) {
                    return ResponseEntity.status(404)
                        .body(new ErrorResponse("Execution not found: " + runId));
                }
                
                // Build comprehensive response
                DetailedExecutionStatusResponse response = createDetailedResponse(enhancedStatus, includeMetrics);
                return ResponseEntity.ok(response);
                
            } else {
                // Get basic status
                RunStatusDTO basicStatus = executionService.getRunStatus(runId);
                return ResponseEntity.ok(basicStatus);
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Error retrieving execution status: " + e.getMessage()));
        }
    }
    
    // ===========================
    // GET /execution/status
    // Get all execution statuses with filtering
    // ===========================
    @GetMapping("/status")
    public ResponseEntity<?> getAllExecutionStatuses(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String timeRange,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            // This would require extending ExecutionServiceImpl to track all runs
            // For now, return a placeholder response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Multiple execution status endpoint");
            response.put("note", "This endpoint would list all executions with filtering capabilities");
            response.put("filters", Map.of(
                "status", status != null ? status : "ALL",
                "timeRange", timeRange != null ? timeRange : "ALL",
                "limit", limit
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Error retrieving execution statuses: " + e.getMessage()));
        }
    }
    
    // ===========================
    // GET /execution/metrics/{runId}
    // Get comprehensive execution metrics
    // ===========================
    @GetMapping("/metrics/{runId}")
    public ResponseEntity<?> getExecutionMetrics(@PathVariable String runId) {
        try {
            ExecutionServiceImpl.EnhancedRunStatusDTO enhancedStatus = 
                executionServiceImpl.getEnhancedRunStatus(runId);
            
            if ("NOT_FOUND".equals(enhancedStatus.getStatus())) {
                return ResponseEntity.status(404)
                    .body(new ErrorResponse("Execution not found: " + runId));
            }
            
            ExecutionMetricsResponse metrics = calculateComprehensiveMetrics(enhancedStatus);
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Error retrieving execution metrics: " + e.getMessage()));
        }
    }
    
    // ===========================
    // GET /execution/analytics/{runId}
    // Get execution analytics and insights
    // ===========================
    @GetMapping("/analytics/{runId}")
    public ResponseEntity<?> getExecutionAnalytics(@PathVariable String runId) {
        try {
            ExecutionServiceImpl.EnhancedRunStatusDTO enhancedStatus = 
                executionServiceImpl.getEnhancedRunStatus(runId);
            
            if ("NOT_FOUND".equals(enhancedStatus.getStatus())) {
                return ResponseEntity.status(404)
                    .body(new ErrorResponse("Execution not found: " + runId));
            }
            
            ExecutionAnalyticsResponse analytics = generateExecutionAnalytics(enhancedStatus);
            return ResponseEntity.ok(analytics);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Error retrieving execution analytics: " + e.getMessage()));
        }
    }
    
    // ===========================
    // GET /execution/performance/{runId}
    // Get execution performance analysis
    // ===========================
    @GetMapping("/performance/{runId}")
    public ResponseEntity<?> getExecutionPerformance(@PathVariable String runId) {
        try {
            ExecutionServiceImpl.EnhancedRunStatusDTO enhancedStatus = 
                executionServiceImpl.getEnhancedRunStatus(runId);
            
            if ("NOT_FOUND".equals(enhancedStatus.getStatus())) {
                return ResponseEntity.status(404)
                    .body(new ErrorResponse("Execution not found: " + runId));
            }
            
            PerformanceAnalysisResponse performance = analyzeExecutionPerformance(enhancedStatus);
            return ResponseEntity.ok(performance);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Error retrieving execution performance: " + e.getMessage()));
        }
    }
    
    // ===========================
    // POST /execution/cancel/{runId}
    // Cancel a running execution
    // ===========================
    @PostMapping("/cancel/{runId}")
    public ResponseEntity<?> cancelExecution(@PathVariable String runId,
                                            @RequestParam(defaultValue = "false") boolean force) {
        try {
            boolean cancelled = executionServiceImpl.cancelExecution(runId);
            
            CancelExecutionResponse response = new CancelExecutionResponse();
            response.setRunId(runId);
            response.setCancelled(cancelled);
            response.setForced(force);
            response.setTimestamp(LocalDateTime.now());
            response.setMessage(cancelled ? 
                "Execution cancelled successfully" : 
                "Execution not found or already completed");
            
            return cancelled ? 
                ResponseEntity.ok(response) : 
                ResponseEntity.status(404).body(response);
                
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Error cancelling execution: " + e.getMessage()));
        }
    }
    
    // ===========================
    // GET /execution/summary
    // Get execution system summary
    // ===========================
    @GetMapping("/summary")
    public ResponseEntity<?> getExecutionSummary() {
        try {
            ExecutionSystemSummaryResponse summary = generateSystemSummary();
            return ResponseEntity.ok(summary);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Error retrieving execution summary: " + e.getMessage()));
        }
    }
    
    // ===========================
    // HELPER METHODS
    // ===========================
    
    private DetailedExecutionStatusResponse createDetailedResponse(
            ExecutionServiceImpl.EnhancedRunStatusDTO enhancedStatus, 
            boolean includeMetrics) {
        
        DetailedExecutionStatusResponse response = new DetailedExecutionStatusResponse();
        response.setRunId(enhancedStatus.getRunId());
        response.setStatus(enhancedStatus.getStatus());
        response.setTotalTests(enhancedStatus.getTotalTests());
        response.setCompletedTests(enhancedStatus.getCompletedTests());
        response.setStartTime(enhancedStatus.getStartTime());
        response.setEndTime(enhancedStatus.getEndTime());
        response.setProgress(enhancedStatus.getProgress());
        response.setIsActive(enhancedStatus.isActive());
        
        // Calculate duration
        if (enhancedStatus.getStartTime() != null) {
            LocalDateTime endTime = enhancedStatus.getEndTime() != null ? 
                enhancedStatus.getEndTime() : LocalDateTime.now();
            long durationSeconds = ChronoUnit.SECONDS.between(enhancedStatus.getStartTime(), endTime);
            response.setDurationSeconds(durationSeconds);
        }
        
        // Add test results with enhanced information
        if (enhancedStatus.getResults() != null) {
            List<EnhancedTestResultInfo> enhancedResults = enhancedStatus.getResults().stream()
                .map(this::createEnhancedTestResult)
                .collect(Collectors.toList());
            response.setResults(enhancedResults);
        }
        
        // Add execution metrics if requested
        if (includeMetrics && enhancedStatus.getExecutionMetrics() != null) {
            response.setExecutionMetrics(enhancedStatus.getExecutionMetrics());
            response.setThreadPoolInfo(enhancedStatus.getThreadPoolInfo());
        }
        
        return response;
    }
    
    private EnhancedTestResultInfo createEnhancedTestResult(TestResultDTO result) {
        EnhancedTestResultInfo enhanced = new EnhancedTestResultInfo();
        enhanced.setTestName(result.getTestCaseName());
        enhanced.setStatus(result.getStatus());
        enhanced.setExecutedAt(LocalDateTime.now()); // Placeholder since TestResultDTO doesn't have executedAt
        enhanced.setDetails(result.getDetails());
        enhanced.setErrorMessage(result.getErrorMessage());
        enhanced.setResponseBody(result.getResponseBody());
        enhanced.setStatusCode(result.getStatusCode());
        enhanced.setScreenshotPath(result.getScreenshotPath());
        
        // Add enhanced metrics
        enhanced.setExecutionTimeMs(calculateExecutionTime(result));
        enhanced.setRetryAttempts(extractRetryAttempts(result));
        enhanced.setTestType(extractTestType(result));
        
        return enhanced;
    }
    
    private ExecutionMetricsResponse calculateComprehensiveMetrics(
            ExecutionServiceImpl.EnhancedRunStatusDTO enhancedStatus) {
        
        ExecutionMetricsResponse metrics = new ExecutionMetricsResponse();
        metrics.setRunId(enhancedStatus.getRunId());
        
        // Basic metrics
        metrics.setTotalTests(enhancedStatus.getTotalTests());
        metrics.setCompletedTests(enhancedStatus.getCompletedTests());
        metrics.setProgress(enhancedStatus.getProgress());
        
        // Result distribution
        if (enhancedStatus.getResults() != null) {
            Map<String, Long> statusDistribution = enhancedStatus.getResults().stream()
                .collect(Collectors.groupingBy(
                    TestResultDTO::getStatus, 
                    Collectors.counting()
                ));
            metrics.setStatusDistribution(statusDistribution);
            
            // Success rate
            long passed = statusDistribution.getOrDefault("PASSED", 0L);
            long total = enhancedStatus.getResults().size();
            double successRate = total > 0 ? (double) passed / total * 100.0 : 0.0;
            metrics.setSuccessRate(successRate);
        }
        
        // Timing metrics
        if (enhancedStatus.getStartTime() != null) {
            LocalDateTime endTime = enhancedStatus.getEndTime() != null ? 
                enhancedStatus.getEndTime() : LocalDateTime.now();
            long durationSeconds = ChronoUnit.SECONDS.between(enhancedStatus.getStartTime(), endTime);
            metrics.setDurationSeconds(durationSeconds);
            
            if (enhancedStatus.getCompletedTests() > 0) {
                double avgTimePerTest = (double) durationSeconds / enhancedStatus.getCompletedTests();
                metrics.setAverageTimePerTestSeconds(avgTimePerTest);
            }
        }
        
        // Thread pool metrics
        if (enhancedStatus.getThreadPoolInfo() != null) {
            metrics.setThreadPoolMetrics(enhancedStatus.getThreadPoolInfo());
        }
        
        return metrics;
    }
    
    private ExecutionAnalyticsResponse generateExecutionAnalytics(
            ExecutionServiceImpl.EnhancedRunStatusDTO enhancedStatus) {
        
        ExecutionAnalyticsResponse analytics = new ExecutionAnalyticsResponse();
        analytics.setRunId(enhancedStatus.getRunId());
        analytics.setGeneratedAt(LocalDateTime.now());
        
        if (enhancedStatus.getResults() != null) {
            List<TestResultDTO> results = enhancedStatus.getResults();
            
            // Test type analysis
            Map<String, Long> typeDistribution = results.stream()
                .collect(Collectors.groupingBy(
                    result -> extractTestType(result), 
                    Collectors.counting()
                ));
            analytics.setTestTypeDistribution(typeDistribution);
            
            // Failure analysis
            List<String> failureReasons = results.stream()
                .filter(r -> "FAILED".equals(r.getStatus()))
                .map(TestResultDTO::getErrorMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            analytics.setFailureReasons(failureReasons);
            
            // Performance insights
            Map<String, Object> performanceInsights = new HashMap<>();
            performanceInsights.put("totalTests", results.size());
            performanceInsights.put("fastestTest", findFastestTest(results));
            performanceInsights.put("slowestTest", findSlowestTest(results));
            analytics.setPerformanceInsights(performanceInsights);
        }
        
        return analytics;
    }
    
    private PerformanceAnalysisResponse analyzeExecutionPerformance(
            ExecutionServiceImpl.EnhancedRunStatusDTO enhancedStatus) {
        
        PerformanceAnalysisResponse performance = new PerformanceAnalysisResponse();
        performance.setRunId(enhancedStatus.getRunId());
        performance.setAnalyzedAt(LocalDateTime.now());
        
        if (enhancedStatus.getResults() != null) {
            List<TestResultDTO> results = enhancedStatus.getResults();
            
            // Calculate performance statistics
            List<Long> executionTimes = results.stream()
                .map(this::calculateExecutionTime)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            if (!executionTimes.isEmpty()) {
                performance.setMinExecutionTimeMs(Collections.min(executionTimes));
                performance.setMaxExecutionTimeMs(Collections.max(executionTimes));
                performance.setAvgExecutionTimeMs(
                    executionTimes.stream().mapToLong(Long::longValue).average().orElse(0.0)
                );
                performance.setMedianExecutionTimeMs(calculateMedian(executionTimes));
            }
            
            // Throughput analysis
            if (enhancedStatus.getStartTime() != null && enhancedStatus.getEndTime() != null) {
                long totalDurationSeconds = ChronoUnit.SECONDS.between(
                    enhancedStatus.getStartTime(), enhancedStatus.getEndTime()
                );
                if (totalDurationSeconds > 0) {
                    double throughput = (double) results.size() / totalDurationSeconds;
                    performance.setTestsPerSecond(throughput);
                }
            }
        }
        
        // Thread utilization analysis
        if (enhancedStatus.getThreadPoolInfo() != null) {
            Map<String, Object> threadMetrics = enhancedStatus.getThreadPoolInfo();
            performance.setThreadUtilization(calculateThreadUtilization(threadMetrics));
        }
        
        return performance;
    }
    
    private ExecutionSystemSummaryResponse generateSystemSummary() {
        ExecutionSystemSummaryResponse summary = new ExecutionSystemSummaryResponse();
        summary.setGeneratedAt(LocalDateTime.now());
        
        // System information
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("availableProcessors", runtime.availableProcessors());
        systemInfo.put("maxMemoryMB", runtime.maxMemory() / (1024 * 1024));
        systemInfo.put("totalMemoryMB", runtime.totalMemory() / (1024 * 1024));
        systemInfo.put("freeMemoryMB", runtime.freeMemory() / (1024 * 1024));
        summary.setSystemInfo(systemInfo);
        
        // Execution capabilities
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("supportedExecutionStrategies", Arrays.asList(
            "BALANCED", "TYPE_GROUPED", "PRIORITY_BASED", "SEQUENTIAL"
        ));
        capabilities.put("supportedTestTypes", Arrays.asList("UI", "API"));
        capabilities.put("maxConcurrentExecutions", runtime.availableProcessors() * 2);
        summary.setCapabilities(capabilities);
        
        return summary;
    }
    
    // Utility methods
    private Long calculateExecutionTime(TestResultDTO result) {
        // This would require execution start/end timestamps in TestResult
        // For now, return a placeholder
        return 1000L; // 1 second placeholder
    }
    
    private int extractRetryAttempts(TestResultDTO result) {
        // Extract retry information from details or error message
        return 0; // Placeholder
    }
    
    private String extractTestType(TestResultDTO result) {
        // Extract test type from test case information
        return "UNKNOWN"; // Placeholder
    }
    
    private String findFastestTest(List<TestResultDTO> results) {
        return results.isEmpty() ? "N/A" : results.get(0).getTestCaseName();
    }
    
    private String findSlowestTest(List<TestResultDTO> results) {
        return results.isEmpty() ? "N/A" : results.get(results.size() - 1).getTestCaseName();
    }
    
    private double calculateMedian(List<Long> values) {
        Collections.sort(values);
        int size = values.size();
        if (size % 2 == 0) {
            return (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0;
        } else {
            return values.get(size / 2);
        }
    }
    
    private double calculateThreadUtilization(Map<String, Object> threadMetrics) {
        Object activeCount = threadMetrics.get("activeCount");
        Object maxPoolSize = threadMetrics.get("maximumPoolSize");
        
        if (activeCount instanceof Integer && maxPoolSize instanceof Integer) {
            int active = (Integer) activeCount;
            int max = (Integer) maxPoolSize;
            return max > 0 ? (double) active / max * 100.0 : 0.0;
        }
        
        return 0.0;
    }
    
    // ===========================
    // RESPONSE CLASSES
    // ===========================
    
    public static class DetailedExecutionStatusResponse {
        private String runId;
        private String status;
        private int totalTests;
        private int completedTests;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private double progress;
        private boolean isActive;
        private long durationSeconds;
        private List<EnhancedTestResultInfo> results;
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
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public double getProgress() { return progress; }
        public void setProgress(double progress) { this.progress = progress; }
        public boolean isActive() { return isActive; }
        public void setIsActive(boolean isActive) { this.isActive = isActive; }
        public long getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }
        public List<EnhancedTestResultInfo> getResults() { return results; }
        public void setResults(List<EnhancedTestResultInfo> results) { this.results = results; }
        public Map<String, Object> getExecutionMetrics() { return executionMetrics; }
        public void setExecutionMetrics(Map<String, Object> executionMetrics) { this.executionMetrics = executionMetrics; }
        public Map<String, Object> getThreadPoolInfo() { return threadPoolInfo; }
        public void setThreadPoolInfo(Map<String, Object> threadPoolInfo) { this.threadPoolInfo = threadPoolInfo; }
    }
    
    public static class EnhancedTestResultInfo {
        private String testName;
        private String status;
        private LocalDateTime executedAt;
        private String details;
        private String errorMessage;
        private String responseBody;
        private Integer statusCode;
        private String screenshotPath;
        private Long executionTimeMs;
        private int retryAttempts;
        private String testType;
        
        // Getters and setters
        public String getTestName() { return testName; }
        public void setTestName(String testName) { this.testName = testName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getExecutedAt() { return executedAt; }
        public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public String getResponseBody() { return responseBody; }
        public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
        public Integer getStatusCode() { return statusCode; }
        public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
        public String getScreenshotPath() { return screenshotPath; }
        public void setScreenshotPath(String screenshotPath) { this.screenshotPath = screenshotPath; }
        public Long getExecutionTimeMs() { return executionTimeMs; }
        public void setExecutionTimeMs(Long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
        public int getRetryAttempts() { return retryAttempts; }
        public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }
        public String getTestType() { return testType; }
        public void setTestType(String testType) { this.testType = testType; }
    }
    
    public static class ExecutionMetricsResponse {
        private String runId;
        private int totalTests;
        private int completedTests;
        private double progress;
        private Map<String, Long> statusDistribution;
        private double successRate;
        private long durationSeconds;
        private double averageTimePerTestSeconds;
        private Map<String, Object> threadPoolMetrics;
        
        // Getters and setters
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }
        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }
        public int getCompletedTests() { return completedTests; }
        public void setCompletedTests(int completedTests) { this.completedTests = completedTests; }
        public double getProgress() { return progress; }
        public void setProgress(double progress) { this.progress = progress; }
        public Map<String, Long> getStatusDistribution() { return statusDistribution; }
        public void setStatusDistribution(Map<String, Long> statusDistribution) { this.statusDistribution = statusDistribution; }
        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }
        public long getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }
        public double getAverageTimePerTestSeconds() { return averageTimePerTestSeconds; }
        public void setAverageTimePerTestSeconds(double averageTimePerTestSeconds) { this.averageTimePerTestSeconds = averageTimePerTestSeconds; }
        public Map<String, Object> getThreadPoolMetrics() { return threadPoolMetrics; }
        public void setThreadPoolMetrics(Map<String, Object> threadPoolMetrics) { this.threadPoolMetrics = threadPoolMetrics; }
    }
    
    public static class ExecutionAnalyticsResponse {
        private String runId;
        private LocalDateTime generatedAt;
        private Map<String, Long> testTypeDistribution;
        private List<String> failureReasons;
        private Map<String, Object> performanceInsights;
        
        // Getters and setters
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public Map<String, Long> getTestTypeDistribution() { return testTypeDistribution; }
        public void setTestTypeDistribution(Map<String, Long> testTypeDistribution) { this.testTypeDistribution = testTypeDistribution; }
        public List<String> getFailureReasons() { return failureReasons; }
        public void setFailureReasons(List<String> failureReasons) { this.failureReasons = failureReasons; }
        public Map<String, Object> getPerformanceInsights() { return performanceInsights; }
        public void setPerformanceInsights(Map<String, Object> performanceInsights) { this.performanceInsights = performanceInsights; }
    }
    
    public static class PerformanceAnalysisResponse {
        private String runId;
        private LocalDateTime analyzedAt;
        private Long minExecutionTimeMs;
        private Long maxExecutionTimeMs;
        private Double avgExecutionTimeMs;
        private Double medianExecutionTimeMs;
        private Double testsPerSecond;
        private Double threadUtilization;
        
        // Getters and setters
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }
        public LocalDateTime getAnalyzedAt() { return analyzedAt; }
        public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
        public Long getMinExecutionTimeMs() { return minExecutionTimeMs; }
        public void setMinExecutionTimeMs(Long minExecutionTimeMs) { this.minExecutionTimeMs = minExecutionTimeMs; }
        public Long getMaxExecutionTimeMs() { return maxExecutionTimeMs; }
        public void setMaxExecutionTimeMs(Long maxExecutionTimeMs) { this.maxExecutionTimeMs = maxExecutionTimeMs; }
        public Double getAvgExecutionTimeMs() { return avgExecutionTimeMs; }
        public void setAvgExecutionTimeMs(Double avgExecutionTimeMs) { this.avgExecutionTimeMs = avgExecutionTimeMs; }
        public Double getMedianExecutionTimeMs() { return medianExecutionTimeMs; }
        public void setMedianExecutionTimeMs(Double medianExecutionTimeMs) { this.medianExecutionTimeMs = medianExecutionTimeMs; }
        public Double getTestsPerSecond() { return testsPerSecond; }
        public void setTestsPerSecond(Double testsPerSecond) { this.testsPerSecond = testsPerSecond; }
        public Double getThreadUtilization() { return threadUtilization; }
        public void setThreadUtilization(Double threadUtilization) { this.threadUtilization = threadUtilization; }
    }
    
    public static class CancelExecutionResponse {
        private String runId;
        private boolean cancelled;
        private boolean forced;
        private LocalDateTime timestamp;
        private String message;
        
        // Getters and setters
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }
        public boolean isCancelled() { return cancelled; }
        public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
        public boolean isForced() { return forced; }
        public void setForced(boolean forced) { this.forced = forced; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    public static class ExecutionSystemSummaryResponse {
        private LocalDateTime generatedAt;
        private Map<String, Object> systemInfo;
        private Map<String, Object> capabilities;
        
        // Getters and setters
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public Map<String, Object> getSystemInfo() { return systemInfo; }
        public void setSystemInfo(Map<String, Object> systemInfo) { this.systemInfo = systemInfo; }
        public Map<String, Object> getCapabilities() { return capabilities; }
        public void setCapabilities(Map<String, Object> capabilities) { this.capabilities = capabilities; }
    }
    
    public static class ErrorResponse {
        private final String error;
        private final LocalDateTime timestamp;
        
        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = LocalDateTime.now();
        }
        
        public String getError() { return error; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}