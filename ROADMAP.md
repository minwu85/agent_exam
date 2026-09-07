# Roadmap

The forward-looking step list for this project: what's built, what's next, and how each
stage maps to the target job's required skills. Read alongside [DEVLOG.md](DEVLOG.md),
which records *why* each decision was made and what went wrong along the way — this file
is the plan, DEVLOG is the diary.

## Target skills → build stage mapping

| Target requirement | Where it's covered | Status |
|---|---|---|
| Generative AI tools, prompt engineering | Stage 2 (Knowledge Extraction Agent) | ✅ Done |
| Python + OCR/LLM experimentation | Stage 10 (OCR) | ⬜ Not started |
| SQL | PostgreSQL throughout | ✅ In use |
| JavaScript / React / Streamlit-style demos | Stage 10 (Frontend) | ⬜ Not started |
| LLM/Agent project experience, using large-model capability to solve problems | Stages 2–8 | ✅ Done (2,3,4,5,6,7,8 all built) |
| Agent framework, tool calling | Stage 5 (Agent Tools) | ✅ Done |
| Harness Engineering | Stage 5 + Stage 6 | ✅ Done |
| Sandbox / simulation environment | Stage 7 (Exam Simulation) | ✅ Done |
| Personalized memory | Stage 8 (Student Memory & Personalization) | ✅ Done |
| Skills (as in: reusable agent capabilities) | Stage 5 | ⬜ Not started |
| Automatic evaluation system | Stage 6 (Evaluation Agent) | ✅ Done |
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
- **Python:** introduced only at Stage 10 for OCR experimentation — the JD explicitly wants
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

### ✅ Stage 3 — Quiz Agent (generate practice questions)
`QuizGenerationAgent` reads a lecture's `LectureKnowledge` (not raw text — cheaper, more
targeted) and generates multiple-choice questions with a correct answer and explanation
tied back to a specific key concept. `POST /api/lectures/{id}/quiz`, `GET /api/quizzes/{id}`.
This is the "provide quiz for user to do" half of your stated goal. Details: [DEVLOG.md](DEVLOG.md#stage-3--quiz-agent).

### ✅ Stage 4 — Student answers + automatic marking
Student submits answers (`POST /api/quizzes/{id}/submit`); `QuizMarkingService` scores
them deterministically (the correct choice is already known from Stage 3 — no LLM call
needed to mark multiple-choice) and returns per-question correctness + explanation.
This closes the loop: transcript → key pointers → quiz → self-test → score. Details: [DEVLOG.md](DEVLOG.md#stage-4--student-answers--automatic-marking).

### ✅ Stage 5 — Agent framework & tools (Harness Engineering)
`LearningAgent` reasons over which **tool** to call (`getLectureKnowledge`,
`generateQuiz`, via `LectureTools`) using Spring AI's tool-calling support, instead of a
controller wiring services together in a fixed order. `getStudentHistory`/
`identifyWeakTopics` tools are deferred to Stages 6/8 — no student/history concept exists
yet to back them. `POST /api/agent/chat`. This is the concrete deliverable behind the
JD's "Agent framework" and "toolchains" line — current production harnesses are described
as more than a prompt: system prompt + tools + runtime + persistent state + feedback loop
([LangChain: Anatomy of an Agent Harness](https://www.langchain.com/blog/the-anatomy-of-an-agent-harness); [Databricks: What is an AI Agent Harness?](https://www.databricks.com/blog/ai-harness)). Details: [DEVLOG.md](DEVLOG.md#stage-5--agent-framework--tools-harness-engineering).

### ✅ Stage 6 — Evaluation Agent (automatic evaluation system)
Aggregates per-topic accuracy across every quiz attempt on a lecture (deterministic, no
LLM) and uses `EvaluationAgent` to classify weak/strong topics, an overall readiness
label, and recommendations — not just a percentage score. `GET /api/lectures/{id}/evaluation`.
Directly answers the JD's "automatic evaluation system" line. Currently scoped per-lecture
rather than per-student (no account concept exists yet — see Stage 8). Details: [DEVLOG.md](DEVLOG.md#stage-6--evaluation-agent-automatic-evaluation-system).

### ✅ Stage 7 — Exam Simulation (sandbox / simulation environment)
A timed, controlled "exam mode" wrapping the existing Quiz/QuizAttempt pipeline rather
than duplicating it: fixed question count, time limit, single submission, deterministic
readiness estimate. `POST /api/lectures/{id}/exam`, `GET /api/exams/{id}`,
`POST /api/exams/{id}/submit`. This is the JD's "sandbox and simulation environment"
concept applied to an exam rather than a code sandbox — same idea (a bounded, controlled
environment a session runs inside of), applied to the domain this project is actually
about. Details: [DEVLOG.md](DEVLOG.md#stage-7--exam-simulation-sandbox--simulation-environment).

### ✅ Stage 8 — Personalized memory
A minimal `Student` identity (no auth) that `QuizAttempt`s can optionally be scoped to;
`EvaluationService` and a new `getStudentHistory` agent tool retrieve one student's own
per-topic performance rather than the class-wide aggregate from Stage 6.
`POST /api/students`, `GET /api/students/{id}/lectures/{id}/evaluation`. Matches the
"selective memory" pattern from current practice — distilled per-topic facts, retrieved
on demand by a tool, not a full history dumped into every prompt ([MachineLearningMastery: architectural patterns for persistent memory in AI agents](https://machinelearningmastery.com/5-architectural-patterns-for-persistent-memory-and-state-in-ai-agents/)). Feeding this
history back into *quiz generation* itself (so question selection adapts) is not yet
done — currently only readable via evaluation/the chat agent. Details: [DEVLOG.md](DEVLOG.md#stage-8--personalized-memory).

### ✅ Stage 9 — RAG / Knowledge base (pgvector)
Lecture text is chunked and embedded into pgvector using a local, in-JVM ONNX embedding
model (no external API/key needed) via `LectureIndexingService`; a new `searchLectureContent`
agent tool lets `LearningAgent` pull a specific verbatim passage on demand — additive to,
not a replacement for, Stage 2/3's capped-transcript approach. `POST /api/lectures/{id}/index`,
`GET /api/lectures/{id}/search?q=`. Matches the *agentic* RAG pattern from 2026 practice —
the agent decides whether/what/when to retrieve, rather than always retrieving once up
front ([RAG in 2026: Architecture Shifts](https://medium.com/@elammarisoufiane/rag-in-2026-architecture-shifts-emerging-patterns-and-what-it-means-for-java-developers-6f2803e39787)). Cross-lecture search (once there's more than one lecture per
course) is a deliberate follow-up, not done yet — see [DEVLOG.md](DEVLOG.md#stage-9--rag--knowledge-base-pgvector).

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
