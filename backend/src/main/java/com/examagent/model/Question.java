package com.examagent.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    /** Which key concept (from LectureKnowledge) this question targets - lets marking be aggregated per-topic later. */
    @Column(nullable = false)
    private String topic;

    @Lob
    @Column(nullable = false)
    private String prompt;

    @ElementCollection
    @CollectionTable(name = "question_choices", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "choice_index")
    @Column(name = "choice_text")
    private List<String> choices;

    @Column(nullable = false)
    private int correctChoiceIndex;

    @Lob
    private String explanation;

    public Question(String topic, String prompt, List<String> choices, int correctChoiceIndex, String explanation) {
        this.topic = topic;
        this.prompt = prompt;
        this.choices = choices;
        this.correctChoiceIndex = correctChoiceIndex;
        this.explanation = explanation;
    }
}
