package com.examagent.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * The "sandbox" wrapper around a Quiz: an exam is the same generated question set as a
 * practice quiz, taken under constraints (time limit, single submission) that a plain
 * practice quiz doesn't enforce. Deliberately does not duplicate question-generation or
 * marking logic - it composes Quiz (Stage 3) and QuizAttempt (Stage 4) rather than
 * reinventing them.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class ExamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false)
    private Instant startedAt = Instant.now();

    @Column(nullable = false)
    private int timeLimitSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamStatus status = ExamStatus.IN_PROGRESS;

    /** Set once the exam is submitted (graded via the same QuizMarkingService as a practice quiz). */
    @ManyToOne
    @JoinColumn(name = "attempt_id")
    private QuizAttempt attempt;

    public ExamSession(Quiz quiz, int timeLimitSeconds) {
        this.quiz = quiz;
        this.timeLimitSeconds = timeLimitSeconds;
    }

    public Instant deadline() {
        return startedAt.plusSeconds(timeLimitSeconds);
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(deadline());
    }
}
