# reviewpilot

> AI Code Review 工作流引擎：拉取 Diff → 静态检查 → AI 评审 → 聚合 → 报告，支持 DAG / 条件 / 规则门禁的可插拔工作流内核 + Spring Boot 自动装配。

`reviewpilot` 把一次 AI 代码评审建模成一条**显式工作流**：每个阶段（Stage）是一个有状态、可观测、可回退的节点，阶段之间可以是任意 **DAG**（有依赖关系，而非写死的线性列表），阶段可以带**条件**（条件为假就跳过），跑完之后由**规则门禁**决定整次评审通过还是失败。

## 特性

- 五阶段默认流水线：`FetchDiff → StaticAnalysis → AiReview → Aggregate → Report`。
- 阶段组成 **DAG**：任意依赖关系，支持扇出/扇入、并行无关阶段、独立阶段失败不影响其它分支。
- **条件阶段**：运行期用谓词判断，条件为假直接 `SKIPPED`，不视为失败。
- **规则门禁**：聚合之后按 `RuleConfig` 判定，例如 BLOCKER 数量 ≥ 阈值则整次评审置为 `GATED`（失败）。
- **可插拔 LLM**：`ReviewLlmClient` 接口 + 默认 `MockReviewLlmClient`（确定性、离线、由脚本化 diff 产出 Finding），真实 HTTP 适配器仅留 STUB，默认不接线、测试零联网。
- **多租户**：租户 id（`String`）贯穿引擎与存储，做轻量隔离。
- **快照版本链 + 回退**：每进入/离开一个阶段都留一份快照，可回退到任意历史版本并重新执行。
- **可测试内核**：`Clock` 注入、`Sleeper` 注入（重试退避可断言），零 Spring（CI 扫描 `org.springframework.*`）。

## 模块

| 模块 | 包名 | 说明 |
| --- | --- | --- |
| `reviewpilot-contract` | `io.github.nephets.reviewpilot.contract` | 异常、枚举、不可变值对象。无 Spring。 |
| `reviewpilot-core` | `io.github.nephets.reviewpilot.core` | 工作流引擎、阶段执行器、内存存储、注册表。零 `org.springframework.*`，仅 JDK 21 + slf4j。 |
| `reviewpilot-llm` | `io.github.nephets.reviewpilot.llm` | `ReviewLlmClient` 接口、确定性 Mock、提示词构造、响应解析、真实 HTTP 适配器 STUB。 |
| `reviewpilot-starter` | `io.github.nephets.reviewpilot.starter` | Spring Boot 自动装配、`@ConfigurationProperties`、可选 REST 接口、Jackson 模块。唯一允许 Spring 依赖。 |
| `reviewpilot-sample` | `io.github.nephets.reviewpilot.sample` | 端到端 demo，用 Mock LLM 跑完整流水线。 |

## 快速开始

```java
ReviewLlmClient llm = new MockReviewLlmClient();
ReviewWorkflowEngine engine = new ReviewWorkflowEngine(new InMemoryReviewRunStore());
ReviewPipeline pipeline = ReviewPipelines.defaultPipeline(llm);

String runId = engine.start(pipeline, "tenant-a", ReviewRequest.scripted("demo"));
ReviewRunInstance run = engine.find("tenant-a", runId).orElseThrow();
assert run.getStatus() == ReviewRunStatus.SUCCEEDED;
```

## 设计决策

下面记录产品层面若干关键「为什么」。

1. **为什么阶段要组成 DAG 而不是写死的线性列表？**
   真实评审流程里，静态检查、依赖扫描、安全扫描往往互相独立、可以并行，而「聚合 / 报告」依赖它们全部完成。把阶段固化为 `Fetch→Static→Ai→Aggregate→Report` 一条直线，会把这种天然结构压平，后续加一个独立扫描阶段就要改引擎代码。DAG 把「顺序」与「依赖」分开：引擎只认依赖边，线性只是 DAG 的特例。

2. **为什么 LLM 默认是 Mock 且离线？**
   评审链路上 LLM 是最不可控、最贵的外部依赖；默认接真模型会让单元测试必须联网、必须烧 token、结果还不确定。把 `MockReviewLlmClient` 作为默认实现，由一段**脚本化 diff** 确定性地产出 Finding，测试全程离线且可断言，真实适配器只留 STUB 不接线。

3. **为什么规则门禁放在聚合之后、而不是每个阶段里？**
   门禁要看「全局视角」：单条静态发现可能是 INFO，但聚合后 BLOCKER 总数越过阈值就该整次失败。把门禁嵌入某个阶段会丢失全局计数；放在 `Aggregate` 之后统一判定，既符合「先汇聚再裁决」的语义，也让 `RuleConfig` 能同时参考静态发现与 AI 发现。

4. **为什么 `Clock` 要由构造器注入？**
   快照时间、报告生成时间、阶段耗时全部走注入的 `Clock`。测试固定 `Clock` 后，可以精确断言版本链上的时间戳与顺序，不必等真实墙钟，也不必在断言里容忍时间抖动。

5. **为什么退避等待抽成可注入的 `Sleeper`？**
   重试的核心语义就是「失败后等多久再试」。若直接 `Thread.sleep`，测试要么真睡拖慢 CI，要么只能用极小延迟蒙混。把等待点抽成 `Sleeper`，测试注入一个记录器，就能直接断言「第 N 次失败后等了多久、退避序列是否正确」。

6. **为什么存储要先落库再改内存状态（store-first）？**
   一次评审是跨请求的长时间对象。任何「先改内存、后补存储」的写法，都会在进程重启/崩溃后丢失状态机位置。「状态迁移只走一条路、且每次迁移前先落存储」让引擎重启后能从中断处恢复，而不是凭空猜状态。

7. **为什么需要条件阶段（conditional stage）？**
   不是所有阶段对每次提交都有意义：例如只在改动含 `*.sql` 时才跑 SQL 审核，只在改动含安全敏感目录时才跑安全扫描。条件为假时跳过该阶段（记为 `SKIPPED`），既不中断流程，也避免为「这次不需要的检查」浪费算力。

8. **为什么 Finding / DiffFile / CodeChunk 等要做成不可变值对象？**
   评审产物会在阶段间、线程间、甚至跨进程（序列化进报告）传递。可变对象一旦被某个阶段就地改掉，下游聚合拿到的就是「被污染」的数据，且很难定位是谁改的。不可变值对象 + 显式 `withXxx` 拷贝，让每一次流转都留下清晰快照，也天然线程安全。

9. **为什么发现（Finding）要区分来源（STATIC / AI）与严重度（BLOCKER…INFO）？**
   门禁与报告都依赖这两个维度：门禁按严重度计数，报告按来源分组展示。把来源与严重度固化进 `Finding`，阶段之间就不需要约定额外的「隐藏字段」，聚合与门禁只看值对象本身。

10. **为什么快照要做成「版本链」且支持回退？**
   评审失败排查、人工重跑、灰度回滚都需要「回到某个中间状态」。版本链保留完整历史，回退只是「把某个版本之后的状态丢弃、重置后续阶段为待执行」——版本号只淘汰最旧、不重排，旧引用不会指错位置。

11. **为什么多租户只做轻量隔离（一个 `String` id 贯穿），不引入租户级数据库？**
   本次定位是「内核 + 自动装配」，重隔离（独立 schema / 行级策略）会显著抬高接入门槛。用租户 id 贯穿引擎与存储、查询都按租户过滤，已能满足「A 租户看不到 B 租户的运行」这一最低契约；需要强隔离时由上层存储实现替换即可。

## 已知遗留

- 真实 HTTP LLM 适配器（`StubHttpReviewLlmClient`）未接线、未实现，仅留接口与 STUB，避免测试联网。
- DAG 执行在当前实现中为「拓扑排序后按序推进」，未启用多线程并行；无关阶段的并行是后续可选项。
- 存储默认内存实现（`InMemoryReviewRunStore`），进程重启即丢失；生产可替换为持久化实现（SPI 已预留）。
