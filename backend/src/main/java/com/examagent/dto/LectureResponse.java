package com.examagent.dto;

import com.examagent.model.Lecture;

import java.time.Instant;

public record LectureResponse(Long id, String title, String originalFilename, Instant uploadedAt,
                               int textLength, boolean analyzed, boolean indexed) {

    public static LectureResponse from(Lecture lecture) {
        int length = lecture.getRawText() == null ? 0 : lecture.getRawText().length();
        return new LectureResponse(
                lecture.getId(),
                lecture.getTitle(),
                lecture.getOriginalFilename(),
                lecture.getUploadedAt(),
                length,
                lecture.getKnowledgeJson() != null,
                lecture.getIndexedAt() != null
        );
    }
}
