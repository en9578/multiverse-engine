# -*- coding: utf-8 -*-
"""
AI 跨境商业多元宇宙引擎 — 百炼 API 调用测试脚本
=================================================
用途：初赛加分项"已做 API 测试"的验证脚本。
覆盖多元宇宙引擎所需的四类核心百炼模型：
  1. Qwen 文本生成      → 评论基因检测（痛点聚类）
  2. 万相生图           → 策略包配图 / 宇宙演化可视化
  3. Qwen-VL 视觉理解   → 合规基因检测
  4. DeepSeek-R1 推理   → 格局推演（竞品关联反应 + 极端风暴存活）
"""

import os, time, requests
from pathlib import Path

try:
    import dashscope
    from dashscope import Generation, ImageSynthesis, MultiModalConversation
except ImportError:
    print("[FATAL] 未安装 dashscope，请先执行 pip install dashscope requests")
    raise SystemExit(1)

for _k in ["HTTP_PROXY","HTTPS_PROXY","ALL_PROXY","http_proxy","https_proxy","all_proxy","NO_PROXY","no_proxy","GIT_HTTP_PROXY","GIT_HTTPS_PROXY"]:
    os.environ.pop(_k, None)

dashscope.api_key = os.getenv("DASHSCOPE_API_KEY", "")
if not dashscope.api_key:
    print("[FATAL] 未检测到 DASHSCOPE_API_KEY 环境变量，请先设置你的百炼 API 密钥。")
    raise SystemExit(1)

OUT_DIR = Path(__file__).parent / "bailian_test_output"
OUT_DIR.mkdir(exist_ok=True)
results = []

def record(name, ok, detail):
    results.append((name, ok, detail))
    print(f"\n[{'PASS' if ok else 'FAIL'}] {name}\n  {detail}")

def test_qwen_gene():
    name = "1. Qwen 文本生成 (评论基因检测)"
    try:
        prompt = ("你是跨境评论基因检测专家。分析宠物饮水机竞品的差评，聚类高频痛点。"
            "对每个缺陷输出：缺陷名、频率(high/medium/low)、严重度(critical/major/minor)、"
            "典型竞品、改良方案。至少给出"漏水/水泵噪音/不易清洗"三个缺陷，用 JSON 输出。")
        resp = Generation.call(model="qwen-plus", messages=[{"role": "user", "content": prompt}], result_format="message")
        if resp.status_code == 200:
            text = resp.output.choices[0].message.content
            (OUT_DIR / "qwen_gene.txt").write_text(text, encoding="utf-8")
            record(name, True, f"生成 {len(text)} 字符")
        else:
            record(name, False, f"status={resp.status_code}")
    except Exception as e:
        record(name, False, f"异常: {e}")

def test_wanx_image():
    name = "2. 万相生图 (策略包配图)"
    try:
        prompt = ("A premium product hero image of a silent pet water dispenser, "
            "highlighting magnetic anti-leak valve and 304 stainless steel bowl, "
            "clean white background, soft studio lighting, e-commerce strategy package cover style, "
            "no text, no watermark, high detail.")
        resp = ImageSynthesis.call(model="wanx2.1-t2i-turbo", prompt=prompt, n=1, size="1024*1024")
        if resp.status_code == 200 and resp.output.results:
            r = requests.get(resp.output.results[0].url, timeout=60); r.raise_for_status()
            (OUT_DIR / "wanx_strategy.png").write_bytes(r.content)
            record(name, True, f"图片已保存 ({len(r.content)//1024} KB)")
        else:
            record(name, False, f"status={resp.status_code}")
    except Exception as e:
        record(name, False, f"异常: {e}")

def test_qwen_vl():
    name = "3. Qwen-VL 视觉理解 (合规基因检测)"
    try:
        prompt = ("你是跨境合规基因检测员。检查这张商品图是否存在违规基因："
            "1)文字或水印 2)医疗/功效暗示 3)侵权logo 4)裸露或敏感内容。")
        resp = MultiModalConversation.call(model="qwen-vl-plus",
            messages=[{"role": "user", "content": [{"image": "https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg"}, {"text": prompt}]}])
        if resp.status_code == 200:
            text = resp.output.choices[0].message.content
            text = text[0]["text"] if isinstance(text, list) else text
            (OUT_DIR / "vl_compliance.txt").write_text(str(text), encoding="utf-8")
            record(name, True, f"视觉理解成功")
        else:
            record(name, False, f"status={resp.status_code}")
    except Exception as e:
        record(name, False, f"异常: {e}")

def test_deepseek_r1():
    name = "4. DeepSeek-R1 推理 (格局推演)"
    try:
        prompt = ("你是多元宇宙格局推演引擎。场景：卖家采用静音磁吸防漏宠物饮水机、定价$29、主攻德国市场。"
            "推演 90 天后格局演化：1)竞品关联反应 2)5种极端风暴存活率 3)综合评级 A/B/C/D/F。")
        resp = Generation.call(model="deepseek-r1", messages=[{"role": "user", "content": prompt}], result_format="message")
        if resp.status_code == 200:
            msg = resp.output.choices[0].message
            (OUT_DIR / "r1_evolution.txt").write_text(
                f"=== 推理 ===\n{getattr(msg, 'reasoning_content', '') or ''}\n\n=== 结论 ===\n{msg.content or ''}", encoding="utf-8")
            record(name, True, f"推演成功")
        else:
            record(name, False, f"status={resp.status_code}")
    except Exception as e:
        record(name, False, f"异常: {e}")

def main():
    print("=" * 64)
    print("AI 跨境商业多元宇宙引擎 — 百炼 API 调用测试")
    print("=" * 64)
    t0 = time.time()
    test_qwen_gene(); test_wanx_image(); test_qwen_vl(); test_deepseek_r1()
    passed = sum(1 for _, ok, _ in results if ok)
    print(f"\n通过 {passed}/{len(results)} 项，耗时 {time.time()-t0:.1f}s")

if __name__ == "__main__":
    main()
