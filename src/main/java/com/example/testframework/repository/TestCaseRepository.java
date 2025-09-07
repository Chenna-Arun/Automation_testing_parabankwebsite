package com.example.testframework.repository;

import com.example.testframework.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    // You can add custom queries here if needed
}
