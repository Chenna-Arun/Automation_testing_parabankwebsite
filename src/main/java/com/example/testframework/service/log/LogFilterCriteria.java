package com.example.testframework.service.log;

import com.example.testframework.model.LogLevel;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Criteria for filtering logs
 */
public class LogFilterCriteria {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Set<LogLevel> logLevels;
    private Set<String> categories;
    private List<Long> testCaseIds;
    private String runId;
    private String searchText;
    private int limit = 1000;
    private int offset = 0;

    // Getters and setters
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public Set<LogLevel> getLogLevels() { return logLevels; }
    public void setLogLevels(Set<LogLevel> logLevels) { this.logLevels = logLevels; }
    public Set<String> getCategories() { return categories; }
    public void setCategories(Set<String> categories) { this.categories = categories; }
    public List<Long> getTestCaseIds() { return testCaseIds; }
    public void setTestCaseIds(List<Long> testCaseIds) { this.testCaseIds = testCaseIds; }
    public String getRunId() { return runId; }
    public void setRunId(String runId) { this.runId = runId; }
    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }
}