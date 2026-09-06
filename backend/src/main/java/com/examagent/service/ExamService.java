package com.examagent.service;

import com.examagent.dto.QuizSubmissionRequest;
import com.examagent.model.ExamSession;
import com.examagent.model.ExamStatus;
import com.examagent.model.Quiz;
import com.examagent.model.QuizAttempt;
import com.examagent.repository.ExamSessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * The "sandbox" layer (Stage 7): controls the constraints a real exam has that a practice
 * quiz (Stage 3/4) doesn't - a time limit and exactly one submission. Question generation
 * and grading are delegated to the existing QuizService/QuizMarkingService rather than
 * duplicated, so an exam and a practice quiz always behave identically in the parts that
 * should be identical.
 */
@Service
public class ExamService {

    private static final int DEFAULT_TIME_LIMIT_MINUTES = 20;
    private static final int MAX_TIME_LIMIT_MINUTES = 180;

    private final QuizService quizService;
    private final QuizMarkingService quizMarkingService;
    private final ExamSessionRepository examSessionRepository;

    public ExamService(QuizService quizService,
                        QuizMarkingService quizMarkingService,
                        ExamSessionRepository examSessionRepository) {
        this.quizService = quizService;
        this.quizMarkingService = quizMarkingService;
        this.examSessionRepository = examSessionRepository;
    }

    public ExamSession startExam(Long lectureId, Integer questionCount, Integer timeLimitMinutes) {
        Quiz quiz = quizService.generateQuiz(lectureId, questionCount);
        int seconds = normalizeTimeLimitMinutes(timeLimitMinutes) * 60;
        ExamSession session = new ExamSession(quiz, seconds);
        return examSessionRepository.save(session);
    }

    public ExamSession getSession(Long sessionId) {
        return examSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exam session not found: " + sessionId));
    }

    public QuizAttempt submitExam(Long sessionId, QuizSubmissionRequest request) {
        ExamSession session = getSession(sessionId);

        if (session.getStatus() != ExamStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Exam session %d is already %s".formatted(sessionId, session.getStatus()));
        }

        if (session.isExpired(Instant.now())) {
            session.setStatus(ExamStatus.EXPIRED);
            examSessionRepository.save(session);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Exam session %d expired at %s - no answers were graded".formatted(sessionId, session.deadline()));
        }

        QuizAttempt attempt = quizMarkingService.submit(session.getQuiz().getId(), request);
        session.setAttempt(attempt);
        session.setStatus(ExamStatus.SUBMITTED);
        examSessionRepository.save(session);
        return attempt;
    }

    /** Deterministic, not an LLM judgment - EvaluationAgent (Stage 6) is the place for reasoned feedback; this is just a threshold on the score. */
    public String estimateReadiness(QuizAttempt attempt) {
        int total = attempt.getAnswers().size();
        if (total == 0) {
            return "NOT_READY";
        }
        double ratio = (double) attempt.score() / total;
        if (ratio >= 0.85) return "READY";
        if (ratio >= 0.70) return "MODERATE";
        if (ratio >= 0.50) return "DEVELOPING";
        return "NOT_READY";
    }

    private int normalizeTimeLimitMinutes(Integer requested) {
        if (requested == null) {
            return DEFAULT_TIME_LIMIT_MINUTES;
        }
        if (requested < 1 || requested > MAX_TIME_LIMIT_MINUTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "timeLimitMinutes must be between 1 and " + MAX_TIME_LIMIT_MINUTES);
        }
        return requested;
    }
}
