import { useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTaskPolling } from '../hooks/useTaskPolling';
import { useTask } from '../hooks/useTask';
import StagePipeline from '../components/StagePipeline';
import FreshnessPanel from '../components/FreshnessPanel';
import DegradedBanner from '../components/DegradedBanner';
import { STAGE_LABEL, rememberTask } from '../lib/format';

export default function RunPage() {
  const { taskId } = useParams();
  const nav = useNavigate();
  const poll = useTaskPolling(taskId!);
  const { task, collected } = useTask(taskId);

  // DONE → 自动进入星图
  useEffect(() => {
    if (poll.done && taskId) nav(`/task/${taskId}/stars`, { replace: true });
  }, [poll.done, taskId, nav]);

  const stageLabel = poll.progress?.currentStage ? (STAGE_LABEL[poll.progress.currentStage] ?? poll.progress.currentStage) : '准备中';

  useEffect(() => { rememberTask(taskId); }, [taskId]);

  return (
    <div className="container page">
      <div className="eyebrow">实时推演</div>
      <h1 className="title" style={{ fontSize: 26 }}>
        {task?.productName ?? '…'} <span className="grad-text">@ {task?.targetMarket ?? '…'}</span>
        <span className="badge" style={{ marginLeft: 12, verticalAlign: 'middle' }}>任务 #{taskId}</span>
      </h1>

      <div className="mt-22 card">
        <h3>编排状态</h3>
        <StagePipeline status={poll.status} stage={poll.progress?.currentStage ?? null} progress={poll.progress?.overallProgress ?? null} />
        <div className="mt-14 flex">
          {poll.failed
            ? <span className="badge" style={{ color: 'var(--r-f-tx)', borderColor: 'rgba(226,75,74,.4)' }}>✗ 任务失败</span>
            : poll.done
              ? <span className="badge" style={{ color: 'var(--r-a-tx)' }}>✓ 全部宇宙已推演，正在进入星图…</span>
              : <span className="badge">当前阶段：{stageLabel} …</span>}
          {poll.error && <span className="muted-tag">{poll.error}</span>}
        </div>
        <div className="mt-14 muted-tag">
          编排链路：数据采集 → 生成 3 时间宇宙 + 5 策略宇宙 → 逐宇宙推演（评分+评级） → 结算定居建议。
          首次运行较慢属正常（每阶段含模型重试退避）。可先切到“决策”页或稍候自动跳转星图。
        </div>
      </div>

      {task?.status === 'FAILED' && (
        <div className="mt-14 card">
          <h3>失败详情</h3>
          {(task.errors ?? []).map((e, i) => (
            <div key={i} className="muted">宇宙 #{e.universeIndex}: {e.message}</div>
          ))}
          <div className="mt-14">
            <button className="btn btn-ghost" onClick={() => nav(`/run/${taskId}`)}>重新开始</button>
          </div>
        </div>
      )}

      <div className="mt-14">
        <DegradedBanner reason="诚实标注：下方采集结果来自真实数据源（frankfurter 汇率 + 本地 KB 知识库）。绿色=新鲜、琥珀=已过期降权、红色=缺数据（此时推演只能凭策略先验 + 压力测试）。" />
      </div>

      <div className="mt-14 card">
        <h3>📡 数据采集新鲜度（P3 数据源）</h3>
        <FreshnessPanel items={collected?.items ?? null} productName={task?.productName} targetMarket={task?.targetMarket} />
      </div>

      {poll.status === 'COLLECTING' && (
        <div className="mt-14 card">
          <h3>🗺️ 待生成宇宙</h3>
          <p className="muted">正在采集市场事实；完成后将生成 8 个平行宇宙（3 时间 × 5 策略）。</p>
        </div>
      )}
    </div>
  );
}
