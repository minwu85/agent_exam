package com.examagent.agent;

import com.examagent.dto.LectureKnowledge;
import com.examagent.dto.QuizDraft;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Turns structured lecture knowledge (not raw transcript text - see KnowledgeExtractionAgent)
 * into multiple-choice practice questions. Reading from LectureKnowledge instead of the
 * transcript keeps questions anchored to what was already identified as important, and is
 * far cheaper than re-sending the whole transcript for every quiz generated from one lecture.
 */
@Component
public class QuizGenerationAgent {

    private final ChatClient chatClient;

    public QuizGenerationAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public QuizDraft generate(String lectureTitle, LectureKnowledge knowledge, int questionCount) {
        String prompt = """
                You are creating a multiple-choice practice quiz for a student studying the
                lecture "%s".

                Use ONLY the structured knowledge below - do not introduce facts that aren't
                implied by it. Spread questions across different topics rather than
                concentrating on one key concept.

                Learning goals:
                %s

                Key concepts:
                %s

                Key terms:
                %s

                Common mistakes students make:
                %s

                Likely exam topics:
                %s

                Generate exactly %d multiple-choice questions. For each question:
                - topic: which key concept or term it targets (short label)
                - prompt: the question text
                - choices: exactly 4 answer options, plausible distractors (not obviously wrong)
                - correctChoiceIndex: 0-based index of the correct option in `choices`
                - explanation: 1-2 sentences explaining why that answer is correct, referencing
                  the relevant concept - this is shown to the student after they answer
                """.formatted(
                lectureTitle,
                bulletList(knowledge.learningGoals()),
                knowledge.keyConcepts().stream()
                        .map(c -> "- %s (%s): %s".formatted(c.topic(), c.importance(), c.summary()))
                        .reduce("", (a, b) -> a + b + "\n"),
                bulletList(knowledge.keyTerms()),
                bulletList(knowledge.commonMistakes()),
                bulletList(knowledge.examTopics()),
                questionCount
        );

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(QuizDraft.class);
    }

    private String bulletList(Iterable<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            sb.append("- ").append(item).append("\n");
        }
        return sb.isEmpty() ? "(none provided)" : sb.toString();
    }
}
