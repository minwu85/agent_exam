package com.examagent.scheduler;

import java.time.Instant;

/**
 * In-memory job record - deliberately not JPA-persisted. This is scheduling state (what's
 * queued, what's running right now), not durable application data; it doesn't need to
 * survive a restart the way a Lecture or QuizAttempt does. Mutated by one worker thread at
 * a time (JobSchedulerService never runs the same job twice), read by request threads
 * polling status - fields are volatile for that single-writer/many-reader visibility.
 */
public class BatchJob {

    private final long id;
    private final BatchJobType type;
    private final BatchJobPriority priority;
    private final Long lectureId;
    private final Integer questionCount;
    private final Instant submittedAt = Instant.now();

    private volatile BatchJobStatus status = BatchJobStatus.QUEUED;
    private volatile Instant startedAt;
    private volatile Instant completedAt;
    private volatile String resultSummary;
    private volatile String error;

    public BatchJob(long id, BatchJobType type, BatchJobPriority priority, Long lectureId, Integer questionCount) {
        this.id = id;
        this.type = type;
        this.priority = priority;
        this.lectureId = lectureId;
        this.questionCount = questionCount;
    }

    public void markRunning() {
        this.status = BatchJobStatus.RUNNING;
        this.startedAt = Instant.now();
    }

    public void markSucceeded(String summary) {
        this.status = BatchJobStatus.SUCCEEDED;
        this.resultSummary = summary;
        this.completedAt = Instant.now();
    }

    public void markFailed(String errorMessage) {
        this.status = BatchJobStatus.FAILED;
        this.error = errorMessage;
        this.completedAt = Instant.now();
    }

    public long getId() {
        return id;
    }

    public BatchJobType getType() {
        return type;
    }

    public BatchJobPriority getPriority() {
        return priority;
    }

    public Long getLectureId() {
        return lectureId;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public BatchJobStatus getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public String getError() {
        return error;
    }
}
