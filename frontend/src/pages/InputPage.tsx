import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import DegradedBanner from '../components/DegradedBanner';
import { rememberTask } from '../lib/format';

interface Preset { productName: string; targetMarket: string; strategyDesc: string; }

const PRESETS: Preset[] = [
  {
    productName: '便携榨汁杯',
    targetMarket: 'DE',
    strategyDesc: '主打大容量与磁吸充电，走 Amazon DE 礼品/通勤两条线',
  },
  {
    productName: '车载磁吸手机支架',
    targetMarket: 'US',
    strategyDesc: 'MagSafe 兼容，主打驾驶员单手取放场景',
  },
  {
    productName: '智能宠物自动喂食器',
    targetMarket: 'FR',
    strategyDesc: '支持分餐定时 + App 提醒，主打养宠上班族',
  },
];

const MARKETS: Array<[string, string]> = [
  ['US', '🇺🇸 美国'], ['DE', '🇩🇪 德国'], ['FR', '🇫🇷 法国'],
  ['GB', '🇬🇧 英国'], ['JP', '🇯🇵 日本'], ['CA', '🇨🇦 加拿大'], ['AU', '🇦🇺 澳大利亚'],
];

export default function InputPage() {
  const nav = useNavigate();
  const [form, setForm] = useState<Preset>(PRESETS[0]);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const submit = async () => {
    if (!form.productName.trim() || !form.targetMarket) return;
    setBusy(true);
    setErr(null);
    try {
      const task = await api.createTask(form);
      if (!task?.id) throw new Error('后端未返回任务 id');
      rememberTask(task.id);
      nav(`/run/${task.id}`);
    } catch (e) {
      setErr(e instanceof Error ? e.message : '创建失败');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="container page">
      <div className="eyebrow">AI 跨境商业多元宇宙引擎 · Scene 3</div>
      <h1 className="title">在新市场推演一款产品 —— <span className="grad-text">亲历平行宇宙中的策略命运</span></h1>
      <p className="sub">
        输入目标市场与产品，引擎将生成 3 个时间宇宙与 5 个策略宇宙：先采集真实汇率与知识库事实，
        再对每个策略宇宙做 5 风暴压力测试与推演，给出可解释的存活率评级与定居建议。
      </p>

      <div className="mt-22 card">
        <h3>① 选择示例 或 自定义</h3>
        <div className="flex" style={{ flexWrap: 'wrap' }}>
          {PRESETS.map((p) => (
            <button
              key={p.productName}
              className="btn btn-ghost"
              style={{ padding: '9px 15px' }}
              onClick={() => { setForm(p); setErr(null); }}
            >
              {p.productName} · {p.targetMarket}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-14 card">
        <h3>② 推演参数</h3>
        <div className="field">
          <label>产品名称</label>
          <input
            className="input"
            value={form.productName}
            placeholder="如：便携榨汁杯"
            onChange={(e) => setForm({ ...form, productName: e.target.value })}
          />
        </div>
        <div className="field">
          <label>目标市场</label>
          <select
            className="select"
            value={form.targetMarket}
            onChange={(e) => setForm({ ...form, targetMarket: e.target.value })}
          >
            {MARKETS.map(([code, label]) => (
              <option key={code} value={code}>{label}</option>
            ))}
          </select>
        </div>
        <div className="field">
          <label>产品卖点 / 运营说明（可选，帮助引擎聚焦）</label>
          <textarea
            className="textarea"
            value={form.strategyDesc}
            placeholder="说明材质、价位段、主推渠道、差异化卖点…"
            onChange={(e) => setForm({ ...form, strategyDesc: e.target.value })}
          />
        </div>

        {err && <div className="degraded-banner" style={{ color: 'var(--r-f-tx)', borderColor: 'rgba(226,75,74,.3)' }}>
          <span className="tag">✗</span><span>{err}</span>
        </div>}

        <div className="flex-between" style={{ marginTop: 8 }}>
          <span className="muted-tag">约 20-40 秒完成推演（未配置模型 key 时自动降级，全流程仍可跑通）</span>
          <button className="btn btn-primary" onClick={submit} disabled={busy || !form.productName.trim()}>
            {busy ? '创建中…' : '🚀 开始推演'}
          </button>
        </div>
      </div>

      <div className="mt-14">
        <DegradedBanner reason="演示说明：配置 DASHSCOPE_API_KEY 后 LLM 全链路开启（采集/生成/推演/结算都调用百炼模型）；未配置时引擎自动降级为「真实数据 + 知识库规则 + 压力测试」，并用诚实标签标注每处数据来源。">
          <span className="muted">两种模式都可完整体验业务闭环；差异在证据来源标签：LLM 在线时含 r1_inferred 模型推断，降级时启发式规则标 heuristic、真 KB 政策引用标 kb/kb_stale。所有证据来源诚实标注，可逐条追溯。</span>
        </DegradedBanner>
      </div>
    </div>
  );
}
