import { useState } from 'react'
import { api } from '../api'
import type { EvaluationResult, ExamSessionResponse, LectureKnowledge, LectureResponse, QuizResponse } from '../types'

type Busy = null | 'analyze' | 'index' | 'search' | 'evaluate' | 'quiz' | 'exam'

export function LectureDetail({
  lecture,
  studentId,
  onLectureUpdated,
  onQuizGenerated,
  onExamStarted,
}: {
  lecture: LectureResponse
  studentId: number | null
  onLectureUpdated: (lecture: LectureResponse) => void
  onQuizGenerated: (quiz: QuizResponse) => void
  onExamStarted: (session: ExamSessionResponse) => void
}) {
  const [knowledge, setKnowledge] = useState<LectureKnowledge | null>(null)
  const [evaluation, setEvaluation] = useState<EvaluationResult | null>(null)
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState<string[] | null>(null)
  const [quizCount, setQuizCount] = useState(5)
  const [examMinutes, setExamMinutes] = useState(20)
  const [busy, setBusy] = useState<Busy>(null)
  const [error, setError] = useState<string | null>(null)

  async function run<T>(which: Busy, fn: () => Promise<T>): Promise<T | null> {
    setBusy(which)
    setError(null)
    try {
      return await fn()
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
      return null
    } finally {
      setBusy(null)
    }
  }

  async function handleAnalyze() {
    const result = await run('analyze', () => api.analyzeLecture(lecture.id))
    if (result) {
      setKnowledge(result)
      onLectureUpdated({ ...lecture, analyzed: true })
    }
  }

  async function handleIndex() {
    const done = await run('index', () => api.indexLecture(lecture.id))
    if (done !== null) {
      onLectureUpdated({ ...lecture, indexed: true })
    }
  }

  async function handleSearch() {
    if (!searchQuery.trim()) return
    const result = await run('search', () => api.searchLecture(lecture.id, searchQuery.trim()))
    if (result) setSearchResults(result.matches)
  }

  async function handleEvaluate() {
    const result = await run('evaluate', () =>
      studentId ? api.getStudentEvaluation(studentId, lecture.id) : api.getEvaluation(lecture.id),
    )
    if (result) setEvaluation(result)
  }

  async function handleGenerateQuiz() {
    const quiz = await run('quiz', () => api.generateQuiz(lecture.id, quizCount))
    if (quiz) onQuizGenerated(quiz)
  }

  async function handleStartExam() {
    const session = await run('exam', () => api.startExam(lecture.id, quizCount, examMinutes))
    if (session) onExamStarted(session)
  }

  return (
    <div className="card">
      <h2>{lecture.title}</h2>
      <p className="muted">
        {lecture.originalFilename} · {lecture.textLength.toLocaleString()} chars
      </p>

      <div className="button-row">
        <button onClick={handleAnalyze} disabled={busy !== null}>
          {busy === 'analyze' ? 'Analyzing…' : lecture.analyzed ? 'Re-analyze' : 'Analyze (extract key concepts)'}
        </button>
        <button onClick={handleIndex} disabled={busy !== null}>
          {busy === 'index' ? 'Indexing…' : lecture.indexed ? 'Re-index' : 'Index for search'}
        </button>
        <button onClick={handleEvaluate} disabled={busy !== null}>
          {busy === 'evaluate' ? 'Evaluating…' : 'View evaluation'}
        </button>
      </div>

      {error && <p className="error">{error}</p>}

      {knowledge && (
        <section className="knowledge">
          <h3>Learning goals</h3>
          <ul>{knowledge.learningGoals.map((g, i) => <li key={i}>{g}</li>)}</ul>
          <h3>Key concepts</h3>
          <ul>
            {knowledge.keyConcepts.map((c, i) => (
              <li key={i}>
                <strong>{c.topic}</strong> <span className="badge">{c.importance}</span> — {c.summary}
              </li>
            ))}
          </ul>
          <h3>Key terms</h3>
          <p>{knowledge.keyTerms.join(', ')}</p>
          <h3>Common mistakes</h3>
          <ul>{knowledge.commonMistakes.map((m, i) => <li key={i}>{m}</li>)}</ul>
          <h3>Likely exam topics</h3>
          <ul>{knowledge.examTopics.map((t, i) => <li key={i}>{t}</li>)}</ul>
        </section>
      )}

      {evaluation && (
        <section className="evaluation">
          <h3>Evaluation — readiness: {evaluation.overallReadiness}</h3>
          <p>
            <strong>Weak topics:</strong> {evaluation.weakTopics.join(', ') || 'none'}
          </p>
          <p>
            <strong>Strong topics:</strong> {evaluation.strongTopics.join(', ') || 'none'}
          </p>
          <ul>{evaluation.recommendations.map((r, i) => <li key={i}>{r}</li>)}</ul>
          <table>
            <thead>
              <tr><th>Topic</th><th>Correct</th><th>Total</th><th>Accuracy</th></tr>
            </thead>
            <tbody>
              {evaluation.topicStats.map((s, i) => (
                <tr key={i}>
                  <td>{s.topic}</td><td>{s.correctAnswered}</td><td>{s.totalAnswered}</td>
                  <td>{Math.round(s.accuracy * 100)}%</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      )}

      <div className="search-box">
        <h3>Semantic search {!lecture.indexed && <span className="muted">(index first)</span>}</h3>
        <div className="button-row">
          <input value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} placeholder="e.g. what does the finally block do?" />
          <button onClick={handleSearch} disabled={busy !== null || !lecture.indexed}>
            {busy === 'search' ? 'Searching…' : 'Search'}
          </button>
        </div>
        {searchResults && (
          <ul>{searchResults.map((r, i) => <li key={i}><pre>{r}</pre></li>)}</ul>
        )}
      </div>

      <div className="practice-box">
        <h3>Practice</h3>
        <p className="muted">Requires the lecture to be analyzed first.</p>
        <label>
          Questions: <input type="number" min={1} max={20} value={quizCount} onChange={(e) => setQuizCount(Number(e.target.value))} />
        </label>
        <label>
          Exam time limit (minutes): <input type="number" min={1} max={180} value={examMinutes} onChange={(e) => setExamMinutes(Number(e.target.value))} />
        </label>
        <div className="button-row">
          <button onClick={handleGenerateQuiz} disabled={busy !== null || !lecture.analyzed}>
            {busy === 'quiz' ? 'Generating…' : 'Generate practice quiz'}
          </button>
          <button onClick={handleStartExam} disabled={busy !== null || !lecture.analyzed}>
            {busy === 'exam' ? 'Starting…' : 'Start timed exam'}
          </button>
        </div>
      </div>
    </div>
  )
}
