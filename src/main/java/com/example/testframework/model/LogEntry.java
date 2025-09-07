package com.example.testframework.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Model class representing a single log entry with comprehensive metadata
 */
public class LogEntry {
    private LocalDateTime timestamp;
    private LogLevel level;
    private String source;
    private String message;
    private String runId;
    private Long testCaseId;
    private String testCaseName;
    private String category;
    private Map<String, Object> context;
    private String stackTrace;
    private String threadName;
    private long sequence;

    // Constructors
    public LogEntry() {
        this.timestamp = LocalDateTime.now();
        this.threadName = Thread.currentThread().getName();
    }

    public LogEntry(LogLevel level, String source, String message) {
        this();
        this.level = level;
        this.source = source;
        this.message = message;
    }

    // Getters and setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public LogLevel getLevel() {
        return level;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public Long getTestCaseId() {
        return testCaseId;
    }

    public void setTestCaseId(Long testCaseId) {
        this.testCaseId = testCaseId;
    }

    public String getTestCaseName() {
        return testCaseName;
    }

    public void setTestCaseName(String testCaseName) {
        this.testCaseName = testCaseName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public long getSequence() {
        return sequence;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }

    // Utility methods
    public boolean isError() {
        return level == LogLevel.ERROR;
    }

    public boolean isWarning() {
        return level == LogLevel.WARN;
    }

    public boolean isInfo() {
        return level == LogLevel.INFO;
    }

    public boolean isDebug() {
        return level == LogLevel.DEBUG;
    }

    public String getFormattedMessage() {
        return String.format("[%s] %s [%s] %s - %s",
                timestamp, level, source, category != null ? category : "GENERAL", message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry logEntry = (LogEntry) o;
        return sequence == logEntry.sequence &&
                Objects.equals(timestamp, logEntry.timestamp) &&
                level == logEntry.level &&
                Objects.equals(source, logEntry.source) &&
                Objects.equals(message, logEntry.message) &&
                Objects.equals(runId, logEntry.runId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, level, source, message, runId, sequence);
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "timestamp=" + timestamp +
                ", level=" + level +
                ", source='" + source + '\'' +
                ", message='" + message + '\'' +
                ", runId='" + runId + '\'' +
                ", category='" + category + '\'' +
                '}';
    }
}