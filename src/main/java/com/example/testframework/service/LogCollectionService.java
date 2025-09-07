package com.example.testframework.service;

import com.example.testframework.model.TestCase;
import com.example.testframework.model.LogEntry;
import com.example.testframework.model.LogLevel;
import com.example.testframework.dto.TestResultDTO;
import com.example.testframework.service.log.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * Service for comprehensive log collection, aggregation, and management
 * Features: Real-time log collection, filtering, storage, archiving, and analysis
 */
@Service
public class LogCollectionService {

    private final String logsPath;
    private final String archivePath;
    private final ExecutionService executionService;
    
    // Thread-safe log storage
    private final Map<String, List<LogEntry>> runLogs = new ConcurrentHashMap<>();
    private final Map<String, LogCollectionConfig> logConfigs = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Pattern cache for performance
    private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();
    
    @Autowired
    public LogCollectionService(@Value("${app.logs.path:./logs}") String logsPath,
                               @Value("${app.logs.archive:./logs/archive}") String archivePath,
                               ExecutionService executionService) {
        this.logsPath = logsPath;
        this.archivePath = archivePath;
        this.executionService = executionService;
        initializeLogDirectories();
    }

    // ===========================
    // LOG COLLECTION METHODS
    // ===========================

    /**
     * Start log collection for a test run
     */
    public String startLogCollection(String runId, LogCollectionConfig config) {
        String collectionId = "log_collection_" + runId + "_" + System.currentTimeMillis();
        
        lock.writeLock().lock();
        try {
            logConfigs.put(collectionId, config);
            runLogs.put(collectionId, new ArrayList<>());
            
            // Create initial log entry
            LogEntry startEntry = new LogEntry();
            startEntry.setTimestamp(LocalDateTime.now());
            startEntry.setLevel(LogLevel.INFO);
            startEntry.setSource("LogCollectionService");
            startEntry.setMessage("Log collection started for run: " + runId);
            startEntry.setRunId(runId);
            startEntry.setCategory("SYSTEM");
            
            runLogs.get(collectionId).add(startEntry);
            
        } finally {
            lock.writeLock().unlock();
        }
        
        return collectionId;
    }

    /**
     * Collect logs from test execution
     */
    public void collectTestExecutionLogs(String collectionId, String runId, TestResultDTO testResult) {
        LogCollectionConfig config = logConfigs.get(collectionId);
        if (config == null) {
            return;
        }

        lock.writeLock().lock();
        try {
            List<LogEntry> logs = runLogs.computeIfAbsent(collectionId, k -> new ArrayList<>());
            
            // Create log entry for test execution
            LogEntry testEntry = new LogEntry();
            testEntry.setTimestamp(LocalDateTime.now());
            testEntry.setLevel(getLogLevelFromStatus(testResult.getStatus()));
            testEntry.setSource("TestExecution");
            testEntry.setMessage(formatTestMessage(testResult));
            testEntry.setRunId(runId);
            testEntry.setTestCaseId(testResult.getTestCaseId());
            testEntry.setTestCaseName(testResult.getTestCaseName());
            testEntry.setCategory("TEST_EXECUTION");
            
            // Add additional context
            Map<String, Object> context = new HashMap<>();
            context.put("status", testResult.getStatus());
            context.put("statusCode", testResult.getStatusCode());
            context.put("executionDetails", testResult.getDetails());
            if (testResult.getErrorMessage() != null) {
                context.put("errorMessage", testResult.getErrorMessage());
            }
            if (testResult.getResponseBody() != null && config.isIncludeResponseBodies()) {
                context.put("responseBody", truncateIfNeeded(testResult.getResponseBody(), config.getMaxResponseBodySize()));
            }
            testEntry.setContext(context);
            
            if (shouldIncludeLog(testEntry, config)) {
                logs.add(testEntry);
            }
            
            // Collect error logs if test failed
            if ("FAILED".equals(testResult.getStatus()) && testResult.getErrorMessage() != null) {
                LogEntry errorEntry = new LogEntry();
                errorEntry.setTimestamp(LocalDateTime.now());
                errorEntry.setLevel(LogLevel.ERROR);
                errorEntry.setSource("TestExecution");
                errorEntry.setMessage("Test Failure: " + testResult.getErrorMessage());
                errorEntry.setRunId(runId);
                errorEntry.setTestCaseId(testResult.getTestCaseId());
                errorEntry.setTestCaseName(testResult.getTestCaseName());
                errorEntry.setCategory("TEST_ERROR");
                errorEntry.setContext(Map.of("fullError", testResult.getErrorMessage()));
                
                if (shouldIncludeLog(errorEntry, config)) {
                    logs.add(errorEntry);
                }
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Collect system logs (execution metrics, performance data)
     */
    public void collectSystemLogs(String collectionId, String runId, Map<String, Object> systemMetrics) {
        LogCollectionConfig config = logConfigs.get(collectionId);
        if (config == null || !config.isIncludeSystemLogs()) {
            return;
        }

        lock.writeLock().lock();
        try {
            List<LogEntry> logs = runLogs.computeIfAbsent(collectionId, k -> new ArrayList<>());
            
            LogEntry systemEntry = new LogEntry();
            systemEntry.setTimestamp(LocalDateTime.now());
            systemEntry.setLevel(LogLevel.INFO);
            systemEntry.setSource("SystemMetrics");
            systemEntry.setMessage("System metrics collected");
            systemEntry.setRunId(runId);
            systemEntry.setCategory("SYSTEM_METRICS");
            systemEntry.setContext(new HashMap<>(systemMetrics));
            
            if (shouldIncludeLog(systemEntry, config)) {
                logs.add(systemEntry);
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Collect application logs from external sources
     */
    public void collectApplicationLogs(String collectionId, String runId, String logSource, List<String> logLines) {
        LogCollectionConfig config = logConfigs.get(collectionId);
        if (config == null || !config.isIncludeApplicationLogs()) {
            return;
        }

        lock.writeLock().lock();
        try {
            List<LogEntry> logs = runLogs.computeIfAbsent(collectionId, k -> new ArrayList<>());
            
            for (String logLine : logLines) {
                LogEntry appEntry = parseApplicationLogLine(logLine, logSource, runId);
                if (appEntry != null && shouldIncludeLog(appEntry, config)) {
                    logs.add(appEntry);
                }
            }
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ===========================
    // LOG RETRIEVAL AND ANALYSIS
    // ===========================

    /**
     * Get logs for a specific collection
     */
    public List<LogEntry> getLogs(String collectionId, LogFilterCriteria filter) {
        lock.readLock().lock();
        try {
            List<LogEntry> logs = runLogs.get(collectionId);
            if (logs == null) {
                return new ArrayList<>();
            }
            
            return filterLogs(logs, filter);
            
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get logs summary and statistics
     */
    public LogsSummary getLogsSummary(String collectionId) {
        lock.readLock().lock();
        try {
            List<LogEntry> logs = runLogs.get(collectionId);
            if (logs == null) {
                return new LogsSummary();
            }
            
            return calculateLogsSummary(logs);
            
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Export logs to various formats
     */
    public String exportLogs(String collectionId, LogExportFormat format, LogFilterCriteria filter) throws IOException {
        List<LogEntry> logs = getLogs(collectionId, filter);
        
        String fileName = generateExportFileName(collectionId, format);
        String filePath = Paths.get(logsPath, fileName).toString();
        
        switch (format) {
            case JSON:
                exportLogsAsJson(logs, filePath);
                break;
            case CSV:
                exportLogsAsCsv(logs, filePath);
                break;
            case TXT:
                exportLogsAsText(logs, filePath);
                break;
            case XML:
                exportLogsAsXml(logs, filePath);
                break;
        }
        
        return filePath;
    }

    /**
     * Archive old logs
     */
    public void archiveLogs(String collectionId, boolean compress) throws IOException {
        lock.writeLock().lock();
        try {
            List<LogEntry> logs = runLogs.get(collectionId);
            if (logs == null) {
                return;
            }
            
            String archiveFileName = generateArchiveFileName(collectionId, compress);
            String archiveFilePath = Paths.get(archivePath, archiveFileName).toString();
            
            if (compress) {
                exportLogsAsCompressed(logs, archiveFilePath);
            } else {
                exportLogsAsJson(logs, archiveFilePath);
            }
            
            // Remove from memory after archiving
            runLogs.remove(collectionId);
            logConfigs.remove(collectionId);
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ===========================
    // LOG ANALYSIS METHODS
    // ===========================

    /**
     * Analyze logs for patterns and insights
     */
    public LogAnalysisResult analyzeLogs(String collectionId) {
        List<LogEntry> logs = getLogs(collectionId, new LogFilterCriteria());
        
        LogAnalysisResult analysis = new LogAnalysisResult();
        analysis.setCollectionId(collectionId);
        analysis.setAnalyzedAt(LocalDateTime.now());
        analysis.setTotalLogEntries(logs.size());
        
        // Analyze by log level
        Map<LogLevel, Long> levelDistribution = logs.stream()
            .collect(Collectors.groupingBy(LogEntry::getLevel, Collectors.counting()));
        analysis.setLevelDistribution(levelDistribution);
        
        // Analyze by category
        Map<String, Long> categoryDistribution = logs.stream()
            .collect(Collectors.groupingBy(LogEntry::getCategory, Collectors.counting()));
        analysis.setCategoryDistribution(categoryDistribution);
        
        // Find error patterns
        List<String> errorPatterns = findErrorPatterns(logs);
        analysis.setCommonErrorPatterns(errorPatterns);
        
        // Calculate time-based metrics
        if (!logs.isEmpty()) {
            LocalDateTime firstLog = logs.get(0).getTimestamp();
            LocalDateTime lastLog = logs.get(logs.size() - 1).getTimestamp();
            analysis.setTimeRange(Map.of(
                "start", firstLog,
                "end", lastLog,
                "durationMinutes", java.time.Duration.between(firstLog, lastLog).toMinutes()
            ));
        }
        
        // Performance insights
        analysis.setPerformanceInsights(analyzePerformance(logs));
        
        return analysis;
    }

    /**
     * Search logs with advanced criteria
     */
    public List<LogEntry> searchLogs(String collectionId, LogSearchCriteria searchCriteria) {
        List<LogEntry> logs = getLogs(collectionId, new LogFilterCriteria());
        
        return logs.stream()
            .filter(log -> matchesSearchCriteria(log, searchCriteria))
            .collect(Collectors.toList());
    }

    // ===========================
    // HELPER METHODS
    // ===========================

    private void initializeLogDirectories() {
        try {
            Files.createDirectories(Paths.get(logsPath));
            Files.createDirectories(Paths.get(archivePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize log directories: " + e.getMessage(), e);
        }
    }

    private LogLevel getLogLevelFromStatus(String status) {
        return switch (status) {
            case "PASSED" -> LogLevel.INFO;
            case "FAILED" -> LogLevel.ERROR;
            case "SKIPPED" -> LogLevel.WARN;
            default -> LogLevel.DEBUG;
        };
    }

    private String formatTestMessage(TestResultDTO testResult) {
        StringBuilder message = new StringBuilder();
        message.append("Test ").append(testResult.getStatus());
        message.append(": ").append(testResult.getTestCaseName());
        
        if (testResult.getStatusCode() != null) {
            message.append(" [").append(testResult.getStatusCode()).append("]");
        }
        
        if (testResult.getDetails() != null) {
            message.append(" - ").append(truncateIfNeeded(testResult.getDetails(), 200));
        }
        
        return message.toString();
    }

    private boolean shouldIncludeLog(LogEntry logEntry, LogCollectionConfig config) {
        // Check log level filter
        if (!config.getLogLevels().contains(logEntry.getLevel())) {
            return false;
        }
        
        // Check category filter
        if (!config.getCategories().isEmpty() && !config.getCategories().contains(logEntry.getCategory())) {
            return false;
        }
        
        // Check keyword filters
        if (!config.getIncludeKeywords().isEmpty()) {
            boolean hasKeyword = config.getIncludeKeywords().stream()
                .anyMatch(keyword -> logEntry.getMessage().toLowerCase().contains(keyword.toLowerCase()));
            if (!hasKeyword) {
                return false;
            }
        }
        
        // Check exclude keywords
        if (!config.getExcludeKeywords().isEmpty()) {
            boolean hasExcludeKeyword = config.getExcludeKeywords().stream()
                .anyMatch(keyword -> logEntry.getMessage().toLowerCase().contains(keyword.toLowerCase()));
            if (hasExcludeKeyword) {
                return false;
            }
        }
        
        return true;
    }

    private List<LogEntry> filterLogs(List<LogEntry> logs, LogFilterCriteria filter) {
        if (filter == null) {
            return logs;
        }
        
        return logs.stream()
            .filter(log -> {
                // Time range filter
                if (filter.getStartTime() != null && log.getTimestamp().isBefore(filter.getStartTime())) {
                    return false;
                }
                if (filter.getEndTime() != null && log.getTimestamp().isAfter(filter.getEndTime())) {
                    return false;
                }
                
                // Level filter
                if (filter.getLogLevels() != null && !filter.getLogLevels().isEmpty() && 
                    !filter.getLogLevels().contains(log.getLevel())) {
                    return false;
                }
                
                // Category filter
                if (filter.getCategories() != null && !filter.getCategories().isEmpty() && 
                    !filter.getCategories().contains(log.getCategory())) {
                    return false;
                }
                
                // Test case filter
                if (filter.getTestCaseIds() != null && !filter.getTestCaseIds().isEmpty() && 
                    !filter.getTestCaseIds().contains(log.getTestCaseId())) {
                    return false;
                }
                
                return true;
            })
            .collect(Collectors.toList());
    }

    private LogEntry parseApplicationLogLine(String logLine, String logSource, String runId) {
        // Simple log parsing - in real implementation would be more sophisticated
        try {
            LogEntry entry = new LogEntry();
            entry.setTimestamp(LocalDateTime.now());
            entry.setSource(logSource);
            entry.setMessage(logLine);
            entry.setRunId(runId);
            entry.setCategory("APPLICATION");
            
            // Extract log level from line if possible
            if (logLine.toLowerCase().contains("error")) {
                entry.setLevel(LogLevel.ERROR);
            } else if (logLine.toLowerCase().contains("warn")) {
                entry.setLevel(LogLevel.WARN);
            } else if (logLine.toLowerCase().contains("debug")) {
                entry.setLevel(LogLevel.DEBUG);
            } else {
                entry.setLevel(LogLevel.INFO);
            }
            
            return entry;
        } catch (Exception e) {
            // Return null if parsing fails
            return null;
        }
    }

    private LogsSummary calculateLogsSummary(List<LogEntry> logs) {
        LogsSummary summary = new LogsSummary();
        summary.setTotalEntries(logs.size());
        
        // Count by level
        Map<LogLevel, Long> levelCounts = logs.stream()
            .collect(Collectors.groupingBy(LogEntry::getLevel, Collectors.counting()));
        summary.setLevelCounts(levelCounts);
        
        // Count by category
        Map<String, Long> categoryCounts = logs.stream()
            .collect(Collectors.groupingBy(LogEntry::getCategory, Collectors.counting()));
        summary.setCategoryCounts(categoryCounts);
        
        // Time range
        if (!logs.isEmpty()) {
            summary.setFirstLogTime(logs.get(0).getTimestamp());
            summary.setLastLogTime(logs.get(logs.size() - 1).getTimestamp());
        }
        
        // Error rate
        long errorCount = levelCounts.getOrDefault(LogLevel.ERROR, 0L);
        summary.setErrorRate(logs.size() > 0 ? (double) errorCount / logs.size() * 100 : 0.0);
        
        return summary;
    }

    private String truncateIfNeeded(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private String generateExportFileName(String collectionId, LogExportFormat format) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return String.format("logs_%s_%s.%s", collectionId, timestamp, format.name().toLowerCase());
    }

    private String generateArchiveFileName(String collectionId, boolean compress) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String extension = compress ? ".json.gz" : ".json";
        return String.format("archived_logs_%s_%s%s", collectionId, timestamp, extension);
    }

    // Export methods implementation would be here
    private void exportLogsAsJson(List<LogEntry> logs, String filePath) throws IOException {
        // Implementation for JSON export
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("[\n");
            for (int i = 0; i < logs.size(); i++) {
                writer.write(logEntryToJson(logs.get(i)));
                if (i < logs.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }
            writer.write("]");
        }
    }

    private void exportLogsAsCsv(List<LogEntry> logs, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // CSV header
            writer.write("timestamp,level,source,category,message,runId,testCaseId,testCaseName\n");
            
            for (LogEntry log : logs) {
                writer.write(String.format("%s,%s,%s,%s,\"%s\",%s,%s,%s\n",
                    log.getTimestamp(),
                    log.getLevel(),
                    log.getSource(),
                    log.getCategory(),
                    log.getMessage().replace("\"", "\"\""), // Escape quotes
                    log.getRunId() != null ? log.getRunId() : "",
                    log.getTestCaseId() != null ? log.getTestCaseId() : "",
                    log.getTestCaseName() != null ? log.getTestCaseName() : ""
                ));
            }
        }
    }

    private void exportLogsAsText(List<LogEntry> logs, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            for (LogEntry log : logs) {
                writer.write(String.format("[%s] %s [%s] %s - %s\n",
                    log.getTimestamp(),
                    log.getLevel(),
                    log.getSource(),
                    log.getCategory(),
                    log.getMessage()
                ));
            }
        }
    }

    private void exportLogsAsXml(List<LogEntry> logs, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<logs>\n");
            
            for (LogEntry log : logs) {
                writer.write(String.format("  <logEntry timestamp=\"%s\" level=\"%s\" source=\"%s\" category=\"%s\">\n",
                    log.getTimestamp(), log.getLevel(), log.getSource(), log.getCategory()));
                writer.write(String.format("    <message><![CDATA[%s]]></message>\n", log.getMessage()));
                if (log.getRunId() != null) {
                    writer.write(String.format("    <runId>%s</runId>\n", log.getRunId()));
                }
                writer.write("  </logEntry>\n");
            }
            
            writer.write("</logs>");
        }
    }

    private void exportLogsAsCompressed(List<LogEntry> logs, String filePath) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath);
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             OutputStreamWriter writer = new OutputStreamWriter(gzos)) {
            
            writer.write("[\n");
            for (int i = 0; i < logs.size(); i++) {
                writer.write(logEntryToJson(logs.get(i)));
                if (i < logs.size() - 1) {
                    writer.write(",");
                }
                writer.write("\n");
            }
            writer.write("]");
        }
    }

    private String logEntryToJson(LogEntry log) {
        // Simple JSON serialization - in real implementation would use ObjectMapper
        StringBuilder json = new StringBuilder();
        json.append("  {\n");
        json.append("    \"timestamp\": \"").append(log.getTimestamp()).append("\",\n");
        json.append("    \"level\": \"").append(log.getLevel()).append("\",\n");
        json.append("    \"source\": \"").append(log.getSource()).append("\",\n");
        json.append("    \"category\": \"").append(log.getCategory()).append("\",\n");
        json.append("    \"message\": \"").append(log.getMessage().replace("\"", "\\\"")).append("\"");
        
        if (log.getRunId() != null) {
            json.append(",\n    \"runId\": \"").append(log.getRunId()).append("\"");
        }
        if (log.getTestCaseId() != null) {
            json.append(",\n    \"testCaseId\": \"").append(log.getTestCaseId()).append("\"");
        }
        if (log.getTestCaseName() != null) {
            json.append(",\n    \"testCaseName\": \"").append(log.getTestCaseName()).append("\"");
        }
        
        json.append("\n  }");
        return json.toString();
    }

    private List<String> findErrorPatterns(List<LogEntry> logs) {
        // Analyze error logs to find common patterns
        Map<String, Integer> errorPatterns = new HashMap<>();
        
        logs.stream()
            .filter(log -> log.getLevel() == LogLevel.ERROR)
            .forEach(log -> {
                String message = log.getMessage();
                // Extract error patterns (simplified)
                if (message.contains("Exception")) {
                    String pattern = extractExceptionType(message);
                    errorPatterns.merge(pattern, 1, Integer::sum);
                }
            });
        
        return errorPatterns.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    private String extractExceptionType(String message) {
        // Simple pattern extraction
        int exceptionIndex = message.indexOf("Exception");
        if (exceptionIndex > 0) {
            int start = Math.max(0, exceptionIndex - 30);
            return message.substring(start, Math.min(message.length(), exceptionIndex + 9));
        }
        return "Unknown Exception";
    }

    private Map<String, Object> analyzePerformance(List<LogEntry> logs) {
        Map<String, Object> insights = new HashMap<>();
        
        // Calculate log frequency
        if (logs.size() > 1) {
            LocalDateTime first = logs.get(0).getTimestamp();
            LocalDateTime last = logs.get(logs.size() - 1).getTimestamp();
            long durationMinutes = java.time.Duration.between(first, last).toMinutes();
            if (durationMinutes > 0) {
                insights.put("logsPerMinute", (double) logs.size() / durationMinutes);
            }
        }
        
        // Calculate error frequency
        long errorCount = logs.stream()
            .mapToLong(log -> log.getLevel() == LogLevel.ERROR ? 1 : 0)
            .sum();
        insights.put("errorRate", logs.size() > 0 ? (double) errorCount / logs.size() * 100 : 0.0);
        
        return insights;
    }

    private boolean matchesSearchCriteria(LogEntry log, LogSearchCriteria criteria) {
        // Implement search logic
        if (criteria.getSearchText() != null && !criteria.getSearchText().isEmpty()) {
            String searchText = criteria.getSearchText().toLowerCase();
            if (!log.getMessage().toLowerCase().contains(searchText)) {
                return false;
            }
        }
        
        if (criteria.getRegexPattern() != null && !criteria.getRegexPattern().isEmpty()) {
            Pattern pattern = patternCache.computeIfAbsent(criteria.getRegexPattern(), Pattern::compile);
            if (!pattern.matcher(log.getMessage()).find()) {
                return false;
            }
        }
        
        return true;
    }
}