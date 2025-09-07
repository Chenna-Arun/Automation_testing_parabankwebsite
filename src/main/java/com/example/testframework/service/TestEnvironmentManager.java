package com.example.testframework.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Test Environment Manager for handling different test environments
 * and environment-specific configurations
 */
@Service
public class TestEnvironmentManager {

    @Value("${parabank.ui.base-url}")
    private String defaultUiBaseUrl;

    @Value("${parabank.api.base-url}")
    private String defaultApiBaseUrl;

    private final Map<String, EnvironmentConfig> environments;

    public TestEnvironmentManager() {
        this.environments = new HashMap<>();
        initializeDefaultEnvironments();
    }

    private void initializeDefaultEnvironments() {
        // Development Environment
        environments.put("dev", new EnvironmentConfig(
            "dev",
            "Development",
            "https://parabank-dev.parasoft.com/parabank",
            "https://parabank-dev.parasoft.com/parabank/services/bank",
            Map.of(
                "timeout", "30",
                "retries", "3",
                "headless", "true",
                "parallel", "true",
                "threadPool", "5"
            )
        ));

        // Testing Environment
        environments.put("test", new EnvironmentConfig(
            "test",
            "Testing",
            "https://parabank-test.parasoft.com/parabank",
            "https://parabank-test.parasoft.com/parabank/services/bank",
            Map.of(
                "timeout", "60",
                "retries", "2",
                "headless", "true",
                "parallel", "true",
                "threadPool", "10"
            )
        ));

        // Staging Environment
        environments.put("staging", new EnvironmentConfig(
            "staging",
            "Staging",
            "https://parabank-staging.parasoft.com/parabank",
            "https://parabank-staging.parasoft.com/parabank/services/bank",
            Map.of(
                "timeout", "45",
                "retries", "1",
                "headless", "false",
                "parallel", "false",
                "threadPool", "3"
            )
        ));

        // Production Environment
        environments.put("prod", new EnvironmentConfig(
            "prod",
            "Production",
            "https://parabank.parasoft.com/parabank",
            "https://parabank.parasoft.com/parabank/services/bank",
            Map.of(
                "timeout", "30",
                "retries", "0",
                "headless", "true",
                "parallel", "false",
                "threadPool", "1"
            )
        ));

        // Local Environment
        environments.put("local", new EnvironmentConfig(
            "local",
            "Local Development",
            "http://localhost:8080/parabank",
            "http://localhost:8080/parabank/services/bank",
            Map.of(
                "timeout", "20",
                "retries", "5",
                "headless", "false",
                "parallel", "true",
                "threadPool", "3"
            )
        ));
    }

    /**
     * Get environment configuration by name
     */
    public EnvironmentConfig getEnvironment(String environmentName) {
        if (environmentName == null || environmentName.trim().isEmpty()) {
            return getDefaultEnvironment();
        }

        EnvironmentConfig config = environments.get(environmentName.toLowerCase());
        return config != null ? config : getDefaultEnvironment();
    }

    /**
     * Get default environment configuration
     */
    public EnvironmentConfig getDefaultEnvironment() {
        return new EnvironmentConfig(
            "default",
            "Default",
            defaultUiBaseUrl,
            defaultApiBaseUrl,
            Map.of(
                "timeout", "30",
                "retries", "3",
                "headless", "true",
                "parallel", "true",
                "threadPool", "5"
            )
        );
    }

    /**
     * Get all available environments
     */
    public Set<String> getAvailableEnvironments() {
        return environments.keySet();
    }

    /**
     * Add or update an environment configuration
     */
    public void addEnvironment(String name, EnvironmentConfig config) {
        environments.put(name.toLowerCase(), config);
    }

    /**
     * Remove an environment configuration
     */
    public boolean removeEnvironment(String name) {
        return environments.remove(name.toLowerCase()) != null;
    }

    /**
     * Validate environment configuration
     */
    public ValidationResult validateEnvironment(String environmentName) {
        EnvironmentConfig config = getEnvironment(environmentName);
        
        List<String> errors = new ArrayList<>();
        
        if (config.getUiBaseUrl() == null || config.getUiBaseUrl().trim().isEmpty()) {
            errors.add("UI Base URL is required");
        } else if (!isValidUrl(config.getUiBaseUrl())) {
            errors.add("UI Base URL is not valid");
        }
        
        if (config.getApiBaseUrl() == null || config.getApiBaseUrl().trim().isEmpty()) {
            errors.add("API Base URL is required");
        } else if (!isValidUrl(config.getApiBaseUrl())) {
            errors.add("API Base URL is not valid");
        }
        
        // Validate timeout
        try {
            int timeout = Integer.parseInt(config.getProperty("timeout"));
            if (timeout <= 0) {
                errors.add("Timeout must be positive");
            }
        } catch (NumberFormatException e) {
            errors.add("Timeout must be a valid number");
        }
        
        // Validate retries
        try {
            int retries = Integer.parseInt(config.getProperty("retries"));
            if (retries < 0) {
                errors.add("Retries cannot be negative");
            }
        } catch (NumberFormatException e) {
            errors.add("Retries must be a valid number");
        }
        
        // Validate thread pool size
        try {
            int threadPool = Integer.parseInt(config.getProperty("threadPool"));
            if (threadPool <= 0) {
                errors.add("Thread pool size must be positive");
            }
        } catch (NumberFormatException e) {
            errors.add("Thread pool size must be a valid number");
        }
        
        return new ValidationResult(errors.isEmpty(),
            errors.isEmpty() ? "Environment configuration is valid" : String.join("; ", errors));
    }

    /**
     * Get environment-specific test configuration
     */
    public TestConfiguration getTestConfiguration(String environmentName) {
        EnvironmentConfig env = getEnvironment(environmentName);
        
        return new TestConfiguration(
            env.getUiBaseUrl(),
            env.getApiBaseUrl(),
            Integer.parseInt(env.getProperty("timeout")),
            Integer.parseInt(env.getProperty("retries")),
            Boolean.parseBoolean(env.getProperty("headless")),
            Boolean.parseBoolean(env.getProperty("parallel")),
            Integer.parseInt(env.getProperty("threadPool"))
        );
    }

    private boolean isValidUrl(String url) {
        try {
            new java.net.URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Environment configuration class
     */
    public static class EnvironmentConfig {
        private final String id;
        private final String name;
        private final String uiBaseUrl;
        private final String apiBaseUrl;
        private final Map<String, String> properties;

        public EnvironmentConfig(String id, String name, String uiBaseUrl, String apiBaseUrl, Map<String, String> properties) {
            this.id = id;
            this.name = name;
            this.uiBaseUrl = uiBaseUrl;
            this.apiBaseUrl = apiBaseUrl;
            this.properties = new HashMap<>(properties);
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getUiBaseUrl() { return uiBaseUrl; }
        public String getApiBaseUrl() { return apiBaseUrl; }
        public Map<String, String> getProperties() { return new HashMap<>(properties); }
        public String getProperty(String key) { return properties.get(key); }

        @Override
        public String toString() {
            return "EnvironmentConfig{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", uiBaseUrl='" + uiBaseUrl + '\'' +
                    ", apiBaseUrl='" + apiBaseUrl + '\'' +
                    ", properties=" + properties +
                    '}';
        }
    }

    /**
     * Test configuration class
     */
    public static class TestConfiguration {
        private final String uiBaseUrl;
        private final String apiBaseUrl;
        private final int timeout;
        private final int retries;
        private final boolean headless;
        private final boolean parallel;
        private final int threadPoolSize;

        public TestConfiguration(String uiBaseUrl, String apiBaseUrl, int timeout, int retries, 
                               boolean headless, boolean parallel, int threadPoolSize) {
            this.uiBaseUrl = uiBaseUrl;
            this.apiBaseUrl = apiBaseUrl;
            this.timeout = timeout;
            this.retries = retries;
            this.headless = headless;
            this.parallel = parallel;
            this.threadPoolSize = threadPoolSize;
        }

        public String getUiBaseUrl() { return uiBaseUrl; }
        public String getApiBaseUrl() { return apiBaseUrl; }
        public int getTimeout() { return timeout; }
        public int getRetries() { return retries; }
        public boolean isHeadless() { return headless; }
        public boolean isParallel() { return parallel; }
        public int getThreadPoolSize() { return threadPoolSize; }

        @Override
        public String toString() {
            return "TestConfiguration{" +
                    "uiBaseUrl='" + uiBaseUrl + '\'' +
                    ", apiBaseUrl='" + apiBaseUrl + '\'' +
                    ", timeout=" + timeout +
                    ", retries=" + retries +
                    ", headless=" + headless +
                    ", parallel=" + parallel +
                    ", threadPoolSize=" + threadPoolSize +
                    '}';
        }
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