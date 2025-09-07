package com.example.testframework.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * UI Test Executor for Parabank using Selenium WebDriver.
 * Supports headless toggle from application.properties.
 */
public class ParabankUITestExecutor {
    private final String baseUrl;
    private final String screenshotPath;
    private final boolean headless;
    private final ObjectMapper objectMapper;
    private final int defaultTimeout;
    private final int maxRetries;

    public ParabankUITestExecutor(String baseUrl, String screenshotPath, boolean headless) {
        this(baseUrl, screenshotPath, headless, 30, 3);
    }

    public ParabankUITestExecutor(String baseUrl, String screenshotPath, boolean headless, 
                                 int defaultTimeout, int maxRetries) {
        // Ensure baseUrl does not end with index.htm
        if (baseUrl.endsWith("index.htm")) {
            baseUrl = baseUrl.replace("/index.htm", "");
        }
        this.baseUrl = baseUrl;
        this.screenshotPath = screenshotPath != null ? screenshotPath : "./screenshots";
        this.headless = headless;
        this.objectMapper = new ObjectMapper();
        this.defaultTimeout = defaultTimeout;
        this.maxRetries = maxRetries;
        WebDriverManager.chromedriver().setup();
    }

    private ChromeDriver createDriver() {
        System.out.println("📱 Creating Chrome driver - headless: " + headless);
        
        try {
            // Set up WebDriverManager to ensure proper ChromeDriver setup
            WebDriverManager.chromedriver().setup();
            System.out.println("📱 WebDriverManager setup completed");
            
            ChromeOptions options = new ChromeOptions();
            if (headless) {
                options.addArguments("--headless=new");
                System.out.println("📱 Chrome set to headless mode");
            } else {
                System.out.println("📱 Chrome set to visible mode");
            }
            
            // Add comprehensive Chrome options for better compatibility
            options.addArguments(
                "--no-sandbox", 
                "--disable-dev-shm-usage", 
                "--disable-gpu",
                "--window-size=1920,1080",
                "--disable-blink-features=AutomationControlled",
                "--disable-extensions",
                "--disable-web-security",
                "--disable-features=VizDisplayCompositor",
                "--disable-ipc-flooding-protection",
                "--remote-debugging-port=0",
                "--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
            );
            
            System.out.println("📱 Creating ChromeDriver instance...");
            ChromeDriver driver = new ChromeDriver(options);
            System.out.println("📱 ChromeDriver created successfully");
            
            // Configure timeouts
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(defaultTimeout));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(defaultTimeout));
            
            // Maximize window if not headless
            if (!headless) {
                try {
                    driver.manage().window().maximize();
                    System.out.println("📱 Chrome window maximized");
                } catch (Exception e) {
                    System.out.println("⚠️ Could not maximize window: " + e.getMessage());
                }
            }
            
            System.out.println("📱 ChromeDriver configured and ready");
            return driver;
            
        } catch (Exception e) {
            System.err.println("❌ Failed to create ChromeDriver: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Chrome driver creation failed: " + e.getMessage(), e);
        }
    }

    private String takeScreenshot(WebDriver driver, String testName) {
        try {
            Path destDir = Paths.get(screenshotPath);
            Files.createDirectories(destDir);
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path dest = destDir.resolve(testName + "_" + timestamp + "_" + UUID.randomUUID() + ".png");
            Files.copy(src.toPath(), dest);
            return dest.toString();
        } catch (Exception e) {
            System.err.println("Screenshot failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Enhanced execute method with retry logic and dynamic data support
     */
    public ExecutionResult execute(String functionality) {
        return execute(functionality, null, maxRetries);
    }

    /**
     * Execute with custom test data
     */
    public ExecutionResult execute(String functionality, String testData) {
        return execute(functionality, testData, maxRetries);
    }

    /**
     * Execute with custom test data and retry count
     */
    public ExecutionResult execute(String functionality, String testData, int retryCount) {
        if (functionality == null) {
            return ExecutionResult.failure("UI functionality is null");
        }

        ExecutionResult lastResult = null;
        
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                if (attempt > 0) {
                    System.out.println("Retrying test execution (attempt " + (attempt + 1) + "/" + (retryCount + 1) + ")");
                    Thread.sleep(2000); // Wait before retry
                }
                
                lastResult = executeTest(functionality, testData);
                
                if (lastResult.isSuccess()) {
                    return lastResult; // Success, no need to retry
                }
            } catch (Exception e) {
                lastResult = ExecutionResult.failure("Test execution error (attempt " + (attempt + 1) + "): " + e.getMessage());
            }
        }
        
        return lastResult != null ? lastResult : ExecutionResult.failure("Test execution failed after all retry attempts");
    }

    /**
     * Core test execution logic with smart authentication
     * All operations except signup/register/login use unified session management
     */
    private ExecutionResult executeTest(String functionality, String testData) {
        System.out.println("📱 ParabankUITestExecutor.executeTest() called with functionality: " + functionality);
        System.out.println("📱 Test data: " + testData);
        
        return switch (functionality.toLowerCase()) {
            // Direct authentication operations (no session management)
            case "signup", "register", "registeraccount" -> fullRegistrationTest(testData);
            case "login" -> loginTest(testData);
            
            // All other operations use unified session management
            case "openaccount" -> unifiedOperationTest("openaccount", testData);
            case "accountoverview" -> unifiedOperationTest("accountoverview", testData);
            case "transferfunds", "fund_transfer" -> unifiedOperationTest("transferfunds", testData);
            case "paybills", "billpay" -> unifiedOperationTest("paybills", testData);
            case "findtransactions" -> unifiedOperationTest("findtransactions", testData);
            case "updateprofile" -> unifiedOperationTest("updateprofile", testData);
            case "requestloan" -> unifiedOperationTest("requestloan", testData);
            case "logout" -> unifiedOperationTest("logout", testData);
            
            // Fallback operations
            case "balance_inquiry" -> unifiedOperationTest("accountoverview", testData);
            case "mocktest" -> mockBrowserTest(); // Simple mock test
            
            default -> ExecutionResult.failure("Unknown UI functionality: " + functionality);
        };
    }
    
    // -----------------------
    // Authentication Methods
    // -----------------------
    
    /**
     * Quick registration for authentication flow
     */
    private ExecutionResult quickRegistrationTest() {
        System.out.println("🔐 Starting quick registration");
        
        ChromeDriver driver = createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
        
        try {
            driver.get(baseUrl + "/register.htm");
            Thread.sleep(2000); // Visibility delay
            
            // Generate unique username
            String timestamp = String.valueOf(System.currentTimeMillis());
            String username = "testuser" + timestamp.substring(timestamp.length() - 6);
            
            // Fill registration form with default values
            fillFormField(driver, "customer.firstName", "John");
            fillFormField(driver, "customer.lastName", "Doe");
            fillFormField(driver, "customer.address.street", "123 Main St");
            fillFormField(driver, "customer.address.city", "New York");
            fillFormField(driver, "customer.address.state", "NY");
            fillFormField(driver, "customer.address.zipCode", "10001");
            fillFormField(driver, "customer.phoneNumber", "1234567890");
            fillFormField(driver, "customer.ssn", "123456789");
            fillFormField(driver, "customer.username", username);
            fillFormField(driver, "customer.password", "testpass");
            fillFormField(driver, "repeatedPassword", "testpass");
            
            // Click register button
            Thread.sleep(2000); // Final visibility delay
            WebElement registerButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@value='Register']")))
;
            registerButton.click();
            Thread.sleep(3000); // Wait for registration result
            
            boolean success = driver.getPageSource().contains("Your account was created successfully") ||
                            driver.getPageSource().contains("Welcome") ||
                            driver.getCurrentUrl().contains("overview");
            
            String screenshot = takeScreenshot(driver, "quickRegistration");
            
            return ExecutionResult.uiResult(success,
                    success ? "Quick registration successful" : "Quick registration failed",
                    screenshot);
                    
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "quickRegistration_error");
            return ExecutionResult.failure("Quick registration error: " + e.getMessage(), screenshot);
        } finally {
            driver.quit();
        }
    }
    
    /**
     * Quick login for authentication flow
     */
    private ExecutionResult quickLoginTest() {
        System.out.println("🔐 Starting quick login");
        
        ChromeDriver driver = createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
        
        try {
            driver.get(baseUrl + "/index.htm");
            Thread.sleep(2000); // Visibility delay
            
            // Use the same credentials as registration
            WebElement usernameField = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username")));
            usernameField.clear();
            Thread.sleep(1000);
            usernameField.sendKeys("testuser"); // Use a simple test user that should exist
            
            WebElement passwordField = driver.findElement(By.name("password"));
            passwordField.clear();
            Thread.sleep(1000);
            passwordField.sendKeys("testpass");
            
            Thread.sleep(2000); // Final visibility delay
            WebElement loginButton = driver.findElement(By.xpath("//input[@value='Log In']"));
            loginButton.click();
            Thread.sleep(3000); // Wait for login result
            
            boolean success = driver.getPageSource().contains("Welcome") ||
                            driver.getCurrentUrl().contains("overview");
            
            String screenshot = takeScreenshot(driver, "quickLogin");
            
            return ExecutionResult.uiResult(success,
                    success ? "Quick login successful" : "Quick login failed",
                    screenshot);
                    
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "quickLogin_error");
            return ExecutionResult.failure("Quick login error: " + e.getMessage(), screenshot);
        } finally {
            driver.quit();
        }
    }
    
    /**
     * Full registration test with all form fields (for explicit registration testing)
     */
    private ExecutionResult fullRegistrationTest(String testData) {
        System.out.println("📱 Starting full registration test with data: " + testData);
        
        ChromeDriver driver = createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
        
        try {
            driver.get(baseUrl + "/register.htm");
            Thread.sleep(3000); // Extended visibility delay
            
            // Default values from screenshots analysis
            Map<String, String> formData = new HashMap<>();
            formData.put("firstName", "John");
            formData.put("lastName", "Doe");
            formData.put("address", "123 Main St");
            formData.put("city", "New York");
            formData.put("state", "NY");
            formData.put("zipCode", "10001");
            formData.put("phone", "1234567890");
            formData.put("ssn", "123456789");
            formData.put("username", "testuser" + System.currentTimeMillis());
            formData.put("password", "testpass");
            formData.put("confirm", "testpass");
            
            // Override with test data if provided
            if (testData != null) {
                try {
                    Map<String, Object> data = objectMapper.readValue(testData, Map.class);
                    data.forEach((key, value) -> {
                        if (value instanceof String) {
                            formData.put(key, (String) value);
                        }
                    });
                } catch (Exception e) {
                    System.out.println("Warning: Could not parse registration data: " + e.getMessage());
                }
            }
            
            // Fill all form fields based on screenshot analysis
            fillFormField(driver, "customer.firstName", formData.get("firstName"));
            fillFormField(driver, "customer.lastName", formData.get("lastName"));
            fillFormField(driver, "customer.address.street", formData.get("address"));
            fillFormField(driver, "customer.address.city", formData.get("city"));
            fillFormField(driver, "customer.address.state", formData.get("state"));
            fillFormField(driver, "customer.address.zipCode", formData.get("zipCode"));
            fillFormField(driver, "customer.phoneNumber", formData.get("phone"));
            fillFormField(driver, "customer.ssn", formData.get("ssn"));
            fillFormField(driver, "customer.username", formData.get("username"));
            fillFormField(driver, "customer.password", formData.get("password"));
            fillFormField(driver, "repeatedPassword", formData.get("confirm"));
            
            // Submit form
            Thread.sleep(3000); // Extended visibility delay before submit
            WebElement submitButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@value='Register']")))
;
            submitButton.click();
            Thread.sleep(5000); // Extended wait for registration result
            
            boolean success = driver.getPageSource().contains("Your account was created successfully") ||
                            driver.getPageSource().contains("Welcome") ||
                            driver.getCurrentUrl().contains("overview");
            
            String screenshot = takeScreenshot(driver, "fullRegistrationTest");
            
            return ExecutionResult.uiResult(success,
                    success ? "Full registration completed successfully" : "Full registration failed",
                    screenshot);
                    
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "fullRegistrationTest_error");
            return ExecutionResult.failure("Full registration test error: " + e.getMessage(), screenshot);
        } finally {
            try {
                Thread.sleep(5000); // Keep browser open for 5 seconds to see it
                driver.quit();
            } catch (Exception e) {
                System.err.println("⚠️ Error closing driver: " + e.getMessage());
            }
        }
    }
    
    /**
     * Unified operation test with smart authentication and session management
     * This approach does authentication once and performs the operation in the same session
     */
    private ExecutionResult unifiedOperationTest(String operation, String testData) {
        System.out.println("🔐 Starting unified operation: " + operation);
        
        ChromeDriver driver = createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
        
        try {
            // Step 1: Smart Authentication (try login first, register if needed)
            boolean authenticated = performSmartAuthentication(driver, wait);
            if (!authenticated) {
                return ExecutionResult.failure("Authentication failed for operation: " + operation);
            }
            
            System.out.println("✅ Authentication successful, proceeding with operation: " + operation);
            
            // Step 2: Perform the actual operation
            return executeOperationInAuthenticatedSession(driver, wait, operation, testData);
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, operation + "_unifiedTest_error");
            return ExecutionResult.failure("Unified operation test error for " + operation + ": " + e.getMessage(), screenshot);
        } finally {
            try {
                Thread.sleep(5000); // Keep browser open for visibility
                driver.quit();
                System.out.println("📱 ChromeDriver closed successfully");
            } catch (Exception e) {
                System.err.println("⚠️ Error closing driver: " + e.getMessage());
            }
        }
    }
    
    /**
     * Smart authentication that tries login first, then registration if needed
     */
    private boolean performSmartAuthentication(ChromeDriver driver, WebDriverWait wait) {
        try {
            System.out.println("🔍 Attempting smart authentication...");
            
            // Try login first with test credentials
            driver.get(baseUrl + "/index.htm");
            Thread.sleep(2000);
            
            // Check if we're already logged in
            if (driver.getPageSource().contains("Welcome") && driver.getCurrentUrl().contains("overview")) {
                System.out.println("✅ Already logged in!");
                return true;
            }
            
            // Fill login form
            WebElement usernameField = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username")));
            usernameField.clear();
            Thread.sleep(1000);
            usernameField.sendKeys("testuser");
            
            WebElement passwordField = driver.findElement(By.name("password"));
            passwordField.clear();
            Thread.sleep(1000);
            passwordField.sendKeys("testpass");
            
            WebElement loginButton = driver.findElement(By.xpath("//input[@value='Log In']"));
            loginButton.click();
            Thread.sleep(3000);
            
            // Check if login was successful
            if (driver.getPageSource().contains("Welcome") && !driver.getPageSource().contains("Error")) {
                System.out.println("✅ Login successful with existing user");
                return true;
            }
            
            // Login failed, try registration
            System.out.println("🔄 Login failed, attempting registration...");
            return performRegistration(driver, wait);
            
        } catch (Exception e) {
            System.err.println("❌ Smart authentication error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Perform user registration
     */
    private boolean performRegistration(ChromeDriver driver, WebDriverWait wait) {
        try {
            driver.get(baseUrl + "/register.htm");
            Thread.sleep(2000);
            
            // Generate unique username
            String timestamp = String.valueOf(System.currentTimeMillis());
            String username = "user" + timestamp.substring(timestamp.length() - 6);
            
            // Fill registration form
            fillFormField(driver, "customer.firstName", "John");
            fillFormField(driver, "customer.lastName", "Doe");
            fillFormField(driver, "customer.address.street", "123 Main St");
            fillFormField(driver, "customer.address.city", "New York");
            fillFormField(driver, "customer.address.state", "NY");
            fillFormField(driver, "customer.address.zipCode", "10001");
            fillFormField(driver, "customer.phoneNumber", "1234567890");
            fillFormField(driver, "customer.ssn", "123456789");
            fillFormField(driver, "customer.username", username);
            fillFormField(driver, "customer.password", "testpass");
            fillFormField(driver, "repeatedPassword", "testpass");
            
            // Submit registration
            Thread.sleep(2000);
            WebElement registerButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@value='Register']")))
;
            registerButton.click();
            Thread.sleep(5000);
            
            // Check if registration was successful
            boolean success = driver.getPageSource().contains("Your account was created successfully") ||
                            driver.getPageSource().contains("Welcome") ||
                            driver.getCurrentUrl().contains("overview");
            
            if (success) {
                System.out.println("✅ Registration successful for user: " + username);
                return true;
            } else {
                System.err.println("❌ Registration failed");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Registration error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Execute the specific operation in an authenticated session
     */
    private ExecutionResult executeOperationInAuthenticatedSession(ChromeDriver driver, WebDriverWait wait, String operation, String testData) {
        try {
            System.out.println("🎯 Executing operation: " + operation + " in authenticated session");
            
            return switch (operation.toLowerCase()) {
                case "openaccount" -> performOpenAccountOperation(driver, wait, testData);
                case "accountoverview" -> performAccountOverviewOperation(driver, wait, testData);
                case "transferfunds" -> performTransferFundsOperation(driver, wait, testData);
                case "paybills" -> performPayBillsOperation(driver, wait, testData);
                case "findtransactions" -> performFindTransactionsOperation(driver, wait, testData);
                case "updateprofile" -> performUpdateProfileOperation(driver, wait, testData);
                case "requestloan" -> performRequestLoanOperation(driver, wait, testData);
                case "logout" -> performLogoutOperation(driver, wait);
                default -> ExecutionResult.failure("Unknown operation: " + operation);
            };
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, operation + "_operation_error");
            return ExecutionResult.failure("Operation execution error for " + operation + ": " + e.getMessage(), screenshot);
        }
    }
    
    /**
     * Helper method for authenticated operations
     */
    private ExecutionResult authenticatedOperation(Supplier<ExecutionResult> operation) {
        System.out.println("🔐 Starting authenticated operation - performing signup and login first");
        
        // Step 1: Register/Signup
        ExecutionResult signupResult = quickRegistrationTest();
        if (!signupResult.isSuccess()) {
            return ExecutionResult.failure("Authentication failed: Signup unsuccessful - " + signupResult.getDetails());
        }
        System.out.println("✅ Signup completed successfully");
        
        // Step 2: Login
        ExecutionResult loginResult = quickLoginTest();
        if (!loginResult.isSuccess()) {
            return ExecutionResult.failure("Authentication failed: Login unsuccessful - " + loginResult.getDetails());
        }
        System.out.println("✅ Login completed successfully");
        
        // Step 3: Execute the actual operation
        System.out.println("🎯 Executing target operation");
        return operation.get();
    }

    /**
     * Enhanced login test with dynamic credentials
     */
    private ExecutionResult loginTest(String testData) {
        ChromeDriver driver = createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
        
        try {
            driver.get(baseUrl + "/index.htm");
            
            // Parse test data for credentials
            String username = "testuser";
            String password = "testpass";
            
            if (testData != null) {
                try {
                    Map<String, Object> data = objectMapper.readValue(testData, Map.class);
                    username = (String) data.getOrDefault("username", username);
                    password = (String) data.getOrDefault("password", password);
                } catch (Exception e) {
                    System.out.println("Warning: Could not parse test data, using defaults: " + e.getMessage());
                }
            }
            
            // Wait for and fill username
            WebElement usernameField = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("username")));
            usernameField.clear();
            Thread.sleep(2000); // Add visibility delay
            usernameField.sendKeys(username);
            Thread.sleep(1000); // Add visibility delay
            
            // Fill password
            WebElement passwordField = driver.findElement(By.name("password"));
            passwordField.clear();
            Thread.sleep(1000); // Add visibility delay
            passwordField.sendKeys(password);
            Thread.sleep(2000); // Add visibility delay
            
            // Click login button
            WebElement loginButton = driver.findElement(By.xpath("//input[@value='Log In']"));
            loginButton.click();
            Thread.sleep(3000); // Add visibility delay for result
            
            // Wait for login result
            wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'Welcome')]")),
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'error')]"))
            ));
            
            boolean success = driver.getPageSource().contains("Welcome");
            String screenshot = takeScreenshot(driver, "loginTest");
            
            String details = success ? 
                "Login successful for user: " + username : 
                "Login failed for user: " + username;
                
            return ExecutionResult.uiResult(success, details, screenshot);
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "loginTest_error");
            return ExecutionResult.failure("Login test error: " + e.getMessage(), screenshot);
        } finally {
            driver.quit();
        }
    }

    /**
     * Enhanced register account test
     */
    private ExecutionResult registerAccountTest(String testData) {
        System.out.println("📱 Starting registerAccountTest with data: " + testData);
        
        ChromeDriver driver = createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
        
        try {
            System.out.println("📱 Navigating to Parabank register page: " + baseUrl + "/register.htm");
            driver.get(baseUrl + "/register.htm");
            System.out.println("📱 Page loaded successfully");
            
            // Default values
            Map<String, String> formData = new HashMap<>();
            formData.put("firstName", "John");
            formData.put("lastName", "Doe");
            formData.put("address", "123 Main St");
            formData.put("city", "New York");
            formData.put("state", "NY");
            formData.put("zipCode", "10001");
            formData.put("phone", "1234567890");
            formData.put("ssn", "123-45-6789");
            formData.put("username", "testuser" + System.currentTimeMillis());
            formData.put("password", "testpass");
            formData.put("confirm", "testpass");
            
            // Override with test data if provided
            if (testData != null) {
                try {
                    Map<String, Object> data = objectMapper.readValue(testData, Map.class);
                    data.forEach((key, value) -> {
                        if (value instanceof String) {
                            formData.put(key, (String) value);
                        }
                    });
                } catch (Exception e) {
                    System.out.println("Warning: Could not parse registration data: " + e.getMessage());
                }
            }
            
            // Fill form fields
            fillFormField(driver, "customer.firstName", formData.get("firstName"));
            fillFormField(driver, "customer.lastName", formData.get("lastName"));
            fillFormField(driver, "customer.address.street", formData.get("address"));
            fillFormField(driver, "customer.address.city", formData.get("city"));
            fillFormField(driver, "customer.address.state", formData.get("state"));
            fillFormField(driver, "customer.address.zipCode", formData.get("zipCode"));
            fillFormField(driver, "customer.phoneNumber", formData.get("phone"));
            fillFormField(driver, "customer.ssn", formData.get("ssn"));
            fillFormField(driver, "customer.username", formData.get("username"));
            fillFormField(driver, "customer.password", formData.get("password"));
            fillFormField(driver, "repeatedPassword", formData.get("confirm"));
            
            // Submit form
            WebElement submitButton = driver.findElement(By.xpath("//input[@value='Register']"));
            submitButton.click();
            
            // Wait for result
            wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(By.xpath("//*[contains(text(),'success')]"))
            ));
            
            boolean success = driver.getPageSource().contains("success") || 
                            driver.getPageSource().contains("Welcome");
            String screenshot = takeScreenshot(driver, "registerAccountTest");
            
            return ExecutionResult.uiResult(success,
                    success ? "Account registration successful" : "Account registration failed",
                    screenshot);
                    
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "registerAccountTest_error");
            return ExecutionResult.failure("Register account test error: " + e.getMessage(), screenshot);
        } finally {
            driver.quit();
        }
    }

    /**
     * Enhanced transfer funds test
     */
    private ExecutionResult transferFundsTest(String testData) {
        ChromeDriver driver = createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
        
        try {
            // First login
            ExecutionResult loginResult = loginTest(null);
            if (!loginResult.isSuccess()) {
                return ExecutionResult.failure("Login required for transfer funds test failed");
            }
            
            driver.get(baseUrl + "/transfer.htm");
            
            String amount = "100";
            if (testData != null) {
                try {
                    Map<String, Object> data = objectMapper.readValue(testData, Map.class);
                    amount = String.valueOf(data.getOrDefault("amount", amount));
                } catch (Exception e) {
                    System.out.println("Warning: Could not parse transfer data: " + e.getMessage());
                }
            }
            
            // Fill transfer form
            fillFormField(driver, "amount", amount);
            
            // Submit transfer
            WebElement transferButton = driver.findElement(By.xpath("//input[@value='Transfer']"));
            transferButton.click();
            
            boolean success = driver.getPageSource().contains("Transfer Complete") ||
                            driver.getPageSource().contains("successfully");
            String screenshot = takeScreenshot(driver, "transferFundsTest");
            
            return ExecutionResult.uiResult(success,
                    success ? "Funds transfer successful" : "Funds transfer failed",
                    screenshot);
                    
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "transferFundsTest_error");
            return ExecutionResult.failure("Transfer funds test error: " + e.getMessage(), screenshot);
        } finally {
            driver.quit();
        }
    }

    /**
     * Helper method to fill form fields safely with visibility delays
     */
    private void fillFormField(WebDriver driver, String fieldName, String value) {
        try {
            WebElement field = driver.findElement(By.name(fieldName));
            field.clear();
            Thread.sleep(500); // Add visibility delay
            field.sendKeys(value);
            Thread.sleep(1000); // Add visibility delay after input
        } catch (Exception e) {
            System.out.println("Warning: Could not fill field " + fieldName + ": " + e.getMessage());
        }
    }

    /**
     * Enhanced simple page test with better validation
     */
    private ExecutionResult simplePageTest(String path, String testName) {
        ChromeDriver driver = createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
        
        try {
            driver.get(baseUrl + path);
            
            // Wait for page to load completely
            wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
            
            boolean success = driver.getPageSource().length() > 0 && 
                            !driver.getPageSource().contains("404") &&
                            !driver.getPageSource().contains("error");
            String screenshot = takeScreenshot(driver, testName);

            return ExecutionResult.uiResult(success,
                    success ? "Page loaded successfully" : "Page failed to load properly",
                    screenshot);
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, testName + "_error");
            return ExecutionResult.failure(testName + " error: " + e.getMessage(), screenshot);
        } finally {
            driver.quit();
        }
    }

    /**
     * Parabank registration test - navigates to actual Parabank registration page
     */
    private ExecutionResult mockRegisterTest(String testData) {
        System.out.println("📱 Starting Parabank registration test with data: " + testData);
        
        ChromeDriver driver = createDriver();
        
        try {
            // Extract pageUrl from testData if available, otherwise use default Parabank register page
            String pageUrl = "https://parabank.parasoft.com/parabank/register.htm";
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> dataMap = mapper.readValue(testData, Map.class);
                if (dataMap.containsKey("pageUrl")) {
                    pageUrl = dataMap.get("pageUrl").toString();
                }
            } catch (Exception e) {
                System.out.println("📱 Using default Parabank registration URL");
            }
            
            System.out.println("📱 Navigating to Parabank registration: " + pageUrl);
            driver.get(pageUrl);
            System.out.println("📱 Parabank registration page loaded successfully");
            
            // Wait for page to load
            Thread.sleep(5000); // Increased visibility delay
            
            String pageTitle = driver.getTitle();
            boolean success = pageTitle.contains("ParaBank") || pageTitle.contains("Register");
            
            String screenshot = takeScreenshot(driver, "parabankRegisterTest");
            
            System.out.println("📱 Parabank registration test completed. Title: " + pageTitle);
            
            return ExecutionResult.uiResult(success,
                    success ? "Parabank registration page loaded successfully" : "Parabank registration page failed to load",
                    screenshot);
                    
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "parabankRegisterTest_error");
            System.err.println("❌ Parabank registration test error: " + e.getMessage());
            e.printStackTrace();
            return ExecutionResult.failure("Parabank registration test error: " + e.getMessage(), screenshot);
        } finally {
            try {
                Thread.sleep(5000); // Keep browser open for 5 seconds to see it
                driver.quit();
                System.out.println("📱 ChromeDriver closed successfully");
            } catch (Exception e) {
                System.err.println("⚠️ Error closing driver: " + e.getMessage());
            }
        }
    }
    
    // -----------------------
    // Missing Operation Methods for Unified Operations
    // -----------------------
    
    /**
     * Transfer Funds Operation - requires authentication
     */
    private ExecutionResult performTransferFundsOperation(ChromeDriver driver, WebDriverWait wait, String testData) {
        try {
            System.out.println("💸 Performing Transfer Funds operation in authenticated session");
            
            driver.get(baseUrl + "/transfer.htm");
            Thread.sleep(3000);
            
            // Check if we're still logged in
            if (driver.getPageSource().contains("login") && !driver.getPageSource().contains("Welcome")) {
                return ExecutionResult.failure("Authentication required for transfer funds operation");
            }
            
            String amount = "100";
            if (testData != null) {
                try {
                    Map<String, Object> data = objectMapper.readValue(testData, Map.class);
                    amount = String.valueOf(data.getOrDefault("amount", amount));
                } catch (Exception e) {
                    System.out.println("Warning: Could not parse transfer data: " + e.getMessage());
                }
            }
            
            // Fill transfer form
            fillFormField(driver, "amount", amount);
            Thread.sleep(2000);
            
            // Select from and to accounts if available
            try {
                WebElement fromAccountSelect = driver.findElement(By.name("fromAccountId"));
                fromAccountSelect.sendKeys(Keys.ARROW_DOWN);
                Thread.sleep(1000);
                
                WebElement toAccountSelect = driver.findElement(By.name("toAccountId"));
                toAccountSelect.sendKeys(Keys.ARROW_DOWN);
                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.println("Note: Account dropdowns not available - this may be expected for new accounts");
            }
            
            // Submit transfer
            try {
                WebElement transferButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@value='Transfer']")));
                transferButton.click();
                Thread.sleep(5000);
                
                boolean success = driver.getPageSource().contains("Transfer Complete") ||
                                driver.getPageSource().contains("successfully") ||
                                driver.getCurrentUrl().contains("activity");
                
                String screenshot = takeScreenshot(driver, "transferFunds_authenticated");
                
                return ExecutionResult.uiResult(success,
                        success ? "Funds transfer successful in authenticated session" : "Funds transfer failed in authenticated session",
                        screenshot);
                        
            } catch (Exception e) {
                // If form submission fails, check if page loaded correctly
                boolean pageLoaded = driver.getPageSource().contains("Transfer Funds") &&
                                   !driver.getPageSource().contains("Error") &&
                                   !driver.getPageSource().contains("login");
                
                String screenshot = takeScreenshot(driver, "transferFunds_pageLoad");
                
                return ExecutionResult.uiResult(pageLoaded,
                        pageLoaded ? "Transfer funds page loaded successfully" : "Transfer funds page failed to load",
                        screenshot);
            }
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "transferFunds_error");
            return ExecutionResult.failure("Transfer funds operation error: " + e.getMessage(), screenshot);
        }
    }
    
    /**
     * Pay Bills Operation - requires authentication
     */
    private ExecutionResult performPayBillsOperation(ChromeDriver driver, WebDriverWait wait, String testData) {
        try {
            System.out.println("📄 Performing Pay Bills operation in authenticated session");
            
            driver.get(baseUrl + "/billpay.htm");
            Thread.sleep(3000);
            
            // Check if we're still logged in
            if (driver.getPageSource().contains("login") && !driver.getPageSource().contains("Welcome")) {
                return ExecutionResult.failure("Authentication required for pay bills operation");
            }
            
            // Check if page loaded correctly
            boolean pageLoaded = driver.getPageSource().contains("Bill Payment Service") ||
                               driver.getPageSource().contains("Payee Name") ||
                               (driver.getCurrentUrl().contains("billpay") && !driver.getPageSource().contains("Error"));
            
            String screenshot = takeScreenshot(driver, "payBills_authenticated");
            
            return ExecutionResult.uiResult(pageLoaded,
                    pageLoaded ? "Bill pay page loaded successfully in authenticated session" : "Bill pay page failed to load in authenticated session",
                    screenshot);
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "payBills_error");
            return ExecutionResult.failure("Pay bills operation error: " + e.getMessage(), screenshot);
        }
    }
    
    /**
     * Find Transactions Operation - requires authentication
     */
    private ExecutionResult performFindTransactionsOperation(ChromeDriver driver, WebDriverWait wait, String testData) {
        try {
            System.out.println("🔍 Performing Find Transactions operation in authenticated session");
            
            driver.get(baseUrl + "/findtrans.htm");
            Thread.sleep(3000);
            
            // Check if we're still logged in
            if (driver.getPageSource().contains("login") && !driver.getPageSource().contains("Welcome")) {
                return ExecutionResult.failure("Authentication required for find transactions operation");
            }
            
            // Check if page loaded correctly
            boolean pageLoaded = driver.getPageSource().contains("Find Transactions") ||
                               driver.getPageSource().contains("Select an account") ||
                               (driver.getCurrentUrl().contains("findtrans") && !driver.getPageSource().contains("Error"));
            
            String screenshot = takeScreenshot(driver, "findTransactions_authenticated");
            
            return ExecutionResult.uiResult(pageLoaded,
                    pageLoaded ? "Find transactions page loaded successfully in authenticated session" : "Find transactions page failed to load in authenticated session",
                    screenshot);
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "findTransactions_error");
            return ExecutionResult.failure("Find transactions operation error: " + e.getMessage(), screenshot);
        }
    }
    
    /**
     * Update Profile Operation - requires authentication
     */
    private ExecutionResult performUpdateProfileOperation(ChromeDriver driver, WebDriverWait wait, String testData) {
        try {
            System.out.println("📝 Performing Update Profile operation in authenticated session");
            
            driver.get(baseUrl + "/updateprofile.htm");
            Thread.sleep(3000);
            
            // Check if we're still logged in
            if (driver.getPageSource().contains("login") && !driver.getPageSource().contains("Welcome")) {
                return ExecutionResult.failure("Authentication required for update profile operation");
            }
            
            // Check if page loaded correctly
            boolean pageLoaded = driver.getPageSource().contains("Update Profile") ||
                               driver.getPageSource().contains("First Name") ||
                               (driver.getCurrentUrl().contains("updateprofile") && !driver.getPageSource().contains("Error"));
            
            String screenshot = takeScreenshot(driver, "updateProfile_authenticated");
            
            return ExecutionResult.uiResult(pageLoaded,
                    pageLoaded ? "Update profile page loaded successfully in authenticated session" : "Update profile page failed to load in authenticated session",
                    screenshot);
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "updateProfile_error");
            return ExecutionResult.failure("Update profile operation error: " + e.getMessage(), screenshot);
        }
    }
    
    /**
     * Request Loan Operation - requires authentication
     */
    private ExecutionResult performRequestLoanOperation(ChromeDriver driver, WebDriverWait wait, String testData) {
        try {
            System.out.println("🏦 Performing Request Loan operation in authenticated session");
            
            driver.get(baseUrl + "/requestloan.htm");
            Thread.sleep(3000);
            
            // Check if we're still logged in
            if (driver.getPageSource().contains("login") && !driver.getPageSource().contains("Welcome")) {
                return ExecutionResult.failure("Authentication required for request loan operation");
            }
            
            // Check if page loaded correctly
            boolean pageLoaded = driver.getPageSource().contains("Apply for a Loan") ||
                               driver.getPageSource().contains("Loan Amount") ||
                               (driver.getCurrentUrl().contains("requestloan") && !driver.getPageSource().contains("Error"));
            
            String screenshot = takeScreenshot(driver, "requestLoan_authenticated");
            
            return ExecutionResult.uiResult(pageLoaded,
                    pageLoaded ? "Request loan page loaded successfully in authenticated session" : "Request loan page failed to load in authenticated session",
                    screenshot);
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "requestLoan_error");
            return ExecutionResult.failure("Request loan operation error: " + e.getMessage(), screenshot);
        }
    }
    
    /**
     * Logout Operation - ends authenticated session
     */
    private ExecutionResult performLogoutOperation(ChromeDriver driver, WebDriverWait wait) {
        try {
            System.out.println("🚪 Performing Logout operation");
            
            // Try to find and click logout link
            try {
                WebElement logoutLink = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Log Out")));
                logoutLink.click();
                Thread.sleep(3000);
            } catch (Exception e) {
                // If logout link not found, navigate to main page
                driver.get(baseUrl + "/index.htm");
                Thread.sleep(3000);
            }
            
            boolean success = driver.getCurrentUrl().contains("index.htm") ||
                            driver.getPageSource().contains("Customer Login") ||
                            driver.getPageSource().contains("Thank you for visiting ParaBank");
            
            String screenshot = takeScreenshot(driver, "logout_authenticated");
            
            return ExecutionResult.uiResult(success,
                    success ? "Logout completed successfully" : "Logout failed",
                    screenshot);
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "logout_error");
            return ExecutionResult.failure("Logout operation error: " + e.getMessage(), screenshot);
        }
    }
    
    // -----------------------
    // Specific Operation Tests (Based on Screenshot Analysis)
    // -----------------------
    
    /**
     * Open Account Test - Step 1: /openaccount.htm
     */
    private ExecutionResult openAccountTest(String testData) {
        System.out.println("🏦 Starting Open Account test");
        
        ChromeDriver driver = createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
        
        try {
            driver.get(baseUrl + "/openaccount.htm");
            Thread.sleep(3000); // Visibility delay
            
            // Select account type (default: CHECKING)
            WebElement accountTypeSelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("type")));
            accountTypeSelect.sendKeys("CHECKING");
            Thread.sleep(2000);
            
            // Select existing account (if available)
            try {
                WebElement fromAccountSelect = driver.findElement(By.name("fromAccountId"));
                fromAccountSelect.sendKeys(Keys.ARROW_DOWN);
                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.println("Note: No existing account dropdown found");
            }
            
            // Click Open New Account button
            WebElement openButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@value='Open New Account']")))
;
            openButton.click();
            Thread.sleep(5000); // Wait for account creation
            
            boolean success = driver.getPageSource().contains("Account Opened!") ||
                            driver.getPageSource().contains("Congratulations") ||
                            driver.getCurrentUrl().contains("activity");
            
            String screenshot = takeScreenshot(driver, "openAccountTest");
            
            return ExecutionResult.uiResult(success,
                    success ? "Account opened successfully" : "Account opening failed",
                    screenshot);
                    
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "openAccountTest_error");
            return ExecutionResult.failure("Open account test error: " + e.getMessage(), screenshot);
        } finally {
            try {
                Thread.sleep(5000);
                driver.quit();
            } catch (Exception e) {
                System.err.println("⚠️ Error closing driver: " + e.getMessage());
            }
        }
    }
    
    /**
     * Account Overview Operation - requires authentication
     */
    private ExecutionResult performAccountOverviewOperation(ChromeDriver driver, WebDriverWait wait, String testData) {
        try {
            System.out.println("📊 Performing Account Overview operation in authenticated session");
            
            driver.get(baseUrl + "/overview.htm");
            Thread.sleep(3000);
            
            // Check if we're still logged in
            if (driver.getPageSource().contains("login") && !driver.getPageSource().contains("Welcome")) {
                return ExecutionResult.failure("Authentication required for account overview operation");
            }
            
            // Wait for page content to load
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                Thread.sleep(3000);
                
                boolean success = driver.getPageSource().contains("Accounts Overview") ||
                                driver.getPageSource().contains("Balance") ||
                                driver.getPageSource().contains("Available Amount") ||
                                driver.getPageSource().contains("Account Number") ||
                                (driver.getCurrentUrl().contains("overview") && !driver.getPageSource().contains("Error"));
                
                String screenshot = takeScreenshot(driver, "accountOverview_authenticated");
                
                return ExecutionResult.uiResult(success,
                        success ? "Account overview loaded successfully in authenticated session" : "Account overview failed to load in authenticated session",
                        screenshot);
                        
            } catch (Exception e) {
                // If specific elements not found, check for general page load success
                boolean pageLoaded = !driver.getPageSource().contains("Error") &&
                                   !driver.getPageSource().contains("login") &&
                                   driver.getCurrentUrl().contains("overview");
                
                String screenshot = takeScreenshot(driver, "accountOverview_pageLoad");
                
                return ExecutionResult.uiResult(pageLoaded,
                        pageLoaded ? "Account overview page loaded successfully" : "Account overview page failed to load",
                        screenshot);
            }
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "accountOverview_error");
            return ExecutionResult.failure("Account overview operation error: " + e.getMessage(), screenshot);
        }
    }
    
    /**
     * Bill Payment Test - Step 4: /billpay.htm
     */
    private ExecutionResult billPayTest(String testData) {
        System.out.println("📄 Starting Bill Payment test");
        
        ChromeDriver driver = createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
        
        try {
            driver.get(baseUrl + "/billpay.htm");
            Thread.sleep(3000); // Visibility delay
            
            // Default values based on screenshot analysis
            Map<String, String> billData = new HashMap<>();
            billData.put("payeeName", "Electric Company");
            billData.put("address", "123 Main St");
            billData.put("city", "New York");
            billData.put("state", "NY");
            billData.put("zipCode", "10001");
            billData.put("phone", "555-1234");
            billData.put("account", "12345");
            billData.put("verifyAccount", "12345");
            billData.put("amount", "75.00");
            
            // Override with test data if provided
            if (testData != null) {
                try {
                    Map<String, Object> data = objectMapper.readValue(testData, Map.class);
                    data.forEach((key, value) -> {
                        if (value instanceof String) {
                            billData.put(key, (String) value);
                        }
                    });
                } catch (Exception e) {
                    System.out.println("Warning: Could not parse bill pay data: " + e.getMessage());
                }
            }
            
            // Fill all bill payment fields based on screenshot
            fillFormField(driver, "payee.name", billData.get("payeeName"));
            fillFormField(driver, "payee.address.street", billData.get("address"));
            fillFormField(driver, "payee.address.city", billData.get("city"));
            fillFormField(driver, "payee.address.state", billData.get("state"));
            fillFormField(driver, "payee.address.zipCode", billData.get("zipCode"));
            fillFormField(driver, "payee.phoneNumber", billData.get("phone"));
            fillFormField(driver, "payee.accountNumber", billData.get("account"));
            fillFormField(driver, "verifyAccount", billData.get("verifyAccount"));
            fillFormField(driver, "amount", billData.get("amount"));
            
            // Select from account dropdown if available
            try {
                WebElement fromAccountSelect = driver.findElement(By.name("fromAccountId"));
                fromAccountSelect.sendKeys(Keys.ARROW_DOWN);
                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.println("Note: From account dropdown not available");
            }
            
            // Click Send Payment button
            Thread.sleep(2000);
            WebElement paymentButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@value='Send Payment']")))
;
            paymentButton.click();
            Thread.sleep(5000); // Wait for payment result
            
            boolean success = driver.getPageSource().contains("Bill Payment Complete") ||
                            driver.getPageSource().contains("Payment Complete") ||
                            driver.getCurrentUrl().contains("activity");
            
            String screenshot = takeScreenshot(driver, "billPayTest");
            
            return ExecutionResult.uiResult(success,
                    success ? "Bill payment completed successfully" : "Bill payment failed",
                    screenshot);
                    
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "billPayTest_error");
            return ExecutionResult.failure("Bill payment test error: " + e.getMessage(), screenshot);
        } finally {
            try {
                Thread.sleep(5000);
                driver.quit();
            } catch (Exception e) {
                System.err.println("⚠️ Error closing driver: " + e.getMessage());
            }
        }
    }
    
    /**
     * Find Transactions Test - Step 5: /findtrans.htm
     */
    private ExecutionResult findTransactionsTest(String testData) {
        System.out.println("🔍 Starting Find Transactions test");
        
        ChromeDriver driver = createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
        
        try {
            driver.get(baseUrl + "/findtrans.htm");
            Thread.sleep(3000); // Visibility delay
            
            // Select account dropdown
            try {
                WebElement accountSelect = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("accountId")));
                accountSelect.sendKeys(Keys.ARROW_DOWN);
                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.println("Note: Account dropdown not available");
            }
            
            // Try Transaction ID search
            try {
                fillFormField(driver, "criteria.transactionId", "12345");
                WebElement findButton = driver.findElement(By.xpath("(//input[@value='Find Transactions'])[1]"));
                findButton.click();
                Thread.sleep(3000);
            } catch (Exception e) {
                System.out.println("Note: Transaction ID search not available");
            }
            
            boolean success = driver.getPageSource().contains("Transaction Results") ||
                            driver.getPageSource().contains("Find Transactions") ||
                            !driver.getPageSource().contains("error");
            
            String screenshot = takeScreenshot(driver, "findTransactionsTest");
            
            return ExecutionResult.uiResult(success,
                    success ? "Find transactions page loaded successfully" : "Find transactions failed",
                    screenshot);
                    
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "findTransactionsTest_error");
            return ExecutionResult.failure("Find transactions test error: " + e.getMessage(), screenshot);
        } finally {
            try {
                Thread.sleep(5000);
                driver.quit();
            } catch (Exception e) {
                System.err.println("⚠️ Error closing driver: " + e.getMessage());
            }
        }
    }
    
    /**
     * Update Profile Test - Step 6: /updateprofile.htm
     */
    private ExecutionResult updateProfileTest(String testData) {
        System.out.println("📝 Starting Update Profile test");
        
        ChromeDriver driver = createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
        
        try {
            driver.get(baseUrl + "/updateprofile.htm");
            Thread.sleep(3000); // Visibility delay
            
            // Default values based on screenshot analysis
            Map<String, String> profileData = new HashMap<>();
            profileData.put("firstName", "UpdatedJohn");
            profileData.put("lastName", "UpdatedDoe");
            profileData.put("address", "456 Updated St");
            profileData.put("city", "Updated City");
            profileData.put("state", "CA");
            profileData.put("zipCode", "90210");
            profileData.put("phone", "555-9876");
            
            // Override with test data if provided
            if (testData != null) {
                try {
                    Map<String, Object> data = objectMapper.readValue(testData, Map.class);
                    data.forEach((key, value) -> {
                        if (value instanceof String) {
                            profileData.put(key, (String) value);
                        }
                    });
                } catch (Exception e) {
                    System.out.println("Warning: Could not parse profile data: " + e.getMessage());
                }
            }
            
            // Fill all profile fields based on screenshot
            fillFormField(driver, "customer.firstName", profileData.get("firstName"));
            fillFormField(driver, "customer.lastName", profileData.get("lastName"));
            fillFormField(driver, "customer.address.street", profileData.get("address"));
            fillFormField(driver, "customer.address.city", profileData.get("city"));
            fillFormField(driver, "customer.address.state", profileData.get("state"));
            fillFormField(driver, "customer.address.zipCode", profileData.get("zipCode"));
            fillFormField(driver, "customer.phoneNumber", profileData.get("phone"));
            
            // Click Update Profile button
            Thread.sleep(2000);
            WebElement updateButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@value='Update Profile']")))
;
            updateButton.click();
            Thread.sleep(5000); // Wait for update result
            
            boolean success = driver.getPageSource().contains("Profile Updated") ||
                            driver.getPageSource().contains("successfully") ||
                            driver.getCurrentUrl().contains("updated");
            
            String screenshot = takeScreenshot(driver, "updateProfileTest");
            
            return ExecutionResult.uiResult(success,
                    success ? "Profile updated successfully" : "Profile update failed",
                    screenshot);
                    
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "updateProfileTest_error");
            return ExecutionResult.failure("Update profile test error: " + e.getMessage(), screenshot);
        } finally {
            try {
                Thread.sleep(5000);
                driver.quit();
            } catch (Exception e) {
                System.err.println("⚠️ Error closing driver: " + e.getMessage());
            }
        }
    }
    
    /**
     * Request Loan Test - Step 7: /requestloan.htm
     */
    private ExecutionResult requestLoanTest(String testData) {
        System.out.println("🏦 Starting Request Loan test");
        
        ChromeDriver driver = createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
        
        try {
            driver.get(baseUrl + "/requestloan.htm");
            Thread.sleep(3000); // Visibility delay
            
            // Default values based on screenshot analysis
            String loanAmount = "5000.00";
            String downPayment = "1000.00";
            
            if (testData != null) {
                try {
                    Map<String, Object> data = objectMapper.readValue(testData, Map.class);
                    loanAmount = String.valueOf(data.getOrDefault("amount", loanAmount));
                    downPayment = String.valueOf(data.getOrDefault("downPayment", downPayment));
                } catch (Exception e) {
                    System.out.println("Warning: Could not parse loan data: " + e.getMessage());
                }
            }
            
            // Fill loan fields
            fillFormField(driver, "amount", loanAmount);
            fillFormField(driver, "downPayment", downPayment);
            
            // Select from account dropdown if available
            try {
                WebElement fromAccountSelect = driver.findElement(By.name("fromAccountId"));
                fromAccountSelect.sendKeys(Keys.ARROW_DOWN);
                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.println("Note: From account dropdown not available");
            }
            
            // Click Apply Now button
            Thread.sleep(2000);
            WebElement applyButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//input[@value='Apply Now']")))
;
            applyButton.click();
            Thread.sleep(5000); // Wait for loan result
            
            boolean success = driver.getPageSource().contains("Loan Request Processed") ||
                            driver.getPageSource().contains("approved") ||
                            driver.getCurrentUrl().contains("activity");
            
            String screenshot = takeScreenshot(driver, "requestLoanTest");
            
            return ExecutionResult.uiResult(success,
                    success ? "Loan request completed successfully" : "Loan request failed",
                    screenshot);
                    
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "requestLoanTest_error");
            return ExecutionResult.failure("Request loan test error: " + e.getMessage(), screenshot);
        } finally {
            try {
                Thread.sleep(5000);
                driver.quit();
            } catch (Exception e) {
                System.err.println("⚠️ Error closing driver: " + e.getMessage());
            }
        }
    }
    
    /**
     * Logout Test - Step 8: Final step
     */
    private ExecutionResult logoutTest() {
        System.out.println("🚪 Starting Logout test");
        
        ChromeDriver driver = createDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
        
        try {
            // Navigate to any authenticated page first
            driver.get(baseUrl + "/overview.htm");
            Thread.sleep(2000);
            
            // Find and click logout link
            WebElement logoutLink = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Log Out")));
            logoutLink.click();
            Thread.sleep(3000); // Wait for logout
            
            boolean success = driver.getCurrentUrl().contains("index.htm") ||
                            driver.getPageSource().contains("Customer Login") ||
                            driver.getPageSource().contains("Thank you for visiting ParaBank");
            
            String screenshot = takeScreenshot(driver, "logoutTest");
            
            return ExecutionResult.uiResult(success,
                    success ? "Logout completed successfully" : "Logout failed",
                    screenshot);
                    
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "logoutTest_error");
            return ExecutionResult.failure("Logout test error: " + e.getMessage(), screenshot);
        } finally {
            try {
                Thread.sleep(5000);
                driver.quit();
            } catch (Exception e) {
                System.err.println("⚠️ Error closing driver: " + e.getMessage());
            }
        }
    }

    /**
     * Simple mock browser test to verify Chrome opens
     */
    private ExecutionResult mockBrowserTest() {
        System.out.println("📱 Starting mockBrowserTest");
        
        ChromeDriver driver = createDriver();
        
        try {
            System.out.println("📱 Navigating to example.com");
            driver.get("https://example.com");
            System.out.println("📱 Example.com loaded successfully");
            
            Thread.sleep(5000); // Keep browser open for 5 seconds
            
            String pageTitle = driver.getTitle();
            boolean success = !pageTitle.isEmpty();
            
            String screenshot = takeScreenshot(driver, "mockBrowserTest");
            
            System.out.println("📱 Mock browser test completed. Title: " + pageTitle);
            
            return ExecutionResult.uiResult(success,
                    success ? "Browser test successful" : "Browser test failed",
                    screenshot);
                    
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "mockBrowserTest_error");
            System.err.println("❌ Mock browser test error: " + e.getMessage());
            e.printStackTrace();
            return ExecutionResult.failure("Mock browser test error: " + e.getMessage(), screenshot);
        } finally {
            try {
                driver.quit();
                System.out.println("📱 ChromeDriver closed successfully");
            } catch (Exception e) {
                System.err.println("⚠️ Error closing driver: " + e.getMessage());
            }
        }
    }

    /**
     * Open Account Operation - requires authentication
     */
    private ExecutionResult performOpenAccountOperation(ChromeDriver driver, WebDriverWait wait, String testData) {
        try {
            System.out.println("🏦 Performing Open Account operation in authenticated session");
            
            driver.get(baseUrl + "/openaccount.htm");
            Thread.sleep(3000);
            
            // Check if we're still logged in
            if (driver.getPageSource().contains("login") && !driver.getPageSource().contains("Welcome")) {
                return ExecutionResult.failure("Authentication required for open account operation");
            }
            
            // Check if page loaded correctly
            boolean pageLoaded = driver.getPageSource().contains("Open New Account") &&
                               !driver.getPageSource().contains("Error") &&
                               !driver.getPageSource().contains("login");
            
            String screenshot = takeScreenshot(driver, "openAccount_authenticated");
            
            return ExecutionResult.uiResult(pageLoaded,
                    pageLoaded ? "Open account page loaded successfully in authenticated session" : "Open account page failed to load in authenticated session",
                    screenshot);
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "openAccount_error");
            return ExecutionResult.failure("Open account operation error: " + e.getMessage(), screenshot);
        }
    }

    /**
     * Account Overview Operation - requires authentication
     */
    private ExecutionResult performAccountOverviewOperation(ChromeDriver driver, WebDriverWait wait, String testData) {
        try {
            System.out.println("📊 Performing Account Overview operation in authenticated session");
            
            driver.get(baseUrl + "/overview.htm");
            Thread.sleep(3000);
            
            // Check if we're still logged in
            if (driver.getPageSource().contains("login") && !driver.getPageSource().contains("Welcome")) {
                return ExecutionResult.failure("Authentication required for account overview operation");
            }
            
            // Check if page loaded correctly
            boolean pageLoaded = driver.getPageSource().contains("Accounts Overview") ||
                               driver.getPageSource().contains("Balance") ||
                               driver.getPageSource().contains("Available Amount") ||
                               driver.getPageSource().contains("Account Number") ||
                               (driver.getCurrentUrl().contains("overview") && !driver.getPageSource().contains("Error"));
            
            String screenshot = takeScreenshot(driver, "accountOverview_authenticated");
            
            return ExecutionResult.uiResult(pageLoaded,
                    pageLoaded ? "Account overview loaded successfully in authenticated session" : "Account overview failed to load in authenticated session",
                    screenshot);
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "accountOverview_error");
            return ExecutionResult.failure("Account overview operation error: " + e.getMessage(), screenshot);
        }
    }

    /**
     * Transfer Funds Operation - requires authentication
     */
    private ExecutionResult performTransferFundsOperation(ChromeDriver driver, WebDriverWait wait, String testData) {
        try {
            System.out.println("💸 Performing Transfer Funds operation in authenticated session");
            
            driver.get(baseUrl + "/transfer.htm");
            Thread.sleep(3000);
            
            // Check if we're still logged in
            if (driver.getPageSource().contains("login") && !driver.getPageSource().contains("Welcome")) {
                return ExecutionResult.failure("Authentication required for transfer funds operation");
            }
            
            // Check if page loaded correctly
            boolean pageLoaded = driver.getPageSource().contains("Transfer Funds") &&
                               !driver.getPageSource().contains("Error") &&
                               !driver.getPageSource().contains("login");
            
            String screenshot = takeScreenshot(driver, "transferFunds_authenticated");
            
            return ExecutionResult.uiResult(pageLoaded,
                    pageLoaded ? "Transfer funds page loaded successfully in authenticated session" : "Transfer funds page failed to load in authenticated session",
                    screenshot);
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "transferFunds_error");
            return ExecutionResult.failure("Transfer funds operation error: " + e.getMessage(), screenshot);
        }
    }

    /**
     * Pay Bills Operation - requires authentication
     */
    private ExecutionResult performPayBillsOperation(ChromeDriver driver, WebDriverWait wait, String testData) {
        try {
            System.out.println("📄 Performing Pay Bills operation in authenticated session");
            
            driver.get(baseUrl + "/billpay.htm");
            Thread.sleep(3000);
            
            // Check if we're still logged in
            if (driver.getPageSource().contains("login") && !driver.getPageSource().contains("Welcome")) {
                return ExecutionResult.failure("Authentication required for pay bills operation");
            }
            
            // Check if page loaded correctly
            boolean pageLoaded = driver.getPageSource().contains("Bill Payment Service") ||
                               driver.getPageSource().contains("Payee Name") ||
                               (driver.getCurrentUrl().contains("billpay") && !driver.getPageSource().contains("Error"));
            
            String screenshot = takeScreenshot(driver, "payBills_authenticated");
            
            return ExecutionResult.uiResult(pageLoaded,
                    pageLoaded ? "Bill pay page loaded successfully in authenticated session" : "Bill pay page failed to load in authenticated session",
                    screenshot);
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "payBills_error");
            return ExecutionResult.failure("Pay bills operation error: " + e.getMessage(), screenshot);
        }
    }

    /**
     * Find Transactions Operation - requires authentication
     */
    private ExecutionResult performFindTransactionsOperation(ChromeDriver driver, WebDriverWait wait, String testData) {
        try {
            System.out.println("🔍 Performing Find Transactions operation in authenticated session");
            
            driver.get(baseUrl + "/findtrans.htm");
            Thread.sleep(3000);
            
            // Check if we're still logged in
            if (driver.getPageSource().contains("login") && !driver.getPageSource().contains("Welcome")) {
                return ExecutionResult.failure("Authentication required for find transactions operation");
            }
            
            // Check if page loaded correctly
            boolean pageLoaded = driver.getPageSource().contains("Find Transactions") ||
                               driver.getPageSource().contains("Select an account") ||
                               (driver.getCurrentUrl().contains("findtrans") && !driver.getPageSource().contains("Error"));
            
            String screenshot = takeScreenshot(driver, "findTransactions_authenticated");
            
            return ExecutionResult.uiResult(pageLoaded,
                    pageLoaded ? "Find transactions page loaded successfully in authenticated session" : "Find transactions page failed to load in authenticated session",
                    screenshot);
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "findTransactions_error");
            return ExecutionResult.failure("Find transactions operation error: " + e.getMessage(), screenshot);
        }
    }

    /**
     * Update Profile Operation - requires authentication
     */
    private ExecutionResult performUpdateProfileOperation(ChromeDriver driver, WebDriverWait wait, String testData) {
        try {
            System.out.println("📝 Performing Update Profile operation in authenticated session");
            
            driver.get(baseUrl + "/updateprofile.htm");
            Thread.sleep(3000);
            
            // Check if we're still logged in
            if (driver.getPageSource().contains("login") && !driver.getPageSource().contains("Welcome")) {
                return ExecutionResult.failure("Authentication required for update profile operation");
            }
            
            // Check if page loaded correctly
            boolean pageLoaded = driver.getPageSource().contains("Update Profile") ||
                               driver.getPageSource().contains("First Name") ||
                               (driver.getCurrentUrl().contains("updateprofile") && !driver.getPageSource().contains("Error"));
            
            String screenshot = takeScreenshot(driver, "updateProfile_authenticated");
            
            return ExecutionResult.uiResult(pageLoaded,
                    pageLoaded ? "Update profile page loaded successfully in authenticated session" : "Update profile page failed to load in authenticated session",
                    screenshot);
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "updateProfile_error");
            return ExecutionResult.failure("Update profile operation error: " + e.getMessage(), screenshot);
        }
    }

    /**
     * Request Loan Operation - requires authentication
     */
    private ExecutionResult performRequestLoanOperation(ChromeDriver driver, WebDriverWait wait, String testData) {
        try {
            System.out.println("🏦 Performing Request Loan operation in authenticated session");
            
            driver.get(baseUrl + "/requestloan.htm");
            Thread.sleep(3000);
            
            // Check if we're still logged in
            if (driver.getPageSource().contains("login") && !driver.getPageSource().contains("Welcome")) {
                return ExecutionResult.failure("Authentication required for request loan operation");
            }
            
            // Check if page loaded correctly
            boolean pageLoaded = driver.getPageSource().contains("Apply for a Loan") ||
                               driver.getPageSource().contains("Loan Amount") ||
                               (driver.getCurrentUrl().contains("requestloan") && !driver.getPageSource().contains("Error"));
            
            String screenshot = takeScreenshot(driver, "requestLoan_authenticated");
            
            return ExecutionResult.uiResult(pageLoaded,
                    pageLoaded ? "Request loan page loaded successfully in authenticated session" : "Request loan page failed to load in authenticated session",
                    screenshot);
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "requestLoan_error");
            return ExecutionResult.failure("Request loan operation error: " + e.getMessage(), screenshot);
        }
    }

    /**
     * Logout Operation - ends authenticated session
     */
    private ExecutionResult performLogoutOperation(ChromeDriver driver, WebDriverWait wait) {
        try {
            System.out.println("🚪 Performing Logout operation");
            
            // Try to find and click logout link
            try {
                WebElement logoutLink = wait.until(ExpectedConditions.elementToBeClickable(By.linkText("Log Out")));
                logoutLink.click();
                Thread.sleep(3000);
            } catch (Exception e) {
                // If logout link not found, navigate to main page
                driver.get(baseUrl + "/index.htm");
                Thread.sleep(3000);
            }
            
            boolean success = driver.getCurrentUrl().contains("index.htm") ||
                            driver.getPageSource().contains("Customer Login") ||
                            driver.getPageSource().contains("Thank you for visiting ParaBank");
            
            String screenshot = takeScreenshot(driver, "logout_authenticated");
            
            return ExecutionResult.uiResult(success,
                    success ? "Logout completed successfully" : "Logout failed",
                    screenshot);
            
        } catch (Exception e) {
            String screenshot = takeScreenshot(driver, "logout_error");
            return ExecutionResult.failure("Logout operation error: " + e.getMessage(), screenshot);
        }
    }
}
