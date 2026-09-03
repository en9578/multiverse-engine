import type {
  ApiResult, CollectedDataVO, DecisionVO, ProgressVO, TaskVO, UniverseVO,
} from '../types/api';

export class ApiError extends Error {
  code: number;
  constructor(code: number, message: string) {
    super(message);
    this.code = code;
    this.name = 'ApiError';
  }
}

/** fetch 封装：HTTP 非 2xx（@Valid 400）抛错；业务 code!==0 抛 ApiError；code===0 返回 data（可为 null） */
async function request<T>(path: string, init?: RequestInit): Promise<T | null> {
  let res: Response;
  try {
    res = await fetch(path, {
      headers: init?.body ? { 'Content-Type': 'application/json' } : undefined,
      ...init,
    });
  } catch {
    throw new ApiError(-1, '网络异常，请确认后端已启动（mvn spring-boot:run）');
  }
  if (!res.ok) {
    // 表单校验失败等：非 Result 体
    let detail = `HTTP ${res.status}`;
    try { const j = await res.json(); detail = j?.message || j?.error || detail; } catch { /* ignore */ }
    throw new ApiError(res.status, detail);
  }
  let json: ApiResult<T>;
  try {
    json = (await res.json()) as ApiResult<T>;
  } catch {
    throw new ApiError(-2, '响应不是合法 JSON');
  }
  if (json.code !== 0) throw new ApiError(json.code, json.message);
  return json.data;
}

function get<T>(path: string) {
  return request<T>(path);
}
function post<T>(path: string, body: unknown) {
  return request<T>(path, { method: 'POST', body: JSON.stringify(body) });
}

export interface CreateTaskInput {
  productName: string;
  targetMarket: string;
  strategyDesc?: string;
  requestId?: string;
}

export const api = {
  /** 创建任务并异步编排 */
  createTask: (input: CreateTaskInput) => post<TaskVO>('/api/v1/tasks', input),
  getTask: (taskId: number | string) => get<TaskVO>(`/api/v1/tasks/${taskId}`),
  getProgress: (taskId: number | string) => get<ProgressVO>(`/api/v1/tasks/${taskId}/progress`),
  /** 星图（瘦版列表） */
  getUniverses: (taskId: number | string) => get<UniverseVO[]>(`/api/v1/tasks/${taskId}/universes`),
  /** 详情（富版：evolutionData/stressTests/weather/reactions/defects） */
  getUniverse: (universeId: number | string) => get<UniverseVO>(`/api/v1/universes/${universeId}`),
  /** 对话式探索（无 key 时降级回复） */
  explore: (universeId: number | string, message: string, sessionId?: string) =>
    post<UniverseVO>(`/api/v1/universes/${universeId}/explore`, { message, sessionId }),
  /** 数据采集新鲜度总览（Fresh/Stale/Missing + source） */
  getCollectedData: (taskId: number | string) => get<CollectedDataVO>(`/api/v1/tasks/${taskId}/collected-data`),
  /** 提交定居决策 */
  submitDecision: (taskId: number | string, universeId: number | string) =>
    post<DecisionVO>('/api/v1/decisions', { taskId: Number(taskId), universeId: Number(universeId) }),
  /** 查询当前决策（可能无 → data null） */
  getDecision: (taskId: number | string) => get<DecisionVO>(`/api/v1/decisions/${taskId}`),
};
