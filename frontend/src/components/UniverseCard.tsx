import type { UniverseVO } from '../types/api';
import { comboLabel, parseStrategyPackage, pct } from '../lib/format';
import RatingBadge from './RatingBadge';
import { useNavigate } from 'react-router-dom';

/** 星图卡片（瘦版 UniverseVO）：评级 + 存活率条 + 策略组合标签 */
export default function UniverseCard({ u }: { u: UniverseVO }) {
  const nav = useNavigate();
  const sp = parseStrategyPackage(u.strategyPackage);
  const rated = !!u.rating;
  const rateCls = rated ? `rating-${u.rating}` : 'rating-n';
  const surv = u.survivalRate ?? 0;

  const open = () => {
    nav(`/task/${u.taskId}/stars/${u.id}`, { state: { universe: u } });
  };

  return (
    <div className={`uni-card ${rateCls}`} onClick={open} role="button" tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && open()}>
      <div className="top">
        <RatingBadge rating={u.rating} />
        <div style={{ minWidth: 0 }}>
          <div className="name">{sp?.universeName ?? `宇宙 #${u.universeIndex}`}</div>
          <div className="combo">{comboLabel(sp)}</div>
        </div>
      </div>
      <div>
        <div className="flex-between" style={{ marginBottom: 5 }}>
          <span className="muted-tag">存活率</span>
          <span className="muted-tag mono">{rated ? pct(surv) : '未推演'}</span>
        </div>
        <div className="surv-bar">
          <div className={`surv-fill ${surv < 0.62 ? 'low' : ''}`} style={{ width: rated ? `${surv * 100}%` : '0%' }} />
        </div>
      </div>
      <div className="foot">
        <span className="pill">{u.subState === 'EVOLVED' ? '已推演' : u.subState === 'GENERATED' ? '已生成·未推演' : u.subState ?? '—'}</span>
        <span className="dim">点卡查看详情 →</span>
      </div>
    </div>
  );
}
