import type { LectureResponse } from '../types'

export function LectureList({
  lectures,
  selectedId,
  onSelect,
}: {
  lectures: LectureResponse[]
  selectedId: number | null
  onSelect: (id: number) => void
}) {
  if (lectures.length === 0) {
    return <p className="muted">No lectures uploaded yet.</p>
  }

  return (
    <ul className="lecture-list">
      {lectures.map((l) => (
        <li key={l.id} className={l.id === selectedId ? 'selected' : ''} onClick={() => onSelect(l.id)}>
          <div className="lecture-title">{l.title}</div>
          <div className="lecture-meta">
            {l.textLength.toLocaleString()} chars
            {l.analyzed && <span className="badge">analyzed</span>}
            {l.indexed && <span className="badge">indexed</span>}
          </div>
        </li>
      ))}
    </ul>
  )
}
