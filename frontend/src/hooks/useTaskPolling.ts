import { useEffect, useRef, useState } from 'react';
import { api } from '../api/client';
import type { ProgressVO, TaskStatus } from '../types/api';

const INTERVAL_MS = 2000;
const MAX_POLL = 90; // 无 LLM key 时单任务约 20-40s，含重试上限兜底

interface State {
  progress: ProgressVO | null;
  status: TaskStatus | null;
  done: boolean;
  failed: boolean;
  error: string | null;
}

/**
 * 轮询任务进度直至 DONE / FAILED。
 * 后端 `/stream`(SSE) 为空壳，故用 setTimeout 链 + AbortController。
 */
export function useTaskPolling(taskId: number | string) {
  const [state, setState] = useState<State>({ progress: null, status: null, done: false, failed: false, error: null });
  const taskIdRef = useRef(taskId);
  const abortRef = useRef<AbortController | null>(null);

  useEffect(() => {
    taskIdRef.current = taskId;
    const controller = new AbortController();
    abortRef.current = controller;
    let timer: ReturnType<typeof setTimeout>;
    let attempts = 0;
    let cancelled = false;

    const tick = async () => {
      if (cancelled) return;
      attempts += 1;
      try {
        const progress = await api.getProgress(taskIdRef.current);
        if (cancelled) return;
        if (progress) {
          const failed = progress.overallProgress === -1 || progress.status === 'FAILED';
          const done = progress.status === 'DONE' || failed;
          setState({
            progress,
            status: progress.status,
            done: done && !failed,
            failed,
            error: null,
          });
          if (done) return; // 停止轮询
        }
      } catch (e) {
        if (cancelled) return;
        setState((s) => ({ ...s, error: e instanceof Error ? e.message : '轮询失败' }));
      }
      if (attempts >= MAX_POLL) {
        if (!cancelled) setState((s) => ({ ...s, error: '等待超时，请稍后刷新查看任务状态' }));
        return;
      }
      timer = setTimeout(tick, INTERVAL_MS);
    };

    tick();
    return () => {
      cancelled = true;
      controller.abort();
      if (timer) clearTimeout(timer);
    };
  }, [taskId]);

  return state;
}
