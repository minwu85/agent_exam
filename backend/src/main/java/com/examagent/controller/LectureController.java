package com.examagent.controller;

import com.examagent.dto.EvaluationResult;
import com.examagent.dto.LectureKnowledge;
import com.examagent.dto.LectureResponse;
import com.examagent.model.Lecture;
import com.examagent.repository.LectureRepository;
import com.examagent.service.EvaluationService;
import com.examagent.service.LectureAnalysisService;
import com.examagent.service.LectureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

@RestController
@RequestMapping("/api/lectures")
public class LectureController {

    private final LectureService lectureService;
    private final LectureRepository lectureRepository;
    private final LectureAnalysisService lectureAnalysisService;
    private final EvaluationService evaluationService;

    public LectureController(LectureService lectureService,
                              LectureRepository lectureRepository,
                              LectureAnalysisService lectureAnalysisService,
                              EvaluationService evaluationService) {
        this.lectureService = lectureService;
        this.lectureRepository = lectureRepository;
        this.lectureAnalysisService = lectureAnalysisService;
        this.evaluationService = evaluationService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<LectureResponse> upload(@RequestParam("file") MultipartFile file,
                                                    @RequestParam("title") String title) {
        try {
            Lecture lecture = lectureService.uploadAndExtract(file, title);
            return ResponseEntity.ok(LectureResponse.from(lecture));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to process uploaded lecture", e);
        }
    }

    @GetMapping
    public List<LectureResponse> list() {
        return lectureRepository.findAll().stream().map(LectureResponse::from).toList();
    }

    @GetMapping("/{id}")
    public LectureResponse get(@PathVariable Long id) {
        return lectureRepository.findById(id)
                .map(LectureResponse::from)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
    }

    /** Runs the KnowledgeExtractionAgent over this lecture and persists the structured result. */
    @PostMapping("/{id}/analyze")
    public LectureKnowledge analyze(@PathVariable Long id) {
        return lectureAnalysisService.analyze(id);
    }

    /** Returns the previously extracted structured knowledge, without re-calling the model. */
    @GetMapping("/{id}/knowledge")
    public LectureKnowledge knowledge(@PathVariable Long id) {
        return lectureAnalysisService.getKnowledge(id);
    }

    /**
     * Aggregates every recorded QuizAttempt against this lecture into per-topic accuracy
     * and returns EvaluationAgent's weak/strong-topic verdict and recommendations.
     */
    @GetMapping("/{id}/evaluation")
    public EvaluationResult evaluation(@PathVariable Long id) {
        return evaluationService.evaluateLecture(id);
    }
}
