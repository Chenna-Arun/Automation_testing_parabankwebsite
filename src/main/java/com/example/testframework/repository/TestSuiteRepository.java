package com.example.testframework.repository;

import com.example.testframework.model.TestSuite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestSuiteRepository extends JpaRepository<TestSuite, Long> {

    // Find by suite ID (unique identifier)
    Optional<TestSuite> findBySuiteId(String suiteId);
    
    // Find by name
    List<TestSuite> findByName(String name);
    
    // Find by status
    List<TestSuite> findByStatus(TestSuite.Status status);
    
    // Find by environment
    List<TestSuite> findByEnvironment(String environment);
    
    // Find by created user
    List<TestSuite> findByCreatedBy(String createdBy);
    
    // Check if suite ID exists
    boolean existsBySuiteId(String suiteId);
    
    // Delete by suite ID
    void deleteBySuiteId(String suiteId);
    
    // Custom query to find suites with test count
    @Query("SELECT s FROM TestSuite s WHERE s.totalTests > :minTests")
    List<TestSuite> findSuitesWithMinTests(@Param("minTests") Integer minTests);
    
    // Find active suites
    @Query("SELECT s FROM TestSuite s WHERE s.status = 'ACTIVE' AND s.activeTests > 0")
    List<TestSuite> findActiveSuitesWithTests();
}