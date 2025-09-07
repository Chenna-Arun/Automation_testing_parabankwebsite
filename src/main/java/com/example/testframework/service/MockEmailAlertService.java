package com.example.testframework.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Mock Email Alert Service for testing environment
 * This service is used when app.email.enabled=false
 */
@Service
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "false", matchIfMissing = true)
public class MockEmailAlertService implements EmailAlertServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(MockEmailAlertService.class);

    @Override
    public void subscribeToRunAlerts(String runId, List<String> emailAddresses) {
        logger.info("Mock: Subscribing {} email addresses to alerts for run: {}", emailAddresses.size(), runId);
        // Do nothing in mock implementation
    }

    @Override
    public CompletableFuture<EmailAlertService.EmailResult> sendExecutionCompletionAlert(String runId) {
        logger.info("Mock: Sending execution completion alert for run: {}", runId);
        return CompletableFuture.completedFuture(new EmailAlertService.EmailResult(true, "Mock completion alert sent for run: " + runId));
    }

    @Override
    public CompletableFuture<EmailAlertService.EmailResult> sendExecutionFailureAlert(String runId, String errorMessage) {
        logger.info("Mock: Sending execution failure alert for run: {} with error: {}", runId, errorMessage);
        return CompletableFuture.completedFuture(new EmailAlertService.EmailResult(true, "Mock failure alert sent for run: " + runId));
    }

    @Override
    public CompletableFuture<EmailAlertService.EmailResult> sendTestReport(String runId, List<String> recipients, byte[] reportContent, String reportFormat) {
        logger.info("Mock: Sending test report for run: {} to {} recipients in {} format", runId, recipients.size(), reportFormat);
        return CompletableFuture.completedFuture(new EmailAlertService.EmailResult(true, "Mock report sent for run: " + runId));
    }

    @Override
    public void cleanupRunSubscriptions(String runId) {
        logger.info("Mock: Cleaning up subscriptions for run: {}", runId);
        // Do nothing in mock implementation
    }
}