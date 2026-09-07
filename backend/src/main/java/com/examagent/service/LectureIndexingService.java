package com.examagent.service;

import com.examagent.model.Lecture;
import com.examagent.repository.LectureRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage 9: chunks a lecture's raw text and embeds it into pgvector, and answers semantic
 * search over it. This is additive, not a replacement for Stage 2/3's approach - the
 * KnowledgeExtractionAgent and QuizGenerationAgent still work off the capped whole
 * transcript/LectureKnowledge as before (that's the right tool for "understand this one
 * lecture as a whole"). What RAG adds is the ability to pull a specific relevant passage on
 * demand - useful once transcripts are longer than the cap, or once search spans multiple
 * lectures - exposed to LearningAgent as a tool (see LectureTools.searchLectureContent) so
 * the agent decides if/when it needs a passage, rather than every prompt always retrieving.
 */
@Service
public class LectureIndexingService {

    private static final String LECTURE_ID_METADATA_KEY = "lectureId";

    private final LectureRepository lectureRepository;
    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    public LectureIndexingService(LectureRepository lectureRepository, VectorStore vectorStore) {
        this.lectureRepository = lectureRepository;
        this.vectorStore = vectorStore;
    }

    /** Idempotent: re-running this on the same lecture replaces its previous chunks rather than accumulating duplicates. */
    public void index(Long lectureId) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lecture not found: " + lectureId));

        if (lecture.getRawText() == null || lecture.getRawText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Lecture %d has no extracted text to index".formatted(lectureId));
        }

        vectorStore.delete(lectureFilter(lectureId));

        List<Document> chunks = splitter.apply(List.of(new Document(lecture.getRawText())));
        List<Document> tagged = chunks.stream()
                .map(chunk -> {
                    Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
                    metadata.put(LECTURE_ID_METADATA_KEY, lectureId);
                    metadata.put("lectureTitle", lecture.getTitle());
                    return Document.builder()
                            .text(chunk.getText())
                            .metadata(metadata)
                            .build();
                })
                .toList();
        vectorStore.add(tagged);

        lecture.setIndexedAt(Instant.now());
        lectureRepository.save(lecture);
    }

    /** Semantic search scoped to one lecture's chunks - not a global search across every lecture, on purpose (see class doc). */
    public List<String> search(Long lectureId, String query, int topK) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression(lectureFilter(lectureId))
                .build();
        return vectorStore.similaritySearch(request).stream()
                .map(Document::getText)
                .toList();
    }

    private org.springframework.ai.vectorstore.filter.Filter.Expression lectureFilter(Long lectureId) {
        return new FilterExpressionBuilder().eq(LECTURE_ID_METADATA_KEY, lectureId).build();
    }
}
