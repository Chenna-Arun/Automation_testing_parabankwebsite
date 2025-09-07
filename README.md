# Complete UI Testing Suite +  API Tests

This Postman collection provides a comprehensive testing solution for the ParaBank application, including both UI and API tests.

## Project Overview

This collection includes:
- **11 UI Tests** covering the complete ParaBank user workflow (Registration → Login → Operations → Logout)
- ** API Tests** for essential backend functionalities
- Sequential authentication workflow for UI tests
- Parallel execution capabilities
- Detailed reporting with screenshots

## Prerequisites

1. **Java 21** or higher
2. **Maven** for building the project
3. **Chrome Browser** for UI testing
4. **Postman** for running the collection
5. **MySQL** database (optional, for persistent storage)

## Project Setup

### 1. Clone and Build the Project

```bash
# Navigate to the project directory
cd testframework

# Build the project
./mvnw clean install
```

### 2. Run the Application

```bash
# Run the Spring Boot application
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

## Importing the Collection into Postman

### Step 1: Open Postman
Launch Postman on your computer.

### Step 2: Import the Collection
1. Click on the **"Import"** button in the top left corner of Postman
2. Select the **"File"** tab
3. Click **"Upload Files"**
4. Browse and select the `Complete_UI_Testing_With_3_API_Suite.json` file
5. Click **"Import"**

### Step 3: Verify Import
You should see a collection named **"Complete UI Testing Suite + 3 API Tests"** in your Postman sidebar.

## Running the Tests

### Step 1: Start the Application
Make sure the Spring Boot application is running on `http://localhost:8080`.

### Step 2: Run the Collection
In Postman:
1. Click on the **"Complete UI Testing Suite + 3 API Tests"** collection
2. Click the **"Run"** button next to the collection name
3. In the Collection Runner:
   - Ensure the environment is set to the default environment
   - Optionally, set the number of iterations
   - Click **"Run Complete UI Testing Suite + 3 API Tests"**

### Alternative: Run Individual Requests
You can also run the tests step by step:

1. **Section 1: Create Suite & Batch Add UI + API Tests**
   - Run **"1.1 Create Test Suite"** to create a new test suite
   - Run **"1.2 Batch Create UI Tests (Steps 1-8) + 3 API Tests"** to add all test cases

2. **Section 2: Execute Complete UI + API Suite**
   - Run **"2.1 Execute UI Testing Suite + 3 API Tests"** to start the test execution
   - Run **"2.2 Check Execution Status & Results"** to monitor progress and view results

3. **Section 3: Generate & Download Reports**
   - Run **"3.1 Generate Complete Test Report"** to generate an HTML report
   - Run **"3.2 Download Complete Report"** to download the report


After the tests finish executing, you can find both screenshots and HTML reports inside the /report folder.

## Test Workflow

### UI Tests (Sequential Authentication)
1. **UI Step 0**: User Registration (Signup)
2. **UI Step 0**: User Login
3. **UI Step 1**: Open Account (Authenticated)
4. **UI Step 2**: Account Overview (Authenticated)
5. **UI Step 3**: Transfer Funds (Authenticated)
6. **UI Step 4**: Pay Bills (Authenticated)
7. **UI Step 5**: Find Transactions (Authenticated)
8. **UI Step 6**: Update Profile (Authenticated)
9. **UI Step 7**: Request Loan (Authenticated)
10. **UI Step 8**: Logout (Final Step)

### API Tests
1. **API 1**: Login Authentication
2. **API 2**: Create Customer
3. **API 3**: Health Check Validation

## Viewing Results

### In Postman
- Check the response body and test results in each request
- Monitor console logs for detailed information

### In Browser (Reports)
- After running the report generation requests, open the generated HTML reports
- Reports include screenshots for UI tests and detailed results for API tests

### In Application Logs
- Check the Spring Boot application console for detailed execution logs

## Troubleshooting

### Common Issues

1. **UI Tests Failing**
   - Ensure Chrome browser is installed
   - Check that no other Chrome instances are running
   - Verify internet connectivity to https://parabank.parasoft.com/

2. **API Tests Failing**
   - Check internet connectivity
   - Verify the ParaBank API endpoints are accessible

3. **Application Not Starting**
   - Ensure port 8080 is free
   - Check Java 21 is installed and in PATH

### Need Help?
If you encounter any issues:
1. Check the application logs in the console
2. Verify all prerequisites are met
3. Ensure the Spring Boot application is running before executing tests