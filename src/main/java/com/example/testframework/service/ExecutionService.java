package com.example.testframework.service;

import com.example.testframework.dto.RunStatusDTO;
import com.example.testframework.model.TestCase;

import java.util.List;

public interface ExecutionService {

    String executeTests(List<TestCase> testCases, boolean parallel, int threadPoolSize);

    RunStatusDTO getRunStatus(String runId);

    enum Status {
        PENDING,
        RUNNING,
        COMPLETED
    }
}
