import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client';
import type { UniverseVO } from '../types/api';
import { comboLabel, isDegraded, parseStrategyPackage, pct, score, rememberTask } from '../lib/format';
import RatingBadge from '../components/RatingBadge';
import DegradedBanner from '../components/DegradedBanner';
import StormRadar from '../components/StormRadar';
import WeatherBadge from '../components/WeatherBadge';
import EvidenceList from '../components/EvidenceList';
import ExploreChat from '../components/ExploreChat';
import EmptyState from '../components/EmptyState';

export default function UniverseDetailPage() {
  const { universeId } = useParams();
  const { taskId } = useParams();
  const nav = useNavigate();
  const loc = useLocation();
  const fromList = (loc.state as { universe?: UniverseVO } | null)?.universe;

  const [detail, setDetail] = useState<UniverseVO | null>(null);
  const [err, setErr] = useState<string | null>(null);
  const [sessionId, setSessionId] = useState<string | null>(null);
  const [settling, setSettling] = useState(false);
  const [settled, setSettled] = useState(false);

  useEffect(() => {
    rememberTask(taskId);
    let dead = false;
    api.getUniverse(universeId!).then((d) => { if (!dead) setDetail(d); }).catch((e) => { if (!dead) setErr(e.message); });
    return () => { dead = true; };
  }, [universeId, taskId]);

  // 头部即时渲染用「list 优先」（detail 的 dimension 为 null）；主体一律等富版 detail
  const headerU = detail ?? fromList;
  const sp = parseStrategyPackage(headerU?.strategyPackage ?? null);
  const dimension = fromList?.dimension ?? detail?.dimension ?? sp?.dimension ?? null;

  const dv = detail;
  const evolved = !!dv && !!dv.rating && !!dv.evolutionData;
  const degraded = isDegraded(dv?.evolutionData ?? null);
  const ev = dv?.evolutionData ?? null;

  const settle = async () => {
    if (!dv) return;
    setSettling(true);
    try {
      await api.submitDecision(taskId!, dv.id);
      setSettled(true);
      nav(`/task/${taskId}/decision`);
    } catch (e) {
      setErr(e instanceof Error ? e.message : '提交失败');
      setSettling(false);
    }
  };

  return (
    <div className="container page">
      <div className="eyebrow">
        <button className="btn btn-ghost" style={{ padding: '6px 12px', fontSize: 13 }} onClick={() => nav(`/task/${taskId}/stars`)}>
          ← 返回星图
        </button>
        宇宙详情
      </div>

      {headerU && (
        <div className="flex" style={{ gap: 14, flexWrap: 'wrap' }}>
          <RatingBadge rating={headerU?.rating ?? ''} size={44} />
          <div>
            <h1 className="title" style={{ fontSize: 24, margin: 0 }}>{sp?.universeName ?? `宇宙 #${headerU?.universeIndex ?? ''}`}</h1>
            <div className="combo mt-8">{comboLabel(sp)}</div>
          </div>
        </div>
      )}

      {err && <div className="mt-14 degraded-banner"><span>✗</span><span>{err}</span></div>}

      {/* 主体：等富版 detail 到达 */}
      {!dv && !err && (
        <div className="mt-22"><EmptyState text={<><span className="spinner" style={{ display: 'inline-block', marginRight: 8 }} />加载演化详情…</>} /></div>
      )}

      {/* 未推演态（TIME 宇宙或空演化） */}
      {dv && !evolved && (
        <div className="mt-22">
          <div className="card">
            <h3>{dimension === 'TIME' ? '⏳ 时间宇宙：宏观场景（未推演）' : 'ℹ️ 该宇宙尚未推演'}</h3>
            <p className="muted">
              {dimension === 'TIME'
                ? '时间宇宙描述产品在不同时间切片的宏观机会，不逐宇宙评分（无评级与存活率）。下方为其场景策略包。'
                : '该宇宙暂无演化数据。'}
            </p>
            <div className="mt-14">
              <h4>策略 / 场景包</h4>
              <pre className="mono" style={{ whiteSpace: 'pre-wrap', background: 'var(--bg-2)', padding: 14, borderRadius: 12, border: '1px solid var(--border)' }}>
                {JSON.stringify(sp, null, 2)}
              </pre>
            </div>
          </div>
        </div>
      )}

      {/* 已推演：评分总览 */}
      {dv && evolved && ev && (
        <>
          <div className="mt-22 card">
            <div className="flex-between">
              <h3>推演评分总览</h3>
              {degraded && <span className="badge" style={{ color: 'var(--amber-soft)', borderColor: 'rgba(240,165,46,.4)' }}>⚙ 兜底评分模式</span>}
            </div>
            <div className="stat-grid">
              <div className="stat"><div className="num">{score(ev.finalScore)}</div><div className="lbl">综合得分 / 100</div></div>
              <div className="stat"><div className="num">{pct(dv.survivalRate)}</div><div className="lbl">90 天存活率</div></div>
              <div className="stat"><div className="num plain">{ev.llmScore !== null ? score(ev.llmScore) : '—'}</div><div className="lbl">LLM 模型分</div></div>
              <div className="stat"><div className="num plain">{ev.ruleScore !== null ? score(ev.ruleScore) : '—'}</div><div className="lbl">规则/先验分</div></div>
            </div>
            <div className="mt-14">
              <h4 style={{ margin: '0 0 8px' }}>推演结论（reasoning）</h4>
              <p className="muted" style={{ margin: 0 }}>{ev.reasoning || '—'}</p>
            </div>
          </div>

          {degraded && (
            <div className="mt-14">
              <DegradedBanner reason="本宇宙按「策略画像先验 + 5 风暴压力融合」评分（市场事实缺失、规则无法扣分）。配置 DASHSCOPE_API_KEY 后将改为 LLM 与规则引擎双通道交叉验证。">
                <span className="muted">评分构成：0.5×策略画像先验 + 0.3×压力平均存活 + 0.2×最差风暴存活。下方证据链可逐条追溯。</span>
              </DegradedBanner>
            </div>
          )}

          <div className="mt-14 card">
            <h3>🌀 5 风暴压力测试</h3>
            <StormRadar tests={dv.stressTests ?? null} />
          </div>

          <div className="mt-14">
            <WeatherBadge weather={dv.weather ?? null} />
          </div>

          <div className="mt-14 card">
            <h3>🧾 可解释证据链 <span className="muted-tag" style={{ fontWeight: 400 }}>每条规则的扣分/加成均可追溯</span></h3>
            <EvidenceList evidences={ev.evidences} />
          </div>

          <div className="mt-14 card">
            <h3>⚔️ 竞品关联反应</h3>
            {(dv.competitorReactions ?? []).length === 0
              ? <EmptyState text="暂无竞品反应记录（该数据由模型生成，未配置 key 时为空）。" />
              : (dv.competitorReactions ?? []).map((r, i) => (
                <div className="evidence" key={i}>
                  <span className="rid">{r.competitorName}</span>
                  <div className="muted" style={{ fontSize: 12.5 }}>{r.reactionType} · 概率 {pct(r.probability)} · {r.impact}</div>
                </div>
              ))}
          </div>

          <div className="mt-14 card">
            <h3>🧬 基因缺陷</h3>
            {(dv.geneDefects ?? []).length === 0
              ? <EmptyState text="该宇宙无基因缺陷标注（由模型识别，未配置 key 时为空）。" />
              : (dv.geneDefects ?? []).map((d, i) => <div key={i} className="muted">{d}</div>)}
          </div>

          <div className="mt-14 card">
            <h3>🗂️ 策略包原始数据</h3>
            <pre className="mono" style={{ whiteSpace: 'pre-wrap', background: 'var(--bg-2)', padding: 14, borderRadius: 12, border: '1px solid var(--border)', maxHeight: 260, overflow: 'auto' }}>
              {JSON.stringify(sp, null, 2)}
            </pre>
          </div>
        </>
      )}

      {/* 对话式探索 */}
      <div className="mt-14 card">
        <h3>💬 穿越向导 · 对话式探索</h3>
        <ExploreChat universeId={universeId!} sessionId={sessionId} onSession={setSessionId} />
      </div>

      {/* 定居 CTA */}
      {dv && evolved && (
        <div className="mt-14 card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 14, flexWrap: 'wrap' }}>
          <div>
            <h4 style={{ margin: 0 }}>🧭 定居该宇宙</h4>
            <div className="muted-tag mt-8">提交后引擎将把此宇宙作为「首选策略」，可到决策页查看完整定居建议与反脆弱组合。</div>
          </div>
          <button className="btn btn-primary" onClick={settle} disabled={settling || settled}>
            {settling ? '提交中…' : settled ? '已提交 ✓' : '定居此宇宙'}
          </button>
        </div>
      )}
    </div>
  );
}
