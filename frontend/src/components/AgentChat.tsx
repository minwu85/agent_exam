import { useState } from 'react'
import { api } from '../api'

interface Message {
  role: 'user' | 'agent'
  text: string
}

export function AgentChat() {
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [busy, setBusy] = useState(false)

  async function handleSend() {
    const text = input.trim()
    if (!text) return
    setMessages((m) => [...m, { role: 'user', text }])
    setInput('')
    setBusy(true)
    try {
      const res = await api.chatWithAgent(text)
      setMessages((m) => [...m, { role: 'agent', text: res.reply }])
    } catch (err) {
      setMessages((m) => [...m, { role: 'agent', text: `Error: ${err instanceof Error ? err.message : String(err)}` }])
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="card chat-card">
      <h2>Ask the study agent</h2>
      <p className="muted">
        It can look up a lecture's key concepts, search a lecture's indexed text, generate a quiz,
        or check a student's own progress — try "what does lecture 1 cover?" or "quiz me on lecture 1".
      </p>
      <div className="chat-log">
        {messages.map((m, i) => (
          <p key={i} className={`chat-msg ${m.role}`}><strong>{m.role === 'user' ? 'You' : 'Agent'}:</strong> {m.text}</p>
        ))}
      </div>
      <div className="button-row">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSend()}
          placeholder="Ask about a lecture, or ask to be quizzed…"
        />
        <button onClick={handleSend} disabled={busy || !input.trim()}>
          {busy ? '…' : 'Send'}
        </button>
      </div>
    </div>
  )
}
