import { useState } from 'react'
import { api } from '../api'
import type { LectureResponse } from '../types'

export function UploadForm({ onUploaded }: { onUploaded: (lecture: LectureResponse) => void }) {
  const [title, setTitle] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [kind, setKind] = useState<'pdf' | 'scan'>('pdf')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!file || !title.trim()) return
    setBusy(true)
    setError(null)
    try {
      const lecture = await api.uploadLecture(file, title.trim(), kind)
      onUploaded(lecture)
      setTitle('')
      setFile(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="card" onSubmit={handleSubmit}>
      <h2>Upload a lecture</h2>
      <label>
        Title
        <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Lecture 6: Java Exceptions" />
      </label>
      <label className="radio-row">
        <span>Source</span>
        <label>
          <input type="radio" checked={kind === 'pdf'} onChange={() => setKind('pdf')} /> Text PDF
        </label>
        <label>
          <input type="radio" checked={kind === 'scan'} onChange={() => setKind('scan')} /> Scanned/handwritten image (OCR)
        </label>
      </label>
      <label>
        File
        <input
          type="file"
          accept={kind === 'pdf' ? 'application/pdf' : 'image/*'}
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
        />
      </label>
      <button type="submit" disabled={busy || !file || !title.trim()}>
        {busy ? 'Uploading…' : 'Upload'}
      </button>
      {error && <p className="error">{error}</p>}
    </form>
  )
}
