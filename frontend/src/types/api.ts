// API 契约 1:1（依据后端 Result<T> / UniverseVO / CollectedDataVO / DecisionVO 实际 JSON）

export type TaskStatus =
  | 'CREATED' | 'COLLECTING' | 'GENERATING' | 'EXPLORING'
  | 'SETTLING' | 'DONE' | 'FAILED';

export type Dimension = 'TIME' | 'STRATEGY';
/** '' = 未推演（TIME 宇宙恒为空）；A/B/C/D/F 走 UniverseRater */
export type Rating = 'A' | 'B' | 'C' | 'D' | 'F' | '';
export type FreshnessStatus = 'FRESH' | 'STALE' | 'MISSING';
export type EvidenceSource = 'kb' | 'kb_stale' | 'r1_inferred' | 'frankfurter' | 'tavily' | string;

export interface ApiResult<T> {
  code: number;
  message: string;
  data: T | null;
}

// ---------- Task / Progress ----------

export interface TaskError {
  universeIndex: number;
  message: string;
  degraded: boolean;
}

export interface TaskVO {
  id: number;
  requestId: string;
  status: TaskStatus;
  overallProgress: number | null;
  productName: string;
  targetMarket: string;
  universeCount: number | null;
  exploredCount: number | null;
  failedCount: number | null;
  confidence: string | null;
  gmtCreate: string;
  errors: TaskError[] | null;
}

export interface UniverseProgress {
  index: number;
  subState: string;
  rating: string;
  degraded: boolean;
}

export interface ProgressVO {
  taskId: number;
  status: TaskStatus;
  overallProgress: number;
  currentStage: string;
  universeProgress: UniverseProgress[] | null;
}

// ---------- Universe ----------

export interface RuleEvidence {
  ruleId: string;
  input: string;
  output: string;
  weight: number;
  source: EvidenceSource;
}

export interface EvolutionData {
  finalScore: number | null;
  llmScore: number | null;
  ruleScore: number | null;
  survivalRate: number | null;
  overallSurvival: number | null;
  reasoning: string | null;
  evidences: RuleEvidence[] | null;
}

export interface StrategyPackage {
  dimension: Dimension;
  universeName?: string;
  description?: string;
  // STRATEGY 维度
  pricingStrategy?: string;
  sellingPointStrategy?: string;
  positioningStrategy?: string;
  // TIME 维度
  timePoint?: string;
  lifecycleStage?: string;
  opportunityType?: string;
  opportunityWindow?: string;
  urgency?: string;
  [k: string]: unknown;
}

export interface StressTestVO {
  storm: string;
  survivalRate: number | null;
  weakestLink: string | null;
  fixSuggestion: string | null;
}

export interface UniverseWeatherVO {
  weather: string;
  searchSignal: string;
  sentimentSignal: string;
  priceSignal: string;
  policySignal: string;
  forecast7d: string;
  forecast30d: string;
  forecast90d: string;
}

export interface CompetitorReactionVO {
  competitorName: string;
  reactionType: string;
  probability: number | null;
  impact: string;
  source: EvidenceSource;
  evidence: string;
}

/** 列表端点为瘦版（detail 字段空）；detail 端点为富版 */
export interface UniverseVO {
  id: number;
  taskId: number;
  universeIndex: number;
  dimension: Dimension | null;
  rating: string; // Rating
  subState: string | null;
  survivalRate: number | null;
  strategyPackage: string | null; // JSON string，需 parseJson
  evolutionData: EvolutionData | null; // 仅 detail 富版返回；null=未推演
  geneDefects: string[]; // 富版
  stressTests: StressTestVO[]; // 富版
  weather: UniverseWeatherVO | null; // 富版
  competitorReactions: CompetitorReactionVO[]; // 富版
  confidence: number | null;
  reply?: string | null; // 仅 explore 端点返回
  sessionId?: string | null; // 仅 explore 端点返回
}

// ---------- CollectedData (FreshnessPanel) ----------

export interface MarketDataItemVO {
  category: string;
  source: string;
  lastVerified: string;
  freshnessTtlDays: number | null;
  freshnessStatus: FreshnessStatus;
  weight: number | null;
  display: string; // 后端已拼好的展示文案
  rawData: Record<string, unknown> | null;
}

export interface CollectedDataVO {
  taskId: number;
  productName: string;
  targetMarket: string;
  items: MarketDataItemVO[];
}

// ---------- Decision ----------

/** 决策 payload：自动决策(rationale) / 手动决策(source=manual_confirm) 两种形态，字段宽松可选 */
export interface DecisionPayload {
  selectedUniverseId?: number;
  selectedUniverseIndex?: number;
  rationale?: string;
  expectedProfit?: number | null;
  antiFragilePortfolio?: unknown[];
  confidence?: number;
  source?: string;
  strategyPackage?: string;
}

export interface DecisionVO {
  taskId: number;
  universeId: number;
  decisionData: string | null; // JSON string
  confidence: number | null;
  isConfirmed: boolean;
}

// ---------- 前端自有 ----------

export type NavTab = 'input' | 'run' | 'stars' | 'decision';
