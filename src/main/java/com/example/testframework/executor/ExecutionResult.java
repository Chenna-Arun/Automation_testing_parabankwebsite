package com.example.testframework.executor;

import java.time.LocalDateTime;

/**
 * A unified result model for both API and UI executions.
 * Captures execution metadata, status, response, and artifacts (e.g., screenshots).
 */
public class ExecutionResult {

    // Generic fields
    private boolean success;
    private String details;
    private String errorMessage;
    private LocalDateTime executedAt;

    // API-specific fields
    private int statusCode;
    private String responseBody;

    // UI-specific fields
    private String screenshotPath;

    // --------------------
    // Constructors
    // --------------------
    public ExecutionResult() {
        this.executedAt = LocalDateTime.now();
    }

    public ExecutionResult(boolean success, String details) {
        this.success = success;
        this.details = details;
        this.executedAt = LocalDateTime.now();
    }

    // --------------------
    // Factory Methods
    // --------------------

    /** Success without extra artifacts */
    public static ExecutionResult success(String details) {
        return new ExecutionResult(true, details);
    }

    /** Failure without screenshot */
    public static ExecutionResult failure(String errorMessage) {
        ExecutionResult result = new ExecutionResult(false, "Execution failed");
        result.setErrorMessage(errorMessage);
        return result;
    }

    /** Failure with screenshot */
    public static ExecutionResult failure(String errorMessage, String screenshotPath) {
        ExecutionResult result = new ExecutionResult(false, "Execution failed");
        result.setErrorMessage(errorMessage);
        result.setScreenshotPath(screenshotPath);
        return result;
    }

    /** API-specific result */
    public static ExecutionResult apiResult(boolean success, String details,
                                            int statusCode, String responseBody) {
        ExecutionResult result = new ExecutionResult(success, details);
        result.setStatusCode(statusCode);
        result.setResponseBody(responseBody);
        return result;
    }

    /** UI-specific result */
    public static ExecutionResult uiResult(boolean success, String details, String screenshotPath) {
        ExecutionResult result = new ExecutionResult(success, details);
        result.setScreenshotPath(screenshotPath);
        return result;
    }

    // --------------------
    // Getters & Setters
    // --------------------
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    public String getScreenshotPath() { return screenshotPath; }
    public void setScreenshotPath(String screenshotPath) { this.screenshotPath = screenshotPath; }

    // --------------------
    // Utility Methods
    // --------------------
    @Override
    public String toString() {
        return "ExecutionResult{" +
                "success=" + success +
                ", details='" + details + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", executedAt=" + executedAt +
                ", statusCode=" + statusCode +
                ", responseBody='" + (responseBody != null
                ? responseBody.substring(0, Math.min(100, responseBody.length())) + "..."
                : null) + '\'' +
                ", screenshotPath='" + screenshotPath + '\'' +
                '}';
    }
}
