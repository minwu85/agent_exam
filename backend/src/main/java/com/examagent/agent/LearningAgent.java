package com.examagent.agent;

import com.examagent.tools.LectureTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * The student-facing agent: unlike KnowledgeExtractionAgent/QuizGenerationAgent (which are
 * single-purpose, called directly by a service for one fixed job), LearningAgent takes a
 * free-form student message and decides for itself which tool - if any - to call to answer
 * it. This is the "Agent framework" piece: reasoning over which capability to invoke, rather
 * than a service calling agents in a fixed, hardcoded order.
 */
@Component
public class LearningAgent {

    private static final String SYSTEM_PROMPT = """
            You are a study assistant helping a student prepare for an exam.

            You have tools to look up what a specific lecture covers (its key concepts,
            terms and likely exam topics) and to generate a practice quiz from a lecture.
            Use a tool whenever the student asks about a specific lecture by id, or asks to
            be quizzed - do not guess at lecture content from memory. If the student refers
            to a lecture without giving its id, ask them for it instead of assuming one.

            Keep replies concise and focused on helping the student study.
            """;

    private final ChatClient chatClient;

    public LearningAgent(ChatClient.Builder chatClientBuilder, LectureTools lectureTools) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(lectureTools)
                .build();
    }

    public String converse(String studentMessage) {
        return chatClient.prompt()
                .user(studentMessage)
                .call()
                .content();
    }
}
