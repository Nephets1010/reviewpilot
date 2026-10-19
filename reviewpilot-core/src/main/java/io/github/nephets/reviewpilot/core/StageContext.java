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

import io.github.nephets.reviewpilot.contract.DiffFile;
import io.github.nephets.reviewpilot.contract.Finding;
import io.github.nephets.reviewpilot.contract.GateResult;
import io.github.nephets.reviewpilot.contract.LlmReviewComment;
import io.github.nephets.reviewpilot.contract.ReviewReport;
import io.github.nephets.reviewpilot.contract.ReviewRunId;
import io.github.nephets.reviewpilot.contract.RuleConfig;
import io.github.nephets.reviewpilot.contract.StaticCheckResult;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 阶段执行上下文：贯穿一次运行的所有阶段，作为发现与中间产物的累加器。
 *
 * <p><b>为什么用单一可变上下文而非“每阶段返回再串联”：</b>阶段之间有依赖（AiReview 需要 FetchDiff 产出的 diff、聚合需要前两者的发现），
 * 单一上下文让「前序产物」对后续阶段天然可见，也避免把一堆输出字段塞进返回值。引擎每次落存储时只保存上下文里的不可变快照， 阶段内部的可变性不会泄漏到存储之外。
 */
public final class StageContext {

    private final ReviewRunId runId;
    private final String tenantId;
    private final String pipelineId;
    private final Clock clock;
    private final ReviewRequest request;
    private final RuleConfig ruleConfig;

    private final List<Finding> findings = new ArrayList<>();
    private final List<LlmReviewComment> llmComments = new ArrayList<>();
    private final List<StaticCheckResult> staticResults = new ArrayList<>();
    private final Map<String, Object> stageOutputs = new LinkedHashMap<>();

    private List<DiffFile> diffFiles = new ArrayList<>();
    private GateResult gateResult;
    private ReviewReport report;

    /**
     * 构造上下文。
     *
     * @param runId 运行标识，不可为 {@code null}
     * @param tenantId 租户 id
     * @param pipelineId 流水线 id
     * @param clock 时钟
     * @param request 评审请求
     * @param ruleConfig 规则配置
     */
    public StageContext(
            ReviewRunId runId,
            String tenantId,
            String pipelineId,
            Clock clock,
            ReviewRequest request,
            RuleConfig ruleConfig) {
        this.runId = Objects.requireNonNull(runId, "runId");
        this.tenantId = tenantId;
        this.pipelineId = pipelineId;
        this.clock = Objects.requireNonNull(clock, "clock");
        this.request = Objects.requireNonNull(request, "request");
        this.ruleConfig = Objects.requireNonNull(ruleConfig, "ruleConfig");
    }

    /** 返回运行标识。 */
    public ReviewRunId getRunId() {
        return runId;
    }

    /** 返回租户 id。 */
    public String getTenantId() {
        return tenantId;
    }

    /** 返回流水线 id。 */
    public String getPipelineId() {
        return pipelineId;
    }

    /** 返回时钟。 */
    public Clock getClock() {
        return clock;
    }

    /** 返回当前时间（来自注入的时钟）。 */
    public Instant now() {
        return Instant.now(clock);
    }

    /** 返回评审请求。 */
    public ReviewRequest getRequest() {
        return request;
    }

    /** 返回规则配置。 */
    public RuleConfig getRuleConfig() {
        return ruleConfig;
    }

    /** 返回累计发现（可变，供阶段追加）。 */
    public List<Finding> getFindings() {
        return findings;
    }

    /** 追加一条发现。 */
    public void addFinding(Finding finding) {
        findings.add(Objects.requireNonNull(finding, "finding"));
    }

    /** 返回 AI 评论列表（可变）。 */
    public List<LlmReviewComment> getLlmComments() {
        return llmComments;
    }

    /** 追加一条 AI 评论。 */
    public void addLlmComment(LlmReviewComment comment) {
        llmComments.add(Objects.requireNonNull(comment, "comment"));
    }

    /** 返回静态检查结果列表（可变）。 */
    public List<StaticCheckResult> getStaticResults() {
        return staticResults;
    }

    /** 追加一条静态检查结果。 */
    public void addStaticResult(StaticCheckResult result) {
        staticResults.add(Objects.requireNonNull(result, "result"));
    }

    /** 返回 diff 文件列表（可变）。 */
    public List<DiffFile> getDiffFiles() {
        return diffFiles;
    }

    /** 设置 diff 文件列表。 */
    public void setDiffFiles(List<DiffFile> diffFiles) {
        this.diffFiles = diffFiles == null ? new ArrayList<>() : new ArrayList<>(diffFiles);
    }

    /** 返回阶段输出表（可变）。 */
    public Map<String, Object> getStageOutputs() {
        return stageOutputs;
    }

    /** 写入一个阶段输出（按阶段 id 命名空间）。 */
    public void putStageOutput(String key, Object value) {
        stageOutputs.put(key, value);
    }

    /** 读取阶段输出。 */
    public Object getStageOutput(String key) {
        return stageOutputs.get(key);
    }

    /** 返回门禁结果（聚合之后由引擎写入）。 */
    public GateResult getGateResult() {
        return gateResult;
    }

    /** 设置门禁结果。 */
    public void setGateResult(GateResult gateResult) {
        this.gateResult = gateResult;
    }

    /** 返回最终报告（Report 阶段写入）。 */
    public ReviewReport getReport() {
        return report;
    }

    /** 设置最终报告。 */
    public void setReport(ReviewReport report) {
        this.report = report;
    }
}
