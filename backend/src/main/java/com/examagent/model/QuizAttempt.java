package com.examagent.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    /** Nullable - an attempt can be anonymous. Set when the submission names a student, enabling per-student history (Stage 8). */
    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AnswerRecord> answers = new ArrayList<>();

    @Column(nullable = false)
    private Instant submittedAt = Instant.now();

    public QuizAttempt(Quiz quiz) {
        this.quiz = quiz;
    }

    public void addAnswer(AnswerRecord answer) {
        answer.setAttempt(this);
        answers.add(answer);
    }

    public long score() {
        return answers.stream().filter(AnswerRecord::isCorrect).count();
    }
}
