package com.example.testframework.service.log;

import com.example.testframework.model.LogLevel;
import java.util.*;

/**
 * Configuration class for log collection behavior
 */
public class LogCollectionConfig {
    private Set<LogLevel> logLevels = EnumSet.allOf(LogLevel.class);
    private Set<String> categories = new HashSet<>();
    private List<String> includeKeywords = new ArrayList<>();
    private List<String> excludeKeywords = new ArrayList<>();
    private boolean includeSystemLogs = true;
    private boolean includeApplicationLogs = true;
    private boolean includeResponseBodies = false;
    private int maxResponseBodySize = 1000;
    private boolean autoArchive = false;
    private int maxLogEntries = 10000;
    private boolean compressArchives = true;

    // Getters and setters
    public Set<LogLevel> getLogLevels() { return logLevels; }
    public void setLogLevels(Set<LogLevel> logLevels) { this.logLevels = logLevels; }
    public Set<String> getCategories() { return categories; }
    public void setCategories(Set<String> categories) { this.categories = categories; }
    public List<String> getIncludeKeywords() { return includeKeywords; }
    public void setIncludeKeywords(List<String> includeKeywords) { this.includeKeywords = includeKeywords; }
    public List<String> getExcludeKeywords() { return excludeKeywords; }
    public void setExcludeKeywords(List<String> excludeKeywords) { this.excludeKeywords = excludeKeywords; }
    public boolean isIncludeSystemLogs() { return includeSystemLogs; }
    public void setIncludeSystemLogs(boolean includeSystemLogs) { this.includeSystemLogs = includeSystemLogs; }
    public boolean isIncludeApplicationLogs() { return includeApplicationLogs; }
    public void setIncludeApplicationLogs(boolean includeApplicationLogs) { this.includeApplicationLogs = includeApplicationLogs; }
    public boolean isIncludeResponseBodies() { return includeResponseBodies; }
    public void setIncludeResponseBodies(boolean includeResponseBodies) { this.includeResponseBodies = includeResponseBodies; }
    public int getMaxResponseBodySize() { return maxResponseBodySize; }
    public void setMaxResponseBodySize(int maxResponseBodySize) { this.maxResponseBodySize = maxResponseBodySize; }
    public boolean isAutoArchive() { return autoArchive; }
    public void setAutoArchive(boolean autoArchive) { this.autoArchive = autoArchive; }
    public int getMaxLogEntries() { return maxLogEntries; }
    public void setMaxLogEntries(int maxLogEntries) { this.maxLogEntries = maxLogEntries; }
    public boolean isCompressArchives() { return compressArchives; }
    public void setCompressArchives(boolean compressArchives) { this.compressArchives = compressArchives; }
}