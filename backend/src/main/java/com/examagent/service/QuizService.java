package com.examagent.service;

import com.examagent.agent.QuizGenerationAgent;
import com.examagent.dto.LectureKnowledge;
import com.examagent.dto.QuizDraft;
import com.examagent.model.Lecture;
import com.examagent.model.Question;
import com.examagent.model.Quiz;
import com.examagent.repository.LectureRepository;
import com.examagent.repository.QuizRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuizService {

    private static final int DEFAULT_QUESTION_COUNT = 5;
    private static final int MAX_QUESTION_COUNT = 20;

    private final LectureRepository lectureRepository;
    private final QuizRepository quizRepository;
    private final QuizGenerationAgent quizGenerationAgent;
    private final LectureAnalysisService lectureAnalysisService;

    public QuizService(LectureRepository lectureRepository,
                        QuizRepository quizRepository,
                        QuizGenerationAgent quizGenerationAgent,
                        LectureAnalysisService lectureAnalysisService) {
        this.lectureRepository = lectureRepository;
        this.quizRepository = quizRepository;
        this.quizGenerationAgent = quizGenerationAgent;
        this.lectureAnalysisService = lectureAnalysisService;
    }

    public Quiz generateQuiz(Long lectureId, Integer requestedCount) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lecture not found: " + lectureId));

        int count = normalizeCount(requestedCount);

        // Reuses persisted knowledge if /analyze was already called; LectureAnalysisService
        // throws a 404 with a clear message if it wasn't, rather than silently falling back
        // to raw text here (a quiz built off unstructured text would be a different, worse
        // agent than the one QuizGenerationAgent is designed for).
        LectureKnowledge knowledge = lectureAnalysisService.getKnowledge(lectureId);

        QuizDraft draft = quizGenerationAgent.generate(lecture.getTitle(), knowledge, count);

        Quiz quiz = new Quiz(lecture);
        for (QuizDraft.QuestionDraft q : draft.questions()) {
            quiz.addQuestion(new Question(q.topic(), q.prompt(), q.choices(), q.correctChoiceIndex(), q.explanation()));
        }
        return quizRepository.save(quiz);
    }

    public Quiz getQuiz(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found: " + quizId));
    }

    private int normalizeCount(Integer requested) {
        if (requested == null) {
            return DEFAULT_QUESTION_COUNT;
        }
        if (requested < 1 || requested > MAX_QUESTION_COUNT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "count must be between 1 and " + MAX_QUESTION_COUNT);
        }
        return requested;
    }
}
