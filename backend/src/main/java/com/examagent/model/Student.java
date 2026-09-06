package com.examagent.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Deliberately minimal: no auth, no password, just an identity to hang history off of.
 * This project has no login system yet - Student exists so QuizAttempt/history can be
 * scoped to "a" learner now, in a shape that plugs into real auth later without a schema
 * rewrite, rather than bolting personalization onto a single implicit global user.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String displayName;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Student(String displayName) {
        this.displayName = displayName;
    }
}
