package com.example.testframework.service;

import com.example.testframework.model.TestCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Comprehensive Test Case Validation Service
 * Validates test cases for business rules, data integrity, and execution readiness
 */
@Service
public class TestCaseValidator {

    private final TestDataManager testDataManager;
    private final TestEnvironmentManager testEnvironmentManager;
    private final ObjectMapper objectMapper;
    
    // Validation patterns
    private static final Pattern SSN_PATTERN = Pattern.compile("\\d{3}-\\d{2}-\\d{4}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\d{3}-\\d{4}|\\d{10}|\\(\\d{3}\\)\\s?\\d{3}-\\d{4}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern ZIP_PATTERN = Pattern.compile("\\d{5}(-\\d{4})?");

    @Autowired
    public TestCaseValidator(TestDataManager testDataManager, 
                           TestEnvironmentManager testEnvironmentManager, 
                           ObjectMapper objectMapper) {
        this.testDataManager = testDataManager;
        this.testEnvironmentManager = testEnvironmentManager;
        this.objectMapper = objectMapper;
    }

    /**
     * Comprehensive test case validation
     */
    public ValidationResult validateTestCase(TestCase testCase) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Basic validation
        validateBasicFields(testCase, errors);
        
        // Business rules validation
        validateBusinessRules(testCase, errors, warnings);
        
        // Test data validation
        validateTestData(testCase, errors, warnings);
        
        // Environment validation
        validateEnvironment(testCase, errors, warnings);
        
        // Execution readiness validation
        validateExecutionReadiness(testCase, errors, warnings);

        return new ValidationResult(
            errors.isEmpty(),
            errors.isEmpty() ? "Test case validation passed" : "Test case validation failed",
            errors,
            warnings
        );
    }

    /**
     * Validate multiple test cases
     */
    public Map<Long, ValidationResult> validateTestCases(List<TestCase> testCases) {
        Map<Long, ValidationResult> results = new HashMap<>();
        
        for (TestCase testCase : testCases) {
            ValidationResult result = validateTestCase(testCase);
            results.put(testCase.getId(), result);
        }
        
        return results;
    }

    /**
     * Quick validation for basic requirements
     */
    public boolean isValidForExecution(TestCase testCase) {
        if (testCase == null) return false;
        
        return testCase.getName() != null && !testCase.getName().trim().isEmpty() &&
               testCase.getType() != null &&
               testCase.getFunctionality() != null && !testCase.getFunctionality().trim().isEmpty() &&
               testCase.getStatus() == TestCase.Status.READY &&
               testCase.getIsActive() != null && testCase.getIsActive();
    }

    /**
     * Validate test case for specific environment
     */
    public ValidationResult validateForEnvironment(TestCase testCase, String environment) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Basic validation first
        ValidationResult basicResult = validateTestCase(testCase);
        errors.addAll(basicResult.getErrors());
        warnings.addAll(basicResult.getWarnings());

        // Environment-specific validation
        TestEnvironmentManager.ValidationResult envResult = 
            testEnvironmentManager.validateEnvironment(environment);
        
        if (!envResult.isValid()) {
            errors.add("Environment validation failed: " + envResult.getMessage());
        }

        // Test data validation for environment
        if (testCase.getTestData() != null) {
            validateTestDataForEnvironment(testCase, environment, errors, warnings);
        }

        return new ValidationResult(
            errors.isEmpty(),
            errors.isEmpty() ? "Test case valid for environment" : "Test case invalid for environment",
            errors,
            warnings
        );
    }

    // Private validation methods

    private void validateBasicFields(TestCase testCase, List<String> errors) {
        if (testCase == null) {
            errors.add("Test case cannot be null");
            return;
        }

        if (testCase.getName() == null || testCase.getName().trim().isEmpty()) {
            errors.add("Test case name is required");
        } else if (testCase.getName().length() > 200) {
            errors.add("Test case name cannot exceed 200 characters");
        }

        if (testCase.getType() == null) {
            errors.add("Test case type is required (UI or API)");
        }

        if (testCase.getFunctionality() == null || testCase.getFunctionality().trim().isEmpty()) {
            errors.add("Test case functionality is required");
        }

        if (testCase.getDescription() != null && testCase.getDescription().length() > 1000) {
            errors.add("Description cannot exceed 1000 characters");
        }

        if (testCase.getTimeout() != null && testCase.getTimeout() <= 0) {
            errors.add("Timeout must be positive");
        }

        if (testCase.getRetryCount() != null && testCase.getRetryCount() < 0) {
            errors.add("Retry count cannot be negative");
        }
    }

    private void validateBusinessRules(TestCase testCase, List<String> errors, List<String> warnings) {
        // Validate functionality for test type
        if (testCase.getType() != null && testCase.getFunctionality() != null) {
            if (!isValidFunctionalityForType(testCase.getType(), testCase.getFunctionality())) {
                errors.add("Functionality '" + testCase.getFunctionality() + 
                          "' is not valid for test type '" + testCase.getType() + "'");
            }
        }

        // Validate category consistency
        if (testCase.getCategory() != null && testCase.getType() != null) {
            if (!testCase.getCategory().equalsIgnoreCase(testCase.getType().toString()) &&
                !testCase.getCategory().toLowerCase().contains(testCase.getType().toString().toLowerCase())) {
                warnings.add("Category '" + testCase.getCategory() + 
                           "' may not be consistent with test type '" + testCase.getType() + "'");
            }
        }

        // Validate execution mode
        if (testCase.getExecutionMode() != null) {
            if (!Arrays.asList("SEQUENTIAL", "PARALLEL").contains(testCase.getExecutionMode().toUpperCase())) {
                errors.add("Execution mode must be SEQUENTIAL or PARALLEL");
            }
        }

        // Validate priority and timeout combination
        if (testCase.getPriority() == TestCase.Priority.HIGH && 
            testCase.getTimeout() != null && testCase.getTimeout() > 300) {
            warnings.add("High priority tests should typically have shorter timeouts");
        }

        // Validate retry count for critical tests
        if (testCase.getPriority() == TestCase.Priority.HIGH && 
            testCase.getRetryCount() != null && testCase.getRetryCount() > 1) {
            warnings.add("High priority tests should have minimal retry attempts");
        }
    }

    private void validateTestData(TestCase testCase, List<String> errors, List<String> warnings) {
        if (testCase.getTestData() == null || testCase.getTestData().trim().isEmpty()) {
            warnings.add("No test data provided - default values will be used");
            return;
        }

        try {
            // Validate JSON format
            Map<String, Object> data = objectMapper.readValue(testCase.getTestData(), Map.class);

            // Validate using TestDataManager
            TestDataManager.ValidationResult dataResult = 
                testDataManager.validateTestData(testCase.getTestData(), 
                                               testCase.getFunctionality(), 
                                               testCase.getType().toString());
            
            if (!dataResult.isValid()) {
                errors.add("Test data validation failed: " + dataResult.getMessage());
            }

            // Additional field-specific validation
            validateSpecificFields(data, errors, warnings);

        } catch (Exception e) {
            errors.add("Invalid test data JSON format: " + e.getMessage());
        }
    }

    private void validateSpecificFields(Map<String, Object> data, List<String> errors, List<String> warnings) {
        // Validate SSN format
        if (data.containsKey("ssn")) {
            String ssn = data.get("ssn").toString();
            if (!SSN_PATTERN.matcher(ssn).matches()) {
                errors.add("SSN must be in format XXX-XX-XXXX");
            }
        }

        // Validate phone format
        if (data.containsKey("phone") || data.containsKey("phoneNumber")) {
            String phone = data.getOrDefault("phone", data.get("phoneNumber")).toString();
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                warnings.add("Phone number format may not be standard");
            }
        }

        // Validate email format
        if (data.containsKey("email")) {
            String email = data.get("email").toString();
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                errors.add("Email format is invalid");
            }
        }

        // Validate zip code format
        if (data.containsKey("zipCode") || data.containsKey("zip")) {
            String zip = data.getOrDefault("zipCode", data.get("zip")).toString();
            if (!ZIP_PATTERN.matcher(zip).matches()) {
                warnings.add("Zip code format may not be standard");
            }
        }

        // Validate monetary amounts
        if (data.containsKey("amount")) {
            try {
                double amount = Double.parseDouble(data.get("amount").toString());
                if (amount < 0) {
                    errors.add("Amount cannot be negative");
                } else if (amount == 0) {
                    warnings.add("Amount is zero - may not be realistic for testing");
                } else if (amount > 1000000) {
                    warnings.add("Amount is very large - may cause test issues");
                }
            } catch (NumberFormatException e) {
                errors.add("Amount must be a valid number");
            }
        }

        // Validate username format
        if (data.containsKey("username")) {
            String username = data.get("username").toString();
            if (username.length() < 3) {
                warnings.add("Username is very short - may not meet application requirements");
            } else if (username.length() > 50) {
                warnings.add("Username is very long - may not meet application requirements");
            }
        }

        // Validate password strength
        if (data.containsKey("password")) {
            String password = data.get("password").toString();
            if (password.length() < 6) {
                warnings.add("Password is weak - may not meet security requirements");
            }
        }
    }

    private void validateEnvironment(TestCase testCase, List<String> errors, List<String> warnings) {
        if (testCase.getTestEnvironment() != null) {
            TestEnvironmentManager.ValidationResult envResult = 
                testEnvironmentManager.validateEnvironment(testCase.getTestEnvironment());
            
            if (!envResult.isValid()) {
                errors.add("Test environment validation failed: " + envResult.getMessage());
            }
        } else {
            warnings.add("No test environment specified - default environment will be used");
        }
    }

    private void validateExecutionReadiness(TestCase testCase, List<String> errors, List<String> warnings) {
        // Check if test case is ready for execution
        if (testCase.getStatus() != TestCase.Status.READY) {
            warnings.add("Test case status is '" + testCase.getStatus() + "' - may not be ready for execution");
        }

        if (testCase.getIsActive() != null && !testCase.getIsActive()) {
            warnings.add("Test case is marked as inactive");
        }

        // Check for missing optional but important fields
        if (testCase.getExpectedResult() == null || testCase.getExpectedResult().trim().isEmpty()) {
            warnings.add("No expected result specified - harder to verify test success");
        }

        if (testCase.getPriority() == null) {
            warnings.add("No priority specified - default priority will be used");
        }

        if (testCase.getTags() == null || testCase.getTags().trim().isEmpty()) {
            warnings.add("No tags specified - test may be harder to categorize and filter");
        }

        // Validate prerequisites
        if (testCase.getPrerequisites() != null && !testCase.getPrerequisites().trim().isEmpty()) {
            // This could be enhanced to validate actual prerequisite dependencies
            warnings.add("Test has prerequisites - ensure they are met before execution");
        }
    }

    private void validateTestDataForEnvironment(TestCase testCase, String environment, 
                                               List<String> errors, List<String> warnings) {
        try {
            Map<String, Object> data = objectMapper.readValue(testCase.getTestData(), Map.class);
            
            // Environment-specific validations
            if ("prod".equalsIgnoreCase(environment) || "production".equalsIgnoreCase(environment)) {
                // Production environment validations
                if (data.containsKey("amount")) {
                    double amount = Double.parseDouble(data.get("amount").toString());
                    if (amount > 100) {
                        warnings.add("Large transaction amounts in production may cause issues");
                    }
                }
                
                if (data.containsKey("username") && data.get("username").toString().contains("test")) {
                    warnings.add("Test usernames should not be used in production environment");
                }
            }
            
        } catch (Exception e) {
            // Already validated in main validation, just ignore here
        }
    }

    private boolean isValidFunctionalityForType(TestCase.Type type, String functionality) {
        Set<String> uiFunctionalities = Set.of(
            "login", "logout", "register", "registeraccount", "openaccount", 
            "accountoverview", "transferfunds", "paybills", "billpay", 
            "findtransactions", "updateprofile", "requestloan"
        );
        
        Set<String> apiFunctionalities = Set.of(
            "login", "createcustomer", "updatecustomer", "deletecustomer",
            "getcustomerdetails", "getaccounts", "getaccountdetails", 
            "gettransactionhistory", "transferfunds", "paybills", 
            "requestloan", "healthcheck", "validateapi"
        );
        
        String funcLower = functionality.toLowerCase();
        
        return switch (type) {
            case UI -> uiFunctionalities.contains(funcLower);
            case API -> apiFunctionalities.contains(funcLower);
        };
    }

    /**
     * Enhanced validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean valid, String message, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.message = message;
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public List<String> getErrors() { return new ArrayList<>(errors); }
        public List<String> getWarnings() { return new ArrayList<>(warnings); }
        public int getErrorCount() { return errors.size(); }
        public int getWarningCount() { return warnings.size(); }

        public String getDetailedReport() {
            StringBuilder report = new StringBuilder();
            report.append("Validation Result: ").append(valid ? "PASSED" : "FAILED").append("\n");
            report.append("Message: ").append(message).append("\n");
            
            if (!errors.isEmpty()) {
                report.append("\nErrors (").append(errors.size()).append("):\n");
                for (int i = 0; i < errors.size(); i++) {
                    report.append("  ").append(i + 1).append(". ").append(errors.get(i)).append("\n");
                }
            }
            
            if (!warnings.isEmpty()) {
                report.append("\nWarnings (").append(warnings.size()).append("):\n");
                for (int i = 0; i < warnings.size(); i++) {
                    report.append("  ").append(i + 1).append(". ").append(warnings.get(i)).append("\n");
                }
            }
            
            return report.toString();
        }

        @Override
        public String toString() {
            return "ValidationResult{" +
                    "valid=" + valid +
                    ", message='" + message + '\'' +
                    ", errors=" + errors.size() +
                    ", warnings=" + warnings.size() +
                    '}';
        }
    }
}