package com.example.testframework.service.log;

import com.example.testframework.model.LogLevel;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Search criteria for advanced log searching
 */
public class LogSearchCriteria {
    private String searchText;
    private String regexPattern;
    private boolean caseSensitive = false;
    private boolean useRegex = false;
    private Set<LogLevel> logLevels;
    private Set<String> sources;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int maxResults = 100;

    // Getters and setters
    public String getSearchText() { return searchText; }
    public void setSearchText(String searchText) { this.searchText = searchText; }
    public String getRegexPattern() { return regexPattern; }
    public void setRegexPattern(String regexPattern) { this.regexPattern = regexPattern; }
    public boolean isCaseSensitive() { return caseSensitive; }
    public void setCaseSensitive(boolean caseSensitive) { this.caseSensitive = caseSensitive; }
    public boolean isUseRegex() { return useRegex; }
    public void setUseRegex(boolean useRegex) { this.useRegex = useRegex; }
    public Set<LogLevel> getLogLevels() { return logLevels; }
    public void setLogLevels(Set<LogLevel> logLevels) { this.logLevels = logLevels; }
    public Set<String> getSources() { return sources; }
    public void setSources(Set<String> sources) { this.sources = sources; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
}