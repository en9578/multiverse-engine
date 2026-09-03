import type { RuleEvidence } from '../types/api';
import EmptyState from './EmptyState';

function srcCls(source: string): string {
  if (source === 'kb') return 'src-tag src-kb';
  if (source === 'kb_stale') return 'src-tag src-stale';
  if (source === 'r1_inferred') return 'src-tag src-r1';
  return 'src-tag src-kb';
}
function srcText(source: string): string {
  if (source === 'kb') return 'KB 规则';
  if (source === 'kb_stale') return 'KB 过期';
  if (source === 'r1_inferred') return '模型推断';
  return source;
}

/** 可解释推演证据链（每条：ruleId + input/output + 来源徽标，kb紫/过期琥珀/模型推断青） */
export default function EvidenceList({ evidences }: { evidences: RuleEvidence[] | null }) {
  if (!evidences || evidences.length === 0) {
    return <EmptyState text="该宇宙尚无推演证据（未推演或数据缺失）。" />;
  }
  return (
    <div>
      {evidences.map((e, i) => (
        <div className="evidence" key={`${e.ruleId}-${i}`}>
          <span className={srcCls(e.source)} style={{ marginTop: 2 }}>{srcText(e.source)}</span>
          <div style={{ flex: 1 }}>
            <span className="rid">{e.ruleId}</span>
            <span className="dim" style={{ marginLeft: 8 }}>w={e.weight}</span>
            <div className="muted" style={{ fontSize: 12.5 }}>{e.input || '—'}</div>
          </div>
          <span className="mono" style={{ color: Number(e.output) < 0 ? '#f07774' : '#5Dcaa5' }}>
            {Number(e.output) === 0 ? '0' : e.output}
          </span>
        </div>
      ))}
    </div>
  );
}
