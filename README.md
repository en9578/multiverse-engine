# AI 跨境商业多元宇宙引擎

把跨境市场建模成多个平行宇宙，卖家先在宇宙中亲历策略 90 天命运演化，再回到现实定居评级最高的最优宇宙，拿到一个四种极端情况都能活的策略包。

## 🚀 复赛 Demo · 快速开始（一体交付）

```bash
cd multiverse-engine
mvn spring-boot:run -Dspring-boot.run.profiles=dev     # 前置：JDK 21 + Maven
```

打开 **http://localhost:8080** 即整站（React 前端已打包进 Spring Boot static，H2 内存库免配置）。Demo 流程：新建推演 → 阶段灯 + 数据新鲜度面板 → 星图（5 策略宇宙 A–D 评级 + 3 时间宇宙）→ 点卡看 **5 风暴压力雷达 / 市场气象 / 可解释证据链** → 穿越向导对话 → 定居决策并确认。

- **零配置即可跑**：未设百炼 key 时引擎自动降级（真实汇率 + 知识库规则 + 压力测试），每处标注来源，**不伪造模型已生成**；
- **全功能需 key**：`export DASHSCOPE_API_KEY=<百炼团队 key>` 后重启。key 仅支持 OpenAI 兼容协议（token-plan 基地址），详见下文「模型调用」；
- 改前端：`cd frontend && npm run build:static`（产物提交进 static，评审机仍单命令起）。
- 完整说明见知识库 `demo-run-guide.md` / `tech-stack-and-models.md`。

## 解决什么问题

跨境卖家选品定价本质是在时间、策略、竞争、极端事件四个维度的不确定性里下注。传统工具只给静态报表，没法回答"如果我用 A 策略，90 天后在各种竞争格局下会怎样"。选品、竞品、评论、定价、合规在传统工具里是五个孤立功能，信息在方向之间断裂。

本方案把时间机器、平行宇宙、竞品关联、压力测试、市场气象五个概念统一成一个框架，卖家在一个界面完成全部探索。

## 核心功能

1. **多元宇宙生成**：输入品类、预算、目标市场，生成 5 个平行宇宙，分别模拟时间差机会、不同定价策略、竞品反应、极端风暴、市场气候
2. **90 天演化推演**：规则引擎匹配历史反应模式，R1 补充推理规则未覆盖场景，输出存活率和 A-F 评级，每条结论带规则 ID 和可追溯证据
3. **穿越体验**：卖家可以"穿越"进任意宇宙，对话式探索策略命运走向
4. **定居决策**：综合评级选最优宇宙，输出反脆弱组合（四种极端情景下都能赢的策略包）
5. **幻觉抑制与降级**：四层防线压住大模型编造事实的倾向，结果带置信度评分，用户能区分哪些结论有数据支撑、哪些是 AI 推断

## 技术栈（复赛现状 · 单体）

| 层 | 技术 | 说明 |
|----|------|------|
| 后端 | Spring Boot 3.4.4 / Java 21 / MyBatis / Resilience4j | 4 层单体：Controller→Service→Manager→DAO；状态机编排 + @Async 宇宙扇出 |
| 模型接入 | Spring AI Alibaba（OpenAI 兼容协议 → 百炼 token-plan） | `ChatModel`/`ImageModel`，key 仅环境变量注入；详见下方「模型调用」 |
| 数据库 | H2(dev) / MySQL(prod)，Flyway 13 迁移 | 13 表 + 11 MyBatis XML |
| 数据源 | frankfurter 真实汇率 + 本地 KB YAML + Tavily(预留) | TTL 新鲜度分层 Fresh/Stale/Missing，last_verified 对用户可见 |
| 前端 | React 18 + Vite + TS（HashRouter） | 构建产物提交进 Spring Boot static → 单命令整站；无图表库，雷达自绘 SVG |
| Python 引擎 | ~~FastAPI + LangGraph~~ | 已按降级迁移收敛移除，业务并入 Java 单体 |

## 模型调用（百炼）

所有文本生成按阶段路由，走 OpenAI 兼容协议（团队 key 仅兼容该协议）：

| 阶段 | 模型 |
|---|---|
| COLLECTING | qwen3.7-plus |
| GENERATING / EXPLORING | deepseek-v4-pro |
| SETTLING | qwen3.8-max |
| 非文本 | wan2.7-image-pro(生图) / qwen-vl-plus(视觉合规) / text-embedding-v3(向量化) |

未设置 `DASHSCOPE_API_KEY` 时引擎自动降级（真实数据 + 规则先验 + 5 风暴压力），全程诚实标注来源，不影响体验闭环。

## 架构（初赛期双服务设计 · 历史）

> 下方为初赛期的 Python/LangGraph 双服务叙述，**已归档**（见 `downgrade-migration-plan.md`）；复赛实现为上面的单体 Java，请以顶部「快速开始」为准。

```
用户一句话需求（品类/预算/市场）
        |
   +----+----+
   | Java API |  <- Spring Boot :8080
   | 编排触发  |    REST API / SSE / 熔断 / 持久化
   +----+----+
        | HTTP
   +----+----+
   | Python  |  <- FastAPI + LangGraph :8000
   | 推理引擎 |    状态机 / 规则推演 / R1 / Qwen 交叉验证
   +----+----+
        |
  +-----+-----+
  v     v     v
COLLECTING  GENERATING  EXPLORING->SETTLING
采集Agent   生成Agent    推演Agent(5并行)  决策Agent
```

两服务独立部署，Python 推理挂了 Java API 照常可用（崩溃隔离）。

## 评委反馈回应

初赛评委肯定了多元宇宙思路和全栈技术选型的完整性，提出三条改进建议，已在 tech-solution.md 22-23 章和本文档中回应：

### 1. 推理方式 + 准确性证明 + 实时数据接入

推理不是让 R1 凭空预测未来，而是基于历史模式 + 实时信号的格局趋势推演。四层幻觉抑制：

- Grounding 强制接地：R1 prompt 必须包含 KB 数据，约束"只能基于以下数据推理，数据不足就说不足"
- Source 标注：kb 全权重(1.0)，r1_inferred 半权重(0.5)，用户看到"存活率 0.87"能展开看 0.6 来自 KB 规则、0.27 来自 R1 推断
- Qwen 交叉验证：R1 推完后用 Qwen-Plus 独立审查(temperature=0.0)，分歧 >0.2 标 confidence=low
- 温度控制：R1 推理 temperature=0.1，交叉验证 temperature=0.0

准确性证明：不预测销量绝对值，只推演格局方向（更集中/更分散/起势/红海）。维护 10-20 个历史场景回测数据集，方向准确率目标 >80%，低于 60% 暂停上线。

实时数据：Tavily 搜索 + frankfurter 汇率 API + KB 预设双源，数据新鲜度 TTL 对用户可见（前端显示 last_verified 日期）。不内置爬虫，数据源全部标注 source。

### 2. 百炼 ModelRouter 接入 + Agent 分工

统一接入：Python 侧 dashscope SDK 封装，Java 不直接调百炼。按阶段路由模型：

| 阶段 | 模型 | 用途 |
|------|------|------|
| COLLECTING | qwen-plus / qwen-vl-plus / text-embedding-v3 | 痛点抽取 / 合规检测 / 向量化 |
| GENERATING | wanx2.1-t2i-turbo | 策略包配图 |
| EXPLORING | deepseek-r1 | 5 宇宙格局推演 + 风暴存活率 |
| SETTLING | qwen-plus | 决策汇总 + 反脆弱组合 |

Agent 分工：采集 Agent（COLLECTING）-> 推演 Agent x5 并行（EXPLORING, Send API fan-out）-> 决策 Agent（SETTLING）。LangGraph StateGraph 串联，PostgresSaver checkpoint 断点恢复，interrupt_before SETTLING 支持人工确认。

幂等：requestId = taskId-stage-type，查 bailian_call_log 命中缓存直接返回。熔断：Resilience4j 50% 失败率触发。成本追踪：bailian_call_log.token_count 实时监控。

### 3. Token 消耗控制

单任务总成本约 0.47 元（R1 占 0.30 元）。控制策略：

- 分级推演：核心维度用 R1 深推，次要维度用规则/Qwen 轻推
- 上下文压缩：超 token 用摘要 + 最近 K 轮，不堆全部历史
- 缓存复用：requestId 幂等，相似任务复用采集数据 + 推演结果
- 并行共享采集：5 宇宙并行 fan-out 但采集只 1 次
- 预算上限：单任务 100K token 上限，超限降级（减宇宙数/减风暴数/规则替代 R1）
- Demo 预跑缓存：提前跑通标准场景，现场命中秒出

## 项目结构

```
minbao/
├── multiverse-engine/              # Java Spring Boot 工程
│   ├── pom.xml                      # Spring Boot 3.4.4 / Java 21
│   └── src/main/
│       ├── java/com/minbao/multiverse/
│       │   ├── controller/          # 4 个 REST 控制器 (/api/v1/)
│       │   ├── service/             # 编排/探索/定居 服务接口+实现
│       │   ├── manager/             # 百炼/引擎/OSS/规则 Manager 接口+实现
│       │   ├── engine/              # 维度构建器/演化/评级 (内部组件)
│       │   ├── dao/                 # 7 个 MyBatis DAO 接口
│       │   ├── domain/              # entity(DO)/dto/vo/bo/query
│       │   ├── enums/              # TaskStatusEnum(状态机)/ErrorCodeEnum(22码)
│       │   ├── config/             # 线程池/熔断/CORS/健康检查
│       │   └── common/             # Result<T>/BusinessException/全局异常
│       └── resources/
│           ├── application.yml      # 主配置 (MySQL/Redis/Flyway/Actuator)
│           ├── application-dev.yml  # 开发配置 (H2 内存模式)
│           ├── db/migration/        # 7 个 Flyway 迁移脚本
│           └── mapper/              # 7 个 MyBatis XML 映射
├── multiverse-engine-python/        # Python FastAPI 引擎骨架
│   ├── main.py                      # /health + /engine/tasks (ch18 契约)
│   ├── requirements.txt             # fastapi/langchain/langgraph/dashscope
│   └── .env.example                 # 环境变量模板
├── bailian_api_test.py              # 百炼 4 模型 API 测试脚本 (4/4 通过)
├── 评委反馈回应.md                   # 初赛评委反馈逐条回应
└── README.md
```

## 当前状态

| 模块 | 状态 | 说明 |
|------|------|------|
| Java 骨架 | 可编译可启动 | 69 源文件，mvn compile 通过，spring-boot:run 启动成功 |
| Flyway 建表 | 7 表自动迁移 | H2 内存模式零依赖启动 |
| MyBatis 映射 | 7 个 mapper XML | insert/select/update 全覆盖 |
| API 端点 | /api/v1/ 全部对齐 ch25 契约 | 含 SSE/upload/explore |
| 健康检查 | Actuator + EngineHealthIndicator | DB UP / Redis+Python DOWN(预期) |
| 百炼测试 | 4/4 模型验证通过 | bailian_api_test.py |
| Python 骨架 | FastAPI 4 端点 | LangGraph 状态机待实现 |
| 业务逻辑 | stub 阶段 | MultiverseEngine/BailianManager/RuleEngine 待填充 |
| 前端 | 待开发 | React + Three.js 星图 |
| 知识库 YAML | 待创建 | 痛点/政策/竞品三类 |

## 快速开始

```bash
# Java 后端 (dev 模式, H2 内存, 零外部依赖)
cd multiverse-engine
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 验证
curl http://localhost:8080/actuator/health
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{"productName":"test","targetMarket":"DE"}'

# Python 引擎
cd multiverse-engine-python
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
curl http://localhost:8000/health
```

## 文档

完整设计文档在 Obsidian 仓库管理。

| 文档 | 内容 |
|------|------|
| design.md (56KB) | 产品设计：多元宇宙五维度/穿越体验/定居决策/商业模式 |
| architecture.md (22KB) | 工程架构：四层分层/线程池/熔断/幂等/阿里规范 |
| tech-solution.md (65KB) | 技术方案 30 章：DDL/API 契约/幻觉抑制/降级/Token 估算 |