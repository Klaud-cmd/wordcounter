package com.metasite.wordcounter.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/wordcount")
@Slf4j
public class JobController {

    @Autowired
    JobLauncher jobLauncher;

    @Autowired
    Job wordCountJob;

    @CrossOrigin
    @PostMapping("/start/{requestId}")
    public String startJob(@PathVariable UUID requestId) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
        log.info("Received request for {}", requestId);
        JobParameters jobParameters = new JobParameters(Map.of(
                "requestID", new JobParameter(requestId.toString())
        ));
        JobExecution execution = jobLauncher.run(wordCountJob, jobParameters);
        return execution.getExitStatus().toString();
    }
}
