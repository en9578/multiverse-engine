import { useNavigate, useParams } from 'react-router-dom';
import { useTask } from '../hooks/useTask';
import UniverseCard from '../components/UniverseCard';
import EmptyState from '../components/EmptyState';
import { rememberTask } from '../lib/format';
import { useEffect } from 'react';

export default function StarMapPage() {
  const { taskId } = useParams();
  const nav = useNavigate();
  const { task, universes, loading, error } = useTask(taskId);
  useEffect(() => { rememberTask(taskId); }, [taskId]);

  const time = universes.filter((u) => u.dimension === 'TIME');
  const strategy = universes.filter((u) => u.dimension === 'STRATEGY');

  return (
    <div className="container page">
      <div className="flex-between">
        <div>
          <div className="eyebrow">推演结果 · 星图</div>
          <h1 className="title" style={{ fontSize: 26 }}>
            {task?.productName ?? '平行宇宙'} <span className="grad-text">@ {task?.targetMarket ?? ''}</span>
            <span className="badge" style={{ marginLeft: 12 }}>共 {universes.length} 个宇宙</span>
          </h1>
        </div>
        <button className="btn btn-primary" onClick={() => nav(`/task/${taskId}/decision`)}>
          🧭 查看定居决策 →
        </button>
      </div>

      {error && <div className="mt-14 degraded-banner"><span>✗</span><span>{error}</span></div>}

      {loading && !universes.length && (
        <div className="mt-22"><EmptyState text={<><span className="spinner" style={{ display: 'inline-block', marginRight: 8 }} />加载中…</>} /></div>
      )}

      {!loading && universes.length === 0 && !error && (
        <div className="mt-22">
          <EmptyState text="该任务暂无宇宙。若任务仍在推演，请回到运行页等待完成后自动进入。" />
        </div>
      )}

      {strategy.length > 0 && (
        <>
          <div className="zone-title"><span className="bar" /><h2 style={{ fontSize: 18, margin: 0 }}>策略宇宙 <span className="muted-tag" style={{ fontWeight: 400 }}>同一产品 × 5 套策略，逐个推演评级</span></h2></div>
          <div className="uni-grid">
            {strategy.map((u) => <UniverseCard key={u.id} u={u} />)}
          </div>
        </>
      )}

      {time.length > 0 && (
        <>
          <div className="zone-title"><span className="bar" style={{ background: 'var(--border-strong)' }} /><h2 style={{ fontSize: 18, margin: 0 }}>时间宇宙 <span className="muted-tag" style={{ fontWeight: 400 }}>3 个时间切片的宏观场景（无逐宇宙评分）</span></h2></div>
          <div className="uni-grid">
            {time.map((u) => <UniverseCard key={u.id} u={u} />)}
          </div>
        </>
      )}
    </div>
  );
}
