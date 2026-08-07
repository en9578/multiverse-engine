# multiverse-engine 项目工程

> 项目文档已迁移至 Obsidian 仓库管理：
> C:\dev\knowledge\project-document\multiverse-engine

## 目录结构

| 路径 | 说明 |
|------|------|
| multiverse-engine/ | Java Spring Boot 工程（四层架构骨架，62 源文件已编译通过） |
| bailian_api_test.py | 百炼 API 调用测试脚本（4/4 已验证） |
| index.html | 前端静态页（Three.js 星图 Demo） |
| bailian_test_output/ | 测试产出（文本/图片） |

## 文档位置

所有 .md 文档（architecture / design / tech-solution 等）已移至 Obsidian：
C:\dev\knowledge\project-document\multiverse-engine

---

# 缺失架构补充设计（供 AI 代理实现参考）

> 本节记录两次架构审查发现的关键缺失，供实现参考。**优先级从上游到下游**。
> 工程现状：骨架完整（62 源文件编译通过），但业务逻辑层全 stub（BailianManager/RuleEngine/MultiverseEngine 空壳），依赖缺（Redis/OSS/百炼SDK/向量库），数据层未贯通（mapper XML/DDL 缺）。

## 一、数据采集 + 检索层（第一堵点，上游）

### 设计依据
`design.md` 第 8 章：6 类数据源（评论/价格/搜索/政策/VL/竞品历史）+ 关联图构建，作为多元宇宙"宇宙初始条件"。
**核心原则**：不内置爬虫，用"知识库 + R1 推理"双引擎，所有数据标注 `source: kb | r1_inferred | public_aggregate`。

### 组件设计（Manager 层）

```java
// 统一采集入口
public interface DataCollector {
    CollectedDataBO collect(CreateTaskDTO brief);
}

// 各子 Collector（内部）
// CommentCollector  : 加载 pain_point_kb.yml → R1 增强 → Qwen 抽取痛点 JSON
// PriceCollector    : 加载价格预设 → R1 推理定价弹性
// PolicyCollector   : 加载 policy_kb.yml → R1 政策影响推理
// CompetitorCollector: 加载 competitor_strategy_kb.yml → embedding 入向量库
```

### 知识库 yml（3 个，放 src/main/resources/）

| 文件 | 内容 | 字段 |
|------|------|------|
| pain_point_kb.yml | 按品类的真实差评痛点聚合 | issue / frequency / severity / typical_competitor / source_tag |
| policy_kb.yml | 各市场政策法规 | market / regulation / impact / source_tag |
| competitor_strategy_kb.yml | 竞品历史反应模式 | competitor / reaction_type / reaction_prob / evidence |

加载方式：Spring `@ConfigurationProperties`，启动即入内存。

### 向量检索（关联图）

- **embedding**：百炼 `text-embedding-v3`（通义千问 embedding）
- **向量库**：Demo 用**内存余弦相似**（`ConcurrentHashMap` + 余弦计算，0 依赖）；生产用 PgVector/Milvus
- **检索逻辑**：卖家策略 → Qwen embedding 向量化 → 检索最相似历史场景 → 推断竞品反应概率

### 技术选型

| 维度 | MVP（0 外部依赖） | stretch（可选） |
|------|------------------|----------------|
| 评论/痛点 | pain_point_kb.yml + R1 | Keepa API |
| 价格 | 预设 + R1 | Keepa API |
| 搜索趋势 | 预设 + R1 | pytrends |
| 政策 | policy_kb.yml + R1 | 政府官网 RSS |
| VL 视觉 | Qwen-VL（百炼，已就绪） | — |
| 竞品策略 | competitor_strategy_kb.yml + R1 | 第三方监控 API |

**不碰爬虫**（跨境爬虫法律风险 + 时间黑洞）。

### 实现要点
- `MultiverseEngineImpl.collectData()` 当前是空壳（只 log），需实现为调 `DataCollector.collect()`
- Qwen 把非结构化文本抽成结构化 JSON（评论 → 痛点 `{issue, frequency, severity, fix}`）
- R1 增强补充标注 `source: r1_inferred`，核心靠知识库（可解释、可答辩）
- 采集数据按 `taskId` 隔离，不同任务数据不串

---

## 二、对话存储 + 上下文隔离（防污染）

### 为什么重要
用户穿越宇宙 A 问"存活率为什么 0.87"，再穿越宇宙 B 问"为什么 0.72"——如果 R1 调用时把 A+B 对话混入上下文，推演结果会串，评委一眼看出 bug。**必须三级隔离**。

### 隔离维度（核心）

| 维度 | 隔离作用 |
|------|---------|
| taskId | 不同卖家任务绝对隔离 |
| universeId | **同任务内宇宙 A 对话不串入宇宙 B 推演**（关键） |
| sessionId | 同宇宙多次会话隔离 |

### conversation 表 DDL

```sql
CREATE TABLE `conversation` (
  `id`          bigint unsigned NOT NULL AUTO_INCREMENT,
  `task_id`     bigint unsigned NOT NULL COMMENT '任务ID',
  `universe_id` varchar(32)  NOT NULL DEFAULT '' COMMENT '宇宙ID(隔离维度)',
  `session_id`  varchar(64)  NOT NULL DEFAULT '' COMMENT '会话ID(隔离维度)',
  `role`        varchar(16)  NOT NULL COMMENT 'user/assistant/system',
  `content`     text         NOT NULL,
  `trace_id`    varchar(64)  NOT NULL DEFAULT '' COMMENT '链路追踪',
  `is_deleted`  tinyint(1)   NOT NULL DEFAULT 0,
  `gmt_create`  datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `gmt_modified`datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_task_universe_session` (`task_id`, `universe_id`, `session_id`),
  KEY `idx_trace_id` (`trace_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话历史(三级隔离)';
```

存储：MySQL 持久 + Redis 热缓存（key: `ctx:{taskId}:{universeId}:{sessionId}`）。

### ContextManager（隔离闸门，Manager 层）

```java
public interface ContextManager {
    /** 组装当前隔离域的上下文（供 LLM 调用，只取当前 task+universe+session） */
    String buildContext(Long taskId, String universeId, String sessionId, int maxTokens);
    /** 追加对话（回写当前域） */
    void appendConversation(Long taskId, String universeId, String sessionId, String role, String content);
}
```

- 调 R1/Qwen **前**，`ContextManager.buildContext()` 按 `taskId+universeId+sessionId` 查 conversation，**只组装当前域**对话
- 上下文窗口管理：超 token → 「系统提示 + 最近 K 轮 + 早期轮次 Qwen 摘要压缩」
- DAO 强制 `WHERE task_id=? AND universe_id=? AND session_id=?`（不带过滤不让查，防呆）

### 防污染保障
- BailianManager 调用 `requestId` 含 `taskId+universeId`
- Redis key 带隔离维度前缀，物理隔离
- 日志带 `taskId+universeId`，串上下文可追溯
- 单测用例：跨任务 / 跨宇宙 context 不串（关键用例）

---

## 三、架构位置

| 补充层 | 位置 | 与现有工程关系 |
|--------|------|---------------|
| 数据采集层 | 编排 COLLECTING 段调用，引擎层上游 | `MultiverseEngineImpl.collectData()` 实现 |
| 对话存储+隔离 | 横切层（影响编排/引擎/Bailian 调用） | `BailianManagerImpl` 调用前接 `ContextManager` 组装上下文 |

两者都在"**编排调百炼前**"这一步组装：采集的 `CollectedDataBO` + 隔离的对话上下文 一起喂给 R1。

---

## 四、实现优先级（从上游到下游）

1. **数据采集 + 检索**（第一堵点）：`DataCollector` + 3 知识库 yml + 向量检索
2. **对话存储 + 上下文隔离**：`conversation` 表 + `ContextManager` + 三级隔离
3. **数据层贯通**：mapper XML + DDL 建表 + pom 补百炼 SDK/Redis/OSS
4. **业务逻辑实现**：`BailianManagerImpl` 真调 + `RuleEngineImpl` 真规则 + 多宇宙并行 + SSE + 前端 React

> 1 和 2 可一起搭（都在"调百炼前组装输入"这一步），建议作为"上游输入层"一次实现。

---

## 五、与现有工程集成点

| 操作 | 类/文件 |
|------|---------|
| 新建 | `DataCollector`（Manager 接口+Impl）、`ContextManager`（Manager 接口+Impl）、`ConversationDAO`+mapper XML、3 个知识库 yml（resources/） |
| 改造 | `MultiverseEngineImpl.collectData()`（实现采集）、`BailianManagerImpl`（调用前接 ContextManager）、`BailianManager` 接口加 `requestId` 含隔离标识 |
| 依赖补 | pom 加百炼 dashscope SDK / spring-boot-starter-data-redis / aliyun-oss-sdk；DDL 建 conversation + 其他表；mapper XML |
