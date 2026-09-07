import { useState } from 'react'
import { api } from '../api'
import type { QuizAttemptResponse, QuizResponse } from '../types'

export function QuizPlayer({
  quiz,
  studentId,
  onClose,
}: {
  quiz: QuizResponse
  studentId: number | null
  onClose: () => void
}) {
  const [answers, setAnswers] = useState<Record<number, number>>({})
  const [result, setResult] = useState<QuizAttemptResponse | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const allAnswered = quiz.questions.every((q) => answers[q.id] !== undefined)

  async function handleSubmit() {
    setBusy(true)
    setError(null)
    try {
      const payload = quiz.questions.map((q) => ({ questionId: q.id, selectedChoiceIndex: answers[q.id] }))
      const attempt = await api.submitQuiz(quiz.id, payload, studentId ?? undefined)
      setResult(attempt)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  if (result) {
    return (
      <div className="card">
        <h2>Quiz result: {result.score} / {result.total}</h2>
        {result.results.map((r) => (
          <div key={r.questionId} className={`result-item ${r.correct ? 'correct' : 'incorrect'}`}>
            <p><strong>{r.topic}</strong>: {r.prompt}</p>
            <p>Your answer: {r.choices[r.selectedChoiceIndex]} {r.correct ? '✓' : '✗'}</p>
            {!r.correct && <p>Correct answer: {r.choices[r.correctChoiceIndex]}</p>}
            <p className="muted">{r.explanation}</p>
          </div>
        ))}
        <button onClick={onClose}>Back to lecture</button>
      </div>
    )
  }

  return (
    <div className="card">
      <h2>Practice quiz</h2>
      {quiz.questions.map((q, i) => (
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
        <button onClick={handleSubmit} disabled={!allAnswered || busy}>
          {busy ? 'Submitting…' : 'Submit answers'}
        </button>
        <button onClick={onClose} className="secondary">Cancel</button>
      </div>
    </div>
  )
}
