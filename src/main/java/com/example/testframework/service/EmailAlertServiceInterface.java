package com.example.testframework.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for Email Alert Service functionality
 */
public interface EmailAlertServiceInterface {
    
    /**
     * Subscribe to run alerts for specific run ID
     */
    void subscribeToRunAlerts(String runId, List<String> emailAddresses);

    /**
     * Send execution completion alert
     */
    CompletableFuture<EmailAlertService.EmailResult> sendExecutionCompletionAlert(String runId);

    /**
     * Send execution failure alert
     */
    CompletableFuture<EmailAlertService.EmailResult> sendExecutionFailureAlert(String runId, String errorMessage);

    /**
     * Send test report via email
     */
    CompletableFuture<EmailAlertService.EmailResult> sendTestReport(String runId, List<String> recipients, byte[] reportContent, String reportFormat);

    /**
     * Cleanup subscriptions for completed runs
     */
    void cleanupRunSubscriptions(String runId);
}