package com.examagent.agent;

import com.examagent.dto.EvaluationDraft;
import com.examagent.dto.LectureKnowledge;
import com.examagent.dto.TopicStats;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Interprets already-computed per-topic accuracy (TopicStats - deterministic, no LLM
 * involved in producing the numbers themselves) into a verdict a student can act on: which
 * topics need more work, which are solid, and what to do next. The model's job is judgment
 * and phrasing, not arithmetic.
 */
@Component
public class EvaluationAgent {

    private final ChatClient chatClient;

    public EvaluationAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public EvaluationDraft evaluate(String lectureTitle, LectureKnowledge knowledge, List<TopicStats> topicStats) {
        String prompt = """
                A student has been practicing quizzes on the lecture "%s". Here is their
                measured accuracy per topic so far (already computed - treat these numbers
                as ground truth, do not recompute or second-guess them):

                %s

                Lecture context, for phrasing recommendations meaningfully:
                Learning goals:
                %s
                Exam topics:
                %s

                Based on this performance data only:
                - weakTopics: topics needing more work. Treat under ~70%% accuracy as weak;
                  also flag a topic as weak (and say so explicitly) if it has fewer than 3
                  answers recorded, since that's not enough data to be confident either way.
                - strongTopics: topics with high accuracy across a reasonable number of answers.
                - overallReadiness: exactly one of NOT_READY, DEVELOPING, MODERATE, READY.
                - recommendations: 3-5 concrete, actionable next steps referencing specific
                  topics (e.g. "revisit the lecture section on X", "attempt more questions on Y").
                """.formatted(
                lectureTitle,
                formatStats(topicStats),
                PromptText.bulletList(knowledge.learningGoals()),
                PromptText.bulletList(knowledge.examTopics())
        );

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(EvaluationDraft.class);
    }

    private String formatStats(List<TopicStats> stats) {
        StringBuilder sb = new StringBuilder();
        for (TopicStats s : stats) {
            double percent = s.accuracy() * 100;
            sb.append("- %s: %d/%d correct (%.0f%%)%n".formatted(s.topic(), s.correctAnswered(), s.totalAnswered(), percent));
        }
        return sb.isEmpty() ? "(no data)" : sb.toString();
    }
}
