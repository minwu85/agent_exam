package com.examagent.dto;

import java.util.List;

/**
 * Structured knowledge extracted from a lecture transcript by {@code KnowledgeExtractionAgent}.
 * Field names/shape intentionally mirror what downstream agents (quiz generation, exam
 * simulation) will consume, so this is the contract between "understanding" and "practice".
 */
public record LectureKnowledge(
        List<String> learningGoals,
        List<KeyConcept> keyConcepts,
        List<String> keyTerms,
        List<String> commonMistakes,
        List<String> examTopics
) {
    public record KeyConcept(String topic, String importance, String summary) {
    }
}
