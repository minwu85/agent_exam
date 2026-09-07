import { useEffect, useState } from 'react'
import { api } from '../api'
import type { ExamResultResponse, ExamSessionResponse } from '../types'

export function ExamPlayer({ session, onClose }: { session: ExamSessionResponse; onClose: () => void }) {
  const [answers, setAnswers] = useState<Record<number, number>>({})
  const [result, setResult] = useState<ExamResultResponse | null>(null)
  const [remainingSeconds, setRemainingSeconds] = useState(() => secondsUntil(session.deadline))
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (result) return
    const timer = setInterval(() => setRemainingSeconds(secondsUntil(session.deadline)), 1000)
    return () => clearInterval(timer)
  }, [session.deadline, result])

  async function handleSubmit() {
    setBusy(true)
    setError(null)
    try {
      const payload = session.questions.map((q) => ({ questionId: q.id, selectedChoiceIndex: answers[q.id] ?? -1 }))
      const examResult = await api.submitExam(session.id, payload)
      setResult(examResult)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  if (result) {
    const a = result.attempt
    return (
      <div className="card">
        <h2>Exam result: {a.score} / {a.total} — readiness: {result.readinessEstimate}</h2>
        {a.results.map((r) => (
          <div key={r.questionId} className={`result-item ${r.correct ? 'correct' : 'incorrect'}`}>
            <p><strong>{r.topic}</strong>: {r.prompt}</p>
            <p>Your answer: {r.selectedChoiceIndex >= 0 ? r.choices[r.selectedChoiceIndex] : '(unanswered)'} {r.correct ? '✓' : '✗'}</p>
            {!r.correct && <p>Correct answer: {r.choices[r.correctChoiceIndex]}</p>}
            <p className="muted">{r.explanation}</p>
          </div>
        ))}
        <button onClick={onClose}>Back to lecture</button>
      </div>
    )
  }

  const expired = remainingSeconds <= 0

  return (
    <div className="card">
      <h2>Timed exam — {formatTime(remainingSeconds)} remaining</h2>
      {expired && <p className="error">Time's up — submit now, no further answers will be graded after this.</p>}
      {session.questions.map((q, i) => (
        <div key={q.id} className="question">
          <p><strong>Q{i + 1}.</strong> [{q.topic}] {q.prompt}</p>
          {q.choices.map((choice, ci) => (
            <label key={ci} className="choice">
              <input
                type="radio"
                name={`q${q.id}`}
                checked={answers[q.id] === ci}
                onChange={() => setAnswers((a) => ({ ...a, [q.id]: ci }))}
              />
              {choice}
            </label>
          ))}
        </div>
      ))}
      {error && <p className="error">{error}</p>}
      <div className="button-row">
        <button onClick={handleSubmit} disabled={busy}>
          {busy ? 'Submitting…' : 'Submit exam'}
        </button>
      </div>
    </div>
  )
}

function secondsUntil(deadline: string): number {
  return Math.floor((new Date(deadline).getTime() - Date.now()) / 1000)
}

function formatTime(totalSeconds: number): string {
  const s = Math.max(0, totalSeconds)
  const m = Math.floor(s / 60)
  const rem = s % 60
  return `${m}:${rem.toString().padStart(2, '0')}`
}
