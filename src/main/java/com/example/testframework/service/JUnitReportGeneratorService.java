package com.example.testframework.service;

import com.example.testframework.dto.RunStatusDTO;
import com.example.testframework.dto.TestResultDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating JUnit XML reports compatible with CI/CD pipelines
 * Supports JUnit 4/5 XML format specifications
 */
@Service
public class JUnitReportGeneratorService {

    private final ExecutionService executionService;
    private final String reportsPath;
    private final XMLOutputFactory xmlOutputFactory;

    @Autowired
    public JUnitReportGeneratorService(ExecutionService executionService,
                                      @Value("${app.reports.path:./reports}") String reportsPath) {
        this.executionService = executionService;
        this.reportsPath = reportsPath;
        this.xmlOutputFactory = XMLOutputFactory.newInstance();
        initializeReportsDirectory();
    }

    /**
     * Generate JUnit XML report for a single test run
     */
    public String generateJUnitReport(String runId, JUnitReportConfiguration config) throws IOException, XMLStreamException {
        RunStatusDTO runStatus = executionService.getRunStatus(runId);
        if ("NOT_FOUND".equals(runStatus.getStatus())) {
            throw new IllegalArgumentException("Run not found: " + runId);
        }

        String reportId = "junit_report_" + runId + "_" + System.currentTimeMillis();
        String fileName = reportId + ".xml";
        String filePath = Paths.get(reportsPath, fileName).toString();

        try (FileWriter fileWriter = new FileWriter(filePath)) {
            XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(fileWriter);
            
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeCharacters("\n");
            
            if (config.isGroupByTestSuite()) {
                generateTestSuitesReport(writer, runId, runStatus, config);
            } else {
                generateSingleTestSuiteReport(writer, runId, runStatus, config);
            }
            
            writer.writeEndDocument();
            writer.flush();
            writer.close();
        }

        return filePath;
    }

    /**
     * Generate JUnit XML report for multiple test runs (comparison)
     */
    public String generateJUnitComparisonReport(List<String> runIds, JUnitReportConfiguration config) throws IOException, XMLStreamException {
        String reportId = "junit_comparison_" + System.currentTimeMillis();
        String fileName = reportId + ".xml";
        String filePath = Paths.get(reportsPath, fileName).toString();

        try (FileWriter fileWriter = new FileWriter(filePath)) {
            XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(fileWriter);
            
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeCharacters("\n");
            writer.writeStartElement("testsuites");
            writer.writeAttribute("name", "Multi-Run Comparison");
            writer.writeAttribute("time", getCurrentTimestamp());
            writer.writeCharacters("\n");

            for (String runId : runIds) {
                RunStatusDTO runStatus = executionService.getRunStatus(runId);
                if (!"NOT_FOUND".equals(runStatus.getStatus())) {
                    generateTestSuiteElement(writer, runId, runStatus, config);
                }
            }

            writer.writeEndElement(); // testsuites
            writer.writeCharacters("\n");
            writer.writeEndDocument();
            writer.flush();
            writer.close();
        }

        return filePath;
    }

    /**
     * Generate JUnit XML for test suite execution
     */
    public String generateJUnitTestSuiteReport(String suiteId, List<String> runIds, JUnitReportConfiguration config) throws IOException, XMLStreamException {
        String reportId = "junit_suite_" + suiteId + "_" + System.currentTimeMillis();
        String fileName = reportId + ".xml";
        String filePath = Paths.get(reportsPath, fileName).toString();

        try (FileWriter fileWriter = new FileWriter(filePath)) {
            XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(fileWriter);
            
            writer.writeStartDocument("UTF-8", "1.0");
            writer.writeCharacters("\n");
            writer.writeStartElement("testsuites");
            writer.writeAttribute("name", "Test Suite: " + suiteId);
            writer.writeAttribute("time", getCurrentTimestamp());
            writer.writeCharacters("\n");

            for (String runId : runIds) {
                RunStatusDTO runStatus = executionService.getRunStatus(runId);
                if (!"NOT_FOUND".equals(runStatus.getStatus())) {
                    generateTestSuiteElement(writer, runId, runStatus, config);
                }
            }

            writer.writeEndElement(); // testsuites
            writer.writeCharacters("\n");
            writer.writeEndDocument();
            writer.flush();
            writer.close();
        }

        return filePath;
    }

    // ===========================
    // XML GENERATION METHODS
    // ===========================

    private void generateTestSuitesReport(XMLStreamWriter writer, String runId, RunStatusDTO runStatus, JUnitReportConfiguration config) throws XMLStreamException {
        writer.writeStartElement("testsuites");
        writer.writeAttribute("name", "Test Execution Run: " + runId);
        writer.writeAttribute("time", getCurrentTimestamp());
        writer.writeCharacters("\n");

        if (runStatus.getResults() != null) {
            // Group tests by test suite or type
            Map<String, List<TestResultDTO>> groupedTests = groupTestResults(runStatus.getResults(), config);
            
            for (Map.Entry<String, List<TestResultDTO>> entry : groupedTests.entrySet()) {
                generateTestSuiteElement(writer, entry.getKey(), entry.getValue(), config);
            }
        }

        writer.writeEndElement(); // testsuites
        writer.writeCharacters("\n");
    }

    private void generateSingleTestSuiteReport(XMLStreamWriter writer, String runId, RunStatusDTO runStatus, JUnitReportConfiguration config) throws XMLStreamException {
        generateTestSuiteElement(writer, runId, runStatus, config);
    }

    private void generateTestSuiteElement(XMLStreamWriter writer, String runId, RunStatusDTO runStatus, JUnitReportConfiguration config) throws XMLStreamException {
        List<TestResultDTO> results = runStatus.getResults();
        if (results == null) {
            results = new ArrayList<>();
        }

        TestSuiteMetrics metrics = calculateTestSuiteMetrics(results);

        writer.writeStartElement("testsuite");
        writer.writeAttribute("name", "Test Run: " + runId);
        writer.writeAttribute("tests", String.valueOf(metrics.getTotalTests()));
        writer.writeAttribute("failures", String.valueOf(metrics.getFailures()));
        writer.writeAttribute("errors", String.valueOf(metrics.getErrors()));
        writer.writeAttribute("skipped", String.valueOf(metrics.getSkipped()));
        writer.writeAttribute("time", String.format("%.3f", metrics.getTotalTime()));
        writer.writeAttribute("timestamp", getCurrentTimestamp());
        
        if (config.isIncludeHostname()) {
            writer.writeAttribute("hostname", getHostname());
        }
        
        writer.writeCharacters("\n");

        // Add properties if configured
        if (config.isIncludeProperties()) {
            generatePropertiesElement(writer, runId, runStatus);
        }

        // Generate test cases
        for (TestResultDTO result : results) {
            generateTestCaseElement(writer, result, config);
        }

        // Add system-out and system-err if configured
        if (config.isIncludeSystemOut()) {
            generateSystemOutElement(writer, results);
        }

        if (config.isIncludeSystemErr()) {
            generateSystemErrElement(writer, results);
        }

        writer.writeEndElement(); // testsuite
        writer.writeCharacters("\n");
    }

    private void generateTestSuiteElement(XMLStreamWriter writer, String suiteName, List<TestResultDTO> results, JUnitReportConfiguration config) throws XMLStreamException {
        TestSuiteMetrics metrics = calculateTestSuiteMetrics(results);

        writer.writeStartElement("testsuite");
        writer.writeAttribute("name", suiteName);
        writer.writeAttribute("tests", String.valueOf(metrics.getTotalTests()));
        writer.writeAttribute("failures", String.valueOf(metrics.getFailures()));
        writer.writeAttribute("errors", String.valueOf(metrics.getErrors()));
        writer.writeAttribute("skipped", String.valueOf(metrics.getSkipped()));
        writer.writeAttribute("time", String.format("%.3f", metrics.getTotalTime()));
        writer.writeAttribute("timestamp", getCurrentTimestamp());
        
        if (config.isIncludeHostname()) {
            writer.writeAttribute("hostname", getHostname());
        }
        
        writer.writeCharacters("\n");

        // Generate test cases
        for (TestResultDTO result : results) {
            generateTestCaseElement(writer, result, config);
        }

        writer.writeEndElement(); // testsuite
        writer.writeCharacters("\n");
    }

    private void generateTestCaseElement(XMLStreamWriter writer, TestResultDTO result, JUnitReportConfiguration config) throws XMLStreamException {
        writer.writeStartElement("testcase");
        writer.writeAttribute("name", result.getTestCaseName());
        writer.writeAttribute("classname", extractClassName(result, config));
        writer.writeAttribute("time", String.format("%.3f", calculateExecutionTime(result)));
        
        if (config.isIncludeTestCaseId()) {
            writer.writeAttribute("id", result.getTestCaseId() != null ? result.getTestCaseId().toString() : "");
        }
        
        writer.writeCharacters("\n");

        String status = result.getStatus();
        
        if ("FAILED".equals(status)) {
            generateFailureElement(writer, result);
        } else if ("SKIPPED".equals(status)) {
            generateSkippedElement(writer, result);
        } else if (!"PASSED".equals(status)) {
            // Treat unknown statuses as errors
            generateErrorElement(writer, result);
        }

        // Add test case details as properties if configured
        if (config.isIncludeTestCaseDetails()) {
            generateTestCaseProperties(writer, result);
        }

        writer.writeEndElement(); // testcase
        writer.writeCharacters("\n");
    }

    private void generateFailureElement(XMLStreamWriter writer, TestResultDTO result) throws XMLStreamException {
        writer.writeStartElement("failure");
        writer.writeAttribute("message", truncateMessage(result.getErrorMessage()));
        writer.writeAttribute("type", "TestFailure");
        
        StringBuilder failureContent = new StringBuilder();
        if (result.getErrorMessage() != null) {
            failureContent.append("Error Message: ").append(result.getErrorMessage()).append("\n");
        }
        if (result.getDetails() != null) {
            failureContent.append("Details: ").append(result.getDetails()).append("\n");
        }
        if (result.getResponseBody() != null) {
            failureContent.append("Response: ").append(truncateContent(result.getResponseBody(), 1000)).append("\n");
        }
        if (result.getStatusCode() != null) {
            failureContent.append("Status Code: ").append(result.getStatusCode()).append("\n");
        }
        
        if (failureContent.length() > 0) {
            writer.writeCData(failureContent.toString());
        }
        
        writer.writeEndElement(); // failure
        writer.writeCharacters("\n");
    }

    private void generateSkippedElement(XMLStreamWriter writer, TestResultDTO result) throws XMLStreamException {
        writer.writeStartElement("skipped");
        if (result.getDetails() != null) {
            writer.writeAttribute("message", truncateMessage(result.getDetails()));
        }
        writer.writeEndElement(); // skipped
        writer.writeCharacters("\n");
    }

    private void generateErrorElement(XMLStreamWriter writer, TestResultDTO result) throws XMLStreamException {
        writer.writeStartElement("error");
        writer.writeAttribute("message", truncateMessage(result.getErrorMessage()));
        writer.writeAttribute("type", "TestError");
        
        if (result.getErrorMessage() != null || result.getDetails() != null) {
            StringBuilder errorContent = new StringBuilder();
            if (result.getErrorMessage() != null) {
                errorContent.append(result.getErrorMessage());
            }
            if (result.getDetails() != null) {
                if (errorContent.length() > 0) errorContent.append("\n");
                errorContent.append(result.getDetails());
            }
            writer.writeCData(errorContent.toString());
        }
        
        writer.writeEndElement(); // error
        writer.writeCharacters("\n");
    }

    private void generatePropertiesElement(XMLStreamWriter writer, String runId, RunStatusDTO runStatus) throws XMLStreamException {
        writer.writeStartElement("properties");
        writer.writeCharacters("\n");
        
        writeProperty(writer, "run.id", runId);
        writeProperty(writer, "run.status", runStatus.getStatus());
        writeProperty(writer, "run.totalTests", String.valueOf(runStatus.getTotalTests()));
        writeProperty(writer, "run.completedTests", String.valueOf(runStatus.getCompletedTests()));
        writeProperty(writer, "framework.name", "Test Integration Framework");
        writeProperty(writer, "framework.version", "1.0.0");
        writeProperty(writer, "java.version", System.getProperty("java.version"));
        writeProperty(writer, "os.name", System.getProperty("os.name"));
        writeProperty(writer, "timestamp", getCurrentTimestamp());
        
        writer.writeEndElement(); // properties
        writer.writeCharacters("\n");
    }

    private void generateTestCaseProperties(XMLStreamWriter writer, TestResultDTO result) throws XMLStreamException {
        writer.writeStartElement("properties");
        writer.writeCharacters("\n");
        
        if (result.getTestCaseId() != null) {
            writeProperty(writer, "testcase.id", result.getTestCaseId().toString());
        }
        if (result.getStatusCode() != null) {
            writeProperty(writer, "http.statusCode", result.getStatusCode().toString());
        }
        if (result.getScreenshotPath() != null) {
            writeProperty(writer, "screenshot.path", result.getScreenshotPath());
        }
        
        writer.writeEndElement(); // properties
        writer.writeCharacters("\n");
    }

    private void generateSystemOutElement(XMLStreamWriter writer, List<TestResultDTO> results) throws XMLStreamException {
        writer.writeStartElement("system-out");
        
        StringBuilder systemOut = new StringBuilder();
        systemOut.append("Test Execution System Output\n");
        systemOut.append("============================\n");
        
        for (TestResultDTO result : results) {
            if ("PASSED".equals(result.getStatus()) && result.getDetails() != null) {
                systemOut.append("PASSED: ").append(result.getTestCaseName())
                         .append(" - ").append(result.getDetails()).append("\n");
            }
        }
        
        writer.writeCData(systemOut.toString());
        writer.writeEndElement(); // system-out
        writer.writeCharacters("\n");
    }

    private void generateSystemErrElement(XMLStreamWriter writer, List<TestResultDTO> results) throws XMLStreamException {
        writer.writeStartElement("system-err");
        
        StringBuilder systemErr = new StringBuilder();
        systemErr.append("Test Execution Error Output\n");
        systemErr.append("===========================\n");
        
        for (TestResultDTO result : results) {
            if ("FAILED".equals(result.getStatus()) && result.getErrorMessage() != null) {
                systemErr.append("FAILED: ").append(result.getTestCaseName())
                         .append(" - ").append(result.getErrorMessage()).append("\n");
            }
        }
        
        if (systemErr.length() > 50) { // Only include if there are actual errors
            writer.writeCData(systemErr.toString());
        }
        
        writer.writeEndElement(); // system-err
        writer.writeCharacters("\n");
    }

    private void writeProperty(XMLStreamWriter writer, String name, String value) throws XMLStreamException {
        writer.writeStartElement("property");
        writer.writeAttribute("name", name);
        writer.writeAttribute("value", value != null ? value : "");
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }

    // ===========================
    // UTILITY METHODS
    // ===========================

    private TestSuiteMetrics calculateTestSuiteMetrics(List<TestResultDTO> results) {
        TestSuiteMetrics metrics = new TestSuiteMetrics();
        metrics.setTotalTests(results.size());
        
        long failures = results.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
        long errors = results.stream().filter(r -> !Arrays.asList("PASSED", "FAILED", "SKIPPED").contains(r.getStatus())).count();
        long skipped = results.stream().filter(r -> "SKIPPED".equals(r.getStatus())).count();
        
        metrics.setFailures((int) failures);
        metrics.setErrors((int) errors);
        metrics.setSkipped((int) skipped);
        
        // Calculate total time (placeholder - would use actual timing data)
        double totalTime = results.size() * 1.5; // 1.5 seconds average per test
        metrics.setTotalTime(totalTime);
        
        return metrics;
    }

    private Map<String, List<TestResultDTO>> groupTestResults(List<TestResultDTO> results, JUnitReportConfiguration config) {
        if (config.getGroupingStrategy() == JUnitReportConfiguration.GroupingStrategy.BY_TEST_TYPE) {
            return results.stream().collect(Collectors.groupingBy(
                result -> extractTestType(result),
                LinkedHashMap::new,
                Collectors.toList()
            ));
        } else if (config.getGroupingStrategy() == JUnitReportConfiguration.GroupingStrategy.BY_STATUS) {
            return results.stream().collect(Collectors.groupingBy(
                TestResultDTO::getStatus,
                LinkedHashMap::new,
                Collectors.toList()
            ));
        } else {
            // Default grouping by class name
            return results.stream().collect(Collectors.groupingBy(
                result -> extractClassName(result, config),
                LinkedHashMap::new,
                Collectors.toList()
            ));
        }
    }

    private String extractClassName(TestResultDTO result, JUnitReportConfiguration config) {
        if (config.isUseTestCaseNameAsClassName()) {
            return result.getTestCaseName() != null ? result.getTestCaseName() : "UnknownTest";
        }
        
        // Extract class name from test case name or use default
        String testName = result.getTestCaseName();
        if (testName != null && testName.contains(".")) {
            int lastDotIndex = testName.lastIndexOf(".");
            return testName.substring(0, lastDotIndex);
        }
        
        return config.getDefaultClassName();
    }

    private String extractTestType(TestResultDTO result) {
        // Extract test type from test case name or details
        String testName = result.getTestCaseName();
        if (testName != null) {
            if (testName.toLowerCase().contains("ui") || testName.toLowerCase().contains("selenium")) {
                return "UI Tests";
            } else if (testName.toLowerCase().contains("api") || testName.toLowerCase().contains("rest")) {
                return "API Tests";
            }
        }
        return "General Tests";
    }

    private double calculateExecutionTime(TestResultDTO result) {
        // Placeholder - would calculate actual execution time from timestamps
        return 1.5; // 1.5 seconds default
    }

    private String truncateMessage(String message) {
        if (message == null) {
            return "";
        }
        return message.length() > 255 ? message.substring(0, 252) + "..." : message;
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        return content.length() > maxLength ? content.substring(0, maxLength - 3) + "..." : content;
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

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

    // ===========================
    // CONFIGURATION CLASSES
    // ===========================

    public static class JUnitReportConfiguration {
        private boolean groupByTestSuite = false;
        private boolean includeProperties = true;
        private boolean includeHostname = true;
        private boolean includeTestCaseId = true;
        private boolean includeTestCaseDetails = true;
        private boolean includeSystemOut = false;
        private boolean includeSystemErr = true;
        private boolean useTestCaseNameAsClassName = false;
        private String defaultClassName = "TestCase";
        private GroupingStrategy groupingStrategy = GroupingStrategy.BY_CLASS_NAME;
        
        public enum GroupingStrategy {
            BY_CLASS_NAME,
            BY_TEST_TYPE,
            BY_STATUS
        }
        
        // Getters and setters
        public boolean isGroupByTestSuite() { return groupByTestSuite; }
        public void setGroupByTestSuite(boolean groupByTestSuite) { this.groupByTestSuite = groupByTestSuite; }
        public boolean isIncludeProperties() { return includeProperties; }
        public void setIncludeProperties(boolean includeProperties) { this.includeProperties = includeProperties; }
        public boolean isIncludeHostname() { return includeHostname; }
        public void setIncludeHostname(boolean includeHostname) { this.includeHostname = includeHostname; }
        public boolean isIncludeTestCaseId() { return includeTestCaseId; }
        public void setIncludeTestCaseId(boolean includeTestCaseId) { this.includeTestCaseId = includeTestCaseId; }
        public boolean isIncludeTestCaseDetails() { return includeTestCaseDetails; }
        public void setIncludeTestCaseDetails(boolean includeTestCaseDetails) { this.includeTestCaseDetails = includeTestCaseDetails; }
        public boolean isIncludeSystemOut() { return includeSystemOut; }
        public void setIncludeSystemOut(boolean includeSystemOut) { this.includeSystemOut = includeSystemOut; }
        public boolean isIncludeSystemErr() { return includeSystemErr; }
        public void setIncludeSystemErr(boolean includeSystemErr) { this.includeSystemErr = includeSystemErr; }
        public boolean isUseTestCaseNameAsClassName() { return useTestCaseNameAsClassName; }
        public void setUseTestCaseNameAsClassName(boolean useTestCaseNameAsClassName) { this.useTestCaseNameAsClassName = useTestCaseNameAsClassName; }
        public String getDefaultClassName() { return defaultClassName; }
        public void setDefaultClassName(String defaultClassName) { this.defaultClassName = defaultClassName; }
        public GroupingStrategy getGroupingStrategy() { return groupingStrategy; }
        public void setGroupingStrategy(GroupingStrategy groupingStrategy) { this.groupingStrategy = groupingStrategy; }
    }

    public static class TestSuiteMetrics {
        private int totalTests;
        private int failures;
        private int errors;
        private int skipped;
        private double totalTime;
        
        // Getters and setters
        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }
        public int getFailures() { return failures; }
        public void setFailures(int failures) { this.failures = failures; }
        public int getErrors() { return errors; }
        public void setErrors(int errors) { this.errors = errors; }
        public int getSkipped() { return skipped; }
        public void setSkipped(int skipped) { this.skipped = skipped; }
        public double getTotalTime() { return totalTime; }
        public void setTotalTime(double totalTime) { this.totalTime = totalTime; }
    }
}