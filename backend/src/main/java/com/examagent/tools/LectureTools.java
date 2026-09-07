package com.examagent.tools;

import com.examagent.dto.LectureKnowledge;
import com.examagent.dto.QuizResponse;
import com.examagent.service.LectureAnalysisService;
import com.examagent.service.LectureIndexingService;
import com.examagent.service.QuizService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tools exposed to {@code LearningAgent} via Spring AI tool calling. This is the harness
 * boundary: the model can only do what a method here lets it do (read already-extracted
 * knowledge, trigger quiz generation, search a lecture's indexed text) - it cannot reach
 * the database, upload files, or call any other service directly. Each new capability the
 * agent should have starts as a new @Tool method here, not as more logic embedded in a prompt.
 */
@Component
public class LectureTools {

    private final LectureAnalysisService lectureAnalysisService;
    private final QuizService quizService;
    private final LectureIndexingService lectureIndexingService;

    public LectureTools(LectureAnalysisService lectureAnalysisService, QuizService quizService,
                         LectureIndexingService lectureIndexingService) {
        this.lectureAnalysisService = lectureAnalysisService;
        this.quizService = quizService;
        this.lectureIndexingService = lectureIndexingService;
    }

    @Tool(description = "Get the structured knowledge already extracted from a lecture: its "
            + "learning goals, key concepts, key terms, common student mistakes, and likely "
            + "exam topics. Call this to find out what a lecture covers before answering "
            + "questions about it or deciding what a quiz should focus on.")
    public LectureKnowledge getLectureKnowledge(@ToolParam(description = "The lecture's database id") Long lectureId) {
        return lectureAnalysisService.getKnowledge(lectureId);
    }

    @Tool(description = "Generate a new multiple-choice practice quiz from a lecture that has "
            + "already been analyzed. Returns the quiz id and its questions with answer "
            + "choices, but never the correct answers - do not try to guess or reveal them.")
    public QuizResponse generateQuiz(
            @ToolParam(description = "The lecture's database id") Long lectureId,
            @ToolParam(description = "Number of questions to generate, 1-20; omit for the default of 5", required = false) Integer questionCount) {
        return QuizResponse.from(quizService.generateQuiz(lectureId, questionCount));
    }

    @Tool(description = "Search a lecture's actual indexed text for passages relevant to a "
            + "specific question, and return the most relevant excerpts verbatim. Use this "
            + "when getLectureKnowledge's summary doesn't have enough detail to answer a "
            + "specific student question precisely - e.g. they ask for an exact definition, "
            + "example, or wording from the lecture. Only works if the lecture has already "
            + "been indexed; if this returns nothing, say so rather than guessing.")
    public List<String> searchLectureContent(
            @ToolParam(description = "The lecture's database id") Long lectureId,
            @ToolParam(description = "What to search for, as a natural-language question or phrase") String query) {
        return lectureIndexingService.search(lectureId, query, 3);
    }
}
