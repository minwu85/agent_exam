package com.examagent.controller;

import com.examagent.dto.QuizAttemptResponse;
import com.examagent.dto.QuizResponse;
import com.examagent.dto.QuizSubmissionRequest;
import com.examagent.model.Quiz;
import com.examagent.service.QuizMarkingService;
import com.examagent.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class QuizController {

    private final QuizService quizService;
    private final QuizMarkingService quizMarkingService;

    public QuizController(QuizService quizService, QuizMarkingService quizMarkingService) {
        this.quizService = quizService;
        this.quizMarkingService = quizMarkingService;
    }

    /** Generates a new quiz from a lecture's extracted knowledge. Requires POST /api/lectures/{id}/analyze to have run first. */
    @PostMapping("/api/lectures/{lectureId}/quiz")
    public QuizResponse generate(@PathVariable Long lectureId,
                                  @RequestParam(required = false) Integer count) {
        Quiz quiz = quizService.generateQuiz(lectureId, count);
        return QuizResponse.from(quiz);
    }

    @GetMapping("/api/quizzes/{quizId}")
    public QuizResponse get(@PathVariable Long quizId) {
        return QuizResponse.from(quizService.getQuiz(quizId));
    }

    /** Marks a completed quiz attempt and returns per-question correctness + explanations. */
    @PostMapping("/api/quizzes/{quizId}/submit")
    public QuizAttemptResponse submit(@PathVariable Long quizId, @Valid @RequestBody QuizSubmissionRequest request) {
        return QuizAttemptResponse.from(quizMarkingService.submit(quizId, request));
    }
}
