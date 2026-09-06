package com.examagent.dto;

import com.examagent.model.ExamSession;
import com.examagent.model.ExamStatus;

import java.time.Instant;
import java.util.List;

/** Public "take the exam" view - like QuizResponse, hides correct answers. Adds the timing constraints a practice quiz doesn't have. */
public record ExamSessionResponse(Long id, Long lectureId, Long quizId, ExamStatus status,
                                   Instant startedAt, Instant deadline, int timeLimitSeconds,
                                   List<QuizResponse.QuestionView> questions) {

    public static ExamSessionResponse from(ExamSession session) {
        List<QuizResponse.QuestionView> views = session.getQuiz().getQuestions().stream()
                .map(q -> new QuizResponse.QuestionView(q.getId(), q.getTopic(), q.getPrompt(), q.getChoices()))
                .toList();
        return new ExamSessionResponse(
                session.getId(),
                session.getQuiz().getLecture().getId(),
                session.getQuiz().getId(),
                session.getStatus(),
                session.getStartedAt(),
                session.deadline(),
                session.getTimeLimitSeconds(),
                views
        );
    }
}
