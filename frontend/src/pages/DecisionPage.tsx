import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from '../api/client';
import type { DecisionPayload, DecisionVO, UniverseVO } from '../types/api';
import { parseJson, parseStrategyPackage, pct, rememberTask, lastTaskId } from '../lib/format';
import RatingBadge from '../components/RatingBadge';
import EmptyState from '../components/EmptyState';
import DegradedBanner from '../components/DegradedBanner';

export default function DecisionPage() {
  const { taskId: taskIdParam } = useParams();
  const nav = useNavigate();
  const [taskId, setTaskId] = useState<string | null>(taskIdParam ?? null);
  const [decision, setDecision] = useState<DecisionVO | null>(null);
  const [universes, setUniverses] = useState<UniverseVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [confirming, setConfirming] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  // 裸路由 /decision 无 taskId → 兜底最近一次任务
  useEffect(() => {
    if (!taskId) setTaskId(lastTaskId());
  }, [taskId]);

  useEffect(() => {
    rememberTask(taskId);
    if (!taskId) { setLoading(false); return; }
    let dead = false;
    (async () => {
      try {
        const [dv, us] = await Promise.all([
          api.getDecision(taskId!),
          api.getUniverses(taskId!).catch(() => []),
        ]);
        if (dead) return;
        setDecision(dv);
        setUniverses(us ?? []);
      } catch (e) {
        if (!dead) setErr(e instanceof Error ? e.message : '加载失败');
      } finally {
        if (!dead) setLoading(false);
      }
    })();
    return () => { dead = true; };
  }, [taskId]);

  const payload = decision ? parseJson<DecisionPayload>(decision.decisionData) : null;
  const manual = payload && !('rationale' in payload) ? payload : null; // manual_confirm 决策无 rationale
  const selected = universes.find((u) => u.id === (decision?.universeId ?? payload?.selectedUniverseId));
  const selectedSp = parseStrategyPackage(selected?.strategyPackage ?? null);

  const confirm = async () => {
    if (!taskId || !decision) return;
    setConfirming(true);
    setErr(null);
    try {
      await api.submitDecision(taskId, decision.universeId);
      setDecision({ ...decision, isConfirmed: true });
    } catch (e) {
      setErr(e instanceof Error ? e.message : '确认失败');
    } finally {
      setConfirming(false);
    }
  };

  return (
    <div className="container page">
      <div className="eyebrow">结算</div>
      <h1 className="title" style={{ fontSize: 26 }}>🧭 定居决策 <span className="grad-text">—— 引擎为这场推演选定的首选宇宙</span></h1>

      {loading && <div className="mt-22"><EmptyState text={<><span className="spinner" style={{ display: 'inline-block', marginRight: 8 }} />加载决策…</>} /></div>}

      {err && <div className="mt-14 degraded-banner"><span>✗</span><span>{err}</span></div>}

      {!loading && !decision && !err && (
        <div className="mt-22">
          <EmptyState text="当前还没有决策。先完成一次推演（新建任务 → 等 DONE → 打开星图选择「定居此宇宙」），引擎会自动生成定居建议并在此展示。" />
          <div className="mt-22" style={{ textAlign: 'center' }}>
            <button className="btn btn-primary" onClick={() => nav('/')}>开始推演 →</button>
          </div>
        </div>
      )}

      {!loading && decision && (
        <>
          {/* 已选宇宙卡 */}
          <div className="mt-22 card">
            <div className="flex-between">
              <h3 style={{ marginBottom: 14 }}>首选宇宙</h3>
              {decision.isConfirmed
                ? <span className="badge" style={{ color: 'var(--r-a-tx)', borderColor: 'rgba(111,181,42,.4)' }}>✓ 已确认定居</span>
                : <span className="badge" style={{ color: 'var(--amber-soft)', borderColor: 'rgba(240,165,46,.4)' }}>待确认（自动建议）</span>}
            </div>
            <div className="flex" style={{ gap: 14, flexWrap: 'wrap' }}>
              <RatingBadge rating={selected?.rating ?? ''} size={44} />
              <div>
                <div style={{ fontSize: 16, fontWeight: 600 }}>
                  {selectedSp?.universeName ?? `宇宙 #${payload?.selectedUniverseIndex ?? decision.universeId}`}
                </div>
                <div className="muted-tag mt-8">
                  {selectedSp ? [selectedSp.pricingStrategy, selectedSp.sellingPointStrategy, selectedSp.positioningStrategy].filter(Boolean).join(' · ') : `宇宙 id=${decision.universeId}`}
                </div>
                {selected?.rating && (
                  <div className="mt-8">
                    <span className="badge">存活率 {pct(selected.survivalRate)}</span>
                    <span className="pill" style={{ marginLeft: 8 }}>评级 {selected.rating}</span>
                  </div>
                )}
              </div>
              {decision.isConfirmed && (
                <button className="btn btn-ghost" style={{ marginLeft: 'auto' }} onClick={() => nav(`/task/${taskId}/stars`)}>查看全部宇宙 →</button>
              )}
            </div>
          </div>

          {/* 自动决策理由（manual 决策则显示手动来源） */}
          {payload && 'rationale' in payload && payload.rationale && (
            <div className="mt-14 card">
              <h3>选择理由</h3>
              <p className="muted" style={{ margin: 0 }}>{payload.rationale}</p>
              {typeof payload.confidence === 'number' && (
                <div className="mt-8 flex"><span className="muted-tag">模型置信度</span>
                  <div style={{ flex: 1, maxWidth: 260 }}><div className="surv-bar"><div className="surv-fill" style={{ width: `${Math.round(payload.confidence * 100)}%` }} /></div></div>
                  <span className="mono muted-tag">{Math.round(payload.confidence * 100)}%</span>
                </div>
              )}
              {payload.expectedProfit != null && (
                <div className="mt-8 muted-tag">预期利润：{payload.expectedProfit}</div>
              )}
            </div>
          )}
          {manual && (
            <div className="mt-14 card">
              <h3>手动确认</h3>
              <p className="muted">你手动选择了宇宙 {manual.selectedUniverseId} 作为定居策略（未走引擎自动建议）。</p>
            </div>
          )}

          {/* 反脆弱组合 */}
          <div className="mt-14 card">
            <h3>🛡️ 反脆弱组合建议 <span className="muted-tag" style={{ fontWeight: 400 }}>多宇宙对冲，避免单一策略裸奔</span></h3>
            {!payload || !Array.isArray(payload.antiFragilePortfolio) || payload.antiFragilePortfolio.length === 0 ? (
              <EmptyState text="引擎在当前模式下未生成组合建议（该能力需模型编排开启；已提交宇宙即为主策略）。" />
            ) : (
              payload.antiFragilePortfolio.map((p, i) => <div key={i} className="muted">• {String(p)}</div>)
            )}
          </div>

          {!decision.isConfirmed && (
            <div className="mt-14 card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 14, flexWrap: 'wrap' }}>
              <div className="muted" style={{ flex: 1, minWidth: 220 }}>
                这是引擎自动选定的「评级与存活率综合最优」宇宙。若你认可该建议，点击确认锁定它作为最终定居决策；也可以回星图挑选其它宇宙手动定居。
              </div>
              <button className="btn btn-primary" onClick={confirm} disabled={confirming}>
                {confirming ? '确认中…' : '✓ 确认此定居决策'}
              </button>
            </div>
          )}

          {!decision.isConfirmed && (
            <div className="mt-14">
              <DegradedBanner reason="该建议由规则兜底生成（无模型 key）。配置 DASHSCOPE_API_KEY 后，结算将由 qwen3.8-max 交叉验证给出带模型理由的定居建议。" />
            </div>
          )}
        </>
      )}
    </div>
  );
}
