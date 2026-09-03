import type { StressTestVO } from '../types/api';
import { pct } from '../lib/format';
import EmptyState from './EmptyState';

const SIZE = 320;
const CX = SIZE / 2;
const CY = SIZE / 2 + 6;
const R = 108;

/** 按风暴存活率自绘 5 轴雷达（无外部图表库；值=该风暴下存活率，越高越好） */
export default function StormRadar({ tests }: { tests: StressTestVO[] | null }) {
  if (!tests || tests.length === 0) {
    return <EmptyState text="该宇宙无压力测试记录（风暴测试仅策略宇宙生成）。" />;
  }
  const n = tests.length;
  const pts = (valueOf: (i: number) => number, rScale = (v: number) => (v / 100) * R) => {
    const arr: string[] = [];
    for (let i = 0; i < n; i++) {
      const ang = -Math.PI / 2 + (i * 2 * Math.PI) / n;
      const v = valueOf(i);
      const r = rScale(v);
      arr.push(`${(CX + r * Math.cos(ang)).toFixed(1)},${(CY + r * Math.sin(ang)).toFixed(1)}`);
    }
    return arr.join(' ');
  };
  const rings = [25, 50, 75, 100];
  const val = (i: number) => Math.max(0, Math.min(100, (tests[i].survivalRate ?? 0.5) * 100));

  return (
    <div style={{ display: 'flex', gap: 18, flexWrap: 'wrap', alignItems: 'center' }}>
      <svg width={SIZE} height={SIZE} viewBox={`0 0 ${SIZE} ${SIZE}`} role="img" aria-label="风暴存活率雷达">
        {/* 网格环 */}
        {rings.map((ring) => (
          <polygon
            key={ring}
            points={pts(() => ring, (v) => (v / 100) * R)}
            fill="none"
            stroke="rgba(255,255,255,0.06)"
            strokeWidth={1}
          />
        ))}
        {/* 轴线 */}
        {Array.from({ length: n }).map((_, i) => {
          const ang = -Math.PI / 2 + (i * 2 * Math.PI) / n;
          const x = CX + R * Math.cos(ang);
          const y = CY + R * Math.sin(ang);
          return <line key={i} x1={CX} y1={CY} x2={x} y2={y} stroke="rgba(255,255,255,0.07)" strokeWidth={1} />;
        })}
        {/* 数据多边形 */}
        <polygon
          points={pts((i) => val(i))}
          fill="rgba(139,130,232,0.22)"
          stroke="#8B82E8"
          strokeWidth={2}
          strokeLinejoin="round"
        />
        {/* 数据点 + 标签 */}
        {tests.map((t, i) => {
          const ang = -Math.PI / 2 + (i * 2 * Math.PI) / n;
          const x = CX + (val(i) / 100) * R * Math.cos(ang);
          const y = CY + (val(i) / 100) * R * Math.sin(ang);
          const lx = CX + (R + 22) * Math.cos(ang);
          const ly = CY + (R + 22) * Math.sin(ang);
          const isWorst = val(i) === Math.min(...tests.map((_, j) => val(j)));
          return (
            <g key={i}>
              <circle cx={x} cy={y} r={4} fill={isWorst ? '#E24B4A' : '#5DCAA5'} stroke="rgba(0,0,0,0.4)" strokeWidth={1} />
              <text
                x={lx} y={ly}
                textAnchor={Math.abs(Math.cos(ang)) < 0.25 ? 'middle' : Math.cos(ang) > 0 ? 'start' : 'end'}
                dominantBaseline="middle"
                fill={isWorst ? '#f07774' : '#9b9a93'}
                fontSize={12}
              >
                {t.storm}{isWorst ? ' ⚠' : ''}
              </text>
            </g>
          );
        })}
      </svg>
      {/* 逐风暴明细 */}
      <div style={{ flex: 1, minWidth: 220 }}>
        {tests.map((t) => {
          const worst = t.survivalRate === Math.min(...tests.map((x) => x.survivalRate ?? 0));
          return (
            <div key={t.storm} style={{ padding: '7px 0', borderBottom: '1px solid var(--border)' }}>
              <div className="flex-between">
                <span style={{ fontSize: 13 }}>
                  {worst ? '🔴' : '🟢'} {t.storm}
                </span>
                <span className={`mono ${worst ? '' : 'muted'}`}>{pct(t.survivalRate)}</span>
              </div>
              <div className="muted" style={{ fontSize: 12, marginTop: 2 }}>{t.weakestLink || '—'}</div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
