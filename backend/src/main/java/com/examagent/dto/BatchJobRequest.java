package com.examagent.dto;

import com.examagent.scheduler.BatchJobPriority;
import com.examagent.scheduler.BatchJobType;
import jakarta.validation.constraints.NotNull;

public record BatchJobRequest(@NotNull BatchJobType type, @NotNull Long lectureId,
                               Integer questionCount, BatchJobPriority priority) {
}
