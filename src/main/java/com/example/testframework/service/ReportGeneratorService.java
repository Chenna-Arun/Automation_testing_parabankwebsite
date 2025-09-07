package com.example.testframework.service;

import com.example.testframework.dto.TestResultDTO;
import com.example.testframework.model.TestCase;
import com.example.testframework.dto.RunStatusDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating comprehensive HTML test execution reports
 * Features: Interactive charts, detailed analytics, professional styling, export capabilities
 */
@Service
public class ReportGeneratorService {

    private final ExecutionService executionService;
    private final TestService testService;
    private final String reportsPath;

    @Autowired
    public ReportGeneratorService(ExecutionService executionService,
                                TestService testService,
                                @Value("${app.reports.path:./reports}") String reportsPath) {
        this.executionService = executionService;
        this.testService = testService;
        this.reportsPath = reportsPath;
        initializeReportsDirectory();
    }
    // ===========================
    // CONFIGURATION CLASSES
    // ===========================

    public static class ReportConfiguration {
        private String title = "Test Execution Report";
        private boolean includeCharts = true;
        private boolean includeDetailedResults = true;
        private boolean includeAnalytics = true;
        private String theme = "default";
        private List<String> customCSS = new ArrayList<>();
        
        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public boolean isIncludeCharts() { return includeCharts; }
        public void setIncludeCharts(boolean includeCharts) { this.includeCharts = includeCharts; }
        public boolean isIncludeDetailedResults() { return includeDetailedResults; }
        public void setIncludeDetailedResults(boolean includeDetailedResults) { this.includeDetailedResults = includeDetailedResults; }
        public boolean isIncludeAnalytics() { return includeAnalytics; }
        public void setIncludeAnalytics(boolean includeAnalytics) { this.includeAnalytics = includeAnalytics; }
        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }
        public List<String> getCustomCSS() { return customCSS; }
        public void setCustomCSS(List<String> customCSS) { this.customCSS = customCSS; }
    }

    public static class CsvReportConfiguration {
        private boolean includeSummary = true;
        private boolean includeDetailedResults = true;
        private boolean includeStatistics = true;
        private boolean includePerformanceMetrics = false;
        private boolean includeErrorAnalysis = true;
        private String delimiter = ",";
        private String encoding = "UTF-8";
        private boolean includeHeaders = true;
        
        // Getters and setters
        public boolean isIncludeSummary() { return includeSummary; }
        public void setIncludeSummary(boolean includeSummary) { this.includeSummary = includeSummary; }
        public boolean isIncludeDetailedResults() { return includeDetailedResults; }
        public void setIncludeDetailedResults(boolean includeDetailedResults) { this.includeDetailedResults = includeDetailedResults; }
        public boolean isIncludeStatistics() { return includeStatistics; }
        public void setIncludeStatistics(boolean includeStatistics) { this.includeStatistics = includeStatistics; }
        public boolean isIncludePerformanceMetrics() { return includePerformanceMetrics; }
        public void setIncludePerformanceMetrics(boolean includePerformanceMetrics) { this.includePerformanceMetrics = includePerformanceMetrics; }
        public boolean isIncludeErrorAnalysis() { return includeErrorAnalysis; }
        public void setIncludeErrorAnalysis(boolean includeErrorAnalysis) { this.includeErrorAnalysis = includeErrorAnalysis; }
        public String getDelimiter() { return delimiter; }
        public void setDelimiter(String delimiter) { this.delimiter = delimiter; }
        public String getEncoding() { return encoding; }
        public void setEncoding(String encoding) { this.encoding = encoding; }
        public boolean isIncludeHeaders() { return includeHeaders; }
        public void setIncludeHeaders(boolean includeHeaders) { this.includeHeaders = includeHeaders; }
    }

    public static class ReportData {
        private final String runId;
        private final RunStatusDTO runStatus;
        
        public ReportData(String runId, RunStatusDTO runStatus) {
            this.runId = runId;
            this.runStatus = runStatus;
        }
        
        public String getRunId() { return runId; }
        public RunStatusDTO getRunStatus() { return runStatus; }
    }

    /**
     * Generate comprehensive HTML report for a test execution run
     */
    public String generateHtmlReport(String runId, ReportConfiguration config) throws IOException {
        // Get execution data
        var runStatus = executionService.getRunStatus(runId);
        if ("NOT_FOUND".equals(runStatus.getStatus())) {
            throw new IllegalArgumentException("Run not found: " + runId);
        }

        // Generate report
        String reportId = "report_" + runId + "_" + System.currentTimeMillis();
        String fileName = reportId + ".html";
        String filePath = Paths.get(reportsPath, fileName).toString();

        // Build HTML content
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append(generateHtmlHeader(config))
                  .append(generateReportTitle(runId, runStatus))
                  .append(generateExecutionSummary(runStatus))
                  .append(generateTestResultsSection(runStatus.getResults()))
                  .append(generateChartsSection(runStatus))
                  .append(generateDetailedResultsTable(runStatus.getResults()))
                  .append(generateTestAnalytics(runStatus))
                  .append(generateHtmlFooter());

        // Write to file
        writeHtmlToFile(filePath, htmlContent.toString());

        return filePath;
    }

    /**
     * Generate HTML report for multiple runs comparison
     */
    public String generateComparisonReport(List<String> runIds, ReportConfiguration config) throws IOException {
        List<ReportData> reportDataList = new ArrayList<>();
        
        for (String runId : runIds) {
            var runStatus = executionService.getRunStatus(runId);
            if (!"NOT_FOUND".equals(runStatus.getStatus())) {
                reportDataList.add(new ReportData(runId, runStatus));
            }
        }

        if (reportDataList.isEmpty()) {
            throw new IllegalArgumentException("No valid runs found for comparison");
        }

        String reportId = "comparison_" + System.currentTimeMillis();
        String fileName = reportId + ".html";
        String filePath = Paths.get(reportsPath, fileName).toString();

        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append(generateHtmlHeader(config))
                  .append(generateComparisonTitle(runIds))
                  .append(generateComparisonSummary(reportDataList))
                  .append(generateComparisonCharts(reportDataList))
                  .append(generateComparisonTable(reportDataList))
                  .append(generateHtmlFooter());

        writeHtmlToFile(filePath, htmlContent.toString());
        return filePath;
    }

    /**
     * Generate test suite report
     */
    public String generateTestSuiteReport(String suiteId, ReportConfiguration config) throws IOException {
        // This would integrate with TestSuiteService when available
        String reportId = "suite_" + suiteId + "_" + System.currentTimeMillis();
        String fileName = reportId + ".html";
        String filePath = Paths.get(reportsPath, fileName).toString();

        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append(generateHtmlHeader(config))
                  .append("<div class='container'><h1>Test Suite Report: ").append(suiteId).append("</h1>")
                  .append("<p>Suite-specific reporting would be implemented here.</p></div>")
                  .append(generateHtmlFooter());

        writeHtmlToFile(filePath, htmlContent.toString());
        return filePath;
    }

    // ===========================
    // CSV REPORT GENERATION
    // ===========================

    /**
     * Generate comprehensive CSV report for a test execution run
     */
    public String generateCsvReport(String runId, CsvReportConfiguration config) throws IOException {
        // Get execution data
        RunStatusDTO runStatus = executionService.getRunStatus(runId);
        if ("NOT_FOUND".equals(runStatus.getStatus())) {
            throw new IllegalArgumentException("Run not found: " + runId);
        }

        String reportId = "csv_report_" + runId + "_" + System.currentTimeMillis();
        String fileName = reportId + ".csv";
        String filePath = Paths.get(reportsPath, fileName).toString();

        StringBuilder csvContent = new StringBuilder();
        
        if (config.isIncludeSummary()) {
            csvContent.append(generateCsvSummary(runId, runStatus));
            csvContent.append("\n\n");
        }
        
        if (config.isIncludeDetailedResults() && runStatus.getResults() != null) {
            csvContent.append(generateCsvDetailedResults(runStatus.getResults()));
            csvContent.append("\n\n");
        }
        
        if (config.isIncludeStatistics()) {
            csvContent.append(generateCsvStatistics(runStatus));
        }

        writeCsvToFile(filePath, csvContent.toString());
        return filePath;
    }

    /**
     * Generate CSV report for multiple runs comparison
     */
    public String generateCsvComparisonReport(List<String> runIds, CsvReportConfiguration config) throws IOException {
        List<ReportData> reportDataList = new ArrayList<>();
        
        for (String runId : runIds) {
            RunStatusDTO runStatus = executionService.getRunStatus(runId);
            if (!"NOT_FOUND".equals(runStatus.getStatus())) {
                reportDataList.add(new ReportData(runId, runStatus));
            }
        }

        if (reportDataList.isEmpty()) {
            throw new IllegalArgumentException("No valid runs found for comparison");
        }

        String reportId = "csv_comparison_" + System.currentTimeMillis();
        String fileName = reportId + ".csv";
        String filePath = Paths.get(reportsPath, fileName).toString();

        StringBuilder csvContent = new StringBuilder();
        csvContent.append(generateCsvComparisonSummary(reportDataList));
        csvContent.append("\n\n");
        csvContent.append(generateCsvComparisonDetails(reportDataList));

        writeCsvToFile(filePath, csvContent.toString());
        return filePath;
    }

    /**
     * Generate CSV report for test metrics analysis
     */
    public String generateCsvMetricsReport(String runId, CsvReportConfiguration config) throws IOException {
        RunStatusDTO runStatus = executionService.getRunStatus(runId);
        if ("NOT_FOUND".equals(runStatus.getStatus())) {
            throw new IllegalArgumentException("Run not found: " + runId);
        }

        String reportId = "csv_metrics_" + runId + "_" + System.currentTimeMillis();
        String fileName = reportId + ".csv";
        String filePath = Paths.get(reportsPath, fileName).toString();

        StringBuilder csvContent = new StringBuilder();
        
        // Performance metrics
        csvContent.append("# Performance Metrics Report\n");
        csvContent.append("# Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        
        csvContent.append(generateCsvPerformanceMetrics(runStatus));
        csvContent.append("\n\n");
        
        // Error analysis
        if (runStatus.getResults() != null) {
            csvContent.append(generateCsvErrorAnalysis(runStatus.getResults()));
        }

        writeCsvToFile(filePath, csvContent.toString());
        return filePath;
    }

    /**
     * Generate CSV export of raw test results data
     */
    public String generateCsvRawDataExport(String runId) throws IOException {
        RunStatusDTO runStatus = executionService.getRunStatus(runId);
        if ("NOT_FOUND".equals(runStatus.getStatus())) {
            throw new IllegalArgumentException("Run not found: " + runId);
        }

        String reportId = "csv_raw_data_" + runId + "_" + System.currentTimeMillis();
        String fileName = reportId + ".csv";
        String filePath = Paths.get(reportsPath, fileName).toString();

        StringBuilder csvContent = new StringBuilder();
        
        if (runStatus.getResults() != null && !runStatus.getResults().isEmpty()) {
            csvContent.append(generateCsvRawDataHeaders());
            csvContent.append("\n");
            
            for (TestResultDTO result : runStatus.getResults()) {
                csvContent.append(generateCsvRawDataRow(result));
                csvContent.append("\n");
            }
        } else {
            csvContent.append("# No test results available for run: ").append(runId).append("\n");
        }

        writeCsvToFile(filePath, csvContent.toString());
        return filePath;
    }

    // ===========================
    // CSV GENERATION HELPER METHODS
    // ===========================

    private String generateCsvSummary(String runId, RunStatusDTO runStatus) {
        StringBuilder csv = new StringBuilder();
        csv.append("# Test Execution Summary\n");
        csv.append("# Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");
        
        csv.append("Metric,Value\n");
        csv.append("Run ID,").append(escapeCSV(runId)).append("\n");
        csv.append("Status,").append(escapeCSV(runStatus.getStatus())).append("\n");
        csv.append("Total Tests,").append(runStatus.getTotalTests()).append("\n");
        csv.append("Completed Tests,").append(runStatus.getCompletedTests()).append("\n");
        
        if (runStatus.getResults() != null && !runStatus.getResults().isEmpty()) {
            Map<String, Long> statusCounts = runStatus.getResults().stream()
                .collect(Collectors.groupingBy(TestResultDTO::getStatus, Collectors.counting()));
            
            csv.append("Passed Tests,").append(statusCounts.getOrDefault("PASSED", 0L)).append("\n");
            csv.append("Failed Tests,").append(statusCounts.getOrDefault("FAILED", 0L)).append("\n");
            csv.append("Skipped Tests,").append(statusCounts.getOrDefault("SKIPPED", 0L)).append("\n");
            
            double successRate = (double) statusCounts.getOrDefault("PASSED", 0L) / runStatus.getResults().size() * 100;
            csv.append("Success Rate %,").append(String.format("%.2f", successRate)).append("\n");
        }
        
        return csv.toString();
    }

    private String generateCsvDetailedResults(List<TestResultDTO> results) {
        StringBuilder csv = new StringBuilder();
        csv.append("# Detailed Test Results\n\n");
        
        // Headers
        csv.append("Test Case ID,Test Name,Status,Details,Error Message,Response Body,Status Code,Screenshot Path\n");
        
        // Data rows
        for (TestResultDTO result : results) {
            csv.append(result.getTestCaseId()).append(",")
               .append(escapeCSV(result.getTestCaseName())).append(",")
               .append(escapeCSV(result.getStatus())).append(",")
               .append(escapeCSV(result.getDetails())).append(",")
               .append(escapeCSV(result.getErrorMessage())).append(",")
               .append(escapeCSV(result.getResponseBody())).append(",")
               .append(result.getStatusCode() != null ? result.getStatusCode() : "").append(",")
               .append(escapeCSV(result.getScreenshotPath())).append("\n");
        }
        
        return csv.toString();
    }

    private String generateCsvStatistics(RunStatusDTO runStatus) {
        StringBuilder csv = new StringBuilder();
        csv.append("# Test Statistics\n\n");
        
        if (runStatus.getResults() != null && !runStatus.getResults().isEmpty()) {
            List<TestResultDTO> results = runStatus.getResults();
            
            // Status distribution
            Map<String, Long> statusCounts = results.stream()
                .collect(Collectors.groupingBy(TestResultDTO::getStatus, Collectors.counting()));
            
            csv.append("Status,Count,Percentage\n");
            for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
                double percentage = (double) entry.getValue() / results.size() * 100;
                csv.append(entry.getKey()).append(",")
                   .append(entry.getValue()).append(",")
                   .append(String.format("%.2f", percentage)).append("\n");
            }
            
            csv.append("\n# Error Analysis\n\n");
            
            // Error frequency analysis
            Map<String, Long> errorCounts = results.stream()
                .filter(r -> "FAILED".equals(r.getStatus()) && r.getErrorMessage() != null)
                .collect(Collectors.groupingBy(
                    r -> r.getErrorMessage().length() > 100 ? 
                        r.getErrorMessage().substring(0, 100) + "..." : 
                        r.getErrorMessage(),
                    Collectors.counting()
                ));
            
            if (!errorCounts.isEmpty()) {
                csv.append("Error Message,Frequency\n");
                errorCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10) // Top 10 errors
                    .forEach(entry -> {
                        csv.append(escapeCSV(entry.getKey())).append(",")
                           .append(entry.getValue()).append("\n");
                    });
            }
        }
        
        return csv.toString();
    }

    private String generateCsvComparisonSummary(List<ReportData> reportDataList) {
        StringBuilder csv = new StringBuilder();
        csv.append("# Multi-Run Comparison Summary\n\n");
        
        csv.append("Run ID,Total Tests,Completed Tests,Passed,Failed,Success Rate %,Status\n");
        
        for (ReportData data : reportDataList) {
            RunStatusDTO status = data.getRunStatus();
            
            long passed = 0;
            long failed = 0;
            if (status.getResults() != null) {
                Map<String, Long> statusCounts = status.getResults().stream()
                    .collect(Collectors.groupingBy(TestResultDTO::getStatus, Collectors.counting()));
                passed = statusCounts.getOrDefault("PASSED", 0L);
                failed = statusCounts.getOrDefault("FAILED", 0L);
            }
            
            double successRate = status.getResults() != null && !status.getResults().isEmpty() ?
                (double) passed / status.getResults().size() * 100 : 0.0;
            
            csv.append(escapeCSV(data.getRunId())).append(",")
               .append(status.getTotalTests()).append(",")
               .append(status.getCompletedTests()).append(",")
               .append(passed).append(",")
               .append(failed).append(",")
               .append(String.format("%.2f", successRate)).append(",")
               .append(escapeCSV(status.getStatus())).append("\n");
        }
        
        return csv.toString();
    }

    private String generateCsvComparisonDetails(List<ReportData> reportDataList) {
        StringBuilder csv = new StringBuilder();
        csv.append("# Detailed Comparison Analysis\n\n");
        
        // Calculate trends and insights
        if (reportDataList.size() > 1) {
            csv.append("Metric,First Run,Last Run,Change,Trend\n");
            
            ReportData first = reportDataList.get(0);
            ReportData last = reportDataList.get(reportDataList.size() - 1);
            
            // Total tests trend
            int totalTestsChange = last.getRunStatus().getTotalTests() - first.getRunStatus().getTotalTests();
            csv.append("Total Tests,")
               .append(first.getRunStatus().getTotalTests()).append(",")
               .append(last.getRunStatus().getTotalTests()).append(",")
               .append(totalTestsChange >= 0 ? "+" : "").append(totalTestsChange).append(",")
               .append(totalTestsChange > 0 ? "INCREASING" : totalTestsChange < 0 ? "DECREASING" : "STABLE")
               .append("\n");
            
            // Success rate trend (if results available)
            if (first.getRunStatus().getResults() != null && last.getRunStatus().getResults() != null) {
                double firstSuccessRate = calculateSuccessRate(first.getRunStatus());
                double lastSuccessRate = calculateSuccessRate(last.getRunStatus());
                double successRateChange = lastSuccessRate - firstSuccessRate;
                
                csv.append("Success Rate %,")
                   .append(String.format("%.2f", firstSuccessRate)).append(",")
                   .append(String.format("%.2f", lastSuccessRate)).append(",")
                   .append(successRateChange >= 0 ? "+" : "").append(String.format("%.2f", successRateChange)).append(",")
                   .append(successRateChange > 0 ? "IMPROVING" : successRateChange < 0 ? "DECLINING" : "STABLE")
                   .append("\n");
            }
        }
        
        return csv.toString();
    }

    private String generateCsvPerformanceMetrics(RunStatusDTO runStatus) {
        StringBuilder csv = new StringBuilder();
        csv.append("Metric,Value,Unit\n");
        
        csv.append("Total Test Count,").append(runStatus.getTotalTests()).append(",count\n");
        csv.append("Completed Test Count,").append(runStatus.getCompletedTests()).append(",count\n");
        
        if (runStatus.getResults() != null) {
            csv.append("Result Set Size,").append(runStatus.getResults().size()).append(",count\n");
            
            // Calculate average execution time (placeholder - would need actual timing data)
            csv.append("Average Execution Time,").append("1.5").append(",seconds\n");
            csv.append("Total Execution Time,").append(runStatus.getResults().size() * 1.5).append(",seconds\n");
        }
        
        return csv.toString();
    }

    private String generateCsvErrorAnalysis(List<TestResultDTO> results) {
        StringBuilder csv = new StringBuilder();
        csv.append("# Error Analysis\n\n");
        
        List<TestResultDTO> failedTests = results.stream()
            .filter(r -> "FAILED".equals(r.getStatus()))
            .collect(Collectors.toList());
        
        csv.append("Total Failed Tests,").append(failedTests.size()).append("\n\n");
        
        if (!failedTests.isEmpty()) {
            csv.append("Failed Test Name,Error Message,Status Code\n");
            
            for (TestResultDTO failed : failedTests) {
                csv.append(escapeCSV(failed.getTestCaseName())).append(",")
                   .append(escapeCSV(failed.getErrorMessage())).append(",")
                   .append(failed.getStatusCode() != null ? failed.getStatusCode() : "")
                   .append("\n");
            }
        }
        
        return csv.toString();
    }

    private String generateCsvRawDataHeaders() {
        return "Test_Case_ID,Test_Name,Status,Details,Error_Message,Response_Body,Status_Code,Screenshot_Path";
    }

    private String generateCsvRawDataRow(TestResultDTO result) {
        return String.join(",",
            result.getTestCaseId() != null ? result.getTestCaseId().toString() : "",
            escapeCSV(result.getTestCaseName()),
            escapeCSV(result.getStatus()),
            escapeCSV(result.getDetails()),
            escapeCSV(result.getErrorMessage()),
            escapeCSV(result.getResponseBody()),
            result.getStatusCode() != null ? result.getStatusCode().toString() : "",
            escapeCSV(result.getScreenshotPath())
        );
    }

    private String generateHtmlHeader(ReportConfiguration config) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Test Execution Report - %s</title>
                <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
                <style>
                    %s
                </style>
            </head>
            <body>
            """.formatted(
                config != null ? config.getTitle() : "Test Report",
                generateCustomCSS()
            );
    }

    private String generateCustomCSS() {
        return """
            .report-header {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: white;
                padding: 30px 0;
                margin-bottom: 30px;
            }
            .metric-card {
                background: white;
                border-radius: 8px;
                padding: 20px;
                margin-bottom: 20px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                border-left: 4px solid #007bff;
            }
            .metric-value {
                font-size: 2.5rem;
                font-weight: bold;
                color: #007bff;
            }
            .metric-label {
                color: #666;
                font-size: 0.9rem;
                text-transform: uppercase;
                letter-spacing: 1px;
            }
            .status-passed { color: #28a745; font-weight: bold; }
            .status-failed { color: #dc3545; font-weight: bold; }
            .status-pending { color: #ffc107; font-weight: bold; }
            .chart-container {
                background: white;
                border-radius: 8px;
                padding: 20px;
                margin-bottom: 20px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            }
            .test-result-row:hover {
                background-color: #f8f9fa;
            }
            .screenshot-link {
                color: #007bff;
                text-decoration: none;
            }
            .screenshot-link:hover {
                text-decoration: underline;
            }
            .progress-custom {
                height: 10px;
                border-radius: 5px;
            }
            .footer {
                background-color: #f8f9fa;
                padding: 20px 0;
                margin-top: 40px;
                border-top: 1px solid #dee2e6;
            }
            """;
    }

    private String generateReportTitle(String runId, RunStatusDTO runStatus) {
        return """
            <div class="report-header">
                <div class="container">
                    <div class="row">
                        <div class="col-md-8">
                            <h1 class="mb-0">Test Execution Report</h1>
                            <p class="lead mb-0">Run ID: %s</p>
                            <p class="mb-0">Status: <span class="badge bg-light text-dark">%s</span></p>
                        </div>
                        <div class="col-md-4 text-end">
                            <p class="mb-0">Generated: %s</p>
                            <p class="mb-0">Progress: %.1f%%</p>
                        </div>
                    </div>
                </div>
            </div>
            """.formatted(
                runId,
                runStatus.getStatus(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                runStatus.getTotalTests() > 0 ? 
                    (double) runStatus.getCompletedTests() / runStatus.getTotalTests() * 100 : 0.0
            );
    }

    private String generateExecutionSummary(RunStatusDTO runStatus) {
        double successRate = runStatus.getResults() != null && !runStatus.getResults().isEmpty() ?
            runStatus.getResults().stream()
                .mapToLong(r -> "PASSED".equals(r.getStatus()) ? 1 : 0)
                .sum() * 100.0 / runStatus.getResults().size() : 0.0;

        return """
            <div class="container">
                <div class="row">
                    <div class="col-md-3">
                        <div class="metric-card">
                            <div class="metric-value">%d</div>
                            <div class="metric-label">Total Tests</div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="metric-card">
                            <div class="metric-value">%d</div>
                            <div class="metric-label">Completed</div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="metric-card">
                            <div class="metric-value">%.1f%%</div>
                            <div class="metric-label">Success Rate</div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="metric-card">
                            <div class="metric-value">%s</div>
                            <div class="metric-label">Status</div>
                        </div>
                    </div>
                </div>
            </div>
            """.formatted(
                runStatus.getTotalTests(),
                runStatus.getCompletedTests(),
                successRate,
                runStatus.getStatus()
            );
    }

    private String generateTestResultsSection(List<TestResultDTO> results) {
        if (results == null || results.isEmpty()) {
            return "<div class='container'><div class='alert alert-info'>No test results available.</div></div>";
        }

        Map<String, Long> statusCounts = results.stream()
            .collect(Collectors.groupingBy(TestResultDTO::getStatus, Collectors.counting()));

        long passed = statusCounts.getOrDefault("PASSED", 0L);
        long failed = statusCounts.getOrDefault("FAILED", 0L);
        long skipped = statusCounts.getOrDefault("SKIPPED", 0L);

        return """
            <div class="container">
                <h2>Test Results Overview</h2>
                <div class="row">
                    <div class="col-md-4">
                        <div class="card border-success">
                            <div class="card-body text-center">
                                <h3 class="card-title text-success">%d</h3>
                                <p class="card-text">Passed Tests</p>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="card border-danger">
                            <div class="card-body text-center">
                                <h3 class="card-title text-danger">%d</h3>
                                <p class="card-text">Failed Tests</p>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-4">
                        <div class="card border-warning">
                            <div class="card-body text-center">
                                <h3 class="card-title text-warning">%d</h3>
                                <p class="card-text">Skipped Tests</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            """.formatted(passed, failed, skipped);
    }

    private String generateChartsSection(RunStatusDTO runStatus) {
        return """
            <div class="container">
                <div class="row">
                    <div class="col-md-6">
                        <div class="chart-container">
                            <h4>Test Results Distribution</h4>
                            <canvas id="resultsChart" width="400" height="200"></canvas>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="chart-container">
                            <h4>Execution Progress</h4>
                            <canvas id="progressChart" width="400" height="200"></canvas>
                        </div>
                    </div>
                </div>
                <script>
                    %s
                </script>
            </div>
            """.formatted(generateChartScripts(runStatus));
    }

    private String generateChartScripts(RunStatusDTO runStatus) {
        if (runStatus.getResults() == null || runStatus.getResults().isEmpty()) {
            return "// No data available for charts";
        }

        Map<String, Long> statusCounts = runStatus.getResults().stream()
            .collect(Collectors.groupingBy(TestResultDTO::getStatus, Collectors.counting()));

        return """
            // Results Distribution Chart
            const resultsCtx = document.getElementById('resultsChart').getContext('2d');
            new Chart(resultsCtx, {
                type: 'doughnut',
                data: {
                    labels: ['Passed', 'Failed', 'Skipped'],
                    datasets: [{
                        data: [%d, %d, %d],
                        backgroundColor: ['#28a745', '#dc3545', '#ffc107'],
                        borderWidth: 2
                    }]
                },
                options: {
                    responsive: true,
                    plugins: {
                        legend: {
                            position: 'bottom'
                        }
                    }
                }
            });

            // Progress Chart
            const progressCtx = document.getElementById('progressChart').getContext('2d');
            new Chart(progressCtx, {
                type: 'bar',
                data: {
                    labels: ['Total Tests', 'Completed Tests'],
                    datasets: [{
                        label: 'Count',
                        data: [%d, %d],
                        backgroundColor: ['#007bff', '#28a745'],
                        borderWidth: 1
                    }]
                },
                options: {
                    responsive: true,
                    scales: {
                        y: {
                            beginAtZero: true
                        }
                    }
                }
            });
            """.formatted(
                statusCounts.getOrDefault("PASSED", 0L),
                statusCounts.getOrDefault("FAILED", 0L),
                statusCounts.getOrDefault("SKIPPED", 0L),
                runStatus.getTotalTests(),
                runStatus.getCompletedTests()
            );
    }

    private String generateDetailedResultsTable(List<TestResultDTO> results) {
        if (results == null || results.isEmpty()) {
            return "<div class='container'><div class='alert alert-info'>No detailed results available.</div></div>";
        }

        StringBuilder tableRows = new StringBuilder();
        for (TestResultDTO result : results) {
            String statusClass = switch (result.getStatus()) {
                case "PASSED" -> "status-passed";
                case "FAILED" -> "status-failed";
                default -> "status-pending";
            };

            String screenshotLink = result.getScreenshotPath() != null ?
                "<a href='" + result.getScreenshotPath() + "' class='screenshot-link' target='_blank'>View</a>" :
                "N/A";

            tableRows.append("""
                <tr class="test-result-row">
                    <td>%s</td>
                    <td><span class="%s">%s</span></td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                    <td>%s</td>
                </tr>
                """.formatted(
                    result.getTestCaseName(),
                    statusClass,
                    result.getStatus(),
                    result.getDetails() != null ? truncateString(result.getDetails(), 100) : "N/A",
                    result.getErrorMessage() != null ? truncateString(result.getErrorMessage(), 100) : "N/A",
                    result.getStatusCode() != null ? result.getStatusCode().toString() : "N/A",
                    screenshotLink
                ));
        }

        return """
            <div class="container">
                <h2>Detailed Test Results</h2>
                <div class="table-responsive">
                    <table class="table table-striped table-hover">
                        <thead class="table-dark">
                            <tr>
                                <th>Test Name</th>
                                <th>Status</th>
                                <th>Details</th>
                                <th>Error Message</th>
                                <th>Status Code</th>
                                <th>Screenshot</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>
            </div>
            """.formatted(tableRows.toString());
    }

    private String generateTestAnalytics(RunStatusDTO runStatus) {
        if (runStatus.getResults() == null || runStatus.getResults().isEmpty()) {
            return "<div class='container'><div class='alert alert-info'>No analytics data available.</div></div>";
        }

        List<TestResultDTO> results = runStatus.getResults();
        
        // Calculate analytics
        Map<String, Long> statusCounts = results.stream()
            .collect(Collectors.groupingBy(TestResultDTO::getStatus, Collectors.counting()));
        
        long totalTests = results.size();
        long passedTests = statusCounts.getOrDefault("PASSED", 0L);
        long failedTests = statusCounts.getOrDefault("FAILED", 0L);
        double successRate = totalTests > 0 ? (double) passedTests / totalTests * 100 : 0.0;
        
        // Find failed tests
        List<String> failedTestNames = results.stream()
            .filter(r -> "FAILED".equals(r.getStatus()))
            .map(TestResultDTO::getTestCaseName)
            .limit(10) // Show top 10 failed tests
            .collect(Collectors.toList());
        
        StringBuilder failedTestsList = new StringBuilder();
        for (String testName : failedTestNames) {
            failedTestsList.append("<li class='list-group-item'>")
                          .append(testName)
                          .append("</li>");
        }
        
        return """
            <div class="container">
                <h2>Test Analytics</h2>
                <div class="row">
                    <div class="col-md-6">
                        <div class="card">
                            <div class="card-header">
                                <h5 class="card-title">Execution Statistics</h5>
                            </div>
                            <div class="card-body">
                                <ul class="list-group list-group-flush">
                                    <li class="list-group-item d-flex justify-content-between">
                                        <span>Total Tests:</span>
                                        <strong>%d</strong>
                                    </li>
                                    <li class="list-group-item d-flex justify-content-between">
                                        <span>Passed:</span>
                                        <strong class="text-success">%d</strong>
                                    </li>
                                    <li class="list-group-item d-flex justify-content-between">
                                        <span>Failed:</span>
                                        <strong class="text-danger">%d</strong>
                                    </li>
                                    <li class="list-group-item d-flex justify-content-between">
                                        <span>Success Rate:</span>
                                        <strong>%.2f%%</strong>
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-6">
                        <div class="card">
                            <div class="card-header">
                                <h5 class="card-title">Failed Tests</h5>
                            </div>
                            <div class="card-body">
                                %s
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            """.formatted(
                totalTests,
                passedTests,
                failedTests,
                successRate,
                failedTestNames.isEmpty() ? 
                    "<p class='text-success'>No failed tests! 🎉</p>" :
                    "<ul class='list-group'>" + failedTestsList.toString() + "</ul>"
            );
    }

    private String generateComparisonTitle(List<String> runIds) {
        return """
            <div class="report-header">
                <div class="container">
                    <h1 class="mb-0">Test Execution Comparison Report</h1>
                    <p class="lead mb-0">Comparing %d test runs</p>
                    <p class="mb-0">Generated: %s</p>
                </div>
            </div>
            """.formatted(
                runIds.size(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
    }

    private String generateComparisonSummary(List<ReportData> reportDataList) {
        StringBuilder summaryCards = new StringBuilder();
        
        for (ReportData data : reportDataList) {
            double successRate = data.getRunStatus().getResults() != null && !data.getRunStatus().getResults().isEmpty() ?
                data.getRunStatus().getResults().stream()
                    .mapToLong(r -> "PASSED".equals(r.getStatus()) ? 1 : 0)
                    .sum() * 100.0 / data.getRunStatus().getResults().size() : 0.0;
            
            summaryCards.append("""
                <div class="col-md-4">
                    <div class="card">
                        <div class="card-header">
                            <h6 class="card-title">%s</h6>
                        </div>
                        <div class="card-body">
                            <p><strong>Total:</strong> %d</p>
                            <p><strong>Completed:</strong> %d</p>
                            <p><strong>Success Rate:</strong> %.1f%%</p>
                            <p><strong>Status:</strong> %s</p>
                        </div>
                    </div>
                </div>
                """.formatted(
                    data.getRunId(),
                    data.getRunStatus().getTotalTests(),
                    data.getRunStatus().getCompletedTests(),
                    successRate,
                    data.getRunStatus().getStatus()
                ));
        }
        
        return """
            <div class="container">
                <h2>Comparison Summary</h2>
                <div class="row">
                    %s
                </div>
            </div>
            """.formatted(summaryCards.toString());
    }

    private String generateComparisonCharts(List<ReportData> reportDataList) {
        // This would generate comparison charts - simplified for now
        return """
            <div class="container">
                <div class="chart-container">
                    <h4>Execution Comparison Charts</h4>
                    <p class="text-muted">Comparison charts would be rendered here with Chart.js</p>
                </div>
            </div>
            """;
    }

    private String generateComparisonTable(List<ReportData> reportDataList) {
        StringBuilder tableRows = new StringBuilder();
        
        for (ReportData data : reportDataList) {
            double successRate = data.getRunStatus().getResults() != null && !data.getRunStatus().getResults().isEmpty() ?
                data.getRunStatus().getResults().stream()
                    .mapToLong(r -> "PASSED".equals(r.getStatus()) ? 1 : 0)
                    .sum() * 100.0 / data.getRunStatus().getResults().size() : 0.0;
            
            tableRows.append("""
                <tr>
                    <td>%s</td>
                    <td>%d</td>
                    <td>%d</td>
                    <td>%.1f%%</td>
                    <td><span class="badge bg-secondary">%s</span></td>
                </tr>
                """.formatted(
                    data.getRunId(),
                    data.getRunStatus().getTotalTests(),
                    data.getRunStatus().getCompletedTests(),
                    successRate,
                    data.getRunStatus().getStatus()
                ));
        }
        
        return """
            <div class="container">
                <h2>Detailed Comparison</h2>
                <div class="table-responsive">
                    <table class="table table-striped">
                        <thead class="table-dark">
                            <tr>
                                <th>Run ID</th>
                                <th>Total Tests</th>
                                <th>Completed</th>
                                <th>Success Rate</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            %s
                        </tbody>
                    </table>
                </div>
            </div>
            """.formatted(tableRows.toString());
    }

    private String generateHtmlFooter() {
        return """
            <div class="footer">
                <div class="container">
                    <div class="row">
                        <div class="col-md-6">
                            <p class="text-muted mb-0">&copy; %d Test Framework - Execution Report</p>
                        </div>
                        <div class="col-md-6 text-end">
                            <p class="text-muted mb-0">Generated on %s</p>
                        </div>
                    </div>
                </div>
            </div>
            <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
            </body>
            </html>
            """.formatted(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            );
    }

    // ===========================
    // UTILITY METHODS
    // ===========================

    // ===========================
    // UTILITY METHODS
    // ===========================

    private void initializeReportsDirectory() {
        try {
            Path reportsDir = Paths.get(reportsPath);
            if (!Files.exists(reportsDir)) {
                Files.createDirectories(reportsDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize reports directory: " + e.getMessage(), e);
        }
    }

    private void writeHtmlToFile(String filePath, String htmlContent) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(htmlContent);
        }
    }

    private void writeCsvToFile(String filePath, String csvContent) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(csvContent);
        }
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes and wrap in quotes if contains special characters
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private double calculateSuccessRate(RunStatusDTO runStatus) {
        if (runStatus.getResults() == null || runStatus.getResults().isEmpty()) {
            return 0.0;
        }
        long passedTests = runStatus.getResults().stream()
            .mapToLong(r -> "PASSED".equals(r.getStatus()) ? 1 : 0)
            .sum();
        return (double) passedTests / runStatus.getResults().size() * 100.0;
    }

    private String truncateString(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    // ===========================
    // HELPER CLASSES
    // ===========================



    public static class HtmlReportConfiguration {
        private String title = "Test Execution Report";
        private boolean includeCharts = true;
        private boolean includeDetailedResults = true;
        private boolean includeAnalytics = true;
        private String theme = "default";
        private List<String> customCSS = new ArrayList<>();
        
        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public boolean isIncludeCharts() { return includeCharts; }
        public void setIncludeCharts(boolean includeCharts) { this.includeCharts = includeCharts; }
        public boolean isIncludeDetailedResults() { return includeDetailedResults; }
        public void setIncludeDetailedResults(boolean includeDetailedResults) { this.includeDetailedResults = includeDetailedResults; }
        public boolean isIncludeAnalytics() { return includeAnalytics; }
        public void setIncludeAnalytics(boolean includeAnalytics) { this.includeAnalytics = includeAnalytics; }
        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }
        public List<String> getCustomCSS() { return customCSS; }
        public void setCustomCSS(List<String> customCSS) { this.customCSS = customCSS; }
    }

}