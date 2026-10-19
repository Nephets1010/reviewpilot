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

import java.util.List;
import java.util.Objects;

/**
 * 一次静态检查工具的执行结果（不可变值对象）。
 *
 * <p>携带工具名、目标文件、产出的发现列表，以及整体是否通过（{@link #passed}）。 即使发现为空，也保留结果以便报告追溯用了哪些工具。
 */
public final class StaticCheckResult {

    private final String toolName;
    private final String filePath;
    private final List<Finding> findings;
    private final boolean passed;
    private final String summary;

    private StaticCheckResult(Builder b) {
        this.toolName = b.toolName;
        this.filePath = b.filePath;
        this.findings = List.copyOf(b.findings);
        this.passed = b.passed;
        this.summary = b.summary;
    }

    /** 返回构造器。 */
    public static Builder builder() {
        return new Builder();
    }

    /** 返回工具名。 */
    public String getToolName() {
        return toolName;
    }

    /** 返回目标文件路径（可为空，表示整仓检查）。 */
    public String getFilePath() {
        return filePath;
    }

    /** 返回发现列表（不可修改）。 */
    public List<Finding> getFindings() {
        return findings;
    }

    /** 返回检查是否通过（无 BLOCKER/CRITICAL 视为通过，由构造器决定）。 */
    public boolean isPassed() {
        return passed;
    }

    /** 返回摘要（可为空）。 */
    public String getSummary() {
        return summary;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StaticCheckResult)) {
            return false;
        }
        StaticCheckResult that = (StaticCheckResult) other;
        return passed == that.passed
                && Objects.equals(toolName, that.toolName)
                && Objects.equals(filePath, that.filePath)
                && Objects.equals(findings, that.findings)
                && Objects.equals(summary, that.summary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toolName, filePath, findings, passed, summary);
    }

    @Override
    public String toString() {
        return "StaticCheckResult{toolName='"
                + toolName
                + "', passed="
                + passed
                + ", findings="
                + findings.size()
                + '}';
    }

    /** {@link StaticCheckResult} 构造器。 */
    public static final class Builder {

        private String toolName;
        private String filePath;
        private final List<Finding> findings = new java.util.ArrayList<>();
        private boolean passed = true;
        private String summary;

        private Builder() {}

        /** 设置工具名，不可为空。 */
        public Builder toolName(String toolName) {
            this.toolName = toolName;
            return this;
        }

        /** 设置目标文件路径，可为空。 */
        public Builder filePath(String filePath) {
            this.filePath = filePath;
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

        /** 设置是否通过。 */
        public Builder passed(boolean passed) {
            this.passed = passed;
            return this;
        }

        /** 设置摘要，可为空。 */
        public Builder summary(String summary) {
            this.summary = summary;
            return this;
        }

        /** 构造静态检查结果。 */
        public StaticCheckResult build() {
            if (toolName == null || toolName.isBlank()) {
                throw new IllegalArgumentException("toolName 不可为空");
            }
            Builder filled = new Builder();
            filled.toolName = toolName;
            filled.filePath = filePath;
            filled.findings.addAll(findings);
            filled.passed = passed;
            filled.summary = summary;
            return new StaticCheckResult(filled);
        }
    }
}
