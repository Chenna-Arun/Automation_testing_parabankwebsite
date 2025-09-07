package com.example.testframework.controller;

import com.example.testframework.service.LogCollectionService;
import com.example.testframework.service.log.*;
import com.example.testframework.model.LogEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Controller for comprehensive log collection and management
 * Features: Log collection, retrieval, analysis, export, and archiving
 */
@RestController
@RequestMapping("/logs")
public class LogController {

    private final LogCollectionService logCollectionService;

    @Autowired
    public LogController(LogCollectionService logCollectionService) {
        this.logCollectionService = logCollectionService;
    }

    // ===========================
    // POST /logs/collect
    // Start log collection for a test run
    // ===========================
    @PostMapping("/collect")
    public ResponseEntity<?> startLogCollection(@RequestBody LogCollectionRequest request) {
        try {
            // Validate request
            if (request.getRunId() == null || request.getRunId().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Run ID is required"));
            }

            // Create log collection configuration
            LogCollectionConfig config = createLogCollectionConfig(request);
            
            // Start log collection
            String collectionId = logCollectionService.startLogCollection(request.getRunId(), config);
            
            LogCollectionResponse response = new LogCollectionResponse();
            response.setCollectionId(collectionId);
            response.setRunId(request.getRunId());
            response.setStartedAt(LocalDateTime.now());
            response.setStatus("ACTIVE");
            response.setMessage("Log collection started successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to start log collection: " + e.getMessage()));
        }
    }

    // ===========================
    // GET /logs/{collectionId}
    // Retrieve logs for a specific collection
    // ===========================
    @GetMapping("/{collectionId}")
    public ResponseEntity<?> getLogs(
            @PathVariable String collectionId,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String searchText,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        
        try {
            LogFilterCriteria filter = new LogFilterCriteria();
            
            // Apply filters if provided
            if (level != null) {
                filter.setLogLevels(Set.of(com.example.testframework.model.LogLevel.valueOf(level.toUpperCase())));
            }
            if (category != null) {
                filter.setCategories(Set.of(category));
            }
            if (startTime != null) {
                filter.setStartTime(LocalDateTime.parse(startTime));
            }
            if (endTime != null) {
                filter.setEndTime(LocalDateTime.parse(endTime));
            }
            if (searchText != null) {
                filter.setSearchText(searchText);
            }
            filter.setLimit(limit);
            filter.setOffset(offset);
            
            List<LogEntry> logs = logCollectionService.getLogs(collectionId, filter);
            
            LogRetrievalResponse response = new LogRetrievalResponse();
            response.setCollectionId(collectionId);
            response.setTotalEntries(logs.size());
            response.setLimit(limit);
            response.setOffset(offset);
            response.setLogs(logs);
            response.setRetrievedAt(LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to retrieve logs: " + e.getMessage()));
        }
    }

    // ===========================
    // GET /logs/{collectionId}/summary
    // Get log collection summary and statistics
    // ===========================
    @GetMapping("/{collectionId}/summary")
    public ResponseEntity<?> getLogsSummary(@PathVariable String collectionId) {
        try {
            LogsSummary summary = logCollectionService.getLogsSummary(collectionId);
            
            LogSummaryResponse response = new LogSummaryResponse();
            response.setCollectionId(collectionId);
            response.setSummary(summary);
            response.setGeneratedAt(LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to get logs summary: " + e.getMessage()));
        }
    }

    // ===========================
    // GET /logs/{collectionId}/analyze
    // Analyze logs for patterns and insights
    // ===========================
    @GetMapping("/{collectionId}/analyze")
    public ResponseEntity<?> analyzeLogs(@PathVariable String collectionId) {
        try {
            LogAnalysisResult analysis = logCollectionService.analyzeLogs(collectionId);
            
            LogAnalysisResponse response = new LogAnalysisResponse();
            response.setCollectionId(collectionId);
            response.setAnalysis(analysis);
            response.setGeneratedAt(LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to analyze logs: " + e.getMessage()));
        }
    }

    // ===========================
    // POST /logs/{collectionId}/search
    // Search logs with advanced criteria
    // ===========================
    @PostMapping("/{collectionId}/search")
    public ResponseEntity<?> searchLogs(
            @PathVariable String collectionId,
            @RequestBody LogSearchRequest searchRequest) {
        
        try {
            LogSearchCriteria criteria = new LogSearchCriteria();
            criteria.setSearchText(searchRequest.getSearchText());
            criteria.setRegexPattern(searchRequest.getRegexPattern());
            criteria.setCaseSensitive(searchRequest.isCaseSensitive());
            criteria.setUseRegex(searchRequest.isUseRegex());
            criteria.setMaxResults(searchRequest.getMaxResults());
            
            if (searchRequest.getLogLevels() != null) {
                Set<com.example.testframework.model.LogLevel> levels = new HashSet<>();
                for (String level : searchRequest.getLogLevels()) {
                    levels.add(com.example.testframework.model.LogLevel.valueOf(level.toUpperCase()));
                }
                criteria.setLogLevels(levels);
            }
            
            List<LogEntry> searchResults = logCollectionService.searchLogs(collectionId, criteria);
            
            LogSearchResponse response = new LogSearchResponse();
            response.setCollectionId(collectionId);
            response.setSearchCriteria(searchRequest);
            response.setResults(searchResults);
            response.setResultCount(searchResults.size());
            response.setSearchedAt(LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to search logs: " + e.getMessage()));
        }
    }

    // ===========================
    // GET /logs/{collectionId}/export
    // Export logs in various formats
    // ===========================
    @GetMapping("/{collectionId}/export")
    public ResponseEntity<?> exportLogs(
            @PathVariable String collectionId,
            @RequestParam(defaultValue = "JSON") String format,
            @RequestParam(defaultValue = "false") boolean download,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        try {
            // Create filter criteria
            LogFilterCriteria filter = new LogFilterCriteria();
            if (level != null) {
                filter.setLogLevels(Set.of(com.example.testframework.model.LogLevel.valueOf(level.toUpperCase())));
            }
            if (category != null) {
                filter.setCategories(Set.of(category));
            }
            if (startTime != null) {
                filter.setStartTime(LocalDateTime.parse(startTime));
            }
            if (endTime != null) {
                filter.setEndTime(LocalDateTime.parse(endTime));
            }
            
            // Export logs
            LogExportFormat exportFormat;
            try {
                exportFormat = LogExportFormat.valueOf(format.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Unsupported export format: " + format));
            }
            
            String exportPath = logCollectionService.exportLogs(collectionId, exportFormat, filter);
            
            if (download) {
                return downloadLogExport(exportPath, exportFormat);
            } else {
                LogExportResponse response = new LogExportResponse();
                response.setCollectionId(collectionId);
                response.setFormat(format.toUpperCase());
                response.setExportPath(exportPath);
                response.setExportedAt(LocalDateTime.now());
                response.setDownloadUrl("/logs/" + collectionId + "/download/" + extractFilename(exportPath));
                
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to export logs: " + e.getMessage()));
        }
    }

    // ===========================
    // GET /logs/{collectionId}/download/{filename}
    // Download exported log files
    // ===========================
    @GetMapping("/{collectionId}/download/{filename}")
    public ResponseEntity<Resource> downloadLogExport(
            @PathVariable String collectionId,
            @PathVariable String filename) {
        
        try {
            // Reconstruct file path (in real implementation, this would be stored/tracked)
            String filePath = "./logs/" + filename;
            File file = new File(filePath);
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            String contentType = determineContentType(filename);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // ===========================
    // POST /logs/{collectionId}/archive
    // Archive log collection
    // ===========================
    @PostMapping("/{collectionId}/archive")
    public ResponseEntity<?> archiveLogs(
            @PathVariable String collectionId,
            @RequestParam(defaultValue = "true") boolean compress) {
        
        try {
            logCollectionService.archiveLogs(collectionId, compress);
            
            LogArchiveResponse response = new LogArchiveResponse();
            response.setCollectionId(collectionId);
            response.setCompressed(compress);
            response.setArchivedAt(LocalDateTime.now());
            response.setMessage("Logs archived successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to archive logs: " + e.getMessage()));
        }
    }

    // ===========================
    // HELPER METHODS
    // ===========================

    private LogCollectionConfig createLogCollectionConfig(LogCollectionRequest request) {
        LogCollectionConfig config = new LogCollectionConfig();
        
        if (request.getLogLevels() != null && !request.getLogLevels().isEmpty()) {
            Set<com.example.testframework.model.LogLevel> levels = new HashSet<>();
            for (String level : request.getLogLevels()) {
                levels.add(com.example.testframework.model.LogLevel.valueOf(level.toUpperCase()));
            }
            config.setLogLevels(levels);
        }
        
        if (request.getCategories() != null) {
            config.setCategories(new HashSet<>(request.getCategories()));
        }
        
        if (request.getIncludeKeywords() != null) {
            config.setIncludeKeywords(request.getIncludeKeywords());
        }
        
        if (request.getExcludeKeywords() != null) {
            config.setExcludeKeywords(request.getExcludeKeywords());
        }
        
        config.setIncludeSystemLogs(request.isIncludeSystemLogs());
        config.setIncludeApplicationLogs(request.isIncludeApplicationLogs());
        config.setIncludeResponseBodies(request.isIncludeResponseBodies());
        config.setMaxResponseBodySize(request.getMaxResponseBodySize());
        config.setAutoArchive(request.isAutoArchive());
        config.setMaxLogEntries(request.getMaxLogEntries());
        config.setCompressArchives(request.isCompressArchives());
        
        return config;
    }

    private ResponseEntity<Resource> downloadLogExport(String exportPath, LogExportFormat format) {
        try {
            File file = new File(exportPath);
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(format.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
                
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    private String extractFilename(String path) {
        return new File(path).getName();
    }

    private String determineContentType(String filename) {
        if (filename.endsWith(".json")) return "application/json";
        if (filename.endsWith(".csv")) return "text/csv";
        if (filename.endsWith(".txt")) return "text/plain";
        if (filename.endsWith(".xml")) return "application/xml";
        return "application/octet-stream";
    }

    // ===========================
    // REQUEST/RESPONSE CLASSES
    // ===========================

    public static class LogCollectionRequest {
        private String runId;
        private List<String> logLevels;
        private List<String> categories;
        private List<String> includeKeywords;
        private List<String> excludeKeywords;
        private boolean includeSystemLogs = true;
        private boolean includeApplicationLogs = true;
        private boolean includeResponseBodies = false;
        private int maxResponseBodySize = 1000;
        private boolean autoArchive = false;
        private int maxLogEntries = 10000;
        private boolean compressArchives = true;

        // Getters and setters
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }
        public List<String> getLogLevels() { return logLevels; }
        public void setLogLevels(List<String> logLevels) { this.logLevels = logLevels; }
        public List<String> getCategories() { return categories; }
        public void setCategories(List<String> categories) { this.categories = categories; }
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

    public static class LogSearchRequest {
        private String searchText;
        private String regexPattern;
        private boolean caseSensitive = false;
        private boolean useRegex = false;
        private List<String> logLevels;
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
        public List<String> getLogLevels() { return logLevels; }
        public void setLogLevels(List<String> logLevels) { this.logLevels = logLevels; }
        public int getMaxResults() { return maxResults; }
        public void setMaxResults(int maxResults) { this.maxResults = maxResults; }
    }

    // Response classes (abbreviated for space)
    public static class LogCollectionResponse {
        private String collectionId;
        private String runId;
        private LocalDateTime startedAt;
        private String status;
        private String message;

        // Getters and setters
        public String getCollectionId() { return collectionId; }
        public void setCollectionId(String collectionId) { this.collectionId = collectionId; }
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }
        public LocalDateTime getStartedAt() { return startedAt; }
        public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class LogRetrievalResponse {
        private String collectionId;
        private int totalEntries;
        private int limit;
        private int offset;
        private List<LogEntry> logs;
        private LocalDateTime retrievedAt;

        // Getters and setters
        public String getCollectionId() { return collectionId; }
        public void setCollectionId(String collectionId) { this.collectionId = collectionId; }
        public int getTotalEntries() { return totalEntries; }
        public void setTotalEntries(int totalEntries) { this.totalEntries = totalEntries; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public int getOffset() { return offset; }
        public void setOffset(int offset) { this.offset = offset; }
        public List<LogEntry> getLogs() { return logs; }
        public void setLogs(List<LogEntry> logs) { this.logs = logs; }
        public LocalDateTime getRetrievedAt() { return retrievedAt; }
        public void setRetrievedAt(LocalDateTime retrievedAt) { this.retrievedAt = retrievedAt; }
    }

    public static class LogSummaryResponse {
        private String collectionId;
        private LogsSummary summary;
        private LocalDateTime generatedAt;

        // Getters and setters
        public String getCollectionId() { return collectionId; }
        public void setCollectionId(String collectionId) { this.collectionId = collectionId; }
        public LogsSummary getSummary() { return summary; }
        public void setSummary(LogsSummary summary) { this.summary = summary; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    }

    public static class LogAnalysisResponse {
        private String collectionId;
        private LogAnalysisResult analysis;
        private LocalDateTime generatedAt;

        // Getters and setters
        public String getCollectionId() { return collectionId; }
        public void setCollectionId(String collectionId) { this.collectionId = collectionId; }
        public LogAnalysisResult getAnalysis() { return analysis; }
        public void setAnalysis(LogAnalysisResult analysis) { this.analysis = analysis; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    }

    public static class LogSearchResponse {
        private String collectionId;
        private LogSearchRequest searchCriteria;
        private List<LogEntry> results;
        private int resultCount;
        private LocalDateTime searchedAt;

        // Getters and setters
        public String getCollectionId() { return collectionId; }
        public void setCollectionId(String collectionId) { this.collectionId = collectionId; }
        public LogSearchRequest getSearchCriteria() { return searchCriteria; }
        public void setSearchCriteria(LogSearchRequest searchCriteria) { this.searchCriteria = searchCriteria; }
        public List<LogEntry> getResults() { return results; }
        public void setResults(List<LogEntry> results) { this.results = results; }
        public int getResultCount() { return resultCount; }
        public void setResultCount(int resultCount) { this.resultCount = resultCount; }
        public LocalDateTime getSearchedAt() { return searchedAt; }
        public void setSearchedAt(LocalDateTime searchedAt) { this.searchedAt = searchedAt; }
    }

    public static class LogExportResponse {
        private String collectionId;
        private String format;
        private String exportPath;
        private LocalDateTime exportedAt;
        private String downloadUrl;

        // Getters and setters
        public String getCollectionId() { return collectionId; }
        public void setCollectionId(String collectionId) { this.collectionId = collectionId; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public String getExportPath() { return exportPath; }
        public void setExportPath(String exportPath) { this.exportPath = exportPath; }
        public LocalDateTime getExportedAt() { return exportedAt; }
        public void setExportedAt(LocalDateTime exportedAt) { this.exportedAt = exportedAt; }
        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    }

    public static class LogArchiveResponse {
        private String collectionId;
        private boolean compressed;
        private LocalDateTime archivedAt;
        private String message;

        // Getters and setters
        public String getCollectionId() { return collectionId; }
        public void setCollectionId(String collectionId) { this.collectionId = collectionId; }
        public boolean isCompressed() { return compressed; }
        public void setCompressed(boolean compressed) { this.compressed = compressed; }
        public LocalDateTime getArchivedAt() { return archivedAt; }
        public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
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