package com.examagent.dto;

import com.examagent.model.AnswerRecord;
import com.examagent.model.QuizAttempt;

import java.time.Instant;
import java.util.List;

/** Post-submission view - unlike QuizResponse, this reveals correctChoiceIndex/explanation since the student has already answered. */
public record QuizAttemptResponse(Long id, Long quizId, long score, int total, Instant submittedAt,
                                   List<AnswerResultView> results) {

    public record AnswerResultView(Long questionId, String topic, String prompt, List<String> choices,
                                    int selectedChoiceIndex, int correctChoiceIndex, boolean correct,
                                    String explanation) {
    }

    public static QuizAttemptResponse from(QuizAttempt attempt) {
        List<AnswerResultView> results = attempt.getAnswers().stream()
                .map(QuizAttemptResponse::toView)
                .toList();
        return new QuizAttemptResponse(
                attempt.getId(),
                attempt.getQuiz().getId(),
                attempt.score(),
                attempt.getAnswers().size(),
                attempt.getSubmittedAt(),
                results
        );
    }

    private static AnswerResultView toView(AnswerRecord record) {
        var question = record.getQuestion();
        return new AnswerResultView(
                question.getId(),
                question.getTopic(),
                question.getPrompt(),
                question.getChoices(),
                record.getSelectedChoiceIndex(),
                question.getCorrectChoiceIndex(),
                record.isCorrect(),
                question.getExplanation()
        );
    }
}
