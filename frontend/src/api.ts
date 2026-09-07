import type {
  AgentChatResponse,
  EvaluationResult,
  ExamResultResponse,
  ExamSessionResponse,
  LectureKnowledge,
  LectureResponse,
  QuizAttemptResponse,
  QuizResponse,
  StudentResponse,
} from './types'

// Requests go to relative /api/... paths - Vite's dev server proxies these to the Spring
// Boot backend (see vite.config.ts), so the backend needs no CORS configuration at all.

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, init)
  const text = await res.text()
  if (!res.ok) {
    throw new Error(`${init?.method ?? 'GET'} ${path} failed: ${res.status} ${text}`)
  }
  // Some endpoints (e.g. POST /index) return 200 with an empty body, not 204 - checking
  // for empty text rather than only status 204 covers both (a bug caught by actually
  // clicking the button in the browser, not by type-checking).
  if (!text) {
    return undefined as T
  }
  return JSON.parse(text) as T
}

export const api = {
  listLectures: () => request<LectureResponse[]>('/api/lectures'),

  getLecture: (id: number) => request<LectureResponse>(`/api/lectures/${id}`),

  uploadLecture: (file: File, title: string, kind: 'pdf' | 'scan') => {
    const form = new FormData()
    form.append('file', file)
    form.append('title', title)
    const path = kind === 'pdf' ? '/api/lectures' : '/api/lectures/upload-scan'
    return request<LectureResponse>(path, { method: 'POST', body: form })
  },

  analyzeLecture: (id: number) =>
    request<LectureKnowledge>(`/api/lectures/${id}/analyze`, { method: 'POST' }),

  getKnowledge: (id: number) => request<LectureKnowledge>(`/api/lectures/${id}/knowledge`),

  indexLecture: (id: number) =>
    request<void>(`/api/lectures/${id}/index`, { method: 'POST' }),

  searchLecture: (id: number, q: string) =>
    request<{ query: string; matches: string[] }>(
      `/api/lectures/${id}/search?q=${encodeURIComponent(q)}`,
    ),

  getEvaluation: (lectureId: number) =>
    request<EvaluationResult>(`/api/lectures/${lectureId}/evaluation`),

  getStudentEvaluation: (studentId: number, lectureId: number) =>
    request<EvaluationResult>(`/api/students/${studentId}/lectures/${lectureId}/evaluation`),

  generateQuiz: (lectureId: number, count: number) =>
    request<QuizResponse>(`/api/lectures/${lectureId}/quiz?count=${count}`, { method: 'POST' }),

  getQuiz: (quizId: number) => request<QuizResponse>(`/api/quizzes/${quizId}`),

  submitQuiz: (quizId: number, answers: { questionId: number; selectedChoiceIndex: number }[], studentId?: number) =>
    request<QuizAttemptResponse>(`/api/quizzes/${quizId}/submit`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ studentId, answers }),
    }),

  startExam: (lectureId: number, count: number, timeLimitMinutes: number) =>
    request<ExamSessionResponse>(
      `/api/lectures/${lectureId}/exam?count=${count}&timeLimitMinutes=${timeLimitMinutes}`,
      { method: 'POST' },
    ),

  getExam: (sessionId: number) => request<ExamSessionResponse>(`/api/exams/${sessionId}`),

  submitExam: (sessionId: number, answers: { questionId: number; selectedChoiceIndex: number }[]) =>
    request<ExamResultResponse>(`/api/exams/${sessionId}/submit`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ answers }),
    }),

  createStudent: (displayName: string) =>
    request<StudentResponse>('/api/students', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ displayName }),
    }),

  chatWithAgent: (message: string) =>
    request<AgentChatResponse>('/api/agent/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message }),
    }),
}
