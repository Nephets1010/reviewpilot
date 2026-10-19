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

class CodeChunkTest {

    @Test
    void buildsWithDefaults() {
        CodeChunk chunk =
                CodeChunk.builder().filePath("A.java").startLine(1).endLine(1).content("x").build();
        assertThat(chunk.getFilePath()).isEqualTo("A.java");
        assertThat(chunk.getStartLine()).isEqualTo(1);
        assertThat(chunk.getEndLine()).isEqualTo(1);
        assertThat(chunk.getContent()).isEqualTo("x");
        assertThat(chunk.getHunkHeader()).isNull();
    }

    @Test
    void lineCountComputesEndMinusStartPlusOne() {
        CodeChunk chunk =
                CodeChunk.builder()
                        .filePath("A.java")
                        .startLine(10)
                        .endLine(12)
                        .content("a\nb\nc")
                        .build();
        assertThat(chunk.lineCount()).isEqualTo(3);
    }

    @Test
    void nullContentBecomesEmptyString() {
        CodeChunk chunk =
                CodeChunk.builder()
                        .filePath("A.java")
                        .startLine(0)
                        .endLine(0)
                        .content(null)
                        .build();
        assertThat(chunk.getContent()).isEqualTo("");
    }

    @Test
    void hunkHeaderIsRetained() {
        CodeChunk chunk =
                CodeChunk.builder()
                        .filePath("A.java")
                        .startLine(1)
                        .endLine(1)
                        .content("x")
                        .hunkHeader("@@ -1,1 +1,1 @@")
                        .build();
        assertThat(chunk.getHunkHeader()).isEqualTo("@@ -1,1 +1,1 @@");
    }

    @Test
    void blankFilePathRejected() {
        assertThatThrownBy(() -> CodeChunk.builder().filePath("  ").startLine(1).endLine(1).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeStartLineRejected() {
        assertThatThrownBy(
                        () ->
                                CodeChunk.builder()
                                        .filePath("A.java")
                                        .startLine(-1)
                                        .endLine(1)
                                        .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void endBeforeStartRejected() {
        assertThatThrownBy(
                        () ->
                                CodeChunk.builder()
                                        .filePath("A.java")
                                        .startLine(5)
                                        .endLine(2)
                                        .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalsAndHashCode() {
        CodeChunk a =
                CodeChunk.builder().filePath("A.java").startLine(1).endLine(1).content("x").build();
        CodeChunk b =
                CodeChunk.builder().filePath("A.java").startLine(1).endLine(1).content("x").build();
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a)
                .isNotEqualTo(
                        CodeChunk.builder()
                                .filePath("A.java")
                                .startLine(1)
                                .endLine(2)
                                .content("x")
                                .build());
    }
}
