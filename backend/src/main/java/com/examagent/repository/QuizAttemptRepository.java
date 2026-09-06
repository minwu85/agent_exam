package com.examagent.repository;

import com.examagent.model.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByQuizIdIn(List<Long> quizIds);

    List<QuizAttempt> findByQuizIdInAndStudentId(List<Long> quizIds, Long studentId);
}
