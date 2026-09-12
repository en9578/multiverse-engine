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

**Decision: Monolith downgrade (in progress)** — removing the Python/FastAPI/LangGraph dual-service layer and consolidating into a single Java service with Spring AI OpenAI starter (ws- 工作空间 key 仅 OpenAI 兼容协议，非 Alibaba starter). See `downgrade-migration-plan.md` in the knowledge base. Java 业务逻辑已实现；Python 引擎仍为骨架，待下线。

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
- P0 技术栈对齐：`BailianManagerImpl` 用 Spring AI OpenAI starter 1.0.0 重写（ws- 工作空间兼容基址，`StageEnum` 路由 + 重试/熔断/幂等 + `bailian_call_log` 落库）
- P1 五维宇宙：`MultiverseGenerator` + 5 个 builder（时间/策略/关联/极端/天气）+ `StressTestEngine` + `R1Enhancer`
- `MultiverseEngineImpl` 四阶段（collect/generate/explore/settle）+ `RuleEngineImpl`（规则扣分 + evidences `source` 标注）
- `UniverseRater`（score≥85→A、≥75→B、≥60→C、≥40→D、else F，切点 2026-09-12 校准）、`Constants`、`StageEnum`、`TaskStatusEnum`
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

1. **Explainable reasoning, not black-box**: `RuleEngine` outputs `evidences` with `source` 四值词表 weight tags（`kb` 全权重 / `kb_stale` 0.5x / `r1_inferred` 模型推断 / `heuristic` 启发式无 KB 依据）。deepseek-v4-pro Grounding (must cite KB data), qwen3.8-max cross-validation (temperature=0.0), direction-only prediction (no absolute values), backtesting >80% accuracy.
2. **Agent orchestration without LangGraph**: `TaskStatusEnum` state machine + `@Async` + `CompletableFuture.allOf` for universe fan-out (3 time + 5 strategy). `last_completed_stage` field + `/retry` endpoint for checkpoint recovery. `updateStatus` before SETTLING for human-in-the-loop.
3. **Model routing**: `StageEnum` → Bailian model: COLLECTING→qwen3.7-plus, GENERATING/EXPLORING→deepseek-v4-pro, SETTLING→qwen3.8-max；生图→wan2.7-image-pro、VL→qwen-vl-plus、向量化→text-embedding-v3（非文本模型走 `BailianManager` 独立方法，不经 `StageEnum`）。
   **API 端点（重要）**: 团队 key 为 ws- 工作空间专属部署 key（`sk-ws-` 开头），仅支持 **OpenAI 兼容协议**；基址由 `spring.ai.openai.base-url = ${BAILIAN_BASE_URL:https://ws-77ukym0vhwm7h7qd.cn-beijing.maas.aliyuncs.com/compatible-mode}` 配置（**不含 `/v1`**，Spring AI OpenAI starter 会自动拼接 `/v1/chat/completions`）。注意：工作空间控制台给的原生基址 `/api/v1` 是 DashScope 原生协议（`/api/v1/chat/completions` 404），必须用 `/compatible-mode` 前缀；配额耗尽报 `403 AccessDenied.Unpurchased`。`BailianManagerImpl` 走 Spring AI OpenAI starter 指向该基地址，key 通过 `DASHSCOPE_API_KEY` 环境变量注入（占位 `sk-dev-placeholder`/空白 → 方法入口短路抛 `LLM_DEGRADED`），勿写入代码/配置文件。
4. **Token budget（2026-09-12 架构级成本护栏落地）**：单任务 LLM 调用 18→10 次（策略宇宙生成 5→1 批量、竞品 R1 反应 5→1 批量、推演/结算 prompt 压缩为紧凑事实），加上 StageEnum 每阶段输出上限（COLLECTING 900 / GENERATING 2000 / EXPLORING 500 / SETTLING 700）与 `TokenBudgetManager` 任务级预算（`minbao.llm.budget-per-task-tokens:30000`，超限抛 LLM_DEGRADED 沿既有降级路径：模板宇宙/规则融合/规则兜底，任务不失败），预计整任务 token 减半以上（未实测，按调用数与 prompt 体量推算）。Graded reasoning（core=V4-pro, secondary=rule/qwen3.7-plus）、幂等缓存、`bailian_call_log` 逐调用明细 + 任务完成日志 totalTokensUsed。
5. **Data freshness**: Tavily + frankfurter + KB dual-source, TTL tiers (Fresh/Stale 0.5x/Missing), `last_verified` visible. Keepa API (€49/mo) designed but disabled for Demo — price history data from manual KB entry.
6. **Recall layer (LLM-Wiki)**: Four-way recall cut to single LLM-Wiki. Wiki stores facts only (what happened), not inferences (no confidence scores). R1 reasons independently from raw facts. Scene-filtered pre-classification, no runtime retrieval overhead. Updated incrementally with data collection.

## Implementation Progress

按设计文档分阶段推进（P0 → P1 → P2 → P3 → P4）。**P0–P4 + 复赛 Demo 已全部落地并推送，HEAD `bafd64d`**（里程碑：ac06933 P0+P1 → 42fd30b P3 → 0111f83·eca5797·44ab13e·7190918 复赛 Demo → bafd64d P2 规则层 + P4 + 测试）：

- **P0 技术栈对齐（完成）**：`dashscope-sdk-java` → Spring AI OpenAI starter 1.0.0（ws- 工作空间兼容基址），`StageEnum` 模型路由修正，`BailianManagerImpl` 重写。
- **P1 五维宇宙（完成）**：`generateUniverses` 重构为 3 时间宇宙 + 5 策略宇宙（每策略宇宙附着 关联/极端/天气），新增 3 张表 + 4 枚举，5 个 builder + `StressTestEngine` + `R1Enhancer` + `MultiverseGenerator` 实现。
- **LLM 全流程 smoke test（完成 2026-09-10）**：切到团队 ws- 工作空间专属部署端点（`sk-ws-` key）后全链路真 key 冒烟跑通 —— `COLLECTING→GENERATING→EXPLORING→SETTLING→DONE`，qwen3.7-plus / deepseek-v4-pro / qwen3.8-max / qwen-vl-plus 四模型全通，宇宙详情 `llmScore` 非空 + LLM reasoning + `kb_stale`/`heuristic` 证据链。期间修复 requestId 碰撞 bug（并行 fan-out 下 `System.currentTimeMillis()` 撞唯一索引 → 改用 `AtomicLong` 序号）。配额耗尽报 `403 AccessDenied.Unpurchased`（充值后恢复）；生图 `/compatible-mode/v1/images/generations` 仍 400（MVP 主链路不涉及）。
- **P2 可解释推演（规则层完成 2026-09，commit bafd64d；交叉验证待做）**：修正 `source` 真实性 —— 新增 `heuristic` 标签，启发式规则不再冒充 kb；`RuleEngineImpl` 新增 `RULE_COMPLIANCE_KB` 真读 policy_kb、逐条引用 KB 条目 id，扣分 = severity×时效权重（kb 1.0 / kb_stale 0.5）；KB 政策市场匹配改 region-aware（`market: EU` 的 GPSR 现命中 DE）；无 LLM 降级证据链清空只留 `RULE_DEGRADED_PRIOR(heuristic)` 保持自洽。仍待做：qwen3.8-max 交叉验证、deepseek-v4-pro Grounding 引用 KB、pain_point/competitor_strategy KB 反哺规则（无产品品类字段，关键词匹配不可靠）。
- **P3 数据源接入（完成数据源+T+展示，LLM-Wiki 待做）**：`DataCollector` 先于 LLM 采集真实数据源并落库 `market_data`（一行=task_id+category，幂等 upsert）。frankfurter 真实汇率（免费无 key，`MarketCurrency` 由 targetMarket 推导货币，读超时 8s）；KB 三类 YAML（`resources/kb/*.yml`，SnakeYAML 加载 + TTL 30/90/90 天）；TTL 三层 `DataFreshnessService`（Fresh 1.0 / Stale 0.5x / Missing 纯 R1，`source` 标注 kb/kb_stale/r1_inferred）；Tavily 降级 stub（预留 `TAVILY_API_KEY`，未配置时 KB 兜底并落库 MISSING 行）。`collectData` 改为「先 DataCollector 后 LLM」，LLM 失败降级真实数据源输出不抛异常（429 下全链路仍跑通）。展示端点 `GET /api/v1/tasks/{id}/collected-data` 返回每类数据的来源+last_verified+Fresh/Stale/Missing+权重。
- **P4 Token 追踪（完成 2026-09，commit bafd64d）**：`BailianManagerImpl` 文本/VL 成功行填充 `token_count`（ChatResponse usage：total 优先、缺失回退 prompt+completion）+ `cost_ms`（实测耗时）；生图走 OpenAI 图片接口无 token 置空；失败行记录耗时（`fail` 重载带 costMs）。接线由 `usageTokens` 静态助手 + JUnit 覆盖；**真实落库观察待配额恢复后真 key 冒烟**（见下）。
- **确定性 JUnit（新增 2026-09-05，commit bafd64d）**：`collector/kb/KnowledgeBaseRegistryTest`（EU⊃DE 区域匹配/representative）、`manager/impl/RuleEngineImplTest`（RULE_COMPLIANCE_KB 引用 PLCY-003、启发式 source=heuristic）、`manager/impl/BailianManagerImplUsageTest`（usage→token_count 四例）。`mvn test` **9 例全绿**；E2E 降级路径复核 POLICY count=3（region 修复实证）、5 宇宙 A–D 差异化、STRATEGY detail 证据链单行 `RULE_DEGRADED_PRIOR(heuristic)` 自洽、降级评分零回归。
- **复赛 Demo（完成，commit 0111f83 / eca5797 / 44ab13e）**：M1–M7 —— 前端四页面（Input/Run/StarMap/Detail/Decision）+ 一体打包（`build:static` 产物提交进 static）；后端 detail 富版聚合 `stressTests`(5 风暴)/`weather`/`competitorReactions`；无 LLM 时按「策略画像先验(0.5)+5 风暴平均(0.3)+最差风暴(0.2)」差异化评分（评级有区分度、B 封顶，`RULE_DEGRADED_PRIOR` 证据）；key 短路后任务 ~2s 即 DONE。运行/口径说明见知识库 `demo-run-guide` / `tech-stack-and-models`。
- **Token 成本护栏（2026-09-12）**：全链路原本 18 次 LLM 调用/任务（同一份市场事实 JSON 重复发送 ~15 次）导致单任务 ~1 元。架构级约束四件套：①`StrategyDimensionBuilder` 5 组策略包合并单次调用（按 index 解析，缺项走模板兜底）；②`EntanglementBuilder` R1 反应 5 宇宙合并单次调用（`enhanceReactions`，`MultiverseGenerator` 循环只做规则反应；解析失败降级仅规则）；③`StageEnum.maxOutputTokens` 输出上限 + `BailianManager.generateText(stage,system,user,taskId)` 新重载：调用前 `TokenBudgetManager.tryAcquire` 超限抛 `LLM_DEGRADED`，成功后按 usage 归账；④推演 prompt 瘦身（`compactFacts`/`compactBaseline`/`compactStrategy`）。预算耗尽各阶段沿既有降级路径，任务不失败。按调用数与 prompt 体量推算 token 减半以上，**未跑真实任务实测**（用户要求不花额度验证）。
- **采集 severity 校准 + 评级实测（2026-09-12）**：实测发现评分结构性封顶 C——采集 LLM 对任何产品（含零风险日用品）都输出 2 条 high 合规项（加州 65 警示标签也标 high），ruleScore 恒 68，finalScore ≤71。修复：COLLECTING 提示词加 level 校准口径（high=禁售级 / medium=准入门槛 / low=警示标注，非电子非儿童非食品接触日用品通常无 high）。实测花盆/舔垫 @US：ruleScore 68→78~83，评级 C→B（花盆 5 宇宙全 B，最高 80.2）。`MultiverseOrchestratorServiceImpl.buildDisplay` 同步人话化（Fresh 全权重→数据新鲜全额计分等）。
- **探索分锚定校准 + A 级打通（2026-09-12）**：EXPLORING 提示词锚定「生存分≠商业成功概率」三档口径（<60 存在禁售级风险 / 75-85 无禁售且压测≥0.70 / 86-95 全维度干净且压测≥0.75 且最差风暴≥0.60）+ 竞品采集改「2-6 个真实主要竞品不凑数」+ **A 切点 90→85**（`UniverseRater`，切点不对评审暴露；`tech-stack-and-models.md` 已同步）。实测花盆 v4 @US：星图 **A(86.7) + 4×B**（llm 78-84 忠实跟随档位、rule 93），评级分布有区分度。压测常数与 0.7/0.3 权重未动。降级模式（无 key）上限 ~79，B 封顶。
- **证据链展示人话化（2026-09-12）**：面向非技术用户 —— 后端 5 条规则与演示模式证据的 `description` 改为完整白话句（写明数据含义与商业后果，`RULE_DEGRADED_PRIOR` label 改「演示模式评分」）；前端 `EvidenceList` 重做（来源徽标白话化+悬停解释、按扣分影响从大到小排序、原始判定数据默认折叠进「详情」）；`UniverseDetailPage`/`FreshnessPanel` 页面术语翻译（reasoning/LLM 模型分/兜底评分模式、Fresh/Stale/Missing→数据新鲜/较旧扣分减半/仅 AI 推断）。ruleId/input/weight 等技术字段未动（评审可追溯口径不变）。

**已知偏差**：~~启发式规则 `source:"kb"` 造假~~（已修正 2026-09：启发式标 `heuristic`，KB 政策规则标 `kb/kb_stale` 并引用 KB 条目 id）；~~EU 政策不命中 DE~~（已修正 region-aware）；~~`demo-run-guide.md`/`tech-stack-and-models.md` source=kb 口径未同步~~（已修正 2026-09-06：KB 文档已同步四值词表 + 徽标四色 + 降级单条 `RULE_DEGRADED_PRIOR(heuristic)`；README.md:38 已改「Spring AI OpenAI starter 1.0.0」）；`RULE_COMPLIANCE_KB` 现按市场级注册负担基线扣分（policy_kb 无产品品类字段，品类关键词过滤留后续）；pain_point / competitor_strategy KB 未反哺规则；LLM-Wiki 召回层未做。

**仓库交付**：`origin` = GitHub `en9578/multiverse-engine`，main 已推送至 `6a31d6e`（P0–P4 + 复赛 Demo + 证据人话化/评分校准/Token 成本护栏），与 origin/main **0 ahead/0 behind**。2026-09-05 起 GitHub **直连推送**（已移除 repo-local `http.proxy=127.0.0.1:7897`，github.com 当前直连可达）。复赛要求的 **GitCode 镜像仍未建**（gitcode.com 从本机 HTTPS 直连超时，需网络可用时在 gitcode.com 建空仓库后从 origin 推送）。`bailian_test_output/` 与复赛提交 docx 模板**不入库**（保持 untracked）。复赛提交物（9/15 截止：PDF/演示视频/线上 demo 或 zip/GitCode）尚未制作。