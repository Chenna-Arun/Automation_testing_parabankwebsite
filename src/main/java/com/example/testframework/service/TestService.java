package com.example.testframework.service;

import com.example.testframework.model.TestCase;
import com.example.testframework.model.TestResult;

import java.util.List;
import java.util.Optional;

public interface TestService {

    // Create a single test case
    TestCase createTestCase(TestCase testCase);

    // Create multiple test cases
    List<TestCase> createTestCases(List<TestCase> testCases);

    // Get all test cases
    List<TestCase> getAllTestCases();

    // Get a test case by ID
    Optional<TestCase> getTestCaseById(Long id);

    // Update a test case
    TestCase updateTestCase(Long id, TestCase testCase);

    // Delete a test case
    void deleteTestCase(Long id);

    // Add test result for a test case
    TestResult addTestResult(Long testCaseId, TestResult testResult);

    // Get all results for a test case
    List<TestResult> getResultsByTestCaseId(Long testCaseId);
}
