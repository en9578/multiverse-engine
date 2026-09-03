# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Java backend (dev mode, H2 in-memory, zero external deps) —— 复赛 Demo 一键运行
# 前端已打包进 src/main/resources/static → 打开 http://localhost:8080 即完整 SPA+API 整站
cd multiverse-engine
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 改前端后重新打包入库（评审机零 node 依赖，产物提交进 git）：
cd frontend && npm run build:static   # dist → multiverse-engine/src/main/resources/static/

# Verify
curl http://localhost:8080/actuator/health
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{"productName":"test","targetMarket":"DE"}'

# Python engine (skeleton)
cd multiverse-engine-python
pip install -r requirements.txt
uvicorn main:app --reload --port 8000

# Bailian model API test (4/4 passed)
python bailian_api_test.py   # requires DASHSCOPE_API_KEY env var
```

## Architecture

**Application**: AI 跨境商业多元宇宙引擎 — cross-border market simulator that models markets as parallel universes for strategy exploration. Contest entry for Alibaba Cloud Bailian Scene 3 (AI Market Insights).

**Decision: Monolith downgrade (in progress)** — removing the Python/FastAPI/LangGraph dual-service layer and consolidating into a single Java service with Spring AI Alibaba. See `downgrade-migration-plan.md` in the knowledge base. Java 业务逻辑已实现；Python 引擎仍为骨架，待下线。

**Java backend** (`multiverse-engine/`, Spring Boot 3.4.4 / Java 21):
- 4-layer: Controller → Service → Manager → DAO
- `MultiverseOrchestratorService` drives the state machine via `executeOrchestration()`
- State machine: `CREATED → COLLECTING → GENERATING → EXPLORING → SETTLING → DONE` (any state → `FAILED`), enforced by `TaskStatusEnum.nextStatus()`
- 3 thread pools: `multiverseExecutor`(4/8/200), `bailianExecutor`(2/4/50), `explorationExecutor`(2/4/100) — all with `MdcTaskDecorator` for traceId propagation
- Idempotency: `requestId` → `selectByRequestId()` hit returns cached result
- Circuit breaker: Resilience4j, 50% failure rate threshold
- 13 Flyway migrations (H2 dev / MySQL prod), 11 MyBatis mapper XMLs
- Error codes: `ErrorCodeEnum` (22 codes) including `BAILIAN_*`, `LLM_DEGRADED`, `RULE_ONLY_FALLBACK`
- Generation: `MultiverseGenerator` → 3 时间宇宙 + 5 策略宇宙（每个策略宇宙附着 关联反应 / 5 风暴压力测试 / 天气，落 `competitor_reaction` / `stress_test` / `universe_weather` 三表）

**Implemented**:
- P0 技术栈对齐：`BailianManagerImpl` 用 Spring AI Alibaba 1.1.2.0 重写（`StageEnum` 路由 + 重试/熔断/幂等 + `bailian_call_log` 落库）
- P1 五维宇宙：`MultiverseGenerator` + 5 个 builder（时间/策略/关联/极端/天气）+ `StressTestEngine` + `R1Enhancer`
- `MultiverseEngineImpl` 四阶段（collect/generate/explore/settle）+ `RuleEngineImpl`（规则扣分 + evidences `source` 标注）
- `UniverseRater`（score≥90→A、≥75→B、≥60→C、≥40→D、else F）、`Constants`、`StageEnum`、`TaskStatusEnum`
- 复赛 Demo 前端：`frontend/`（React + Vite + TS，HashRouter，仅 react/react-dom/react-router-dom 三依赖）。四页面 Input→Run→StarMap→Detail→Decision。`npm run build:static` 把 Vite dist 复制进 `multiverse-engine/src/main/resources/static/`（**提交进 git**）→ 评审机零 node 依赖，`mvn spring-boot:run` 单命令整站。详情聚合：STRATEGY detail 返回 `stressTests`(5 风暴)/`weather`/`competitorReactions`；无 LLM key 时全链路降级跑 DONE，5 策略宇宙按「策略画像先验(0.5) + 5 风暴平均(0.3) + 最差风暴(0.2)」差异化评分（A–D 分布，附 `RULE_DEGRADED_PRIOR` 证据链）；TIME 宇宙 `rating:""` 未推演；`GET /decisions` 无决策返回 `data:null`。

**Remaining stubs**:
- `OssManagerImpl` — returns fake URLs
- `BailianManagerImpl#generateVideo` / `#tts` — `not_implemented`（MVP 不含视频/语音）

**Python engine** (`multiverse-engine-python/`): FastAPI skeleton with 4 endpoints (`/health`, `/engine/tasks`, `/engine/tasks/{id}/state`, `/engine/tasks/{id}/resume`), in-memory dict store. LangGraph workflow is `TODO`. Likely to be removed per the downgrade decision.

**Frontend**: 复赛 SPA 在 `frontend/`（React+Vite+TS，详情见下方 Implemented「复赛 Demo 前端」），产物经 `build:static` 进 Spring static。仓库根 `index.html` 是**独立营销落地页**（纯静态展示，非 SPA 入口，与前端工程无关）。

## Documentation Convention

All project documentation (except `README.md`) lives in the Obsidian knowledge base at:
```
C:\dev\knowledge\project-document\multiverse-engine\
```
Do **not** put markdown docs in the project directory. Key docs in the knowledge base:
- `design.md` — full product design (17 chapters)
- `architecture.md` — system architecture (Alibaba Java spec compliant)
- `tech-solution.md` — tech stack decisions (monolith edition)
- `submission.md` — contest submission with version evolution appendix
- `judge-feedback-response.md` — judge feedback point-by-point response
- `downgrade-migration-plan.md` — monolith downgrade migration plan
- `demo-run-guide.md` — 复赛 Demo 使用说明（一键运行/页面导览/双模式/FAQ）
- `tech-stack-and-models.md` — 复赛技术架构 + 百炼模型路由/降级/API 契约口径
- `archive/` — deprecated docs (old project, dual-service editions)

## Key Technical Decisions

1. **Explainable reasoning, not black-box**: `RuleEngine` outputs `evidences` with `source: kb | r1_inferred` weight tags. deepseek-v4-pro Grounding (must cite KB data), qwen3.8-max cross-validation (temperature=0.0), direction-only prediction (no absolute values), backtesting >80% accuracy.
2. **Agent orchestration without LangGraph**: `TaskStatusEnum` state machine + `@Async` + `CompletableFuture.allOf` for universe fan-out (3 time + 5 strategy). `last_completed_stage` field + `/retry` endpoint for checkpoint recovery. `updateStatus` before SETTLING for human-in-the-loop.
3. **Model routing**: `StageEnum` → Bailian model: COLLECTING→qwen3.7-plus, GENERATING/EXPLORING→deepseek-v4-pro, SETTLING→qwen3.8-max；生图→wan2.7-image-pro、VL→qwen-vl-plus、向量化→text-embedding-v3（非文本模型走 `BailianManager` 独立方法，不经 `StageEnum`）。
   **API 端点（重要）**: 团队 key 为 token-plan 团队标准版，仅支持 **OpenAI 兼容协议**；基址由 `spring.ai.openai.base-url = https://token-plan.cn-beijing.maas.aliyuncs.com/compatible-mode` 配置（**不含 `/v1`**，Spring AI OpenAI starter 会自动拼接 `/v1/chat/completions`）。标准 `dashscope.aliyuncs.com` 对该 key 返回 401。`BailianManagerImpl` 走 Spring AI OpenAI starter 指向该基地址，key 通过 `DASHSCOPE_API_KEY` 环境变量注入（占位 `sk-dev-placeholder`/空白 → 方法入口短路抛 `LLM_DEGRADED`），勿写入代码/配置文件。
4. **Token budget**: ~47 Credits per task (V4-pro 5 universes ≈ 35), 25,000 Credits package ≈ 530+ tasks. Graded reasoning (core=V4-pro, secondary=rule/qwen3.7-plus), idempotency cache, context compression, budget cap with auto-degrade.
5. **Data freshness**: Tavily + frankfurter + KB dual-source, TTL tiers (Fresh/Stale 0.5x/Missing), `last_verified` visible. Keepa API (€49/mo) designed but disabled for Demo — price history data from manual KB entry.
6. **Recall layer (LLM-Wiki)**: Four-way recall cut to single LLM-Wiki. Wiki stores facts only (what happened), not inferences (no confidence scores). R1 reasons independently from raw facts. Scene-filtered pre-classification, no runtime retrieval overhead. Updated incrementally with data collection.

## Implementation Progress

按设计文档分阶段推进（P0 → P1 → P2 → P3 → P4）。P0+P1 已提交（commit ac06933）：

- **P0 技术栈对齐（完成）**：`dashscope-sdk-java` → Spring AI Alibaba 1.1.2.0，`StageEnum` 模型路由修正，`BailianManagerImpl` 重写。
- **P1 五维宇宙（完成）**：`generateUniverses` 重构为 3 时间宇宙 + 5 策略宇宙（每策略宇宙附着 关联/极端/天气），新增 3 张表 + 4 枚举，5 个 builder + `StressTestEngine` + `R1Enhancer` + `MultiverseGenerator` 实现。
- **LLM 全流程 smoke test（待做）**：唯一阻塞是 token-plan 配额（2026-08-24 实测 429 insufficient_quota）。协议切换已验证接线正确（错误从 dashscope 401 InvalidApiKey 变为 token-plan 429，认证已通过、仅剩配额）；Demo 已加 **key 短路**（占位/空 key 直接抛 `LLM_DEGRADED`，跳过重试退避），充值配 key 后即可真 smoke。
- **P2 可解释推演（待做）**：qwen3.8-max 交叉验证、deepseek-v4-pro Grounding 引用 KB、修正 `source: "kb"` 标签（当前为无 KB 的启发式规则）。
- **P3 数据源接入（完成数据源+T+展示，LLM-Wiki 待做）**：`DataCollector` 先于 LLM 采集真实数据源并落库 `market_data`（一行=task_id+category，幂等 upsert）。frankfurter 真实汇率（免费无 key，`MarketCurrency` 由 targetMarket 推导货币，读超时 8s）；KB 三类 YAML（`resources/kb/*.yml`，SnakeYAML 加载 + TTL 30/90/90 天）；TTL 三层 `DataFreshnessService`（Fresh 1.0 / Stale 0.5x / Missing 纯 R1，`source` 标注 kb/kb_stale/r1_inferred）；Tavily 降级 stub（预留 `TAVILY_API_KEY`，未配置时 KB 兜底并落库 MISSING 行）。`collectData` 改为「先 DataCollector 后 LLM」，LLM 失败降级真实数据源输出不抛异常（429 下全链路仍跑通）。展示端点 `GET /api/v1/tasks/{id}/collected-data` 返回每类数据的来源+last_verified+Fresh/Stale/Missing+权重。
- **P4 Token 追踪（待做）**：填充 `bailian_call_log.token_count`。
- **复赛 Demo（完成，commit 0111f83 / eca5797 / 44ab13e）**：M1–M7 —— 前端四页面（Input/Run/StarMap/Detail/Decision）+ 一体打包（`build:static` 产物提交进 static）；后端 detail 富版聚合 `stressTests`(5 风暴)/`weather`/`competitorReactions`；无 LLM 时按「策略画像先验(0.5)+5 风暴平均(0.3)+最差风暴(0.2)」差异化评分（A–D，`RULE_DEGRADED_PRIOR` 证据）；key 短路后任务 ~2s 即 DONE。运行/口径说明见知识库 `demo-run-guide` / `tech-stack-and-models`。

**已知偏差**：`RuleEngineImpl` / `EntanglementBuilder` 的 `source: "kb"` 目前是无 KB 的启发式规则，P2 一并修正。P3 KB 政策市场匹配为精确匹配（`market: EU` 的 GPSR 不命中 DE），LLM-Wiki 召回层未做。

**仓库交付**：`origin` = GitHub `en9578/multiverse-engine`（main 已推送，含 P0→P3 + 复赛 Demo；复赛要求的 GitCode 镜像地址若建，从该 GitHub 推即可）。`bailian_test_output/` 与复赛提交 docx 模板**不入库**（保持 untracked）。