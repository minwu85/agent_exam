package com.examagent.service;

import com.examagent.dto.QuizSubmissionRequest;
import com.examagent.model.AnswerRecord;
import com.examagent.model.Question;
import com.examagent.model.Quiz;
import com.examagent.model.QuizAttempt;
import com.examagent.repository.QuizAttemptRepository;
import com.examagent.repository.QuizRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Marking a multiple-choice quiz is a lookup, not a reasoning task - the correct choice was
 * decided at generation time (QuizGenerationAgent). Deliberately deterministic and LLM-free:
 * cheaper, instant, and 100% consistent, which an LLM re-grading call would not guarantee.
 */
@Service
public class QuizMarkingService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;

    public QuizMarkingService(QuizRepository quizRepository, QuizAttemptRepository quizAttemptRepository) {
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
    }

    public QuizAttempt submit(Long quizId, QuizSubmissionRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found: " + quizId));

        Map<Long, Question> byId = quiz.getQuestions().stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        QuizAttempt attempt = new QuizAttempt(quiz);
        for (QuizSubmissionRequest.AnswerSubmission answer : request.answers()) {
            Question question = byId.get(answer.questionId());
            if (question == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Question %d does not belong to quiz %d".formatted(answer.questionId(), quizId));
            }
            attempt.addAnswer(new AnswerRecord(question, answer.selectedChoiceIndex()));
        }

        return quizAttemptRepository.save(attempt);
    }
}
