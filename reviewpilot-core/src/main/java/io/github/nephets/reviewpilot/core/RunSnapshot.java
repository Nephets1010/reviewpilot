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
import io.github.nephets.reviewpilot.contract.ReviewRunStatus;
import io.github.nephets.reviewpilot.contract.StaticCheckResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 运行快照（不可变）：某一时刻运行状态的完整副本，组成版本链。
 *
 * <p><b>为什么快照要持有完整副本而非仅版本号 / 计数：</b>回退需要“回到那一刻的精确状态”——阶段状态、累计发现、评论、静态结果。
 * 只存计数无法还原被回退区间新增的发现；持副本虽多占一点内存，但回退语义最简单、最不易错。
 */
public final class RunSnapshot {

    private final int version;
    private final ReviewRunStatus status;
    private final Map<String, StageStatus> stageStatuses;
    private final List<Finding> findings;
    private final List<LlmReviewComment> llmComments;
    private final List<StaticCheckResult> staticResults;
    private final List<DiffFile> diffFiles;
    private final Instant timestamp;
    private final String note;

    RunSnapshot(Builder b) {
        this.version = b.version;
        this.status = b.status;
        this.stageStatuses = b.stageStatuses;
        this.findings = b.findings;
        this.llmComments = b.llmComments;
        this.staticResults = b.staticResults;
        this.diffFiles = b.diffFiles;
        this.timestamp = b.timestamp;
        this.note = b.note;
    }

    /** 返回快照版本号。 */
    public int getVersion() {
        return version;
    }

    /** 返回快照时的运行状态。 */
    public ReviewRunStatus getStatus() {
        return status;
    }

    /** 返回快照时的阶段状态（不可修改）。 */
    public Map<String, StageStatus> getStageStatuses() {
        return stageStatuses;
    }

    /** 返回快照时的发现列表（不可修改）。 */
    public List<Finding> getFindings() {
        return findings;
    }

    /** 返回快照时的 AI 评论列表（不可修改）。 */
    public List<LlmReviewComment> getLlmComments() {
        return llmComments;
    }

    /** 返回快照时的静态结果列表（不可修改）。 */
    public List<StaticCheckResult> getStaticResults() {
        return staticResults;
    }

    /** 返回快照时的 diff 文件列表（不可修改）。 */
    public List<DiffFile> getDiffFiles() {
        return diffFiles;
    }

    /** 返回快照时间戳。 */
    public Instant getTimestamp() {
        return timestamp;
    }

    /** 返回快照备注（如“进入阶段 fetch-diff”）。 */
    public String getNote() {
        return note;
    }

    @Override
    public String toString() {
        return "RunSnapshot{version="
                + version
                + ", status="
                + status
                + ", stageStatuses="
                + stageStatuses.size()
                + ", findings="
                + findings.size()
                + '}';
    }

    /** {@link RunSnapshot} 构造器。 */
    static final class Builder {

        private int version;
        private ReviewRunStatus status;
        private Map<String, StageStatus> stageStatuses;
        private List<Finding> findings;
        private List<LlmReviewComment> llmComments;
        private List<StaticCheckResult> staticResults;
        private List<DiffFile> diffFiles;
        private Instant timestamp;
        private String note;

        Builder version(int version) {
            this.version = version;
            return this;
        }

        Builder status(ReviewRunStatus status) {
            this.status = status;
            return this;
        }

        Builder stageStatuses(Map<String, StageStatus> stageStatuses) {
            this.stageStatuses = stageStatuses;
            return this;
        }

        Builder findings(List<Finding> findings) {
            this.findings = findings;
            return this;
        }

        Builder llmComments(List<LlmReviewComment> llmComments) {
            this.llmComments = llmComments;
            return this;
        }

        Builder staticResults(List<StaticCheckResult> staticResults) {
            this.staticResults = staticResults;
            return this;
        }

        Builder diffFiles(List<DiffFile> diffFiles) {
            this.diffFiles = diffFiles;
            return this;
        }

        Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        Builder note(String note) {
            this.note = note;
            return this;
        }

        RunSnapshot build() {
            return new RunSnapshot(this);
        }
    }
}
