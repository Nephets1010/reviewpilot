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

import io.github.nephets.reviewpilot.contract.CodeChunk;
import io.github.nephets.reviewpilot.contract.DiffFile;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 一次评审运行的输入（不可变值对象）。
 *
 * <p>携带目标仓库/分支信息与可选的内联 diff。内置阶段 {@code FetchDiffExecutor} 优先使用内联 diff； 未提供时回退到一份确定性脚本化
 * diff，保证离线、可断言。多租户 id 不在此处，由引擎显式传入。
 */
public final class ReviewRequest {

    private final String repoRef;
    private final String baseRef;
    private final String headRef;
    private final Map<String, Object> options;
    private final List<DiffFile> diffFiles;

    private ReviewRequest(Builder b) {
        this.repoRef = b.repoRef;
        this.baseRef = b.baseRef;
        this.headRef = b.headRef;
        this.options = b.options == null ? Map.of() : Map.copyOf(b.options);
        this.diffFiles = b.diffFiles == null ? List.of() : List.copyOf(b.diffFiles);
    }

    /** 返回构造器。 */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 构造一份带确定性「干净」脚本化 diff 的请求（不含任何会触发安全/性能规则的内容），用于默认跑通。
     *
     * @param name 脚本标识（影响文件路径，便于断言）
     * @return 请求
     */
    public static ReviewRequest scripted(String name) {
        String file =
                "src/main/java/io/github/nephets/demo/" + (name == null ? "Demo" : name) + ".java";
        DiffFile diff =
                DiffFile.builder()
                        .path(file)
                        .status(DiffFile.FileStatus.ADDED)
                        .language("java")
                        .addChunk(
                                CodeChunkOf(
                                        file,
                                        10,
                                        "public int add(int a, int b) {\n    return a + b;\n}"))
                        .build();
        return builder()
                .repoRef("demo-repo")
                .headRef("feature/" + name)
                .diffFiles(List.of(diff))
                .build();
    }

    /** 返回仓库引用（可为空）。 */
    public String getRepoRef() {
        return repoRef;
    }

    /** 返回基准分支（可为空）。 */
    public String getBaseRef() {
        return baseRef;
    }

    /** 返回待评审分支（可为空）。 */
    public String getHeadRef() {
        return headRef;
    }

    /** 返回选项表（不可修改）。 */
    public Map<String, Object> getOptions() {
        return options;
    }

    /** 返回内联 diff（不可修改，可能为空）。 */
    public List<DiffFile> getDiffFiles() {
        return diffFiles;
    }

    /** 是否提供了内联 diff。 */
    public boolean hasInlineDiff() {
        return !diffFiles.isEmpty();
    }

    @Override
    public String toString() {
        return "ReviewRequest{repoRef='"
                + repoRef
                + "', headRef='"
                + headRef
                + "', diffFiles="
                + diffFiles.size()
                + '}';
    }

    /** {@link ReviewRequest} 构造器。 */
    public static final class Builder {

        private String repoRef;
        private String baseRef;
        private String headRef;
        private Map<String, Object> options;
        private List<DiffFile> diffFiles;

        private Builder() {}

        /** 设置仓库引用。 */
        public Builder repoRef(String repoRef) {
            this.repoRef = repoRef;
            return this;
        }

        /** 设置基准分支。 */
        public Builder baseRef(String baseRef) {
            this.baseRef = baseRef;
            return this;
        }

        /** 设置待评审分支。 */
        public Builder headRef(String headRef) {
            this.headRef = headRef;
            return this;
        }

        /** 设置选项表。 */
        public Builder options(Map<String, Object> options) {
            this.options = options;
            return this;
        }

        /** 设置内联 diff。 */
        public Builder diffFiles(List<DiffFile> diffFiles) {
            this.diffFiles = diffFiles;
            return this;
        }

        /** 构造请求。 */
        public ReviewRequest build() {
            Builder filled = new Builder();
            filled.repoRef = repoRef;
            filled.baseRef = baseRef;
            filled.headRef = headRef;
            filled.options = options;
            filled.diffFiles = diffFiles;
            return new ReviewRequest(filled);
        }
    }

    /** 内联代码块构造的小工具，避免在每个调用处重复样板。 */
    static CodeChunk CodeChunkOf(String file, int startLine, String content) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(content, "content");
        int endLine = startLine + content.split("\\R", -1).length - 1;
        return CodeChunk.builder()
                .filePath(file)
                .startLine(startLine)
                .endLine(endLine)
                .content(content)
                .build();
    }
}
