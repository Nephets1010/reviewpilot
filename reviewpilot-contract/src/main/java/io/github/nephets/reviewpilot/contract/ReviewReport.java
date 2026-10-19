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
package io.github.nephets.reviewpilot.contract;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 评审报告（不可变值对象）：一次运行结束后的汇总，含状态、发现列表、门禁结果与摘要。
 *
 * <p>由 Report 阶段在流水线末端构造。报告只读、可序列化，便于直接返回给调用方或落库。
 */
public final class ReviewReport {

    private final ReviewRunId runId;
    private final String tenantId;
    private final String pipelineId;
    private final ReviewRunStatus status;
    private final Instant generatedAt;
    private final List<Finding> findings;
    private final GateResult gate;
    private final String summary;

    private ReviewReport(Builder b) {
        this.runId = b.runId;
        this.tenantId = b.tenantId;
        this.pipelineId = b.pipelineId;
        this.status = b.status;
        this.generatedAt = b.generatedAt;
        this.findings = List.copyOf(b.findings);
        this.gate = b.gate;
        this.summary = b.summary;
    }

    /** 返回构造器。 */
    public static Builder builder() {
        return new Builder();
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

    /** 返回运行状态。 */
    public ReviewRunStatus getStatus() {
        return status;
    }

    /** 返回报告生成时间。 */
    public Instant getGeneratedAt() {
        return generatedAt;
    }

    /** 返回发现列表（不可修改）。 */
    public List<Finding> getFindings() {
        return findings;
    }

    /** 返回门禁结果（门禁关闭时可能为 {@code null}）。 */
    public GateResult getGate() {
        return gate;
    }

    /** 返回摘要（可为空）。 */
    public String getSummary() {
        return summary;
    }

    /** 返回发现总数。 */
    public int totalFindings() {
        return findings.size();
    }

    /**
     * 统计指定严重度的发现数量。
     *
     * @param severity 严重度
     * @return 数量
     */
    public long countBySeverity(ReviewSeverity severity) {
        Objects.requireNonNull(severity, "severity");
        long count = 0;
        for (Finding f : findings) {
            if (f.getSeverity() == severity) {
                count++;
            }
        }
        return count;
    }

    /** 是否至少含一条 BLOCKER 发现。 */
    public boolean hasBlocker() {
        return countBySeverity(ReviewSeverity.BLOCKER) > 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ReviewReport)) {
            return false;
        }
        ReviewReport that = (ReviewReport) other;
        return Objects.equals(runId, that.runId)
                && Objects.equals(tenantId, that.tenantId)
                && Objects.equals(pipelineId, that.pipelineId)
                && status == that.status
                && Objects.equals(generatedAt, that.generatedAt)
                && Objects.equals(findings, that.findings)
                && Objects.equals(gate, that.gate)
                && Objects.equals(summary, that.summary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                runId, tenantId, pipelineId, status, generatedAt, findings, gate, summary);
    }

    @Override
    public String toString() {
        return "ReviewReport{runId="
                + runId
                + ", status="
                + status
                + ", findings="
                + findings.size()
                + '}';
    }

    /** {@link ReviewReport} 构造器。 */
    public static final class Builder {

        private ReviewRunId runId;
        private String tenantId;
        private String pipelineId;
        private ReviewRunStatus status = ReviewRunStatus.PENDING;
        private Instant generatedAt = Instant.EPOCH;
        private final List<Finding> findings = new java.util.ArrayList<>();
        private GateResult gate;
        private String summary;

        private Builder() {}

        /** 设置运行标识，不可为空。 */
        public Builder runId(ReviewRunId runId) {
            this.runId = runId;
            return this;
        }

        /** 设置租户 id，可为空。 */
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        /** 设置流水线 id，可为空。 */
        public Builder pipelineId(String pipelineId) {
            this.pipelineId = pipelineId;
            return this;
        }

        /** 设置运行状态，未设置默认 PENDING。 */
        public Builder status(ReviewRunStatus status) {
            this.status = status == null ? ReviewRunStatus.PENDING : status;
            return this;
        }

        /** 设置生成时间，未设置默认 EPOCH。 */
        public Builder generatedAt(Instant generatedAt) {
            this.generatedAt = generatedAt == null ? Instant.EPOCH : generatedAt;
            return this;
        }

        /** 追加一条发现。 */
        public Builder addFinding(Finding finding) {
            if (finding == null) {
                throw new IllegalArgumentException("finding 不可为 null");
            }
            findings.add(finding);
            return this;
        }

        /** 覆盖设置发现列表。 */
        public Builder findings(List<Finding> findings) {
            if (findings == null) {
                throw new IllegalArgumentException("findings 不可为 null");
            }
            this.findings.clear();
            this.findings.addAll(findings);
            return this;
        }

        /** 设置门禁结果，可为空。 */
        public Builder gate(GateResult gate) {
            this.gate = gate;
            return this;
        }

        /** 设置摘要，可为空。 */
        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        /** 构造报告。 */
        public ReviewReport build() {
            if (runId == null) {
                throw new IllegalArgumentException("runId 不可为空");
            }
            Builder filled = new Builder();
            filled.runId = runId;
            filled.tenantId = tenantId;
            filled.pipelineId = pipelineId;
            filled.status = status;
            filled.generatedAt = generatedAt;
            filled.findings.addAll(findings);
            filled.gate = gate;
            filled.summary = summary;
            return new ReviewReport(filled);
        }
    }
}
