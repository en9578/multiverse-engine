import { useState } from 'react';
import { api } from '../api/client';

interface Msg {
  role: 'user' | 'assistant';
  content: string;
}

interface Props {
  universeId: number | string;
  sessionId: string | null;
  onSession: (sessionId: string) => void;
}

/** 对话式探索：无 LLM key 时后端返回降级文案（诚实提示） */
export default function ExploreChat({ universeId, sessionId, onSession }: Props) {
  const [msgs, setMsgs] = useState<Msg[]>([]);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);

  const send = async () => {
    const text = input.trim();
    if (!text || busy) return;
    setMsgs((m) => [...m, { role: 'user', content: text }]);
    setInput('');
    setBusy(true);
    try {
      const vo = await api.explore(universeId, text, sessionId ?? undefined);
      const reply = vo?.reply ?? '（向导无回复）';
      if (vo?.sessionId) onSession(vo.sessionId);
      setMsgs((m) => [...m, { role: 'assistant', content: reply }]);
    } catch (e) {
      setMsgs((m) => [...m, { role: 'assistant', content: e instanceof Error ? e.message : '对话失败' }]);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div>
      <p className="muted" style={{ marginTop: 0 }}>
        「穿越向导」：基于该宇宙的策略包与 90 天演化结果答疑。未配置模型 key 时向导会给出降级回复。
      </p>
      <div className="chat-box">
        {msgs.length === 0 && <div className="muted-tag">向向导提问，例如：这个宇宙最大的风险是什么？</div>}
        {msgs.map((m, i) => (
          <div key={i} className={`msg ${m.role}`}>{m.content}</div>
        ))}
        {busy && <div className="msg asst"><span className="spinner" style={{ display: 'inline-block', marginRight: 8 }} />思考中…</div>}
      </div>
      <div className="chat-input">
        <input
          className="input"
          value={input}
          placeholder="输入你的问题…"
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && send()}
        />
        <button className="btn btn-primary" onClick={send} disabled={busy || !input.trim()}>发送</button>
      </div>
    </div>
  );
}
