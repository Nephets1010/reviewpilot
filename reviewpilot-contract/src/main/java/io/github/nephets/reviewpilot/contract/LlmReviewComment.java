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
 * AI 评审产出的单条评论（不可变值对象）。
 *
 * <p>由 {@code ReviewLlmClient} 产出，携带位置、严重度、建议、模型标识与原始片段。 聚合阶段会把它转成 {@link Finding} 进入统一发现列表。
 */
public final class LlmReviewComment {

    /** 评论类型。 */
    public enum CommentKind {
        /** 明确的问题。 */
        ISSUE,

        /** 改进建议。 */
        SUGGESTION,

        /** 吹毛求疵。 */
        NIT,

        /** 肯定 / 表扬。 */
        PRAISE
    }

    private final String id;
    private final String filePath;
    private final int line;
    private final ReviewSeverity severity;
    private final String message;
    private final String suggestion;
    private final String model;
    private final CommentKind kind;
    private final String raw;

    private LlmReviewComment(Builder b) {
        this.id = b.id;
        this.filePath = b.filePath;
        this.line = b.line;
        this.severity = b.severity;
        this.message = b.message;
        this.suggestion = b.suggestion;
        this.model = b.model;
        this.kind = b.kind;
        this.raw = b.raw;
    }

    /** 返回构造器。 */
    public static Builder builder() {
        return new Builder();
    }

    /** 返回评论 id。 */
    public String getId() {
        return id;
    }

    /** 返回关联文件路径（可为空）。 */
    public String getFilePath() {
        return filePath;
    }

    /** 返回关联行号，无位置时为 0。 */
    public int getLine() {
        return line;
    }

    /** 返回严重度。 */
    public ReviewSeverity getSeverity() {
        return severity;
    }

    /** 返回说明文字。 */
    public String getMessage() {
        return message;
    }

    /** 返回修复建议（可为空）。 */
    public String getSuggestion() {
        return suggestion;
    }

    /** 返回模型标识（可为空）。 */
    public String getModel() {
        return model;
    }

    /** 返回评论类型。 */
    public CommentKind getKind() {
        return kind;
    }

    /** 返回原始片段（可为空）。 */
    public String getRaw() {
        return raw;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LlmReviewComment)) {
            return false;
        }
        LlmReviewComment that = (LlmReviewComment) other;
        return line == that.line
                && severity == that.severity
                && kind == that.kind
                && Objects.equals(id, that.id)
                && Objects.equals(filePath, that.filePath)
                && Objects.equals(message, that.message)
                && Objects.equals(suggestion, that.suggestion)
                && Objects.equals(model, that.model)
                && Objects.equals(raw, that.raw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, filePath, line, severity, message, suggestion, model, kind, raw);
    }

    @Override
    public String toString() {
        return "LlmReviewComment{severity="
                + severity
                + ", filePath="
                + filePath
                + ", line="
                + line
                + ", message='"
                + message
                + "'}";
    }

    /** {@link LlmReviewComment} 构造器。 */
    public static final class Builder {

        private String id = "llm-" + UUID.randomUUID();
        private String filePath;
        private int line;
        private ReviewSeverity severity = ReviewSeverity.MAJOR;
        private String message = "";
        private String suggestion;
        private String model;
        private CommentKind kind = CommentKind.SUGGESTION;
        private String raw;

        private Builder() {}

        /** 设置评论 id，为 {@code null} 时自动生成。 */
        public Builder id(String id) {
            this.id = id;
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

        /** 设置严重度，未设置默认 MAJOR。 */
        public Builder severity(ReviewSeverity severity) {
            this.severity = severity == null ? ReviewSeverity.MAJOR : severity;
            return this;
        }

        /** 设置说明文字，为 {@code null} 时按空串。 */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /** 设置修复建议，可为空。 */
        public Builder suggestion(String suggestion) {
            this.suggestion = suggestion;
            return this;
        }

        /** 设置模型标识，可为空。 */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /** 设置评论类型，未设置默认 SUGGESTION。 */
        public Builder kind(CommentKind kind) {
            this.kind = kind == null ? CommentKind.SUGGESTION : kind;
            return this;
        }

        /** 设置原始片段，可为空。 */
        public Builder raw(String raw) {
            this.raw = raw;
            return this;
        }

        /** 构造评论。 */
        public LlmReviewComment build() {
            Builder filled = new Builder();
            filled.id = id;
            filled.filePath = filePath;
            filled.line = line;
            filled.severity = severity;
            filled.message = message == null ? "" : message;
            filled.suggestion = suggestion;
            filled.model = model;
            filled.kind = kind;
            filled.raw = raw;
            return new LlmReviewComment(filled);
        }
    }
}
