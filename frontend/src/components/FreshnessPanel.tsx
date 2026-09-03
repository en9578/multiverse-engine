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
  frankfurter: 'frankfurter 真实汇率',
  kb: 'KB 知识库',
  kb_stale: 'KB 知识库(过期)',
  tavily: 'Tavily(未配置)',
};
const STATUS_TEXT: Record<string, string> = {
  FRESH: 'Fresh 全权重',
  STALE: 'Stale 降权 0.5x',
  MISSING: 'Missing 纯 R1',
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
        市场事实先于模型采集并落库：{productName && targetMarket ? `${productName} @ ${targetMarket}` : ''} ——
        Fresh 全权重 / Stale 降权 / Missing 时模型只能凭先验推断（R1）。
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
              {it.freshnessTtlDays ? ` · ${it.freshnessTtlDays}d` : ''}
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
