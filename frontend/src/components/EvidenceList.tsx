import { useState } from 'react';
import type { RuleEvidence } from '../types/api';
import EmptyState from './EmptyState';

/** 来源徽标：界面白话文案 + 悬停解释（面向非技术用户） */
const SRC_META: Record<string, { text: string; cls: string; tip: string }> = {
  kb: { text: '知识库依据', cls: 'src-tag src-kb', tip: '数据来自内置的政策/市场知识库，真实可查，全权重计分' },
  kb_stale: { text: '知识库·数据较旧', cls: 'src-tag src-stale', tip: '知识库里有这条记录但已超过保鲜期，该条扣分按一半力度计' },
  r1_inferred: { text: 'AI 模型推断', cls: 'src-tag src-r1', tip: '大模型基于已采集的市场事实推演得出，供决策参考' },
  heuristic: { text: '经验规则', cls: 'src-tag src-heu', tip: '平台经验阈值判断（未引用真实知识库数据），仅供参考' },
};

function srcMeta(source: string) {
  return SRC_META[source] ?? { text: source, cls: 'src-tag src-heu', tip: '' };
}

/** 扣分/得分语义化：负→扣分(红)，0→不扣分(灰)，正→加分(绿)；非数字（如 AI 推演长文）返回 null 不显示 */
function impact(output: string): { text: string; color: string } | null {
  if (output == null || output === '') return null;
  const n = Number(output);
  if (Number.isNaN(n)) return null;
  const v = Math.abs(n) % 1 === 0 ? String(Math.abs(n)) : Math.abs(n).toFixed(1);
  if (n === 0) return { text: '不扣分', color: '#8a94a6' };
  if (n < 0) return { text: `扣 ${v} 分`, color: '#f07774' };
  return { text: `+${v} 分`, color: '#5Dcaa5' };
}

/** 排序键：扣分/加分绝对值大的排前面；0 分随后；AI 推演长文（非数值）垫底 */
function magnitude(e: RuleEvidence): number {
  const n = Number(e.output);
  return Number.isNaN(n) ? -1 : Math.abs(n);
}

/** 可解释证据链（白话版）：来源徽标(悬停有解释) + 中文标题 + 人话原因 + 影响分；原始判定数据默认折叠 */
export default function EvidenceList({ evidences }: { evidences: RuleEvidence[] | null }) {
  const [openIdx, setOpenIdx] = useState<number | null>(null);
  if (!evidences || evidences.length === 0) {
    return <EmptyState text="该宇宙尚无推演证据（未推演或数据缺失）。" />;
  }
  const sorted = [...evidences].sort((a, b) => magnitude(b) - magnitude(a));
  return (
    <div>
      <p className="muted" style={{ fontSize: 12.5, margin: '0 0 10px' }}>
        每一分扣减都有依据：下面按影响从大到小列出全部判定项，鼠标放到标签上可看数据来源说明。
      </p>
      {sorted.map((e, i) => {
        const imp = impact(e.output);
        const meta = srcMeta(e.source);
        const main = (e.description ?? e.input) || '—';
        const hasDetail = !!e.input && e.input !== main;
        return (
          <div className="evidence" key={`${e.ruleId}-${i}`}>
            <span className={meta.cls} title={meta.tip} style={{ marginTop: 2 }}>{meta.text}</span>
            <div style={{ flex: 1 }}>
              <span className="rid" title={e.ruleId}>{e.label ?? e.ruleId}</span>
              <div style={{ fontSize: 13, marginTop: 2 }}>{main}</div>
              {hasDetail && (
                <>
                  <button
                    className="btn btn-ghost"
                    style={{ padding: '0 6px', fontSize: 11.5, marginTop: 4 }}
                    onClick={() => setOpenIdx(openIdx === i ? null : i)}
                  >
                    {openIdx === i ? '收起 ▴' : '原始判定数据 ▾'}
                  </button>
                  {openIdx === i && (
                    <pre className="mono" style={{
                      whiteSpace: 'pre-wrap', fontSize: 11, margin: '6px 0 0',
                      background: 'var(--bg-2)', padding: 8, borderRadius: 8, border: '1px solid var(--border)',
                    }}>
                      {`${e.ruleId}（权重 ${e.weight}）：${e.input}`}
                    </pre>
                  )}
                </>
              )}
            </div>
            {imp && <span className="mono" style={{ color: imp.color, flex: 'none' }}>{imp.text}</span>}
          </div>
        );
      })}
    </div>
  );
}
