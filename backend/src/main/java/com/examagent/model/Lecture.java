package com.examagent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Lecture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String originalFilename;

    @Lob
    private String rawText;

    /** Structured output of {@code KnowledgeExtractionAgent}, serialized as JSON. Null until analyzed. */
    @Lob
    private String knowledgeJson;

    /** Set once this lecture's text has been chunked + embedded into the vector store (Stage 9). Null until indexed. */
    private Instant indexedAt;

    @Column(nullable = false)
    private Instant uploadedAt = Instant.now();

    public Lecture(String title, String originalFilename, String rawText) {
        this.title = title;
        this.originalFilename = originalFilename;
        this.rawText = rawText;
    }
}
