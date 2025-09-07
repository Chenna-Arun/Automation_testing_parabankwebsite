package com.example.testframework.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Email Alert Service for sending notifications related to test execution
 * Follows Spring Boot best practices with proper dependency injection
 */
@Service
@ConditionalOnProperty(name = "app.email.enabled", havingValue = "true", matchIfMissing = false)
public class EmailAlertService implements EmailAlertServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(EmailAlertService.class);
    
    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String appName;
    private final boolean emailEnabled;
    
    // Store email subscriptions for run alerts
    private final Map<String, List<String>> runAlertSubscriptions = new ConcurrentHashMap<>();

    @Autowired
    public EmailAlertService(
            JavaMailSender mailSender,
            @Value("${app.email.from:noreply@testframework.com}") String fromEmail,
            @Value("${app.name:Test Integration Engine}") String appName,
            @Value("${app.email.enabled:false}") boolean emailEnabled) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.appName = appName;
        this.emailEnabled = emailEnabled;
    }

    /**
     * Subscribe to run alerts for specific run ID
     */
    public void subscribeToRunAlerts(String runId, List<String> emailAddresses) {
        if (!emailEnabled) {
            logger.info("Email is disabled, skipping alert subscription for run: {}", runId);
            return;
        }
        
        logger.info("Subscribing {} email addresses to alerts for run: {}", emailAddresses.size(), runId);
        runAlertSubscriptions.put(runId, emailAddresses);
    }

    /**
     * Send execution completion alert
     */
    public CompletableFuture<EmailResult> sendExecutionCompletionAlert(String runId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!emailEnabled) {
                    logger.info("Email is disabled, skipping completion alert for run: {}", runId);
                    return new EmailResult(true, "Email disabled - alert skipped");
                }

                List<String> subscribers = runAlertSubscriptions.get(runId);
                if (subscribers == null || subscribers.isEmpty()) {
                    logger.warn("No email subscribers found for run: {}", runId);
                    return new EmailResult(false, "No subscribers for run: " + runId);
                }

                String subject = String.format("[%s] Test Execution Completed - Run ID: %s", appName, runId);
                String body = buildCompletionEmailBody(runId);

                for (String email : subscribers) {
                    sendEmail(email, subject, body);
                }

                logger.info("Execution completion alerts sent for run: {} to {} recipients", runId, subscribers.size());
                return new EmailResult(true, "Completion alerts sent to " + subscribers.size() + " recipients");
                
            } catch (Exception e) {
                logger.error("Failed to send execution completion alert for run: {}", runId, e);
                return new EmailResult(false, "Failed to send completion alert: " + e.getMessage());
            }
        });
    }

    /**
     * Send execution failure alert
     */
    public CompletableFuture<EmailResult> sendExecutionFailureAlert(String runId, String errorMessage) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!emailEnabled) {
                    logger.info("Email is disabled, skipping failure alert for run: {}", runId);
                    return new EmailResult(true, "Email disabled - alert skipped");
                }

                List<String> subscribers = runAlertSubscriptions.get(runId);
                if (subscribers == null || subscribers.isEmpty()) {
                    logger.warn("No email subscribers found for run: {}", runId);
                    return new EmailResult(false, "No subscribers for run: " + runId);
                }

                String subject = String.format("[%s] Test Execution FAILED - Run ID: %s", appName, runId);
                String body = buildFailureEmailBody(runId, errorMessage);

                for (String email : subscribers) {
                    sendEmail(email, subject, body);
                }

                logger.info("Execution failure alerts sent for run: {} to {} recipients", runId, subscribers.size());
                return new EmailResult(true, "Failure alerts sent to " + subscribers.size() + " recipients");
                
            } catch (Exception e) {
                logger.error("Failed to send execution failure alert for run: {}", runId, e);
                return new EmailResult(false, "Failed to send failure alert: " + e.getMessage());
            }
        });
    }

    /**
     * Send test report via email
     */
    public CompletableFuture<EmailResult> sendTestReport(String runId, List<String> recipients, byte[] reportContent, String reportFormat) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!emailEnabled) {
                    logger.info("Email is disabled, skipping report email for run: {}", runId);
                    return new EmailResult(true, "Email disabled - report email skipped");
                }

                String subject = String.format("[%s] Test Execution Report - Run ID: %s", appName, runId);
                String body = buildReportEmailBody(runId, reportFormat);
                String attachmentName = String.format("test-report-%s.%s", runId, reportFormat.toLowerCase());

                for (String email : recipients) {
                    sendEmailWithAttachment(email, subject, body, reportContent, attachmentName);
                }

                logger.info("Test reports sent for run: {} to {} recipients", runId, recipients.size());
                return new EmailResult(true, "Reports sent to " + recipients.size() + " recipients");
                
            } catch (Exception e) {
                logger.error("Failed to send test report for run: {}", runId, e);
                return new EmailResult(false, "Failed to send report: " + e.getMessage());
            }
        });
    }

    /**
     * Cleanup subscriptions for completed runs
     */
    public void cleanupRunSubscriptions(String runId) {
        runAlertSubscriptions.remove(runId);
        logger.debug("Cleaned up email subscriptions for run: {}", runId);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            mailSender.send(message);
            logger.debug("Email sent to: {}", to);
            
        } catch (Exception e) {
            logger.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private void sendEmailWithAttachment(String to, String subject, String body, byte[] attachment, String attachmentName) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, false);
        helper.addAttachment(attachmentName, new org.springframework.core.io.ByteArrayResource(attachment));
        
        mailSender.send(message);
        logger.debug("Email with attachment sent to: {}", to);
    }

    private String buildCompletionEmailBody(String runId) {
        return String.format("""
            Dear Team,
            
            The test execution has been completed successfully.
            
            Run Details:
            - Run ID: %s
            - Status: COMPLETED
            - Timestamp: %s
            
            Please check the test reports for detailed results.
            
            Best regards,
            %s Test Framework
            """, runId, java.time.LocalDateTime.now(), appName);
    }

    private String buildFailureEmailBody(String runId, String errorMessage) {
        return String.format("""
            Dear Team,
            
            The test execution has FAILED and requires attention.
            
            Run Details:
            - Run ID: %s
            - Status: FAILED
            - Error: %s
            - Timestamp: %s
            
            Please investigate the failure and check logs for more details.
            
            Best regards,
            %s Test Framework
            """, runId, errorMessage, java.time.LocalDateTime.now(), appName);
    }

    private String buildReportEmailBody(String runId, String reportFormat) {
        return String.format("""
            Dear Team,
            
            Please find the test execution report attached.
            
            Run Details:
            - Run ID: %s
            - Report Format: %s
            - Generated: %s
            
            Best regards,
            %s Test Framework
            """, runId, reportFormat, java.time.LocalDateTime.now(), appName);
    }

    /**
     * Email result wrapper class
     */
    public static class EmailResult {
        private final boolean success;
        private final String message;

        public EmailResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format("EmailResult{success=%s, message='%s'}", success, message);
        }
    }
}