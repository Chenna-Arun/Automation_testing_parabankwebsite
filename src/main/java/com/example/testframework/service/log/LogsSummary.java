package com.example.testframework.service.log;

import com.example.testframework.model.LogLevel;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Summary statistics for log collections
 */
public class LogsSummary {
    private int totalEntries;
    private Map<LogLevel, Long> levelCounts = new HashMap<>();
    private Map<String, Long> categoryCounts = new HashMap<>();
    private LocalDateTime firstLogTime;
    private LocalDateTime lastLogTime;
    private double errorRate;
    private List<String> topErrorMessages = new ArrayList<>();
    private Map<String, Object> additionalMetrics = new HashMap<>();

    // Getters and setters
    public int getTotalEntries() { return totalEntries; }
    public void setTotalEntries(int totalEntries) { this.totalEntries = totalEntries; }
    public Map<LogLevel, Long> getLevelCounts() { return levelCounts; }
    public void setLevelCounts(Map<LogLevel, Long> levelCounts) { this.levelCounts = levelCounts; }
    public Map<String, Long> getCategoryCounts() { return categoryCounts; }
    public void setCategoryCounts(Map<String, Long> categoryCounts) { this.categoryCounts = categoryCounts; }
    public LocalDateTime getFirstLogTime() { return firstLogTime; }
    public void setFirstLogTime(LocalDateTime firstLogTime) { this.firstLogTime = firstLogTime; }
    public LocalDateTime getLastLogTime() { return lastLogTime; }
    public void setLastLogTime(LocalDateTime lastLogTime) { this.lastLogTime = lastLogTime; }
    public double getErrorRate() { return errorRate; }
    public void setErrorRate(double errorRate) { this.errorRate = errorRate; }
    public List<String> getTopErrorMessages() { return topErrorMessages; }
    public void setTopErrorMessages(List<String> topErrorMessages) { this.topErrorMessages = topErrorMessages; }
    public Map<String, Object> getAdditionalMetrics() { return additionalMetrics; }
    public void setAdditionalMetrics(Map<String, Object> additionalMetrics) { this.additionalMetrics = additionalMetrics; }
}