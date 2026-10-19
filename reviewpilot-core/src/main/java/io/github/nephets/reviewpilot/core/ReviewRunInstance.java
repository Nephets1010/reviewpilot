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
import io.github.nephets.reviewpilot.contract.LlmReviewComment;
import io.github.nephets.reviewpilot.contract.ReviewReport;
import io.github.nephets.reviewpilot.contract.ReviewRunId;
import io.github.nephets.reviewpilot.contract.ReviewRunStatus;
import io.github.nephets.reviewpilot.contract.StaticCheckResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 一次评审运行的内存态：状态、各阶段状态、累计产物与快照版本链。
 *
 * <p>引擎持有并就地修改此对象，每次变更前先落存储（store-first），因此进程重启后能恢复到中断处。 快照保留完整版本链，供 {@link
 * ReviewWorkflowEngine#rollbackTo} 回退。
 */
public final class ReviewRunInstance {

    private final ReviewRunId runId;
    private final String tenantId;
    private final String pipelineId;
    private final int pipelineVersion;
    private final Instant createdAt;
    private final ReviewRequest request;

    private ReviewRunStatus status;
    private Instant updatedAt;
    private final Map<String, StageStatus> stageStatuses = new LinkedHashMap<>();
    private final List<Finding> findings = new ArrayList<>();
    private final List<LlmReviewComment> llmComments = new ArrayList<>();
    private final List<StaticCheckResult> staticResults = new ArrayList<>();
    private final List<DiffFile> diffFiles = new ArrayList<>();
    private final List<RunSnapshot> snapshots = new ArrayList<>();
    private boolean gated;
    private String gateReason;
    private String error;
    private ReviewReport report;

    private ReviewRunInstance(Builder b) {
        this.runId = b.runId;
        this.tenantId = b.tenantId;
        this.pipelineId = b.pipelineId;
        this.pipelineVersion = b.pipelineVersion;
        this.createdAt = b.createdAt;
        this.request = b.request;
        this.status = ReviewRunStatus.PENDING;
        this.updatedAt = b.createdAt;
    }

    /** 创建运行实例并初始化各阶段为 PENDING。 */
    public static ReviewRunInstance create(
            ReviewRunId runId,
            String tenantId,
            ReviewPipeline pipeline,
            ReviewRequest request,
            Instant createdAt) {
        ReviewRunInstance instance =
                new ReviewRunInstance(
                        new Builder()
                                .runId(runId)
                                .tenantId(tenantId)
                                .pipelineId(pipeline.getPipelineId())
                                .pipelineVersion(pipeline.getVersion())
                                .request(request)
                                .createdAt(createdAt));
        for (ReviewStageDef stage : pipeline.getStages()) {
            instance.stageStatuses.put(stage.getStageId(), StageStatus.PENDING);
        }
        return instance;
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

    /** 返回流水线版本。 */
    public int getPipelineVersion() {
        return pipelineVersion;
    }

    /** 返回本次运行的请求（含内联 diff 等输入）。 */
    public ReviewRequest getRequest() {
        return request;
    }

    /** 返回运行状态。 */
    public ReviewRunStatus getStatus() {
        return status;
    }

    /** 设置运行状态。 */
    public void setStatus(ReviewRunStatus status) {
        this.status = status;
    }

    /** 返回创建时间。 */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /** 返回最后更新时间。 */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** 记录阶段状态。 */
    public void recordStageStatus(String stageId, StageStatus status) {
        stageStatuses.put(stageId, status);
    }

    /** 读取阶段状态（未知阶段返回 {@code null}）。 */
    public StageStatus stageStatus(String stageId) {
        return stageStatuses.get(stageId);
    }

    /** 整体替换阶段状态表（回退时使用）。 */
    public void replaceStageStatuses(Map<String, StageStatus> statuses) {
        stageStatuses.clear();
        if (statuses != null) {
            stageStatuses.putAll(statuses);
        }
    }

    /** 返回阶段状态表（不可修改）。 */
    public Map<String, StageStatus> getStageStatuses() {
        return Collections.unmodifiableMap(stageStatuses);
    }

    /** 返回累计发现（可变）。 */
    public List<Finding> getFindings() {
        return findings;
    }

    /** 返回 AI 评论列表（可变）。 */
    public List<LlmReviewComment> getLlmComments() {
        return llmComments;
    }

    /** 返回静态结果列表（可变）。 */
    public List<StaticCheckResult> getStaticResults() {
        return staticResults;
    }

    /** 返回 diff 文件列表（可变）。 */
    public List<DiffFile> getDiffFiles() {
        return diffFiles;
    }

    /** 设置 diff 文件列表（拷贝）。 */
    public void setDiffFiles(List<DiffFile> diffFiles) {
        this.diffFiles.clear();
        if (diffFiles != null) {
            this.diffFiles.addAll(diffFiles);
        }
    }

    /** 返回快照版本链（可变，按版本递增）。 */
    public List<RunSnapshot> getSnapshots() {
        return snapshots;
    }

    /** 追加一份快照。 */
    public void addSnapshot(RunSnapshot snapshot) {
        snapshots.add(Objects.requireNonNull(snapshot, "snapshot"));
    }

    /** 当前最新快照版本号（无快照时返回 0）。 */
    public int latestSnapshotVersion() {
        return snapshots.isEmpty() ? 0 : snapshots.get(snapshots.size() - 1).getVersion();
    }

    /** 按版本号取快照。 */
    public Optional<RunSnapshot> snapshot(int version) {
        for (RunSnapshot s : snapshots) {
            if (s.getVersion() == version) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    /** 丢弃版本严格大于 {@code version} 的所有快照（回退时使用）。 */
    public void dropSnapshotsAfter(int version) {
        snapshots.removeIf(s -> s.getVersion() > version);
    }

    /** 是否被门禁拦截。 */
    public boolean isGated() {
        return gated;
    }

    /** 设置门禁拦截标记。 */
    public void setGated(boolean gated) {
        this.gated = gated;
    }

    /** 返回门禁原因（未拦截时为 {@code null}）。 */
    public String getGateReason() {
        return gateReason;
    }

    /** 设置门禁原因。 */
    public void setGateReason(String gateReason) {
        this.gateReason = gateReason;
    }

    /** 返回失败/错误信息（可为空）。 */
    public String getError() {
        return error;
    }

    /** 设置失败/错误信息。 */
    public void setError(String error) {
        this.error = error;
    }

    /** 返回最终报告（未生成时为 {@code null}）。 */
    public ReviewReport getReport() {
        return report;
    }

    /** 设置最终报告。 */
    public void setReport(ReviewReport report) {
        this.report = report;
    }

    /** 刷新最后更新时间（store-first：落库前调用）。 */
    public void touch(Instant now) {
        this.updatedAt = now;
    }

    @Override
    public String toString() {
        return "ReviewRunInstance{runId="
                + runId
                + ", tenantId="
                + tenantId
                + ", status="
                + status
                + ", stageStatuses="
                + stageStatuses
                + ", findings="
                + findings.size()
                + '}';
    }

    /** 构造器（仅引擎内部使用）。 */
    private static final class Builder {

        private ReviewRunId runId;
        private String tenantId;
        private String pipelineId;
        private int pipelineVersion;
        private Instant createdAt;
        private ReviewRequest request;

        Builder runId(ReviewRunId runId) {
            this.runId = runId;
            return this;
        }

        Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        Builder pipelineId(String pipelineId) {
            this.pipelineId = pipelineId;
            return this;
        }

        Builder pipelineVersion(int pipelineVersion) {
            this.pipelineVersion = pipelineVersion;
            return this;
        }

        Builder request(ReviewRequest request) {
            this.request = request;
            return this;
        }

        Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
    }
}
