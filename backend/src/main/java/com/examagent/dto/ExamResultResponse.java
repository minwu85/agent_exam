package com.examagent.dto;

/** Result of a submitted exam: the same grading detail as a practice quiz attempt, plus a deterministic readiness label (see ExamService - not an LLM judgment, that's Stage 6's EvaluationAgent). */
public record ExamResultResponse(Long examSessionId, QuizAttemptResponse attempt, String readinessEstimate) {
}
