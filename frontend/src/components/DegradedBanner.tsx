/** 诚实降级横幅：LLM key 未配置 / 模型调用降级时展示 */
export default function DegradedBanner({ reason, children }: { reason: string; children?: React.ReactNode }) {
  return (
    <div className="degraded-banner">
      <span className="tag">⚠️ 降级模式</span>
      <div>
        <div>{reason}</div>
        {children}
      </div>
    </div>
  );
}
