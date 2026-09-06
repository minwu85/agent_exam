package com.examagent.controller;

import com.examagent.agent.LearningAgent;
import com.examagent.dto.AgentChatRequest;
import com.examagent.dto.AgentChatResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Entry point to the agent framework (Stage 5). Unlike LectureController/QuizController,
 * which call services directly for one fixed job each, this hands the student's raw message
 * to LearningAgent and lets it decide which LectureTools method (if any) to invoke.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final LearningAgent learningAgent;

    public AgentController(LearningAgent learningAgent) {
        this.learningAgent = learningAgent;
    }

    @PostMapping("/chat")
    public AgentChatResponse chat(@Valid @RequestBody AgentChatRequest request) {
        return new AgentChatResponse(learningAgent.converse(request.message()));
    }
}
