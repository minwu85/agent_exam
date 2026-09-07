package com.examagent.dto;

import com.examagent.scheduler.BatchJob;
import com.examagent.scheduler.BatchJobPriority;
import com.examagent.scheduler.BatchJobStatus;
import com.examagent.scheduler.BatchJobType;

import java.time.Instant;

public record BatchJobResponse(long id, BatchJobType type, BatchJobPriority priority, Long lectureId,
                                BatchJobStatus status, Instant submittedAt, Instant startedAt,
                                Instant completedAt, String resultSummary, String error) {

    public static BatchJobResponse from(BatchJob job) {
        return new BatchJobResponse(
                job.getId(), job.getType(), job.getPriority(), job.getLectureId(),
                job.getStatus(), job.getSubmittedAt(), job.getStartedAt(),
                job.getCompletedAt(), job.getResultSummary(), job.getError()
        );
    }
}
