package com.examagent.dto;

import java.util.List;

/** Raw structured output of EvaluationAgent - the interpretation only. TopicStats are computed deterministically and merged in separately (see EvaluationResult), never asked of the model. */
public record EvaluationDraft(
        List<String> weakTopics,
        List<String> strongTopics,
        String overallReadiness,
        List<String> recommendations
) {
}
