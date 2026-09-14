# AI 跨境商业多元宇宙引擎

> 把跨境市场洞察从「看静态报表」升级为「穿越多个平行宇宙亲历策略 90 天命运」，再回到现实定居评级最高的最优宇宙。
>
> 复赛赛道：AI 市场洞察 ｜ 团队：大爱仙尊

---

## 快速开始

```bash
cd multiverse-engine
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 打开 http://localhost:8080 即整站（前端已打包进 static，H2 内存库零配置）
```

- **无需百炼 key 即可完整演示**：引擎自动降级为真实数据源 + 知识库规则 + 压力测试，每处诚实标注来源
- **全功能需 key**：`export DASHSCOPE_API_KEY=<百炼工作空间 key>` 后重启，走 deepseek-v4-pro / qwen3.7-plus 等模型增强推演
- 需要 JDK 21 + Maven（无其他外部依赖）

---

## 解决什么问题

跨境卖家选品定价本质上是在 **时间 × 策略 × 竞争 × 极端事件** 四维不确定性中下注。传统工具只给静态报表，选品、竞品、评论、定价、合规五个功能孤立断裂，无法回答"如果我用 A 策略，90 天后在各种竞争格局下会怎样"。

本方案把市场建模成多个平行宇宙，卖家在一个界面完成从采集、生成、推演到定居的全流程决策。

---

## 核心功能

### 1. 多元宇宙生成
输入产品 + 目标市场 + 卖点，引擎生成 **8 个平行宇宙**：
- **3 个时间宇宙**：穿越到 6 个月前 / 现在 / 3 个月后，判断品类生命周期（起势/爆发/饱和）和三类时间差机会（地理时间差 / 趋势时间差 / 品类传导），回答"该不该现在入场"
- **5 个策略宇宙**：从 定价(高端/性价比/低价) × 卖点(功能/情感/差异化) × 定位(头部/垂直/搅局) 的 27 种组合中精选 5 个代表性打法

### 2. 90 天演化推演
每个策略宇宙经历完整的 90 天市场演化：
- **5 种极端风暴压力测试**：价格海啸、合规风暴、供应链断裂、需求雪崩、流量断崖 — 雷达图可视化
- **竞品关联反应**：竞品跟价、跟款、差异化分流等可能反应
- **市场气象**：搜索热度、评论情绪、政策风向

### 3. 可解释证据链
每条扣分带规则 ID + 来源徽标，按影响从大到小排列：
- 🟣 知识库依据（全权重计分）
- 🟠 知识库·数据较旧（扣分减半）
- 🔵 AI 模型推断
- ⚪ 经验规则

悬停看白话解释，点"原始判定数据"展开完整参数——拒绝黑盒。

### 4. 定居决策
综合评级（A~D）和存活率选出最优宇宙，输出 **反脆弱策略组合**——四种极端情景都能赢的组合方案。

### 5. 无 key 自动降级
未配置百炼 key 仍可完整跑通全链路，每处标注来源（经验规则），不伪造模型已生成。

### 6. 数据新鲜度透明
三类数据源（frankfurter 实时汇率 / KB 知识库 / Tavily 预留），每类标注 Fresh/Stale/Missing 新鲜度与 last_verified 时间，结论不冒充有数据支撑。

---

## 系统架构

```
[Browser SPA: React+Vite+TS]  →  /api/v1/*  →  [Spring Boot :8080]
                                                    │
                                    Controller → Service（状态机编排）
                                      → Manager（生成/推演/决策/百炼）
                                        → DAO（MyBatis + Flyway 13 迁移）

数据源：frankfurter 汇率 + resources/kb/*.yml 知识库 + Tavily（预留）
存储：H2(dev) / MySQL(prod)，无 Redis 依赖
```

**技术栈**

| 层 | 技术 |
|----|------|
| 后端 | Java 21 / Spring Boot 3.4.4 / MyBatis / Resilience4j |
| 模型接入 | Spring AI OpenAI starter 1.0.0 → 百炼 ws- 工作空间（OpenAI 兼容协议） |
| 前端 | React 18 + Vite + TypeScript（HashRouter，3 依赖，雷达图自绘 SVG） |
| 数据库 | H2(dev) / MySQL(prod)，Flyway 13 迁移，11 个 MyBatis Mapper |
| 编排 | 自研状态机（TaskStatusEnum）+ @Async 三线程池 + CompletableFuture 扇出 |

---

## 百炼模型调用

所有文本生成按阶段自动路由，走 OpenAI 兼容协议：

| 阶段 | 模型 | 用途 |
|------|------|------|
| COLLECTING | qwen3.7-plus | 竞品事实提取 / 合规分析 |
| GENERATING / EXPLORING | deepseek-v4-pro | 宇宙策略包生成 / 90 天格局推演 / R1 推理增强 |
| SETTLING | qwen3.8-max | 最优宇宙选择 / 反脆弱组合 / 交叉验证 |
| 视觉合规 | qwen-vl-plus | 商品图像合规检测 |
| 生图 | wan2.7-image-pro | 策略包配图（预留） |

**成本控制**：全链路由 18 次模型调用优化至 10 次（策略包 5 合 1 批量生成 + 竞品反应 5 合 1 批量推理 + prompt 紧凑化），实测单任务约 1.8 万 token（约 0.5 元）。阶段输出上限 + 任务级 token 预算护栏，超限自动降级、任务不失败。`bailian_call_log` 逐调用记录可对账。

---

## 项目结构

```
minbao/
├── multiverse-engine/                 # Java Spring Boot 工程
│   └── src/main/
│       ├── java/com/minbao/multiverse/
│       │   ├── controller/            # REST 控制器 (/api/v1/)
│       │   ├── service/               # 编排 / 探索 / 定居 服务
│       │   ├── manager/               # 百炼接入 / 规则引擎 / 引擎实现
│       │   ├── engine/                # 维度构建器 / 演化引擎 / 评级器
│       │   │   └── dimensions/        # TimeDimensionBuilder / StrategyDimensionBuilder
│       │   ├── dao/                   # MyBatis DAO
│       │   ├── domain/                # Entity / DTO / VO / BO
│       │   ├── enums/                 # TaskStatusEnum / StageEnum / ErrorCodeEnum
│       │   └── common/                # 全局异常 / Result 统一响应
│       └── resources/
│           ├── application*.yml       # 主配置 / dev(H2) 配置
│           ├── kb/                    # 知识库 YAML（合规政策/痛点/竞品策略）
│           ├── db/migration/          # Flyway 13 迁移脚本
│           └── mapper/                # MyBatis XML 映射
├── frontend/                          # React SPA（构建产物复制进 multiverse-engine/src/main/resources/static/）
└── README.md
```

---

## 当前完成度

| 模块 | 状态 |
|------|------|
| P0 技术栈对齐（Spring AI OpenAI starter 接入百炼） | ✅ |
| P1 五维宇宙（3 时间 + 5 策略 + 竞品关联 + 极端风暴 + 市场气象） | ✅ |
| P2 可解释推演（规则引擎 + R1 增强 + 证据链四源标注） | ✅ |
| P3 数据源接入（frankfurter 汇率 + KB YAML + 新鲜度分层） | ✅ |
| P4 Token 追踪（调用日志落库 + 预算护栏 + 成本可对账） | ✅ |
| 复赛 Demo 前端（React 四页面 + 一体打包） | ✅ |
| LLM 全流程冒烟测试（真 key 四模型全链路跑通） | ✅ |
| JUnit 确定性测试（9 例全绿） | ✅ |
| 降级模式（无 key 完整演示 + 差异化评分 + 诚实标注） | ✅ |
| 交叉验证（qwen3.8-max 独立审查） | 🔜 |
| LLM-Wiki 召回层 | 🔜 |

---

## 演示视频

> 演示视频链接：[待补充]