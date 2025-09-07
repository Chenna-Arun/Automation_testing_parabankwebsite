package com.example.testframework.repository;

import com.example.testframework.model.TestResult;
import com.example.testframework.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    // Get all results for a specific test case
    List<TestResult> findByTestCase(TestCase testCase);

    // Optional: get results by status
    List<TestResult> findByStatus(String status);
}
