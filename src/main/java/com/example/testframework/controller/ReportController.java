package com.example.testframework.controller;

import com.example.testframework.service.ReportGeneratorService;
import com.example.testframework.service.JUnitReportGeneratorService;
import com.example.testframework.service.ExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for comprehensive report generation and management
 * Features: HTML, CSV, JUnit reports, comparison reports, download capabilities
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportGeneratorService reportGeneratorService;
    private final JUnitReportGeneratorService junitReportGeneratorService;
    private final ExecutionService executionService;

    @Autowired
    public ReportController(ReportGeneratorService reportGeneratorService,
                           JUnitReportGeneratorService junitReportGeneratorService,
                           ExecutionService executionService) {
        this.reportGeneratorService = reportGeneratorService;
        this.junitReportGeneratorService = junitReportGeneratorService;
        this.executionService = executionService;
    }

    // ===========================
    // GET /reports/generate
    // Generate comprehensive test execution reports
    // ===========================
    @GetMapping("/generate")
    public ResponseEntity<?> generateReport(
            @RequestParam String runId,
            @RequestParam(defaultValue = "HTML") String format,
            @RequestParam(defaultValue = "false") boolean download,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "true") boolean includeCharts,
            @RequestParam(defaultValue = "true") boolean includeDetails,
            @RequestParam(defaultValue = "true") boolean includeAnalytics) {
        
        try {
            // Validate run exists
            var runStatus = executionService.getRunStatus(runId);
            if ("NOT_FOUND".equals(runStatus.getStatus())) {
                return ResponseEntity.status(404)
                    .body(new ErrorResponse("Run not found: " + runId));
            }

            String reportPath;
            String contentType;
            String filename;

            switch (format.toUpperCase()) {
                case "HTML":
                    ReportGeneratorService.ReportConfiguration htmlConfig = new ReportGeneratorService.ReportConfiguration();
                    htmlConfig.setTitle(title != null ? title : "Test Execution Report");
                    htmlConfig.setIncludeCharts(includeCharts);
                    htmlConfig.setIncludeDetailedResults(includeDetails);
                    htmlConfig.setIncludeAnalytics(includeAnalytics);
                    
                    reportPath = reportGeneratorService.generateHtmlReport(runId, htmlConfig);
                    contentType = "text/html";
                    filename = "report_" + runId + ".html";
                    break;

                case "CSV":
                    ReportGeneratorService.CsvReportConfiguration csvConfig = new ReportGeneratorService.CsvReportConfiguration();
                    csvConfig.setIncludeDetailedResults(includeDetails);
                    csvConfig.setIncludeStatistics(includeAnalytics);
                    
                    reportPath = reportGeneratorService.generateCsvReport(runId, csvConfig);
                    contentType = "text/csv";
                    filename = "report_" + runId + ".csv";
                    break;

                case "JUNIT":
                    JUnitReportGeneratorService.JUnitReportConfiguration junitConfig = 
                        new JUnitReportGeneratorService.JUnitReportConfiguration();
                    junitConfig.setIncludeProperties(includeDetails);
                    junitConfig.setIncludeTestCaseDetails(includeAnalytics);
                    
                    reportPath = junitReportGeneratorService.generateJUnitReport(runId, junitConfig);
                    contentType = "application/xml";
                    filename = "junit_report_" + runId + ".xml";
                    break;

                default:
                    return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Unsupported format: " + format + ". Supported: HTML, CSV, JUNIT"));
            }

            if (download) {
                return downloadReport(reportPath, contentType, filename);
            } else {
                ReportGenerationResponse response = new ReportGenerationResponse();
                response.setReportId(extractReportId(reportPath));
                response.setRunId(runId);
                response.setFormat(format.toUpperCase());
                response.setFilePath(reportPath);
                response.setGeneratedAt(LocalDateTime.now());
                response.setDownloadUrl("/reports/download/" + extractReportId(reportPath));
                
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to generate report: " + e.getMessage()));
        }
    }

    // ===========================
    // GET /reports/compare
    // Generate comparison reports for multiple runs
    // ===========================
    @GetMapping("/compare")
    public ResponseEntity<?> generateComparisonReport(
            @RequestParam List<String> runIds,
            @RequestParam(defaultValue = "HTML") String format,
            @RequestParam(defaultValue = "false") boolean download,
            @RequestParam(required = false) String title) {
        
        try {
            if (runIds.size() < 2) {
                return ResponseEntity.badRequest()
                    .body(new ErrorResponse("At least 2 run IDs required for comparison"));
            }

            String reportPath;
            String contentType;
            String filename;

            switch (format.toUpperCase()) {
                case "HTML":
                    ReportGeneratorService.ReportConfiguration config = new ReportGeneratorService.ReportConfiguration();
                    config.setTitle(title != null ? title : "Test Execution Comparison Report");
                    
                    reportPath = reportGeneratorService.generateComparisonReport(runIds, config);
                    contentType = "text/html";
                    filename = "comparison_report.html";
                    break;

                case "CSV":
                    ReportGeneratorService.CsvReportConfiguration csvConfig = new ReportGeneratorService.CsvReportConfiguration();
                    reportPath = reportGeneratorService.generateCsvComparisonReport(runIds, csvConfig);
                    contentType = "text/csv";
                    filename = "comparison_report.csv";
                    break;

                case "JUNIT":
                    JUnitReportGeneratorService.JUnitReportConfiguration junitConfig = 
                        new JUnitReportGeneratorService.JUnitReportConfiguration();
                    reportPath = junitReportGeneratorService.generateJUnitComparisonReport(runIds, junitConfig);
                    contentType = "application/xml";
                    filename = "comparison_report.xml";
                    break;

                default:
                    return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Unsupported format: " + format));
            }

            if (download) {
                return downloadReport(reportPath, contentType, filename);
            } else {
                ComparisonReportResponse response = new ComparisonReportResponse();
                response.setReportId(extractReportId(reportPath));
                response.setRunIds(runIds);
                response.setFormat(format.toUpperCase());
                response.setFilePath(reportPath);
                response.setGeneratedAt(LocalDateTime.now());
                response.setDownloadUrl("/reports/download/" + extractReportId(reportPath));
                
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to generate comparison report: " + e.getMessage()));
        }
    }

    // ===========================
    // GET /reports/download/{reportId}
    // Download generated reports
    // ===========================
    @GetMapping("/download/{reportId}")
    public ResponseEntity<Resource> downloadReport(@PathVariable String reportId) {
        try {
            // This would typically look up the report path from a database or cache
            // For now, we'll assume the reportId contains enough info to reconstruct the path
            String reportPath = reconstructReportPath(reportId);
            
            File reportFile = new File(reportPath);
            if (!reportFile.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(reportFile);
            String contentType = determineContentType(reportPath);
            String filename = reportFile.getName();

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // ===========================
    // GET /reports/metrics
    // Generate metrics and analytics reports
    // ===========================
    @GetMapping("/metrics")
    public ResponseEntity<?> generateMetricsReport(
            @RequestParam String runId,
            @RequestParam(defaultValue = "false") boolean download) {
        
        try {
            var runStatus = executionService.getRunStatus(runId);
            if ("NOT_FOUND".equals(runStatus.getStatus())) {
                return ResponseEntity.status(404)
                    .body(new ErrorResponse("Run not found: " + runId));
            }

            ReportGeneratorService.CsvReportConfiguration config = new ReportGeneratorService.CsvReportConfiguration();
            config.setIncludePerformanceMetrics(true);
            config.setIncludeErrorAnalysis(true);
            
            String reportPath = reportGeneratorService.generateCsvMetricsReport(runId, config);

            if (download) {
                return downloadReport(reportPath, "text/csv", "metrics_" + runId + ".csv");
            } else {
                MetricsReportResponse response = new MetricsReportResponse();
                response.setReportId(extractReportId(reportPath));
                response.setRunId(runId);
                response.setFilePath(reportPath);
                response.setGeneratedAt(LocalDateTime.now());
                response.setDownloadUrl("/reports/download/" + extractReportId(reportPath));
                
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to generate metrics report: " + e.getMessage()));
        }
    }

    // ===========================
    // GET /reports/suite/{suiteId}
    // Generate test suite reports
    // ===========================
    @GetMapping("/suite/{suiteId}")
    public ResponseEntity<?> generateTestSuiteReport(
            @PathVariable String suiteId,
            @RequestParam(defaultValue = "HTML") String format,
            @RequestParam(defaultValue = "false") boolean download,
            @RequestParam(required = false) List<String> runIds) {
        
        try {
            String reportPath;
            String contentType;
            String filename;

            switch (format.toUpperCase()) {
                case "HTML":
                    ReportGeneratorService.ReportConfiguration config = new ReportGeneratorService.ReportConfiguration();
                    config.setTitle("Test Suite Report: " + suiteId);
                    
                    reportPath = reportGeneratorService.generateTestSuiteReport(suiteId, config);
                    contentType = "text/html";
                    filename = "suite_" + suiteId + "_report.html";
                    break;

                case "JUNIT":
                    if (runIds == null || runIds.isEmpty()) {
                        return ResponseEntity.badRequest()
                            .body(new ErrorResponse("Run IDs required for JUnit suite report"));
                    }
                    
                    JUnitReportGeneratorService.JUnitReportConfiguration junitConfig = 
                        new JUnitReportGeneratorService.JUnitReportConfiguration();
                    reportPath = junitReportGeneratorService.generateJUnitTestSuiteReport(suiteId, runIds, junitConfig);
                    contentType = "application/xml";
                    filename = "suite_" + suiteId + "_report.xml";
                    break;

                default:
                    return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Unsupported format for suite report: " + format));
            }

            if (download) {
                return downloadReport(reportPath, contentType, filename);
            } else {
                TestSuiteReportResponse response = new TestSuiteReportResponse();
                response.setReportId(extractReportId(reportPath));
                response.setSuiteId(suiteId);
                response.setFormat(format.toUpperCase());
                response.setFilePath(reportPath);
                response.setGeneratedAt(LocalDateTime.now());
                response.setDownloadUrl("/reports/download/" + extractReportId(reportPath));
                
                return ResponseEntity.ok(response);
            }

        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(new ErrorResponse("Failed to generate suite report: " + e.getMessage()));
        }
    }

    // ===========================
    // HELPER METHODS
    // ===========================

    private ResponseEntity<Resource> downloadReport(String reportPath, String contentType, String filename) {
        try {
            File reportFile = new File(reportPath);
            if (!reportFile.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(reportFile);

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    private String extractReportId(String reportPath) {
        // Extract a unique ID from the report path for download reference
        String filename = new File(reportPath).getName();
        return filename.replaceAll("\\.[^.]+$", ""); // Remove extension
    }

    private String reconstructReportPath(String reportId) {
        // This would typically query a database or cache to get the actual path
        // For now, we'll make a simple assumption about the file structure
        return "./reports/" + reportId + ".html"; // Default assumption
    }

    private String determineContentType(String filePath) {
        if (filePath.endsWith(".html")) return "text/html";
        if (filePath.endsWith(".csv")) return "text/csv";
        if (filePath.endsWith(".xml")) return "application/xml";
        return "application/octet-stream";
    }

    // ===========================
    // RESPONSE CLASSES
    // ===========================

    public static class ReportGenerationResponse {
        private String reportId;
        private String runId;
        private String format;
        private String filePath;
        private LocalDateTime generatedAt;
        private String downloadUrl;

        // Getters and setters
        public String getReportId() { return reportId; }
        public void setReportId(String reportId) { this.reportId = reportId; }
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    }

    public static class ComparisonReportResponse {
        private String reportId;
        private List<String> runIds;
        private String format;
        private String filePath;
        private LocalDateTime generatedAt;
        private String downloadUrl;

        // Getters and setters
        public String getReportId() { return reportId; }
        public void setReportId(String reportId) { this.reportId = reportId; }
        public List<String> getRunIds() { return runIds; }
        public void setRunIds(List<String> runIds) { this.runIds = runIds; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    }

    public static class MetricsReportResponse {
        private String reportId;
        private String runId;
        private String filePath;
        private LocalDateTime generatedAt;
        private String downloadUrl;

        // Getters and setters
        public String getReportId() { return reportId; }
        public void setReportId(String reportId) { this.reportId = reportId; }
        public String getRunId() { return runId; }
        public void setRunId(String runId) { this.runId = runId; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    }

    public static class TestSuiteReportResponse {
        private String reportId;
        private String suiteId;
        private String format;
        private String filePath;
        private LocalDateTime generatedAt;
        private String downloadUrl;

        // Getters and setters
        public String getReportId() { return reportId; }
        public void setReportId(String reportId) { this.reportId = reportId; }
        public String getSuiteId() { return suiteId; }
        public void setSuiteId(String suiteId) { this.suiteId = suiteId; }
        public String getFormat() { return format; }
        public void setFormat(String format) { this.format = format; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
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