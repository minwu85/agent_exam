package com.examagent.scheduler;

import com.examagent.service.LectureAnalysisService;
import com.examagent.service.QuizService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stage 12 (stretch): a toy stand-in for the target role's GPU compute scheduling
 * requirement. This is explicitly NOT a claim of GPU-cluster scheduling experience - see
 * ROADMAP.md's own framing. What it demonstrates at small scale is the same shape of
 * problem: a limited pool of expensive workers (there, GPUs; here, threads making LLM
 * calls), a priority queue deciding what runs next, backpressure when the queue is full,
 * and elastic resizing of worker capacity at runtime - all real java.util.concurrent
 * primitives, not simulated.
 */
@Service
public class JobSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(JobSchedulerService.class);
    private static final int MAX_QUEUE_SIZE = 50;

    private final LectureAnalysisService lectureAnalysisService;
    private final QuizService quizService;
    private final ThreadPoolExecutor executor;
    private final Map<Long, BatchJob> jobsById = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();
    private final AtomicLong sequenceGenerator = new AtomicLong();

    public JobSchedulerService(LectureAnalysisService lectureAnalysisService,
                                QuizService quizService,
                                @Value("${app.scheduler.worker-count:2}") int workerCount) {
        this.lectureAnalysisService = lectureAnalysisService;
        this.quizService = quizService;
        // A priority queue of Runnables needs a comparator since PriorityTask can only be
        // compared to its own type, not to Runnable in general - safe here because this
        // executor only ever receives PriorityTask instances (see submit()).
        Comparator<Runnable> byPriorityThenOrder = (a, b) -> ((PriorityTask) a).compareTo((PriorityTask) b);
        this.executor = new ThreadPoolExecutor(
                workerCount, workerCount,
                0L, TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<>(MAX_QUEUE_SIZE, byPriorityThenOrder)
        );
    }

    public BatchJob submit(BatchJobType type, Long lectureId, Integer questionCount, BatchJobPriority priority) {
        if (executor.getQueue().size() >= MAX_QUEUE_SIZE) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Job queue is full (%d pending) - try again shortly".formatted(MAX_QUEUE_SIZE));
        }

        BatchJob job = new BatchJob(idGenerator.incrementAndGet(), type, priority, lectureId, questionCount);
        jobsById.put(job.getId(), job);
        executor.execute(new PriorityTask(job, sequenceGenerator.incrementAndGet()));
        return job;
    }

    public BatchJob getJob(long id) {
        BatchJob job = jobsById.get(id);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found: " + id);
        }
        return job;
    }

    public List<BatchJob> listJobs() {
        return jobsById.values().stream()
                .sorted(Comparator.comparing(BatchJob::getSubmittedAt).reversed())
                .toList();
    }

    public SchedulerStatus status() {
        return new SchedulerStatus(
                executor.getPoolSize(),
                executor.getActiveCount(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount()
        );
    }

    /** The "elastic" half of the demo - resize the worker pool at runtime, same as a real scheduler adding/removing GPU workers under load. */
    public void resizeWorkerPool(int newSize) {
        if (newSize < 1 || newSize > 32) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "workerCount must be between 1 and 32");
        }
        // setCorePoolSize/setMaximumPoolSize each reject shrinking past the other's
        // current value, so grow the ceiling before the floor and shrink the floor
        // before the ceiling.
        if (newSize > executor.getMaximumPoolSize()) {
            executor.setMaximumPoolSize(newSize);
            executor.setCorePoolSize(newSize);
        } else {
            executor.setCorePoolSize(newSize);
            executor.setMaximumPoolSize(newSize);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    public record SchedulerStatus(int poolSize, int activeWorkers, int queuedJobs, long completedJobs) {
    }

    /** Wraps one BatchJob as executable work with a total order: priority first (HIGH before LOW), then submission order within the same priority. */
    private final class PriorityTask implements Runnable, Comparable<PriorityTask> {
        private final BatchJob job;
        private final long sequence;

        PriorityTask(BatchJob job, long sequence) {
            this.job = job;
            this.sequence = sequence;
        }

        @Override
        public int compareTo(PriorityTask other) {
            int byPriority = job.getPriority().compareTo(other.job.getPriority());
            return byPriority != 0 ? byPriority : Long.compare(sequence, other.sequence);
        }

        @Override
        public void run() {
            job.markRunning();
            try {
                String summary = switch (job.getType()) {
                    case ANALYZE_LECTURE -> {
                        var knowledge = lectureAnalysisService.analyze(job.getLectureId());
                        yield "Extracted %d key concepts".formatted(knowledge.keyConcepts().size());
                    }
                    case GENERATE_QUIZ -> {
                        var quiz = quizService.generateQuiz(job.getLectureId(), job.getQuestionCount());
                        yield "Generated quiz %d with %d questions".formatted(quiz.getId(), quiz.getQuestions().size());
                    }
                };
                job.markSucceeded(summary);
            } catch (Exception e) {
                log.warn("Batch job {} failed: {}", job.getId(), e.toString());
                job.markFailed(e.getMessage() != null ? e.getMessage() : e.toString());
            }
        }
    }
}
