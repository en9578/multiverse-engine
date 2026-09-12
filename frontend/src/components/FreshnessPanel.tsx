import type { MarketDataItemVO } from '../types/api';
import EmptyState from './EmptyState';

const CAT_LABEL: Record<string, string> = {
  EXCHANGE_RATE: '汇率',
  PAIN_POINT: '痛点基因库',
  POLICY: '政策法规',
  COMPETITOR_STRATEGY: '竞品策略',
  COMPETITOR: '竞品',
  REVIEW: '评论情绪',
  MARKET_SIZE: '市场规模',
  TAVILY: '实时搜索',
};
const SRC_LABEL: Record<string, string> = {
  frankfurter: '实时汇率数据源',
  kb: '内置知识库',
  kb_stale: '内置知识库（数据较旧）',
  tavily: '联网搜索（未配置）',
};
const STATUS_TEXT: Record<string, string> = {
  FRESH: '数据新鲜 · 全额计分',
  STALE: '数据较旧 · 扣分减半',
  MISSING: '暂无数据 · 仅 AI 推断',
};

function catLabel(c: string) { return CAT_LABEL[c] ?? c; }
function srcLabel(s: string) { return SRC_LABEL[s] ?? s; }

/** P3 采集面板：每类数据来源 + lastVerified + Fresh/Stale/Missing + 权重（诚实数据新鲜度可视化） */
export default function FreshnessPanel({ items, productName, targetMarket }: {
  items: MarketDataItemVO[] | null;
  productName?: string;
  targetMarket?: string;
}) {
  if (!items || items.length === 0) {
    return (
      <div>
        <p className="muted">数据采集结果会在此展示：每类数据标注来源、验证时间与新鲜度等级。</p>
        <EmptyState text="采集阶段尚未产生数据，或后端正在降级运行。" />
      </div>
    );
  }
  return (
    <div>
      <p className="muted">
        进入市场前先采集真实市场数据（{productName && targetMarket ? `${productName} @ ${targetMarket}` : '本任务'}）：
        数据新鲜则全额计分，数据较旧则扣分减半，缺数据时只能由 AI 凭经验推断。
      </p>
      <div>
        <div className="fresh-row head">
          <span>数据类别</span><span>来源</span><span>新鲜度</span><span>说明</span><span>状态</span>
        </div>
        {items.map((it, i) => (
          <div className="fresh-row" key={`${it.category}-${i}`}>
            <span style={{ fontWeight: 600 }}>{catLabel(it.category)}</span>
            <span className="muted" style={{ fontSize: 12 }}>{srcLabel(it.source)}</span>
            <span className="muted-tag">
              {it.lastVerified || '—'}
              {it.freshnessTtlDays ? ` · 保鲜期 ${it.freshnessTtlDays} 天` : ''}
            </span>
            <span className="muted" style={{ fontSize: 12.5 }} title={it.display}>{it.display}</span>
            <span>
              <i className={`dot ${(it.freshnessStatus || '').toLowerCase()}`} />
              {STATUS_TEXT[it.freshnessStatus] ?? it.freshnessStatus}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
