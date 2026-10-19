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

/**
 * diff 中的一个代码块（hunk）：文件路径、起止行与内容。
 *
 * <p>不可变值对象。起止行必须满足 {@code startLine <= endLine} 且均非负。
 */
public final class CodeChunk {

    private final String filePath;
    private final int startLine;
    private final int endLine;
    private final String content;
    private final String hunkHeader;

    private CodeChunk(Builder b) {
        this.filePath = b.filePath;
        this.startLine = b.startLine;
        this.endLine = b.endLine;
        this.content = b.content;
        this.hunkHeader = b.hunkHeader;
    }

    /** 返回构造器。 */
    public static Builder builder() {
        return new Builder();
    }

    /** 返回所属文件路径。 */
    public String getFilePath() {
        return filePath;
    }

    /** 返回起始行（含）。 */
    public int getStartLine() {
        return startLine;
    }

    /** 返回结束行（含）。 */
    public int getEndLine() {
        return endLine;
    }

    /** 返回代码内容。 */
    public String getContent() {
        return content;
    }

    /** 返回 hunk 头（如 {@code @@ -10,3 +10,4 @@}，可为空）。 */
    public String getHunkHeader() {
        return hunkHeader;
    }

    /** 返回代码块行数（{@code endLine - startLine + 1}）。 */
    public int lineCount() {
        return endLine - startLine + 1;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CodeChunk)) {
            return false;
        }
        CodeChunk that = (CodeChunk) other;
        return startLine == that.startLine
                && endLine == that.endLine
                && Objects.equals(filePath, that.filePath)
                && Objects.equals(content, that.content)
                && Objects.equals(hunkHeader, that.hunkHeader);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, startLine, endLine, content, hunkHeader);
    }

    @Override
    public String toString() {
        return "CodeChunk{filePath='"
                + filePath
                + "', startLine="
                + startLine
                + ", endLine="
                + endLine
                + '}';
    }

    /** {@link CodeChunk} 构造器。 */
    public static final class Builder {

        private String filePath;
        private int startLine;
        private int endLine;
        private String content = "";
        private String hunkHeader;

        private Builder() {}

        /** 设置文件路径，不可为空。 */
        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        /** 设置起始行（含），必须 &gt;= 0。 */
        public Builder startLine(int startLine) {
            this.startLine = startLine;
            return this;
        }

        /** 设置结束行（含），必须 &gt;= startLine。 */
        public Builder endLine(int endLine) {
            this.endLine = endLine;
            return this;
        }

        /** 设置代码内容，为 {@code null} 时按空串处理。 */
        public Builder content(String content) {
            this.content = content;
            return this;
        }

        /** 设置 hunk 头，可为空。 */
        public Builder hunkHeader(String hunkHeader) {
            this.hunkHeader = hunkHeader;
            return this;
        }

        /** 构造代码块。 */
        public CodeChunk build() {
            if (filePath == null || filePath.isBlank()) {
                throw new IllegalArgumentException("filePath 不可为空");
            }
            if (startLine < 0) {
                throw new IllegalArgumentException("startLine 必须 >= 0，当前：" + startLine);
            }
            if (endLine < startLine) {
                throw new IllegalArgumentException(
                        "endLine 必须 >= startLine，当前 startLine="
                                + startLine
                                + ", endLine="
                                + endLine);
            }
            Builder filled = new Builder();
            filled.filePath = filePath;
            filled.startLine = startLine;
            filled.endLine = endLine;
            filled.content = content == null ? "" : content;
            filled.hunkHeader = hunkHeader;
            return new CodeChunk(filled);
        }
    }
}
