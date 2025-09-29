package com.longineers.batcher.config;

import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobExecutionException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ApplicationEvent {
    private final JobLauncher jobLauncher;
    private final Job job;

    @EventListener(ApplicationReadyEvent.class)
    public void launchJob() throws JobExecutionException {
        jobLauncher.run(job, new JobParameters());
    }
}
