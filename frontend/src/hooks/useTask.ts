import { useCallback, useEffect, useState } from 'react';
import { api } from '../api/client';
import type { CollectedDataVO, TaskVO, UniverseVO } from '../types/api';

/** Run 页之后的共享数据加载：任务详情 + 星图列表 + 采集新鲜度总览 */
export function useTask(taskId: string | number | undefined) {  const [task, setTask] = useState<TaskVO | null>(null);
  const [universes, setUniverses] = useState<UniverseVO[]>([]);
  const [collected, setCollected] = useState<CollectedDataVO | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    if (taskId === undefined) return;
    setLoading(true);
    setError(null);
    try {
      const [t, us, c] = await Promise.all([
        api.getTask(taskId),
        api.getUniverses(taskId),
        api.getCollectedData(taskId).catch(() => null), // 采集总览失败不阻塞
      ]);
      setTask(t);
      setUniverses(us ?? []);
      setCollected(c);
    } catch (e) {
      setError(e instanceof Error ? e.message : '加载失败');
    } finally {
      setLoading(false);
    }
  }, [taskId]);

  useEffect(() => {
    reload();
  }, [reload]);

  return { task, universes, collected, loading, error, reload };
}
