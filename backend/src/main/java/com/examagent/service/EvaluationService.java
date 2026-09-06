package com.examagent.service;

import com.examagent.agent.EvaluationAgent;
import com.examagent.dto.EvaluationDraft;
import com.examagent.dto.EvaluationResult;
import com.examagent.dto.LectureKnowledge;
import com.examagent.dto.TopicStats;
import com.examagent.model.AnswerRecord;
import com.examagent.model.Lecture;
import com.examagent.model.Quiz;
import com.examagent.model.QuizAttempt;
import com.examagent.repository.LectureRepository;
import com.examagent.repository.QuizAttemptRepository;
import com.examagent.repository.QuizRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates every recorded QuizAttempt for a lecture into per-topic accuracy, then asks
 * EvaluationAgent to interpret it. Scoped per-lecture rather than per-student for now: there
 * is no student/login concept yet (that's Stage 8 - Personalized memory), so this evaluates
 * "how is practice on this lecture going" across all attempts recorded against it, which is
 * the right granularity for a single-user setup and a straightforward migration to
 * per-student scoping once accounts exist.
 */
@Service
public class EvaluationService {

    private final LectureRepository lectureRepository;
    private final LectureAnalysisService lectureAnalysisService;
    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final EvaluationAgent evaluationAgent;

    public EvaluationService(LectureRepository lectureRepository,
                              LectureAnalysisService lectureAnalysisService,
                              QuizRepository quizRepository,
                              QuizAttemptRepository quizAttemptRepository,
                              EvaluationAgent evaluationAgent) {
        this.lectureRepository = lectureRepository;
        this.lectureAnalysisService = lectureAnalysisService;
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.evaluationAgent = evaluationAgent;
    }

    public EvaluationResult evaluateLecture(Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lecture not found: " + lectureId));

        List<Long> quizIds = quizRepository.findByLectureId(lectureId).stream().map(Quiz::getId).toList();
        if (quizIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No quizzes have been generated for lecture %d yet".formatted(lectureId));
        }

        List<QuizAttempt> attempts = quizAttemptRepository.findByQuizIdIn(quizIds);
        if (attempts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No quiz attempts have been recorded for lecture %d yet".formatted(lectureId));
        }

        List<TopicStats> topicStats = computeTopicStats(attempts);
        LectureKnowledge knowledge = lectureAnalysisService.getKnowledge(lectureId);

        EvaluationDraft draft = evaluationAgent.evaluate(lecture.getTitle(), knowledge, topicStats);
        return EvaluationResult.from(draft, topicStats);
    }

    private List<TopicStats> computeTopicStats(List<QuizAttempt> attempts) {
        // topic -> {correct, total}
        Map<String, int[]> tally = new LinkedHashMap<>();
        for (QuizAttempt attempt : attempts) {
            for (AnswerRecord answer : attempt.getAnswers()) {
                int[] counts = tally.computeIfAbsent(answer.getQuestion().getTopic(), t -> new int[2]);
                counts[1]++;
                if (answer.isCorrect()) {
                    counts[0]++;
                }
            }
        }
        return tally.entrySet().stream()
                .map(e -> {
                    int correct = e.getValue()[0];
                    int total = e.getValue()[1];
                    double accuracy = total == 0 ? 0.0 : (double) correct / total;
                    return new TopicStats(e.getKey(), total, correct, accuracy);
                })
                .toList();
    }
}
