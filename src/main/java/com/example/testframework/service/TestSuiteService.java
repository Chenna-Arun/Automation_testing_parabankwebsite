package com.example.testframework.service;

import com.example.testframework.model.TestSuite;

import java.util.List;
import java.util.Optional;

public interface TestSuiteService {

    // Create a test suite
    TestSuite createTestSuite(TestSuite testSuite);

    // Get all test suites
    List<TestSuite> getAllTestSuites();

    // Get test suite by ID
    Optional<TestSuite> getTestSuiteById(Long id);

    // Get test suite by suite ID
    Optional<TestSuite> getTestSuiteBySuiteId(String suiteId);

    // Update test suite
    TestSuite updateTestSuite(String suiteId, TestSuite testSuite);

    // Delete test suite
    void deleteTestSuite(String suiteId);

    // Find suites by status
    List<TestSuite> getTestSuitesByStatus(TestSuite.Status status);

    // Find suites by environment
    List<TestSuite> getTestSuitesByEnvironment(String environment);

    // Update suite statistics
    void updateSuiteStatistics(String suiteId, int totalTests, int activeTests);

    // Check if suite exists
    boolean suiteExists(String suiteId);
}