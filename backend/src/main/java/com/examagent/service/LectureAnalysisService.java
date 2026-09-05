package com.examagent.service;

import com.examagent.agent.KnowledgeExtractionAgent;
import com.examagent.dto.LectureKnowledge;
import com.examagent.model.Lecture;
import com.examagent.repository.LectureRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.UncheckedIOException;

/**
 * Orchestrates the "lecture understanding" step: load a lecture, run it through the
 * KnowledgeExtractionAgent, and persist the structured result so it doesn't need to be
 * recomputed (and re-billed) on every read.
 */
@Service
public class LectureAnalysisService {

    private final LectureRepository lectureRepository;
    private final KnowledgeExtractionAgent knowledgeExtractionAgent;
    private final ObjectMapper objectMapper;

    public LectureAnalysisService(LectureRepository lectureRepository,
                                   KnowledgeExtractionAgent knowledgeExtractionAgent,
                                   ObjectMapper objectMapper) {
        this.lectureRepository = lectureRepository;
        this.knowledgeExtractionAgent = knowledgeExtractionAgent;
        this.objectMapper = objectMapper;
    }

    public LectureKnowledge analyze(Long lectureId) {
        Lecture lecture = findLecture(lectureId);

        LectureKnowledge knowledge = knowledgeExtractionAgent.extract(lecture.getTitle(), lecture.getRawText());

        lecture.setKnowledgeJson(writeJson(knowledge));
        lectureRepository.save(lecture);

        return knowledge;
    }

    public LectureKnowledge getKnowledge(Long lectureId) {
        Lecture lecture = findLecture(lectureId);
        if (lecture.getKnowledgeJson() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Lecture %d has not been analyzed yet - call POST /api/lectures/%d/analyze first"
                            .formatted(lectureId, lectureId));
        }
        return readJson(lecture.getKnowledgeJson());
    }

    private Lecture findLecture(Long lectureId) {
        return lectureRepository.findById(lectureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lecture not found: " + lectureId));
    }

    private String writeJson(LectureKnowledge knowledge) {
        try {
            return objectMapper.writeValueAsString(knowledge);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Failed to serialize extracted lecture knowledge", e);
        }
    }

    private LectureKnowledge readJson(String json) {
        try {
            return objectMapper.readValue(json, LectureKnowledge.class);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Failed to deserialize stored lecture knowledge", e);
        }
    }
}
