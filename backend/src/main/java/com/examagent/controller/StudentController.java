package com.examagent.controller;

import com.examagent.dto.EvaluationResult;
import com.examagent.dto.StudentRequest;
import com.examagent.dto.StudentResponse;
import com.examagent.service.EvaluationService;
import com.examagent.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final EvaluationService evaluationService;

    public StudentController(StudentService studentService, EvaluationService evaluationService) {
        this.studentService = studentService;
        this.evaluationService = evaluationService;
    }

    @PostMapping
    public StudentResponse create(@Valid @RequestBody StudentRequest request) {
        return StudentResponse.from(studentService.create(request.displayName()));
    }

    @GetMapping("/{id}")
    public StudentResponse get(@PathVariable Long id) {
        return StudentResponse.from(studentService.get(id));
    }

    /** Stage 8: the personalized version of GET /api/lectures/{lectureId}/evaluation - scoped to this student's own attempts only. */
    @GetMapping("/{studentId}/lectures/{lectureId}/evaluation")
    public EvaluationResult evaluation(@PathVariable Long studentId, @PathVariable Long lectureId) {
        studentService.get(studentId); // 404s clearly if the student doesn't exist, rather than an empty-attempts 404 that reads the same as "never attempted"
        return evaluationService.evaluateStudentOnLecture(lectureId, studentId);
    }
}
