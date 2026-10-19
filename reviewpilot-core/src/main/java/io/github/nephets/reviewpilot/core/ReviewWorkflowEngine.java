/*
 * Copyright 2026 The reviewpilot Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.nephets.reviewpilot.core;

import io.github.nephets.reviewpilot.contract.GateResult;
import io.github.nephets.reviewpilot.contract.ReviewReport;
import io.github.nephets.reviewpilot.contract.ReviewRunId;
import io.github.nephets.reviewpilot.contract.ReviewRunNotFoundException;
import io.github.nephets.reviewpilot.contract.ReviewRunStatus;
import io.github.nephets.reviewpilot.contract.ReviewStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

/**
 * 评审工作流引擎：按 DAG 依赖推进一次运行，负责重试退避、条件跳过、依赖阻断、规则门禁与快照回退。
 *
 * <p>执行语义：
 *
 * <ul>
 *   <li>阶段按拓扑序执行；某前置阶段 {@link StageStatus#FAILED} 时，依赖它的阶段记为 {@link StageStatus#BLOCKED} 而不执行；
 *   <li>阶段带 {@link StageCondition} 且运行期求值为假时，记为 {@link StageStatus#SKIPPED}，不视为失败，也不阻断下游；
 *   <li>阶段执行抛异常或返回 {@link StageStatus#FAILED} 消耗一次重试预算，预算内按 {@link ReviewRetryPolicy} 退避后重跑，
 *       耗尽则整次运行置为 {@link ReviewRunStatus#FAILED}；
 *   <li>聚合阶段完成后立即套用规则门禁（{@link io.github.nephets.reviewpilot.contract.RuleConfig}）， 门禁未通过整次运行置为
 *       {@link ReviewRunStatus#GATED}；
 *   <li>进入/离开每个阶段都生成一份 {@link RunSnapshot}，可用 {@link #rollbackTo} 回到历史版本并重新执行。
 * </ul>
 *
 * <p><b>设计约束（为什么）：</b>
 *
 * <ul>
 *   <li>状态迁移只走 {@link #recordStageStatus} / {@link #applyGate} 等少数入口，且每次迁移前先 {@code
 *       store.save}（store-first）： 运行是跨请求的长生命周期对象，任何“先改内存后补存”都会让进程重启后丢状态；
 *   <li>{@link Clock} 由构造器注入：快照时间、报告时间全部走它，测试固定时间即可断言版本链，不必依赖真实时钟；
 *   <li>退避等待走可注入的 {@link Sleeper}（默认系统睡眠）：重试的核心语义是“失败后等多久再试”， 直接 {@link Thread#sleep}
 *       会让测试要么真睡、要么靠极小时长蒙混，抽成注入点后才能对退避序列本身断言；
 *   <li>快照保留完整版本链：回退是相对历史版本的操作，版本号被回收或重排会让旧引用指错位置，因此只淘汰最旧、不重排；
 *   <li>观测入口（{@link #find}、{@link #findByStatus}、{@link #findAll}）只读存储且对 {@code null} 入参返回空结果：
 *       监控与运维脚本不应因为一次手滑查询把引擎打挂。
 * </ul>
 */
public final class ReviewWorkflowEngine implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ReviewWorkflowEngine.class);

    private static final int DEFAULT_SNAPSHOT_LIMIT = 50;

    private final ReviewRunStore store;
    private final ReviewRegistry registry;
    private final Clock clock;
    private final int snapshotLimit;
    private final Sleeper sleeper;

    /**
     * 用内存存储与默认配置构造引擎。
     *
     * @param store 运行实例存储
     */
    public ReviewWorkflowEngine(ReviewRunStore store) {
        this(store, new ReviewRegistry(), Clock.systemUTC(), DEFAULT_SNAPSHOT_LIMIT);
    }

    /**
     * 构造引擎。
     *
     * @param store 运行实例存储
     * @param registry 流水线注册表
     * @param clock 时钟，便于测试固定时间
     * @param snapshotLimit 单个运行保留的快照上限
     */
    public ReviewWorkflowEngine(
            ReviewRunStore store, ReviewRegistry registry, Clock clock, int snapshotLimit) {
        this(store, registry, clock, snapshotLimit, ReviewWorkflowEngine::systemSleep);
    }

    /**
     * 构造引擎，退避等待可注入。
     *
     * <p>包私有：仅供同包测试替换退避等待，避免测试真睡；生产路径一律走 {@link #ReviewWorkflowEngine(ReviewRunStore,
     * ReviewRegistry, Clock, int)}。
     *
     * @param store 运行实例存储
     * @param registry 流水线注册表
     * @param clock 时钟
     * @param snapshotLimit 单个运行保留的快照上限
     * @param sleeper 退避等待实现，为 {@code null} 时回退系统睡眠
     */
    ReviewWorkflowEngine(
            ReviewRunStore store,
            ReviewRegistry registry,
            Clock clock,
            int snapshotLimit,
            Sleeper sleeper) {
        this.store = Objects.requireNonNull(store, "store");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (snapshotLimit < 1) {
            throw new IllegalArgumentException("snapshotLimit 必须 >= 1，当前值：" + snapshotLimit);
        }
        this.snapshotLimit = snapshotLimit;
        this.sleeper = sleeper == null ? ReviewWorkflowEngine::systemSleep : sleeper;
    }

    /**
     * 退避等待的注入点。
     *
     * <p>为什么存在：重试路径上唯一的真实等待就是退避。默认实现走 {@link Thread#sleep}，测试可替换为记录器， 直接断言“第 N 次失败后等了多久”，而不是靠墙钟时间猜。
     */
    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private static void systemSleep(Duration duration) throws InterruptedException {
        Thread.sleep(duration.toMillis());
    }

    /** 返回运行实例存储。 */
    public ReviewRunStore getRunStore() {
        return store;
    }

    /** 返回流水线注册表。 */
    public ReviewRegistry getRegistry() {
        return registry;
    }

    /**
     * 启动一次运行并同步跑到底，返回运行 id。
     *
     * @param pipeline 流水线定义
     * @param tenantId 租户 id，空白时回退默认租户
     * @param request 评审请求
     * @return 运行 id（字符串）
     */
    public String start(ReviewPipeline pipeline, String tenantId, ReviewRequest request) {
        Objects.requireNonNull(pipeline, "pipeline");
        Objects.requireNonNull(request, "request");
        String resolved = ReviewTenant.resolve(tenantId);
        log.info(
                "start 启动运行: pipelineId={}, version={}, tenantId={}, stageCount={}",
                pipeline.getPipelineId(),
                pipeline.getVersion(),
                resolved,
                pipeline.size());
        registry.register(pipeline);
        ReviewRunId runId = ReviewRunId.generate();
        Instant now = Instant.now(clock);
        ReviewRunInstance instance =
                ReviewRunInstance.create(runId, resolved, pipeline, request, now);
        instance.touch(now);
        save(instance);
        ReviewTenant.runWith(
                resolved, () -> runAll(instance, pipeline, newContext(instance, pipeline)));
        return runId.asString();
    }

    /**
     * 按已注册的流水线 id 启动一次运行。
     *
     * @param pipelineId 流水线 id
     * @param tenantId 租户 id
     * @param request 评审请求
     * @return 运行 id
     * @throws io.github.nephets.reviewpilot.contract.InvalidReviewDefinitionException 流水线未注册
     */
    public String start(String pipelineId, String tenantId, ReviewRequest request) {
        ReviewPipeline pipeline = registry.require(pipelineId);
        return start(pipeline, tenantId, request);
    }

    /**
     * 从当前状态继续运行（用于回退之后重新执行 pending 阶段）。
     *
     * @param tenantId 租户 id
     * @param runId 运行 id
     */
    public void resume(String tenantId, String runId) {
        ReviewRunInstance instance = load(tenantId, runId);
        ReviewRunStatus current = instance.getStatus();
        if (current == ReviewRunStatus.SUCCEEDED
                || current == ReviewRunStatus.FAILED
                || current == ReviewRunStatus.GATED) {
            log.info(
                    "resume 运行已处于终态，跳过恢复: tenantId={}, runId={}, currentStatus={}",
                    instance.getTenantId(),
                    runId,
                    current);
            return;
        }
        ReviewPipeline pipeline = registry.require(instance.getPipelineId());
        log.info(
                "resume 继续运行: tenantId={}, runId={}, currentStatus={}",
                instance.getTenantId(),
                runId,
                current);
        ReviewTenant.runWith(
                instance.getTenantId(),
                () -> runAll(instance, pipeline, newContext(instance, pipeline)));
    }

    /**
     * 回退到指定快照版本：还原阶段状态与累计产物，状态置为 {@link ReviewRunStatus#ROLLED_BACK}，并丢弃该版本之后的快照。
     *
     * @param tenantId 租户 id
     * @param runId 运行 id
     * @param snapshotVersion 快照版本号
     */
    public void rollbackTo(String tenantId, String runId, int snapshotVersion) {
        ReviewRunInstance instance = load(tenantId, runId);
        log.info(
                "rollbackTo 回退: tenantId={}, runId={}, snapshotVersion={}",
                instance.getTenantId(),
                runId,
                snapshotVersion);
        RunSnapshot target =
                instance.snapshot(snapshotVersion)
                        .orElseThrow(
                                () -> {
                                    log.error(
                                            "rollbackTo 快照版本不存在: tenantId={}, runId={},"
                                                    + " snapshotVersion={}",
                                            instance.getTenantId(),
                                            runId,
                                            snapshotVersion);
                                    return new IllegalArgumentException(
                                            "快照版本不存在：" + snapshotVersion + "，runId=" + runId);
                                });
        instance.replaceStageStatuses(new LinkedHashMap<>(target.getStageStatuses()));
        instance.getFindings().clear();
        instance.getFindings().addAll(target.getFindings());
        instance.getLlmComments().clear();
        instance.getLlmComments().addAll(target.getLlmComments());
        instance.getStaticResults().clear();
        instance.getStaticResults().addAll(target.getStaticResults());
        instance.setDiffFiles(target.getDiffFiles());
        instance.setGated(false);
        instance.setGateReason(null);
        instance.setStatus(ReviewRunStatus.ROLLED_BACK);
        instance.dropSnapshotsAfter(snapshotVersion);
        instance.touch(Instant.now(clock));
        save(instance);
        log.info(
                "rollbackTo 回退完成: tenantId={}, runId={}, snapshotVersion={}",
                instance.getTenantId(),
                runId,
                snapshotVersion);
    }

    /** 释放引擎资源（当前无后台线程，仅记录日志，便于在 Spring 容器关闭时对称调用）。 */
    @Override
    public void close() {
        log.info("close 关闭评审工作流引擎");
    }

    /**
     * 查询运行实例。
     *
     * @param tenantId 租户 id
     * @param runId 运行 id
     * @return 运行实例，不存在时返回 {@link java.util.Optional#empty()}
     */
    public java.util.Optional<ReviewRunInstance> find(String tenantId, String runId) {
        log.debug("find 查询运行: tenantId={}, runId={}", tenantId, runId);
        return store.findById(tenantId, runId);
    }

    /**
     * 按状态列出某租户下的运行实例。
     *
     * @param tenantId 租户 id
     * @param status 运行状态
     * @return 运行实例列表
     */
    public List<ReviewRunInstance> findByStatus(String tenantId, ReviewRunStatus status) {
        log.debug("findByStatus 按状态查询: tenantId={}, status={}", tenantId, status);
        return store.findByStatus(tenantId, status);
    }

    /**
     * 列出某租户下的全部运行实例。
     *
     * @param tenantId 租户 id
     * @return 运行实例列表
     */
    public List<ReviewRunInstance> findAll(String tenantId) {
        log.debug("findAll 列出全部: tenantId={}", tenantId);
        return store.findAll(tenantId);
    }

    private void runAll(ReviewRunInstance instance, ReviewPipeline pipeline, StageContext ctx) {
        instance.setStatus(ReviewRunStatus.RUNNING);
        save(instance);
        log.debug(
                "runAll 执行运行: runId={}, pipelineId={}, stageCount={}",
                instance.getRunId(),
                instance.getPipelineId(),
                pipeline.size());
        for (ReviewStageDef stage : pipeline.topologicalOrder()) {
            String stageId = stage.getStageId();
            StageStatus current = instance.stageStatus(stageId);
            if (current == StageStatus.SUCCEEDED) {
                // 回退后续跑：已成功的阶段不再重跑，但前置产物（diff/发现）已在 ctx 中恢复。
                continue;
            }
            StageStatus blocker = dependencyBlocker(instance, stage);
            if (blocker != null) {
                instance.recordStageStatus(stageId, StageStatus.BLOCKED);
                snapshot(instance, "stage " + stageId + " blocked by " + blocker);
                save(instance);
                log.warn(
                        "runAll 阶段被阻断: runId={}, stageId={}, by={}",
                        instance.getRunId(),
                        stageId,
                        blocker);
                continue;
            }
            if (stage.hasCondition() && !stage.getCondition().test(ctx)) {
                instance.recordStageStatus(stageId, StageStatus.SKIPPED);
                snapshot(instance, "stage " + stageId + " skipped: condition false");
                save(instance);
                log.info("runAll 阶段条件为假被跳过: runId={}, stageId={}", instance.getRunId(), stageId);
                continue;
            }
            instance.recordStageStatus(stageId, StageStatus.RUNNING);
            snapshot(instance, "enter stage " + stageId);
            save(instance);
            StageResult result = executeStage(instance, stage, ctx);
            syncArtifacts(instance, ctx);
            if (result.getStatus() == StageStatus.FAILED) {
                instance.recordStageStatus(stageId, StageStatus.FAILED);
                instance.setError(result.getMessage());
                snapshot(instance, "stage " + stageId + " failed");
                instance.setStatus(ReviewRunStatus.FAILED);
                save(instance);
                log.error(
                        "runAll 阶段失败，整次运行失败: runId={}, stageId={}, message={}",
                        instance.getRunId(),
                        stageId,
                        result.getMessage());
                return;
            }
            instance.recordStageStatus(stageId, result.getStatus());
            if (stage.getStageId().equals(ReviewStage.AGGREGATE.id())) {
                applyGate(instance, ctx);
            }
            snapshot(instance, "exit stage " + stageId);
            save(instance);
        }
        if (!instance.isGated() && instance.getStatus() != ReviewRunStatus.FAILED) {
            applyGate(instance, ctx);
        }
        finish(instance);
        save(instance);
        log.info("runAll 运行结束: runId={}, status={}", instance.getRunId(), instance.getStatus());
    }

    private StageStatus dependencyBlocker(ReviewRunInstance instance, ReviewStageDef stage) {
        for (String dep : stage.getDependsOn()) {
            StageStatus depStatus = instance.stageStatus(dep);
            if (depStatus == StageStatus.FAILED || depStatus == StageStatus.BLOCKED) {
                return depStatus;
            }
        }
        return null;
    }

    private StageResult executeStage(
            ReviewRunInstance instance, ReviewStageDef stage, StageContext ctx) {
        ReviewRetryPolicy retry = stage.getRetryPolicy();
        int attempt = 1;
        while (true) {
            StageResult result;
            try {
                result = stage.getExecutor().execute(ctx);
            } catch (RuntimeException e) {
                if (retry.allows(attempt)) {
                    log.debug(
                            "executeStage 阶段异常，退避后重试: runId={}, stageId={}, attempt={}",
                            instance.getRunId(),
                            stage.getStageId(),
                            attempt);
                    backoff(retry, attempt);
                    attempt++;
                    continue;
                }
                log.error(
                        "executeStage 阶段异常且重试耗尽: runId={}, stageId={}, attempt={}",
                        instance.getRunId(),
                        stage.getStageId(),
                        attempt,
                        e);
                return StageResult.failure("阶段执行异常且重试耗尽：" + e.getMessage());
            }
            if (result == null) {
                log.warn(
                        "executeStage 执行器返回 null，按失败处理: runId={}, stageId={}",
                        instance.getRunId(),
                        stage.getStageId());
                result = StageResult.failure("执行器返回 null：" + stage.getStageId());
            }
            if (result.getStatus() == StageStatus.SUCCEEDED
                    || result.getStatus() == StageStatus.SKIPPED) {
                return result;
            }
            if (retry.allows(attempt)) {
                log.debug(
                        "executeStage 阶段失败，退避后重试: runId={}, stageId={}, attempt={}",
                        instance.getRunId(),
                        stage.getStageId(),
                        attempt);
                backoff(retry, attempt);
                attempt++;
                continue;
            }
            log.error(
                    "executeStage 阶段失败且重试耗尽: runId={}, stageId={}, message={}",
                    instance.getRunId(),
                    stage.getStageId(),
                    result.getMessage());
            return result;
        }
    }

    private void applyGate(ReviewRunInstance instance, StageContext ctx) {
        GateResult gate = ctx.getRuleConfig().evaluate(ctx.getFindings());
        ctx.setGateResult(gate);
        if (!gate.isPassed()) {
            instance.setGated(true);
            instance.setGateReason(gate.getReason());
            log.info("applyGate 门禁未通过: runId={}, reason={}", instance.getRunId(), gate.getReason());
        }
    }

    private void finish(ReviewRunInstance instance) {
        if (instance.getStatus() == ReviewRunStatus.FAILED) {
            return;
        }
        ReviewRunStatus finalStatus =
                instance.isGated() ? ReviewRunStatus.GATED : ReviewRunStatus.SUCCEEDED;
        instance.setStatus(finalStatus);
        ReviewReport report = instance.getReport();
        if (report != null) {
            ReviewReport rebuilt =
                    ReviewReport.builder()
                            .runId(report.getRunId())
                            .tenantId(report.getTenantId())
                            .pipelineId(report.getPipelineId())
                            .status(finalStatus)
                            .generatedAt(report.getGeneratedAt())
                            .findings(report.getFindings())
                            .gate(report.getGate())
                            .summary(report.getSummary())
                            .build();
            instance.setReport(rebuilt);
        }
    }

    private void syncArtifacts(ReviewRunInstance instance, StageContext ctx) {
        instance.getFindings().clear();
        instance.getFindings().addAll(ctx.getFindings());
        instance.getLlmComments().clear();
        instance.getLlmComments().addAll(ctx.getLlmComments());
        instance.getStaticResults().clear();
        instance.getStaticResults().addAll(ctx.getStaticResults());
        instance.setDiffFiles(ctx.getDiffFiles());
        if (ctx.getReport() != null) {
            instance.setReport(ctx.getReport());
        }
    }

    private void snapshot(ReviewRunInstance instance, String note) {
        int version = instance.latestSnapshotVersion() + 1;
        RunSnapshot snap =
                new RunSnapshot.Builder()
                        .version(version)
                        .status(instance.getStatus())
                        .stageStatuses(new LinkedHashMap<>(instance.getStageStatuses()))
                        .findings(new ArrayList<>(instance.getFindings()))
                        .llmComments(new ArrayList<>(instance.getLlmComments()))
                        .staticResults(new ArrayList<>(instance.getStaticResults()))
                        .diffFiles(new ArrayList<>(instance.getDiffFiles()))
                        .timestamp(Instant.now(clock))
                        .note(note)
                        .build();
        instance.addSnapshot(snap);
        while (instance.getSnapshots().size() > snapshotLimit) {
            instance.getSnapshots().remove(0);
        }
    }

    private StageContext newContext(ReviewRunInstance instance, ReviewPipeline pipeline) {
        StageContext ctx =
                new StageContext(
                        instance.getRunId(),
                        instance.getTenantId(),
                        instance.getPipelineId(),
                        clock,
                        instance.getRequest(),
                        pipeline.getRuleConfig());
        ctx.getFindings().addAll(instance.getFindings());
        ctx.getLlmComments().addAll(instance.getLlmComments());
        ctx.getStaticResults().addAll(instance.getStaticResults());
        ctx.setDiffFiles(instance.getDiffFiles());
        return ctx;
    }

    private void save(ReviewRunInstance instance) {
        instance.touch(Instant.now(clock));
        store.save(instance);
    }

    private ReviewRunInstance load(String tenantId, String runId) {
        return store.findById(tenantId, runId)
                .orElseThrow(
                        () -> {
                            log.error("load 未找到运行实例: tenantId={}, runId={}", tenantId, runId);
                            return new ReviewRunNotFoundException(
                                    "未找到运行实例：tenant=" + tenantId + "，runId=" + runId);
                        });
    }

    private void backoff(ReviewRetryPolicy retry, int attempt) {
        Duration pause = retry.backoffFor(attempt);
        if (pause == null || pause.isZero() || pause.isNegative()) {
            return;
        }
        try {
            sleeper.sleep(pause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("backoff 重试退避被中断: attempt={}", attempt, e);
            throw new IllegalStateException("重试退避等待被中断", e);
        }
    }
}
