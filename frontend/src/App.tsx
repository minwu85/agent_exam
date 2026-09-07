import { useEffect, useState } from 'react'
import './App.css'
import { api } from './api'
import { AgentChat } from './components/AgentChat'
import { ExamPlayer } from './components/ExamPlayer'
import { LectureDetail } from './components/LectureDetail'
import { LectureList } from './components/LectureList'
import { QuizPlayer } from './components/QuizPlayer'
import { UploadForm } from './components/UploadForm'
import type { ExamSessionResponse, LectureResponse, QuizResponse } from './types'

type Mode = { kind: 'browse' } | { kind: 'quiz'; quiz: QuizResponse } | { kind: 'exam'; session: ExamSessionResponse }

const STUDENT_ID_KEY = 'examAgent.studentId'
const STUDENT_NAME_KEY = 'examAgent.studentName'

function App() {
  const [lectures, setLectures] = useState<LectureResponse[]>([])
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [mode, setMode] = useState<Mode>({ kind: 'browse' })
  const [studentId, setStudentId] = useState<number | null>(() => {
    const stored = localStorage.getItem(STUDENT_ID_KEY)
    return stored ? Number(stored) : null
  })
  const [studentName, setStudentName] = useState(() => localStorage.getItem(STUDENT_NAME_KEY) ?? '')
  const [loadError, setLoadError] = useState<string | null>(null)

  useEffect(() => {
    refreshLectures()
  }, [])

  async function refreshLectures() {
    try {
      const list = await api.listLectures()
      setLectures(list)
      if (selectedId === null && list.length > 0) setSelectedId(list[0].id)
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : String(err))
    }
  }

  async function handleCreateStudent() {
    if (!studentName.trim()) return
    const student = await api.createStudent(studentName.trim())
    setStudentId(student.id)
    localStorage.setItem(STUDENT_ID_KEY, String(student.id))
    localStorage.setItem(STUDENT_NAME_KEY, studentName.trim())
  }

  function handleLectureUpdated(updated: LectureResponse) {
    setLectures((ls) => ls.map((l) => (l.id === updated.id ? updated : l)))
  }

  const selectedLecture = lectures.find((l) => l.id === selectedId) ?? null

  return (
    <div className="app">
      <header>
        <h1>Exam Agent</h1>
        <div className="student-box">
          {studentId ? (
            <span>Signed in as <strong>{studentName}</strong> (id {studentId})</span>
          ) : (
            <>
              <input
                placeholder="Your name (optional, for personalized evaluation)"
                value={studentName}
                onChange={(e) => setStudentName(e.target.value)}
              />
              <button onClick={handleCreateStudent} disabled={!studentName.trim()}>Set name</button>
            </>
          )}
        </div>
      </header>

      {loadError && <p className="error">Could not reach the backend: {loadError}</p>}

      {mode.kind === 'quiz' && (
        <QuizPlayer quiz={mode.quiz} studentId={studentId} onClose={() => setMode({ kind: 'browse' })} />
      )}
      {mode.kind === 'exam' && (
        <ExamPlayer session={mode.session} onClose={() => setMode({ kind: 'browse' })} />
      )}

      {mode.kind === 'browse' && (
        <div className="layout">
          <aside>
            <UploadForm onUploaded={(l) => { setLectures((ls) => [...ls, l]); setSelectedId(l.id) }} />
            <div className="card">
              <h2>Lectures</h2>
              <LectureList lectures={lectures} selectedId={selectedId} onSelect={setSelectedId} />
            </div>
          </aside>
          <main>
            {selectedLecture ? (
              <LectureDetail
                lecture={selectedLecture}
                studentId={studentId}
                onLectureUpdated={handleLectureUpdated}
                onQuizGenerated={(quiz) => setMode({ kind: 'quiz', quiz })}
                onExamStarted={(session) => setMode({ kind: 'exam', session })}
              />
            ) : (
              <p className="muted">Upload or select a lecture to get started.</p>
            )}
            <AgentChat />
          </main>
        </div>
      )}
    </div>
  )
}

export default App
