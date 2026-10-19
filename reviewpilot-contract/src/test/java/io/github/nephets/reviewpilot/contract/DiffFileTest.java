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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiffFileTest {

    private static CodeChunk chunk(String path, int start, int end) {
        return CodeChunk.builder()
                .filePath(path)
                .startLine(start)
                .endLine(end)
                .content("line")
                .build();
    }

    @Test
    void buildsWithAddedStatusAndChunks() {
        DiffFile file =
                DiffFile.builder()
                        .path("A.java")
                        .status(DiffFile.FileStatus.ADDED)
                        .language("java")
                        .addChunk(chunk("A.java", 1, 2))
                        .build();
        assertThat(file.getPath()).isEqualTo("A.java");
        assertThat(file.getStatus()).isEqualTo(DiffFile.FileStatus.ADDED);
        assertThat(file.getLanguage()).isEqualTo("java");
        assertThat(file.getChunks()).hasSize(1);
    }

    @Test
    void defaultStatusIsModified() {
        DiffFile file = DiffFile.builder().path("A.java").addChunk(chunk("A.java", 1, 1)).build();
        assertThat(file.getStatus()).isEqualTo(DiffFile.FileStatus.MODIFIED);
    }

    @Test
    void chunksAreImmutableCopy() {
        java.util.List<CodeChunk> source = new java.util.ArrayList<>();
        source.add(chunk("A.java", 1, 1));
        DiffFile file = DiffFile.builder().path("A.java").chunks(source).build();
        source.add(chunk("A.java", 2, 2));
        assertThat(file.getChunks()).hasSize(1);
    }

    @Test
    void nullChunksRejected() {
        assertThatThrownBy(() -> DiffFile.builder().path("A.java").chunks(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankPathRejected() {
        assertThatThrownBy(() -> DiffFile.builder().path("").build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullChunkRejected() {
        assertThatThrownBy(() -> DiffFile.builder().path("A.java").addChunk(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunk");
    }

    @Test
    void allStatusesListed() {
        assertThat(DiffFile.FileStatus.values())
                .containsExactlyInAnyOrder(
                        DiffFile.FileStatus.ADDED,
                        DiffFile.FileStatus.MODIFIED,
                        DiffFile.FileStatus.DELETED,
                        DiffFile.FileStatus.RENAMED,
                        DiffFile.FileStatus.COPIED);
    }

    @Test
    void equalsAndHashCode() {
        DiffFile a =
                DiffFile.builder()
                        .path("A.java")
                        .status(DiffFile.FileStatus.ADDED)
                        .addChunk(chunk("A.java", 1, 1))
                        .build();
        DiffFile b =
                DiffFile.builder()
                        .path("A.java")
                        .status(DiffFile.FileStatus.ADDED)
                        .addChunk(chunk("A.java", 1, 1))
                        .build();
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
