package com.examagent.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuizSubmissionRequest(Long studentId, @NotEmpty List<AnswerSubmission> answers) {

    public record AnswerSubmission(@NotNull Long questionId, int selectedChoiceIndex) {
    }
}
