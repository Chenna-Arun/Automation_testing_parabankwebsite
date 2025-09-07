package com.example.testframework.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Advanced Test Data Management Service
 * Handles test data templates, dynamic generation, and validation
 */
@Service
public class TestDataManager {

    private final ObjectMapper objectMapper;
    private final Map<String, Map<String, Object>> dataTemplates;
    private final Random random;

    public TestDataManager() {
        this.objectMapper = new ObjectMapper();
        this.dataTemplates = new HashMap<>();
        this.random = new Random();
        initializeDataTemplates();
    }

    /**
     * Initialize predefined data templates
     */
    private void initializeDataTemplates() {
        // UI Test Data Templates
        dataTemplates.put("ui_login", Map.of(
            "username", "john.doe",
            "password", "demo"
        ));

        Map<String, Object> uiRegisterTemplate = new HashMap<>();
        uiRegisterTemplate.put("firstName", "John");
        uiRegisterTemplate.put("lastName", "Doe");
        uiRegisterTemplate.put("address", "123 Main Street");
        uiRegisterTemplate.put("city", "Springfield");
        uiRegisterTemplate.put("state", "IL");
        uiRegisterTemplate.put("zipCode", "62701");
        uiRegisterTemplate.put("phone", "555-1234");
        uiRegisterTemplate.put("ssn", "123-45-6789");
        uiRegisterTemplate.put("username", "john.doe.{timestamp}");
        uiRegisterTemplate.put("password", "demo123");
        uiRegisterTemplate.put("confirm", "demo123");
        dataTemplates.put("ui_register", uiRegisterTemplate);

        dataTemplates.put("ui_transfer", Map.of(
            "fromAccount", "12345",
            "toAccount", "67890",
            "amount", "100.00"
        ));

        Map<String, Object> uiBillpayTemplate = new HashMap<>();
        uiBillpayTemplate.put("payeeName", "Electric Company");
        uiBillpayTemplate.put("address", "456 Power St");
        uiBillpayTemplate.put("city", "Springfield");
        uiBillpayTemplate.put("state", "IL");
        uiBillpayTemplate.put("zipCode", "62701");
        uiBillpayTemplate.put("phone", "555-POWER");
        uiBillpayTemplate.put("account", "999888777");
        uiBillpayTemplate.put("verifyAccount", "999888777");
        uiBillpayTemplate.put("amount", "85.50");
        uiBillpayTemplate.put("fromAccountId", "12345");
        dataTemplates.put("ui_billpay", uiBillpayTemplate);

        // API Test Data Templates
        dataTemplates.put("api_login", Map.of(
            "username", "john",
            "password", "demo"
        ));

        Map<String, Object> apiCreateCustomerTemplate = new HashMap<>();
        apiCreateCustomerTemplate.put("firstName", "Jane");
        apiCreateCustomerTemplate.put("lastName", "Smith");
        apiCreateCustomerTemplate.put("address", "789 Oak Avenue");
        apiCreateCustomerTemplate.put("city", "Springfield");
        apiCreateCustomerTemplate.put("state", "IL");
        apiCreateCustomerTemplate.put("zipCode", "62702");
        apiCreateCustomerTemplate.put("phoneNumber", "555-5678");
        apiCreateCustomerTemplate.put("ssn", "987-65-4321");
        apiCreateCustomerTemplate.put("username", "jane.smith.{timestamp}");
        apiCreateCustomerTemplate.put("password", "password123");
        dataTemplates.put("api_create_customer", apiCreateCustomerTemplate);

        Map<String, Object> apiUpdateCustomerTemplate = new HashMap<>();
        apiUpdateCustomerTemplate.put("id", 12212);
        apiUpdateCustomerTemplate.put("firstName", "Jane");
        apiUpdateCustomerTemplate.put("lastName", "Johnson");
        apiUpdateCustomerTemplate.put("address", "321 Pine Street");
        apiUpdateCustomerTemplate.put("city", "Springfield");
        apiUpdateCustomerTemplate.put("state", "IL");
        apiUpdateCustomerTemplate.put("zipCode", "62703");
        apiUpdateCustomerTemplate.put("phoneNumber", "555-9999");
        dataTemplates.put("api_update_customer", apiUpdateCustomerTemplate);

        dataTemplates.put("api_transfer_funds", Map.of(
            "fromAccountId", 13344,
            "toAccountId", 13355,
            "amount", "250.00"
        ));

        dataTemplates.put("api_request_loan", Map.of(
            "customerId", 12212,
            "amount", "5000.00",
            "downPayment", "500.00",
            "fromAccountId", 13344
        ));

        dataTemplates.put("api_pay_bills", Map.of(
            "accountId", 13344,
            "payeeName", "Gas Company",
            "amount", "75.25"
        ));
    }

    /**
     * Get test data by template name with dynamic value replacement
     */
    public String getTestData(String templateName) {
        return getTestData(templateName, Collections.emptyMap());
    }

    /**
     * Get test data by template name with custom overrides
     */
    public String getTestData(String templateName, Map<String, Object> overrides) {
        Map<String, Object> template = dataTemplates.get(templateName.toLowerCase());
        if (template == null) {
            throw new IllegalArgumentException("Test data template not found: " + templateName);
        }

        Map<String, Object> data = new HashMap<>(template);
        
        // Apply overrides
        if (overrides != null) {
            data.putAll(overrides);
        }

        // Process dynamic values
        Map<String, Object> processedData = processDynamicValues(data);

        try {
            return objectMapper.writeValueAsString(processedData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize test data: " + e.getMessage());
        }
    }

    /**
     * Generate random test data for a specific functionality
     */
    public String generateRandomTestData(String functionality, String type) {
        String templateKey = type.toLowerCase() + "_" + functionality.toLowerCase();
        
        if (dataTemplates.containsKey(templateKey)) {
            return getTestData(templateKey);
        }

        // Generate basic random data if no template exists
        Map<String, Object> randomData = generateBasicRandomData(functionality, type);
        
        try {
            return objectMapper.writeValueAsString(randomData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate random test data: " + e.getMessage());
        }
    }

    /**
     * Validate test data format and content
     */
    public ValidationResult validateTestData(String testData, String functionality, String type) {
        try {
            Map<String, Object> data = objectMapper.readValue(testData, Map.class);
            return validateDataContent(data, functionality, type);
        } catch (Exception e) {
            return new ValidationResult(false, "Invalid JSON format: " + e.getMessage());
        }
    }

    /**
     * Get available data templates
     */
    public Set<String> getAvailableTemplates() {
        return dataTemplates.keySet();
    }

    /**
     * Add or update a data template
     */
    public void addTemplate(String name, Map<String, Object> template) {
        dataTemplates.put(name.toLowerCase(), new HashMap<>(template));
    }

    /**
     * Remove a data template
     */
    public boolean removeTemplate(String name) {
        return dataTemplates.remove(name.toLowerCase()) != null;
    }

    /**
     * Get template content
     */
    public Map<String, Object> getTemplate(String name) {
        return dataTemplates.get(name.toLowerCase());
    }

    // Private helper methods

    private Map<String, Object> processDynamicValues(Map<String, Object> data) {
        Map<String, Object> processed = new HashMap<>();
        
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            
            if (value instanceof String) {
                String stringValue = (String) value;
                stringValue = replaceDynamicPlaceholders(stringValue);
                processed.put(entry.getKey(), stringValue);
            } else {
                processed.put(entry.getKey(), value);
            }
        }
        
        return processed;
    }

    private String replaceDynamicPlaceholders(String value) {
        if (value == null) return null;

        // Replace {timestamp} with current timestamp
        if (value.contains("{timestamp}")) {
            String timestamp = String.valueOf(System.currentTimeMillis());
            value = value.replace("{timestamp}", timestamp);
        }

        // Replace {datetime} with formatted date time
        if (value.contains("{datetime}")) {
            String datetime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            value = value.replace("{datetime}", datetime);
        }

        // Replace {random} with random number
        if (value.contains("{random}")) {
            String randomNum = String.valueOf(random.nextInt(999999));
            value = value.replace("{random}", randomNum);
        }

        // Replace {uuid} with UUID
        if (value.contains("{uuid}")) {
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            value = value.replace("{uuid}", uuid);
        }

        return value;
    }

    private Map<String, Object> generateBasicRandomData(String functionality, String type) {
        Map<String, Object> data = new HashMap<>();
        
        switch (functionality.toLowerCase()) {
            case "login":
                data.put("username", "user" + random.nextInt(1000));
                data.put("password", "pass" + random.nextInt(1000));
                break;
                
            case "register", "createcustomer":
                data.put("firstName", getRandomFirstName());
                data.put("lastName", getRandomLastName());
                data.put("username", "user" + System.currentTimeMillis());
                data.put("password", "password123");
                data.put("address", random.nextInt(999) + " " + getRandomStreetName());
                data.put("city", getRandomCity());
                data.put("state", getRandomState());
                data.put("zipCode", String.format("%05d", random.nextInt(99999)));
                data.put("phone", String.format("555-%04d", random.nextInt(9999)));
                data.put("ssn", String.format("%03d-%02d-%04d", 
                    random.nextInt(999), random.nextInt(99), random.nextInt(9999)));
                break;
                
            case "transfer", "transferfunds":
                data.put("fromAccountId", 12000 + random.nextInt(1000));
                data.put("toAccountId", 13000 + random.nextInt(1000));
                data.put("amount", String.format("%.2f", 10.0 + random.nextDouble() * 1000.0));
                break;
                
            case "paybills":
                data.put("payeeName", getRandomPayeeName());
                data.put("amount", String.format("%.2f", 25.0 + random.nextDouble() * 500.0));
                data.put("accountId", 12000 + random.nextInt(1000));
                break;
                
            default:
                data.put("testData", "random_value_" + random.nextInt(1000));
        }
        
        return data;
    }

    private ValidationResult validateDataContent(Map<String, Object> data, String functionality, String type) {
        List<String> errors = new ArrayList<>();
        
        switch (functionality.toLowerCase()) {
            case "login":
                if (!data.containsKey("username") || data.get("username").toString().trim().isEmpty()) {
                    errors.add("Username is required for login");
                }
                if (!data.containsKey("password") || data.get("password").toString().trim().isEmpty()) {
                    errors.add("Password is required for login");
                }
                break;
                
            case "register", "createcustomer":
                String[] requiredFields = {"firstName", "lastName", "username", "password"};
                for (String field : requiredFields) {
                    if (!data.containsKey(field) || data.get(field).toString().trim().isEmpty()) {
                        errors.add(field + " is required for registration");
                    }
                }
                
                // Validate SSN format
                if (data.containsKey("ssn")) {
                    String ssn = data.get("ssn").toString();
                    if (!ssn.matches("\\d{3}-\\d{2}-\\d{4}")) {
                        errors.add("SSN must be in format XXX-XX-XXXX");
                    }
                }
                break;
                
            case "transfer", "transferfunds":
                if (!data.containsKey("amount")) {
                    errors.add("Amount is required for transfer");
                } else {
                    try {
                        double amount = Double.parseDouble(data.get("amount").toString());
                        if (amount <= 0) {
                            errors.add("Transfer amount must be positive");
                        }
                    } catch (NumberFormatException e) {
                        errors.add("Transfer amount must be a valid number");
                    }
                }
                break;
        }
        
        return new ValidationResult(errors.isEmpty(), 
            errors.isEmpty() ? "Test data validation passed" : String.join("; ", errors));
    }

    // Random data generators
    private String getRandomFirstName() {
        String[] names = {"John", "Jane", "Mike", "Sarah", "David", "Lisa", "Robert", "Emily", "James", "Anna"};
        return names[random.nextInt(names.length)];
    }

    private String getRandomLastName() {
        String[] names = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"};
        return names[random.nextInt(names.length)];
    }

    private String getRandomStreetName() {
        String[] streets = {"Main St", "Oak Ave", "Pine Rd", "Elm Dr", "Maple Ln", "Cedar Blvd", "Park Ave", "First St", "Second St", "Third St"};
        return streets[random.nextInt(streets.length)];
    }

    private String getRandomCity() {
        String[] cities = {"Springfield", "Franklin", "Georgetown", "Madison", "Riverside", "Oakland", "Fairview", "Bristol", "Salem", "Dover"};
        return cities[random.nextInt(cities.length)];
    }

    private String getRandomState() {
        String[] states = {"CA", "TX", "FL", "NY", "PA", "IL", "OH", "GA", "NC", "MI"};
        return states[random.nextInt(states.length)];
    }

    private String getRandomPayeeName() {
        String[] payees = {"Electric Company", "Gas Company", "Water Utility", "Cable Provider", "Internet Service", "Phone Company", "Insurance Co", "Credit Card Co"};
        return payees[random.nextInt(payees.length)];
    }

    /**
     * Validation result class
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return "ValidationResult{valid=" + valid + ", message='" + message + "'}";
        }
    }
}