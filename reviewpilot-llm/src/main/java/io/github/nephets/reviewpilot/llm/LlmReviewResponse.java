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
package io.github.nephets.reviewpilot.llm;

import io.github.nephets.reviewpilot.contract.Finding;
import io.github.nephets.reviewpilot.contract.LlmReviewComment;

import java.util.ArrayList;
import java.util.List;

/** LLM 评审响应（不可变值对象）：归一化后的评论、由评论衍生的发现、原始文本与模型标识。 */
public final class LlmReviewResponse {

    private final List<LlmReviewComment> comments;
    private final List<Finding> findings;
    private final String rawText;
    private final String model;

    private LlmReviewResponse(Builder b) {
        this.comments = List.copyOf(b.comments);
        this.findings = List.copyOf(b.findings);
        this.rawText = b.rawText;
        this.model = b.model;
    }

    /** 返回构造器。 */
    public static Builder builder() {
        return new Builder();
    }

    /** 返回评论列表（不可修改）。 */
    public List<LlmReviewComment> getComments() {
        return comments;
    }

    /** 返回由评论衍生的发现（不可修改）。 */
    public List<Finding> getFindings() {
        return findings;
    }

    /** 返回原始模型输出（可为空）。 */
    public String getRawText() {
        return rawText;
    }

    /** 返回实际使用的模型标识（可为空）。 */
    public String getModel() {
        return model;
    }

    @Override
    public String toString() {
        return "LlmReviewResponse{comments="
                + comments.size()
                + ", findings="
                + findings.size()
                + ", model="
                + model
                + '}';
    }

    /** {@link LlmReviewResponse} 构造器。 */
    public static final class Builder {

        private final List<LlmReviewComment> comments = new ArrayList<>();
        private final List<Finding> findings = new ArrayList<>();
        private String rawText;
        private String model;

        private Builder() {}

        /** 追加一条评论。 */
        public Builder addComment(LlmReviewComment comment) {
            if (comment == null) {
                throw new IllegalArgumentException("comment 不可为 null");
            }
            comments.add(comment);
            return this;
        }

        /** 覆盖设置评论列表。 */
        public Builder comments(List<LlmReviewComment> comments) {
            if (comments == null) {
                throw new IllegalArgumentException("comments 不可为 null");
            }
            this.comments.clear();
            this.comments.addAll(comments);
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

        /** 设置原始文本。 */
        public Builder rawText(String rawText) {
            this.rawText = rawText;
            return this;
        }

        /** 设置模型标识。 */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /** 构造响应。 */
        public LlmReviewResponse build() {
            Builder filled = new Builder();
            filled.comments.addAll(comments);
            filled.findings.addAll(findings);
            filled.rawText = rawText;
            filled.model = model;
            return new LlmReviewResponse(filled);
        }
    }
}
