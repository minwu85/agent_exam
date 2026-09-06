package com.examagent.dto;

/** Deterministically computed (no LLM) accuracy for one topic across every recorded QuizAttempt on a lecture. */
public record TopicStats(String topic, int totalAnswered, int correctAnswered, double accuracy) {
}
