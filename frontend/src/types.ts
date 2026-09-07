// Mirrors the backend's DTO records (see backend/src/main/java/com/examagent/dto/).
// Kept as one file since the backend contract is small enough that a generated client
// would be more ceremony than value at this size.

export interface LectureResponse {
  id: number
  title: string
  originalFilename: string
  uploadedAt: string
  textLength: number
  analyzed: boolean
  indexed: boolean
}

export interface KeyConcept {
  topic: string
  importance: string
  summary: string
}

export interface LectureKnowledge {
  learningGoals: string[]
  keyConcepts: KeyConcept[]
  keyTerms: string[]
  commonMistakes: string[]
  examTopics: string[]
}

export interface QuestionView {
  id: number
  topic: string
  prompt: string
  choices: string[]
}

export interface QuizResponse {
  id: number
  lectureId: number
  createdAt: string
  questions: QuestionView[]
}

export interface AnswerResultView {
  questionId: number
  topic: string
  prompt: string
  choices: string[]
  selectedChoiceIndex: number
  correctChoiceIndex: number
  correct: boolean
  explanation: string
}

export interface QuizAttemptResponse {
  id: number
  quizId: number
  score: number
  total: number
  submittedAt: string
  results: AnswerResultView[]
}

export interface TopicStats {
  topic: string
  totalAnswered: number
  correctAnswered: number
  accuracy: number
}

export interface EvaluationResult {
  weakTopics: string[]
  strongTopics: string[]
  overallReadiness: string
  recommendations: string[]
  topicStats: TopicStats[]
}

export interface ExamSessionResponse {
  id: number
  lectureId: number
  quizId: number
  status: 'IN_PROGRESS' | 'SUBMITTED' | 'EXPIRED'
  startedAt: string
  deadline: string
  timeLimitSeconds: number
  questions: QuestionView[]
}

export interface ExamResultResponse {
  examSessionId: number
  attempt: QuizAttemptResponse
  readinessEstimate: string
}

export interface StudentResponse {
  id: number
  displayName: string
  createdAt: string
}

export interface AgentChatResponse {
  reply: string
}
