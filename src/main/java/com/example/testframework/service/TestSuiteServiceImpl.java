package com.example.testframework.service;

import com.example.testframework.model.TestSuite;
import com.example.testframework.repository.TestSuiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TestSuiteServiceImpl implements TestSuiteService {

    private final TestSuiteRepository testSuiteRepository;

    @Autowired
    public TestSuiteServiceImpl(TestSuiteRepository testSuiteRepository) {
        this.testSuiteRepository = testSuiteRepository;
    }

    @Override
    public TestSuite createTestSuite(TestSuite testSuite) {
        // Ensure unique suite ID
        if (testSuite.getSuiteId() != null && testSuiteRepository.existsBySuiteId(testSuite.getSuiteId())) {
            throw new RuntimeException("Test suite with ID " + testSuite.getSuiteId() + " already exists");
        }
        
        return testSuiteRepository.save(testSuite);
    }

    @Override
    public List<TestSuite> getAllTestSuites() {
        return testSuiteRepository.findAll();
    }

    @Override
    public Optional<TestSuite> getTestSuiteById(Long id) {
        return testSuiteRepository.findById(id);
    }

    @Override
    public Optional<TestSuite> getTestSuiteBySuiteId(String suiteId) {
        return testSuiteRepository.findBySuiteId(suiteId);
    }

    @Override
    public TestSuite updateTestSuite(String suiteId, TestSuite testSuite) {
        Optional<TestSuite> existingOpt = testSuiteRepository.findBySuiteId(suiteId);
        if (existingOpt.isPresent()) {
            TestSuite existing = existingOpt.get();
            
            // Update fields
            if (testSuite.getName() != null) {
                existing.setName(testSuite.getName());
            }
            if (testSuite.getDescription() != null) {
                existing.setDescription(testSuite.getDescription());
            }
            if (testSuite.getEnvironment() != null) {
                existing.setEnvironment(testSuite.getEnvironment());
            }
            if (testSuite.getTags() != null) {
                existing.setTags(testSuite.getTags());
            }
            if (testSuite.getStatus() != null) {
                existing.setStatus(testSuite.getStatus());
            }
            if (testSuite.getCreatedBy() != null) {
                existing.setCreatedBy(testSuite.getCreatedBy());
            }
            
            // Update timestamp
            existing.setUpdatedAt(LocalDateTime.now());
            
            return testSuiteRepository.save(existing);
        } else {
            throw new RuntimeException("Test suite with ID " + suiteId + " not found");
        }
    }

    @Override
    public void deleteTestSuite(String suiteId) {
        if (!testSuiteRepository.existsBySuiteId(suiteId)) {
            throw new RuntimeException("Test suite with ID " + suiteId + " not found");
        }
        testSuiteRepository.deleteBySuiteId(suiteId);
    }

    @Override
    public List<TestSuite> getTestSuitesByStatus(TestSuite.Status status) {
        return testSuiteRepository.findByStatus(status);
    }

    @Override
    public List<TestSuite> getTestSuitesByEnvironment(String environment) {
        return testSuiteRepository.findByEnvironment(environment);
    }

    @Override
    public void updateSuiteStatistics(String suiteId, int totalTests, int activeTests) {
        Optional<TestSuite> suiteOpt = testSuiteRepository.findBySuiteId(suiteId);
        if (suiteOpt.isPresent()) {
            TestSuite suite = suiteOpt.get();
            suite.setTotalTests(totalTests);
            suite.setActiveTests(activeTests);
            suite.setUpdatedAt(LocalDateTime.now());
            testSuiteRepository.save(suite);
        }
    }

    @Override
    public boolean suiteExists(String suiteId) {
        return testSuiteRepository.existsBySuiteId(suiteId);
    }
}