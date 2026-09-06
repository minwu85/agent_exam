package com.examagent.dto;

import java.util.List;

/** API response: EvaluationAgent's interpretation plus the deterministic numbers it was based on, so a caller can see both the verdict and the raw evidence. */
public record EvaluationResult(
        List<String> weakTopics,
        List<String> strongTopics,
        String overallReadiness,
        List<String> recommendations,
        List<TopicStats> topicStats
) {
    public static EvaluationResult from(EvaluationDraft draft, List<TopicStats> topicStats) {
        return new EvaluationResult(
                draft.weakTopics(),
                draft.strongTopics(),
                draft.overallReadiness(),
                draft.recommendations(),
                topicStats
        );
    }
}
