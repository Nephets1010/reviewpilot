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

import io.github.nephets.reviewpilot.contract.DiffFile;
import io.github.nephets.reviewpilot.contract.Finding;

import java.util.ArrayList;
import java.util.List;

/** LLM 评审请求（不可变值对象）：待评审 diff、既有发现（静态检查已产出）与可选的模型/提示选项。 */
public final class LlmReviewRequest {

    private final List<DiffFile> diffFiles;
    private final List<Finding> existingFindings;
    private final String model;
    private final String locale;

    private LlmReviewRequest(Builder b) {
        this.diffFiles = List.copyOf(b.diffFiles);
        this.existingFindings = List.copyOf(b.existingFindings);
        this.model = b.model;
        this.locale = b.locale;
    }

    /** 返回构造器。 */
    public static Builder builder() {
        return new Builder();
    }

    /** 返回待评审 diff（不可修改）。 */
    public List<DiffFile> getDiffFiles() {
        return diffFiles;
    }

    /** 返回既有发现（不可修改）。 */
    public List<Finding> getExistingFindings() {
        return existingFindings;
    }

    /** 返回目标模型（可为空，由实现决定默认模型）。 */
    public String getModel() {
        return model;
    }

    /** 返回语言区域（如 zh-CN，可为空）。 */
    public String getLocale() {
        return locale;
    }

    @Override
    public String toString() {
        return "LlmReviewRequest{diffFiles="
                + diffFiles.size()
                + ", existingFindings="
                + existingFindings.size()
                + ", model="
                + model
                + '}';
    }

    /** {@link LlmReviewRequest} 构造器。 */
    public static final class Builder {

        private final List<DiffFile> diffFiles = new ArrayList<>();
        private final List<Finding> existingFindings = new ArrayList<>();
        private String model;
        private String locale;

        private Builder() {}

        /** 追加一个 diff 文件。 */
        public Builder addDiffFile(DiffFile diffFile) {
            if (diffFile == null) {
                throw new IllegalArgumentException("diffFile 不可为 null");
            }
            diffFiles.add(diffFile);
            return this;
        }

        /** 覆盖设置 diff 文件列表。 */
        public Builder diffFiles(List<DiffFile> diffFiles) {
            if (diffFiles == null) {
                throw new IllegalArgumentException("diffFiles 不可为 null");
            }
            this.diffFiles.clear();
            this.diffFiles.addAll(diffFiles);
            return this;
        }

        /** 追加一条既有发现。 */
        public Builder addExistingFinding(Finding finding) {
            if (finding == null) {
                throw new IllegalArgumentException("finding 不可为 null");
            }
            existingFindings.add(finding);
            return this;
        }

        /** 覆盖设置既有发现列表。 */
        public Builder existingFindings(List<Finding> existingFindings) {
            if (existingFindings == null) {
                throw new IllegalArgumentException("existingFindings 不可为 null");
            }
            this.existingFindings.clear();
            this.existingFindings.addAll(existingFindings);
            return this;
        }

        /** 设置目标模型。 */
        public Builder model(String model) {
            this.model = model;
            return this;
        }

        /** 设置语言区域。 */
        public Builder locale(String locale) {
            this.locale = locale;
            return this;
        }

        /** 构造请求。 */
        public LlmReviewRequest build() {
            Builder filled = new Builder();
            filled.diffFiles.addAll(diffFiles);
            filled.existingFindings.addAll(existingFindings);
            filled.model = model;
            filled.locale = locale;
            return new LlmReviewRequest(filled);
        }
    }
}
