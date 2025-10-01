package com.longineers.batcher.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.util.Collections;

@RequiredArgsConstructor
@RestController
public class BatchJobController {
    private final JobLauncher jobLauncher;
    private final Job batchJob;

    @Data
    private static class JobLaunchRequest {
        private List<String> categories = Collections.emptyList();
    }

    @PostMapping("/run")
    public ResponseEntity<String> runBatchJob(@RequestBody(required = false) JobLaunchRequest request) {
        try {
            JobParametersBuilder jobParametersBuilder = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis()); // Ensures uniqueness for re-runs

            if (request != null && !request.getCategories().isEmpty()) {
                jobParametersBuilder.addString("categories", String.join(",", request.getCategories()));
            }

            JobParameters jobParameters = jobParametersBuilder.toJobParameters();
            jobLauncher.run(batchJob, jobParameters);

            return ResponseEntity.ok("Batch job started successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error starting batch job: " + e.getMessage());
        }
    }
}
