package com.examagent.controller;

import com.examagent.dto.ExamResultResponse;
import com.examagent.dto.ExamSessionResponse;
import com.examagent.dto.QuizAttemptResponse;
import com.examagent.dto.QuizSubmissionRequest;
import com.examagent.model.QuizAttempt;
import com.examagent.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    /** Starts a timed exam session from a lecture's extracted knowledge. Requires the lecture to have been analyzed already (same precondition as generating a practice quiz). */
    @PostMapping("/api/lectures/{lectureId}/exam")
    public ExamSessionResponse start(@PathVariable Long lectureId,
                                      @RequestParam(required = false) Integer count,
                                      @RequestParam(required = false) Integer timeLimitMinutes) {
        return ExamSessionResponse.from(examService.startExam(lectureId, count, timeLimitMinutes));
    }

    @GetMapping("/api/exams/{sessionId}")
    public ExamSessionResponse get(@PathVariable Long sessionId) {
        return ExamSessionResponse.from(examService.getSession(sessionId));
    }

    /** Submits an exam attempt exactly once - rejected with 409 if already submitted, or if the time limit has passed. */
    @PostMapping("/api/exams/{sessionId}/submit")
    public ExamResultResponse submit(@PathVariable Long sessionId, @Valid @RequestBody QuizSubmissionRequest request) {
        QuizAttempt attempt = examService.submitExam(sessionId, request);
        String readiness = examService.estimateReadiness(attempt);
        return new ExamResultResponse(sessionId, QuizAttemptResponse.from(attempt), readiness);
    }
}
