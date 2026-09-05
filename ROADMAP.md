# Roadmap

The forward-looking step list for this project: what's built, what's next, and how each
stage maps to the target job's required skills. Read alongside [DEVLOG.md](DEVLOG.md),
which records *why* each decision was made and what went wrong along the way — this file
is the plan, DEVLOG is the diary.

## Target skills → build stage mapping

| Target requirement | Where it's covered | Status |
|---|---|---|
| Generative AI tools, prompt engineering | Stage 2 (Knowledge Extraction Agent) | ✅ Done |
| Python + OCR/LLM experimentation | Stage 9 (OCR) | ⬜ Not started |
| SQL | PostgreSQL throughout | ✅ In use |
| JavaScript / React / Streamlit-style demos | Stage 10 (Frontend) | ⬜ Not started |
| LLM/Agent project experience, using large-model capability to solve problems | Stages 2–8 | 🔶 In progress |
| Agent framework, tool calling | Stage 5 (Agent Tools) | ⬜ Not started |
| Harness Engineering | Stage 5 + Stage 6 | ⬜ Not started |
| Sandbox / simulation environment | Stage 7 (Exam Simulation) | ⬜ Not started |
| Personalized memory | Stage 8 (Student Memory & Personalization) | ⬜ Not started |
| Skills (as in: reusable agent capabilities) | Stage 5 | ⬜ Not started |
| Automatic evaluation system | Stage 6 (Evaluation Agent) | ⬜ Not started |
| GPU compute resource management/scheduling | Stage 11 (stretch — see note) | ⬜ Not started |

Everything below the table is the same set of stages, in build order, with detail.

## Tech stack

- **Backend (main app):** Java 21, Spring Boot 4, Spring AI, Maven — matches your language
  preference and covers the JD's "Agent infrastructure" ask directly (Spring AI ships
  first-class tool calling, RAG/VectorStore, chat memory, and evaluator abstractions —
  the same primitives named in the JD, just under Java instead of Python).
- **Database:** PostgreSQL + pgvector (one datastore for relational + vector data).
- **Frontend:** plain REST via Postman/curl through Stage 6, then React + TypeScript from
  Stage 10 onward (matches the JD's "modern web frameworks" line without building UI
  before the API contract is stable).
- **Python:** introduced only at Stage 9 for OCR experimentation — the JD explicitly wants
  Python + OCR familiarity, and Python's OCR/ML tooling (pytesseract, easyocr, layout
  models) is materially better than Java's for this one slice, called from the Java
  backend as a subprocess or small FastAPI sidecar rather than rewriting the app in Python.
- **Environment:** Docker Desktop (Postgres via `docker-compose`, later a sandbox
  container for Stage 7), Git/GitHub, Postman for manual API testing, IntelliJ IDEA or
  any Java IDE. `ANTHROPIC_API_KEY` as an environment variable for Spring AI's Anthropic
  starter.

## Stages

### ✅ Stage 1 — Project scaffold + lecture upload
Java 21 / Spring Boot / Maven project; `Lecture` entity + repository; PDF text extraction
via Spring AI's `PagePdfDocumentReader`; upload/list/get endpoints. Details: [DEVLOG.md](DEVLOG.md#stage-1--project-scaffold--lecture-upload).

### ✅ Stage 2 — Lecture understanding (Knowledge Extraction Agent)
Structured extraction (learning goals, key concepts, key terms, common mistakes, exam
topics) via `ChatClient.entity()`. This *is* the "summarize lecture transcript into key
pointers" step from your original goal. Details: [DEVLOG.md](DEVLOG.md#stage-2--lecture-understanding-knowledge-extraction-agent).

### ⬜ Stage 3 — Quiz Agent (generate practice questions)
A `QuizGenerationAgent` reads a lecture's `LectureKnowledge` (not raw text — cheaper,
more targeted) and generates multiple-choice questions with the correct answer and an
explanation tied back to a specific key concept. New entities: `Quiz`, `Question`,
`Choice`. Endpoint: `POST /api/lectures/{id}/quiz`. This is the "provide quiz for user to
do" half of your stated goal.

### ⬜ Stage 4 — Student answers + automatic marking
Student submits answers to a generated quiz; a `QuizMarkingService` scores them
deterministically (the correct choice is already known — no LLM call needed to mark
multiple-choice) and returns per-question correctness + the agent's explanation.
New entity: `QuizAttempt`. This closes the loop: transcript → key pointers → quiz →
self-test → score.

### ⬜ Stage 5 — Agent framework & tools (Harness Engineering)
Formalize what's currently three separate agent classes into one `LearningAgent` that
reasons over which **tool** to call (`generateQuiz`, `getLectureKnowledge`,
`getStudentHistory`, `identifyWeakTopics`) via Spring AI's tool-calling support, instead
of the controller wiring services together directly. This is the concrete deliverable
behind the JD's "Agent framework" and "toolchains" line — current production harnesses
are described as more than a prompt: system prompt + tools + runtime + persistent state +
feedback loop ([LangChain: Anatomy of an Agent Harness](https://www.langchain.com/blog/the-anatomy-of-an-agent-harness); [Databricks: What is an AI Agent Harness?](https://www.databricks.com/blog/ai-harness)).

### ⬜ Stage 6 — Evaluation Agent (automatic evaluation system)
After each quiz attempt, aggregate per-topic accuracy and use an `EvaluationAgent` to
classify weak/strong topics and produce a recommendation, not just a percentage score.
Directly answers the JD's "automatic evaluation system" line.

### ⬜ Stage 7 — Exam Simulation (sandbox / simulation environment)
A timed, controlled "exam mode": fixed question count, topic distribution, time limit,
no going back, single final score + readiness estimate. This is the JD's "sandbox and
simulation environment" concept applied to an exam rather than a code sandbox — same
idea (a bounded, controlled environment the agent operates a session inside of), applied
to the domain this project is actually about.

### ⬜ Stage 8 — Personalized memory
Persist student history (attempts, per-topic accuracy over time) and feed a summary of
it into the Quiz/Evaluation agents' prompts so question difficulty and topic selection
adapt over time, instead of every session starting cold. Current practice favors
*selective* memory — store distilled facts (per-topic mastery), not full transcripts —
retrieved semantically rather than dumped wholesale into the prompt ([MachineLearningMastery: architectural patterns for persistent memory in AI agents](https://machinelearningmastery.com/5-architectural-patterns-for-persistent-memory-and-state-in-ai-agents/)).

### ⬜ Stage 9 — RAG / Knowledge base (pgvector)
Chunk + embed lecture transcripts into pgvector; let the Quiz/Evaluation agents retrieve
relevant chunks instead of relying on the capped 15k-char transcript from Stage 2. 2026
practice trends toward *agentic* RAG — the agent decides whether/what/when to retrieve,
rather than always retrieving once up front ([RAG in 2026: Architecture Shifts](https://medium.com/@elammarisoufiane/rag-in-2026-architecture-shifts-emerging-patterns-and-what-it-means-for-java-developers-6f2803e39787)) — worth adopting once there's more than one lecture per course to
search across.

### ⬜ Stage 10 — OCR + Python sidecar
Accept handwritten/scanned notes as input, not just text-native PDFs. Python
(pytesseract/easyocr) as a small sidecar service the Java backend calls, output text fed
into the existing Stage 1 pipeline unchanged.

### ⬜ Stage 11 — Frontend (React + TypeScript)
Upload UI, quiz-taking UI, exam-mode UI with timer, progress dashboard. Deferred this
late deliberately — the API contract from Stages 1–8 should be stable before building
against it.

### ⬜ Stage 12 (stretch) — Toy GPU/compute scheduler
The JD's GPU resource-scheduling requirement is a distinct, large infrastructure problem
(cluster scheduling, elastic allocation across training/inference/eval workloads) that
doesn't naturally fit inside an exam-prep app and isn't feasible to build for real without
an actual GPU cluster. As a **portfolio-only stand-in**, worth building a small job queue
that schedules this app's own batch LLM calls (quiz generation for a whole course,
bulk re-analysis) across a limited "worker pool" with priority/backpressure — demonstrates
the scheduling concepts (queueing, priority, elastic worker count) in miniature without
overclaiming GPU-cluster experience you can speak to honestly in an interview as "the
scheduling logic, at small scale" rather than as equivalent production GPU-fleet
experience.

## Working agreement going forward

Every stage: implement → compile-check → update DEVLOG.md (what/why/issues) → commit.
No stage is "done" without a passing compile at minimum; end-to-end runtime verification
against live Postgres + a real API key happens as soon as Docker is available on this
machine (currently the one open blocker noted in DEVLOG).
