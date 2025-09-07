package com.example.testframework.service;

import com.example.testframework.model.TestCase;
import com.example.testframework.model.TestResult;
import com.example.testframework.repository.TestCaseRepository;
import com.example.testframework.repository.TestResultRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TestServiceImpl implements TestService {

    private final TestCaseRepository testCaseRepository;
    private final TestResultRepository testResultRepository;

    @Autowired
    public TestServiceImpl(TestCaseRepository testCaseRepository,
                           TestResultRepository testResultRepository) {
        this.testCaseRepository = testCaseRepository;
        this.testResultRepository = testResultRepository;
    }

    @Override
    public TestCase createTestCase(TestCase testCase) {
        return testCaseRepository.save(testCase);
    }

    @Override
    public List<TestCase> createTestCases(List<TestCase> testCases) {
        return testCaseRepository.saveAll(testCases);
    }

    @Override
    public List<TestCase> getAllTestCases() {
        return testCaseRepository.findAll();
    }

    @Override
    public Optional<TestCase> getTestCaseById(Long id) {
        return testCaseRepository.findById(id);
    }

    @Override
    public TestCase updateTestCase(Long id, TestCase testCase) {
        Optional<TestCase> existing = testCaseRepository.findById(id);
        if (existing.isPresent()) {
            TestCase tc = existing.get();

            // Always update required fields
            tc.setName(testCase.getName());
            tc.setType(testCase.getType());
            tc.setDescription(testCase.getDescription());
            tc.setStatus(testCase.getStatus());

            // Update optional fields only if provided
            if (testCase.getFunctionality() != null) {
                tc.setFunctionality(testCase.getFunctionality());
            }
            if (testCase.getPriority() != null) {
                tc.setPriority(testCase.getPriority());
            }
            if (testCase.getTestSuiteId() != null) {
                tc.setTestSuiteId(testCase.getTestSuiteId());
            }
            if (testCase.getCategory() != null) {
                tc.setCategory(testCase.getCategory());
            }
            if (testCase.getTestData() != null) {
                tc.setTestData(testCase.getTestData());
            }
            if (testCase.getExpectedResult() != null) {
                tc.setExpectedResult(testCase.getExpectedResult());
            }
            if (testCase.getTestEnvironment() != null) {
                tc.setTestEnvironment(testCase.getTestEnvironment());
            }
            if (testCase.getTimeout() != null) {
                tc.setTimeout(testCase.getTimeout());
            }
            if (testCase.getRetryCount() != null) {
                tc.setRetryCount(testCase.getRetryCount());
            }
            if (testCase.getIsActive() != null) {
                tc.setIsActive(testCase.getIsActive());
            }
            if (testCase.getExecutionMode() != null) {
                tc.setExecutionMode(testCase.getExecutionMode());
            }
            if (testCase.getTags() != null) {
                tc.setTags(testCase.getTags());
            }

            return testCaseRepository.save(tc);
        } else {
            throw new RuntimeException("Test case with ID " + id + " not found");
        }
    }

    @Override
    public void deleteTestCase(Long id) {
        testCaseRepository.deleteById(id);
    }

    @Override
    public TestResult addTestResult(Long testCaseId, TestResult testResult) {
        Optional<TestCase> testCaseOpt = testCaseRepository.findById(testCaseId);
        if (testCaseOpt.isPresent()) {
            testResult.setTestCase(testCaseOpt.get());
            return testResultRepository.save(testResult);
        } else {
            throw new RuntimeException("Test case with ID " + testCaseId + " not found");
        }
    }

    @Override
    public List<TestResult> getResultsByTestCaseId(Long testCaseId) {
        Optional<TestCase> testCaseOpt = testCaseRepository.findById(testCaseId);
        if (testCaseOpt.isPresent()) {
            return testResultRepository.findByTestCase(testCaseOpt.get());
        } else {
            throw new RuntimeException("Test case with ID " + testCaseId + " not found");
        }
    }
}
