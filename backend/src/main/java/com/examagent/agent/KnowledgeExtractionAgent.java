package com.examagent.agent;

import com.examagent.dto.LectureKnowledge;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * "Lecture understanding" step of the pipeline: turns a raw lecture transcript into
 * structured educational knowledge (learning goals, key concepts, terms, common mistakes,
 * likely exam topics), instead of a plain-text summary. Downstream agents (quiz generation,
 * evaluation, exam simulation) consume this structure rather than re-reading raw text.
 */
@Component
public class KnowledgeExtractionAgent {

    /**
     * Lecture transcripts can be long; cap what we send to bound cost/latency on this first
     * pass. Revisit once chunking + RAG (Stage: Knowledge Base) is in place.
     */
    private static final int MAX_TRANSCRIPT_CHARS = 15_000;

    private final ChatClient chatClient;

    public KnowledgeExtractionAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public LectureKnowledge extract(String lectureTitle, String lectureText) {
        String transcript = lectureText == null ? "" : lectureText;
        if (transcript.length() > MAX_TRANSCRIPT_CHARS) {
            transcript = transcript.substring(0, MAX_TRANSCRIPT_CHARS);
        }

        String prompt = """
                You are an expert teaching assistant preparing exam study material for a student.

                Analyse the lecture transcript below and extract structured knowledge that will
                later be used to generate practice questions and identify weak areas. Be specific
                to this lecture's actual content - do not invent topics that are not present.

                Lecture title: %s

                Lecture transcript:
                ---
                %s
                ---

                Extract:
                - learningGoals: 3 to 6 concise learning objectives a student should be able to
                  demonstrate after studying this lecture
                - keyConcepts: the most important concepts, each with a topic, an importance of
                  HIGH, MEDIUM or LOW, and a 1-3 sentence summary
                - keyTerms: important vocabulary/terminology introduced in this lecture
                - commonMistakes: mistakes students commonly make with this material
                - examTopics: topics from this lecture that are likely to appear on an exam
                """.formatted(lectureTitle, transcript);

        return chatClient.prompt()
                .user(prompt)
                .call()
                .entity(LectureKnowledge.class);
    }
}
