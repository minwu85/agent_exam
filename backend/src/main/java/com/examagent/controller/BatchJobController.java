package com.examagent.controller;

import com.examagent.dto.BatchJobRequest;
import com.examagent.dto.BatchJobResponse;
import com.examagent.scheduler.BatchJobPriority;
import com.examagent.scheduler.JobSchedulerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Stage 12 (stretch): submit/inspect batch LLM jobs, and observe/resize the worker pool they run on. See JobSchedulerService for what this is (and isn't) meant to demonstrate. */
@RestController
@RequestMapping("/api/jobs")
public class BatchJobController {

    private final JobSchedulerService jobSchedulerService;

    public BatchJobController(JobSchedulerService jobSchedulerService) {
        this.jobSchedulerService = jobSchedulerService;
    }

    @PostMapping
    public BatchJobResponse submit(@Valid @RequestBody BatchJobRequest request) {
        var priority = request.priority() != null ? request.priority() : BatchJobPriority.NORMAL;
        var job = jobSchedulerService.submit(request.type(), request.lectureId(), request.questionCount(), priority);
        return BatchJobResponse.from(job);
    }

    @GetMapping("/{id}")
    public BatchJobResponse get(@PathVariable long id) {
        return BatchJobResponse.from(jobSchedulerService.getJob(id));
    }

    @GetMapping
    public List<BatchJobResponse> list() {
        return jobSchedulerService.listJobs().stream().map(BatchJobResponse::from).toList();
    }

    @GetMapping("/scheduler/status")
    public JobSchedulerService.SchedulerStatus schedulerStatus() {
        return jobSchedulerService.status();
    }

    /** The "elastic worker count" demo - resize the pool live and watch /scheduler/status's poolSize change. */
    @PostMapping("/scheduler/workers")
    public JobSchedulerService.SchedulerStatus resizeWorkers(@RequestParam int count) {
        jobSchedulerService.resizeWorkerPool(count);
        return jobSchedulerService.status();
    }
}
