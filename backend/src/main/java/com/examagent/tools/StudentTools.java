package com.examagent.tools;

import com.examagent.dto.EvaluationResult;
import com.examagent.service.EvaluationService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Stage 8: the tool LectureTools was deliberately missing at Stage 5 - there was no
 * student/history concept to back it yet. Now that Student and per-student QuizAttempt
 * scoping exist, LearningAgent can look up a specific student's own performance instead of
 * only general lecture knowledge.
 */
@Component
public class StudentTools {

    private final EvaluationService evaluationService;

    public StudentTools(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @Tool(description = "Get a specific student's personalized performance on a lecture: "
            + "their own per-topic accuracy, weak/strong topics, overall readiness, and "
            + "recommendations - based only on that student's quiz attempts, not everyone's. "
            + "Use this when the student asks about their own progress or history, instead "
            + "of guessing how they're doing.")
    public EvaluationResult getStudentHistory(
            @ToolParam(description = "The student's database id") Long studentId,
            @ToolParam(description = "The lecture's database id") Long lectureId) {
        return evaluationService.evaluateStudentOnLecture(lectureId, studentId);
    }
}
