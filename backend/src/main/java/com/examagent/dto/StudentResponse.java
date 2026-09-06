package com.examagent.dto;

import com.examagent.model.Student;

import java.time.Instant;

public record StudentResponse(Long id, String displayName, Instant createdAt) {

    public static StudentResponse from(Student student) {
        return new StudentResponse(student.getId(), student.getDisplayName(), student.getCreatedAt());
    }
}
