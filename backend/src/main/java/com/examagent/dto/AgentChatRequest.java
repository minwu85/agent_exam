package com.examagent.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentChatRequest(@NotBlank String message) {
}
