package com.examagent.agent;

import com.examagent.tools.LectureTools;
import com.examagent.tools.StudentTools;
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
            terms and likely exam topics), to generate a practice quiz from a lecture, and
            to look up a specific student's own performance history on a lecture. Use a
            tool whenever the student asks about a specific lecture by id, asks to be
            quizzed, or asks about their own progress/history - do not guess at lecture
            content or a student's performance from memory. If the student refers to a
            lecture or themselves without giving an id, ask them for it instead of assuming
            one.

            Keep replies concise and focused on helping the student study.
            """;

    private final ChatClient chatClient;

    public LearningAgent(ChatClient.Builder chatClientBuilder, LectureTools lectureTools, StudentTools studentTools) {
        this.chatClient = chatClientBuilder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(lectureTools, studentTools)
                .build();
    }

    public String converse(String studentMessage) {
        return chatClient.prompt()
                .user(studentMessage)
                .call()
                .content();
    }
}
