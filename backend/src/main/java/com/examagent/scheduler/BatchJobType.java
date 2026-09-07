package com.examagent.scheduler;

/** The batch operations this app has that are worth queueing rather than running inline - both are LLM calls, the same kind of workload a real GPU/inference scheduler would be queueing. */
public enum BatchJobType {
    ANALYZE_LECTURE,
    GENERATE_QUIZ
}
