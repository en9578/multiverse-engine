import type { StrategyPackage } from '../types/api';

/** 安全解析后端 JSON-string 字段（strategyPackage / decisionData），坏串返回 null */
export function parseJson<T>(raw: string | null | undefined): T | null {
  if (!raw) return null;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

export function parseStrategyPackage(raw: string | null | undefined): StrategyPackage | null {
  const p = parseJson<StrategyPackage>(raw);
  return p && typeof p === 'object' && !Array.isArray(p) ? p : null;
}

/** 策略宇宙完整三元组标签（5 组合唯一）；TIME 宇宙返回时间点标签 */
export function comboLabel(sp: StrategyPackage | null): string {
  if (!sp) return '未知策略';
  if (sp.dimension === 'TIME') {
    const t = sp.timePoint || sp.lifecycleStage || sp.opportunityType;
    return `⏳ ${t ?? '时间宇宙'}`;
  }
  const p = sp.pricingStrategy || '—';
  const s = sp.sellingPointStrategy || '—';
  const pos = sp.positioningStrategy || '—';
  if (p === '—' && s === '—' && pos === '—') return sp.universeName ?? '策略宇宙';
  return `${p} · ${s} · ${pos}`;
}

export const STAGE_ORDER = ['COLLECTING', 'GENERATING', 'EXPLORING', 'SETTLING', 'DONE'] as const;

export const STAGE_LABEL: Record<string, string> = {
  CREATED: '已创建',
  COLLECTING: '采集',
  GENERATING: '生成宇宙',
  EXPLORING: '推演',
  SETTLING: '结算',
  DONE: '完成',
  FAILED: '失败',
};

/** 是否处于降级（无 LLM）模式：evolutionData.llmScore === null 且存在 ruleScore */
export function isDegraded(ev: { llmScore: number | null; ruleScore: number | null } | null | undefined): boolean {
  return !!ev && ev.llmScore === null && ev.ruleScore !== null;
}

export function pct(n: number | null | undefined, digits = 0): string {
  if (n === null || n === undefined || Number.isNaN(n)) return '—';
  return `${(n * 100).toFixed(digits)}%`;
}

export function score(n: number | null | undefined): string {
  if (n === null || n === undefined || Number.isNaN(n)) return '—';
  return n.toFixed(1);
}

/** 记录最近一次任务 id（供无 taskId 参数的路由如 /decision 兜底） */
export function rememberTask(taskId: string | number | undefined | null) {
  if (taskId !== undefined && taskId !== null && taskId !== '') {
    try { localStorage.setItem('mb_last_task', String(taskId)); } catch { /* ignore */ }
  }
}
export function lastTaskId(): string | null {
  try { return localStorage.getItem('mb_last_task'); } catch { return null; }
}
