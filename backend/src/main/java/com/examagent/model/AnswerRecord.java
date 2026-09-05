package com.examagent.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class AnswerRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "attempt_id", nullable = false)
    private QuizAttempt attempt;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(nullable = false)
    private int selectedChoiceIndex;

    @Column(nullable = false)
    private boolean correct;

    public AnswerRecord(Question question, int selectedChoiceIndex) {
        this.question = question;
        this.selectedChoiceIndex = selectedChoiceIndex;
        this.correct = selectedChoiceIndex == question.getCorrectChoiceIndex();
    }
}
