import type { TaskStatus } from '../types/api';
import { STAGE_LABEL } from '../lib/format';

interface Props {
  status: TaskStatus | null;
  stage: string | null; // currentStage: COLLECTING/GENERATING/EXPLORING/SETTLING
  progress: number | null;
}

const SEQUENCE = ['COLLECTING', 'GENERATING', 'EXPLORING', 'SETTLING'] as const;

/** 阶段灯：完成阶段 purple、当前 teal、失败 red */
export default function StagePipeline({ status, stage, progress }: Props) {
  const failed = status === 'FAILED';
  const done = status === 'DONE';
  const idx = stage ? SEQUENCE.indexOf(stage as (typeof SEQUENCE)[number]) : -1;

  const chipCls = (s: (typeof SEQUENCE)[number]) => {
    const pos = SEQUENCE.indexOf(s);
    if (failed) return 'stage-chip fail';
    if (done) return 'stage-chip done';
    if (idx >= pos) return 'stage-chip active';
    return 'stage-chip';
  };

  return (
    <div>
      <div className="stages">
        <span className="stage-chip done">✓ 已创建</span>
        {SEQUENCE.map((s) => (
          <span key={s} className={chipCls(s)}>{STAGE_LABEL[s] ?? s}</span>
        ))}
        <span className={`stage-chip ${done ? 'done' : failed ? 'fail' : ''}`}>
          {done ? '✓ 完成' : failed ? '✗ 失败' : '…'}
        </span>
      </div>
      {progress !== null && progress !== undefined && (
        <div className="mt-8 flex" style={{ gap: 10 }}>
          <div style={{ flex: 1 }}>
            <div className="surv-bar">
              <div className="surv-fill" style={{ width: `${Math.max(0, Math.min(100, progress))}%` }} />
            </div>
          </div>
          <span className="muted-tag mono">{progress}%</span>
        </div>
      )}
    </div>
  );
}
