package com.examagent.dto;

import java.util.List;

/** Raw structured output of {@code QuizGenerationAgent}, before being persisted as Quiz/Question entities. */
public record QuizDraft(List<QuestionDraft> questions) {

    public record QuestionDraft(
            String topic,
            String prompt,
            List<String> choices,
            int correctChoiceIndex,
            String explanation
    ) {
    }
}
