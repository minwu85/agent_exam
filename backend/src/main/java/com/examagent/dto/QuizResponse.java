package com.examagent.dto;

import com.examagent.model.Quiz;

import java.time.Instant;
import java.util.List;

/** Public view of a Quiz - deliberately omits correctChoiceIndex/explanation so a student can't see the answer before submitting. */
public record QuizResponse(Long id, Long lectureId, Instant createdAt, List<QuestionView> questions) {

    public record QuestionView(Long id, String topic, String prompt, List<String> choices) {
    }

    public static QuizResponse from(Quiz quiz) {
        List<QuestionView> views = quiz.getQuestions().stream()
                .map(q -> new QuestionView(q.getId(), q.getTopic(), q.getPrompt(), q.getChoices()))
                .toList();
        return new QuizResponse(quiz.getId(), quiz.getLecture().getId(), quiz.getCreatedAt(), views);
    }
}
