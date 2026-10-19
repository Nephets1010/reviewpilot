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

import java.util.Objects;
import java.util.UUID;

/**
 * 一条评审发现（不可变值对象）。
 *
 * <p>贯穿静态检查与 AI 评审两个来源，聚合后统一进入报告。关键维度是严重度（{@link ReviewSeverity}） 与来源（{@link
 * FindingSource}）：门禁与报告都只依据这两个维度做判定与分组。
 */
public final class Finding {

    private final String id;
    private final ReviewSeverity severity;
    private final FindingCategory category;
    private final FindingSource source;
    private final String filePath;
    private final int line;
    private final String message;
    private final String ruleId;
    private final String suggestion;
    private final String code;

    private Finding(Builder b) {
        this.id = b.id;
        this.severity = b.severity;
        this.category = b.category;
        this.source = b.source;
        this.filePath = b.filePath;
        this.line = b.line;
        this.message = b.message;
        this.ruleId = b.ruleId;
        this.suggestion = b.suggestion;
        this.code = b.code;
    }

    /** 返回构造器。 */
    public static Builder builder() {
        return new Builder();
    }

    /** 返回发现 id。 */
    public String getId() {
        return id;
    }

    /** 返回严重度。 */
    public ReviewSeverity getSeverity() {
        return severity;
    }

    /** 返回归类。 */
    public FindingCategory getCategory() {
        return category;
    }

    /** 返回来源。 */
    public FindingSource getSource() {
        return source;
    }

    /** 返回关联文件路径（可为空）。 */
    public String getFilePath() {
        return filePath;
    }

    /** 返回关联行号，无位置信息时为 0。 */
    public int getLine() {
        return line;
    }

    /** 返回说明文字。 */
    public String getMessage() {
        return message;
    }

    /** 返回规则 id（可为空）。 */
    public String getRuleId() {
        return ruleId;
    }

    /** 返回修复建议（可为空）。 */
    public String getSuggestion() {
        return suggestion;
    }

    /** 返回问题代码片段（可为空）。 */
    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Finding)) {
            return false;
        }
        Finding that = (Finding) other;
        return line == that.line
                && severity == that.severity
                && category == that.category
                && source == that.source
                && Objects.equals(id, that.id)
                && Objects.equals(filePath, that.filePath)
                && Objects.equals(message, that.message)
                && Objects.equals(ruleId, that.ruleId)
                && Objects.equals(suggestion, that.suggestion)
                && Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id, severity, category, source, filePath, line, message, ruleId, suggestion, code);
    }

    @Override
    public String toString() {
        return "Finding{severity="
                + severity
                + ", source="
                + source
                + ", filePath="
                + filePath
                + ", line="
                + line
                + ", message='"
                + message
                + "'}";
    }

    /** {@link Finding} 构造器。 */
    public static final class Builder {

        private String id = "fnd-" + UUID.randomUUID();
        private ReviewSeverity severity = ReviewSeverity.MAJOR;
        private FindingCategory category = FindingCategory.OTHER;
        private FindingSource source = FindingSource.STATIC;
        private String filePath;
        private int line;
        private String message = "";
        private String ruleId;
        private String suggestion;
        private String code;

        private Builder() {}

        /** 设置发现 id，为 {@code null} 时自动生成。 */
        public Builder id(String id) {
            this.id = id;
            return this;
        }

        /** 设置严重度，未设置默认 MAJOR。 */
        public Builder severity(ReviewSeverity severity) {
            this.severity = severity == null ? ReviewSeverity.MAJOR : severity;
            return this;
        }

        /** 设置归类，未设置默认 OTHER。 */
        public Builder category(FindingCategory category) {
            this.category = category == null ? FindingCategory.OTHER : category;
            return this;
        }

        /** 设置来源，未设置默认 STATIC。 */
        public Builder source(FindingSource source) {
            this.source = source == null ? FindingSource.STATIC : source;
            return this;
        }

        /** 设置关联文件路径，可为空。 */
        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        /** 设置关联行号，未设置默认 0。 */
        public Builder line(int line) {
            this.line = line;
            return this;
        }

        /** 设置说明文字，为 {@code null} 时按空串。 */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /** 设置规则 id，可为空。 */
        public Builder ruleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        /** 设置修复建议，可为空。 */
        public Builder suggestion(String suggestion) {
            this.suggestion = suggestion;
            return this;
        }

        /** 设置问题代码片段，可为空。 */
        public Builder code(String code) {
            this.code = code;
            return this;
        }

        /** 构造发现。 */
        public Finding build() {
            Builder filled = new Builder();
            filled.id = id;
            filled.severity = severity;
            filled.category = category;
            filled.source = source;
            filled.filePath = filePath;
            filled.line = line;
            filled.message = message == null ? "" : message;
            filled.ruleId = ruleId;
            filled.suggestion = suggestion;
            filled.code = code;
            return new Finding(filled);
        }
    }
}
