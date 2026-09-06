package com.examagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4 auto-configures a Jackson 3 (tools.jackson.databind) ObjectMapper for HTTP
 * message conversion, not the classic Jackson 2 (com.fasterxml.jackson.databind) one - that
 * jar is only on the classpath here as a transitive dependency of Spring AI's JSON-schema
 * tooling, with no bean of its own. LectureAnalysisService needs a Jackson 2 ObjectMapper to
 * serialize LectureKnowledge into the knowledgeJson column, so it's defined explicitly here
 * rather than relying on auto-configuration that no longer produces this type. This is
 * unrelated to (and doesn't affect) how HTTP responses are serialized - that still goes
 * through Boot's own Jackson 3 auto-configuration untouched. No extra modules registered -
 * LectureKnowledge/QuizDraft/EvaluationDraft are plain records of strings and nested
 * records, nothing date/time-typed that would need jackson-datatype-jsr310 (which is only
 * on the classpath at runtime scope here anyway, not compile).
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
