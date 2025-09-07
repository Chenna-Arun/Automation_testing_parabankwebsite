package com.example.testframework.service.log;

import com.example.testframework.model.LogLevel;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Log analysis results
 */
public class LogAnalysisResult {
    private String collectionId;
    private LocalDateTime analyzedAt;
    private int totalLogEntries;
    private Map<LogLevel, Long> levelDistribution;
    private Map<String, Long> categoryDistribution;
    private List<String> commonErrorPatterns;
    private Map<String, Object> timeRange;
    private Map<String, Object> performanceInsights;
    private List<String> recommendations = new ArrayList<>();

    // Getters and setters
    public String getCollectionId() { return collectionId; }
    public void setCollectionId(String collectionId) { this.collectionId = collectionId; }
    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
    public int getTotalLogEntries() { return totalLogEntries; }
    public void setTotalLogEntries(int totalLogEntries) { this.totalLogEntries = totalLogEntries; }
    public Map<LogLevel, Long> getLevelDistribution() { return levelDistribution; }
    public void setLevelDistribution(Map<LogLevel, Long> levelDistribution) { this.levelDistribution = levelDistribution; }
    public Map<String, Long> getCategoryDistribution() { return categoryDistribution; }
    public void setCategoryDistribution(Map<String, Long> categoryDistribution) { this.categoryDistribution = categoryDistribution; }
    public List<String> getCommonErrorPatterns() { return commonErrorPatterns; }
    public void setCommonErrorPatterns(List<String> commonErrorPatterns) { this.commonErrorPatterns = commonErrorPatterns; }
    public Map<String, Object> getTimeRange() { return timeRange; }
    public void setTimeRange(Map<String, Object> timeRange) { this.timeRange = timeRange; }
    public Map<String, Object> getPerformanceInsights() { return performanceInsights; }
    public void setPerformanceInsights(Map<String, Object> performanceInsights) { this.performanceInsights = performanceInsights; }
    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
}