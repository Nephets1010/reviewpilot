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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 一次 diff 中的一个文件：路径、变更类型、语言与若干代码块。
 *
 * <p>不可变值对象，chunks 以不可修改列表保存。
 */
public final class DiffFile {

    /** 文件变更类型。 */
    public enum FileStatus {
        ADDED,
        MODIFIED,
        DELETED,
        RENAMED,
        COPIED
    }

    private final String path;
    private final FileStatus status;
    private final String language;
    private final List<CodeChunk> chunks;

    private DiffFile(Builder b) {
        this.path = b.path;
        this.status = b.status;
        this.language = b.language;
        this.chunks = List.copyOf(b.chunks);
    }

    /** 返回构造器。 */
    public static Builder builder() {
        return new Builder();
    }

    /** 返回文件路径。 */
    public String getPath() {
        return path;
    }

    /** 返回变更类型。 */
    public FileStatus getStatus() {
        return status;
    }

    /** 返回语言（可为空）。 */
    public String getLanguage() {
        return language;
    }

    /** 返回代码块（不可修改）。 */
    public List<CodeChunk> getChunks() {
        return chunks;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DiffFile)) {
            return false;
        }
        DiffFile that = (DiffFile) other;
        return Objects.equals(path, that.path)
                && status == that.status
                && Objects.equals(language, that.language)
                && Objects.equals(chunks, that.chunks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, status, language, chunks);
    }

    @Override
    public String toString() {
        return "DiffFile{path='" + path + "', status=" + status + ", chunks=" + chunks.size() + '}';
    }

    /** {@link DiffFile} 构造器。 */
    public static final class Builder {

        private String path;
        private FileStatus status = FileStatus.MODIFIED;
        private String language;
        private final List<CodeChunk> chunks = new ArrayList<>();

        private Builder() {}

        /** 设置文件路径，不可为空。 */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /** 设置变更类型，未设置时默认 {@link FileStatus#MODIFIED}。 */
        public Builder status(FileStatus status) {
            this.status = status == null ? FileStatus.MODIFIED : status;
            return this;
        }

        /** 设置语言，可为空。 */
        public Builder language(String language) {
            this.language = language;
            return this;
        }

        /** 追加一个代码块。 */
        public Builder addChunk(CodeChunk chunk) {
            if (chunk == null) {
                throw new IllegalArgumentException("chunk 不可为 null");
            }
            chunks.add(chunk);
            return this;
        }

        /** 覆盖设置代码块列表。 */
        public Builder chunks(List<CodeChunk> chunks) {
            if (chunks == null) {
                throw new IllegalArgumentException("chunks 不可为 null");
            }
            this.chunks.clear();
            this.chunks.addAll(chunks);
            return this;
        }

        /** 构造 diff 文件。 */
        public DiffFile build() {
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("path 不可为空");
            }
            Builder filled = new Builder();
            filled.path = path;
            filled.status = status;
            filled.language = language;
            filled.chunks.addAll(chunks);
            return new DiffFile(filled);
        }
    }
}
