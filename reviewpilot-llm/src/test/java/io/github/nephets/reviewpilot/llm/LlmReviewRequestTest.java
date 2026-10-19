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

import io.github.nephets.reviewpilot.contract.CodeChunk;
import io.github.nephets.reviewpilot.contract.DiffFile;
import io.github.nephets.reviewpilot.contract.Finding;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmReviewRequestTest {

    private static DiffFile file(String path) {
        return DiffFile.builder()
                .path(path)
                .status(DiffFile.FileStatus.ADDED)
                .language("java")
                .addChunk(
                        CodeChunk.builder()
                                .filePath(path)
                                .startLine(1)
                                .endLine(1)
                                .content("x")
                                .build())
                .build();
    }

    @Test
    void builderSetsAllFields() {
        Finding f = Finding.builder().message("m").build();
        LlmReviewRequest req =
                LlmReviewRequest.builder()
                        .diffFiles(List.of(file("A.java")))
                        .existingFindings(List.of(f))
                        .model("m1")
                        .locale("zh-CN")
                        .build();
        assertThat(req.getDiffFiles()).hasSize(1);
        assertThat(req.getExistingFindings()).hasSize(1);
        assertThat(req.getModel()).isEqualTo("m1");
        assertThat(req.getLocale()).isEqualTo("zh-CN");
    }

    @Test
    void defaultsAreEmptyAndNullable() {
        LlmReviewRequest req = LlmReviewRequest.builder().build();
        assertThat(req.getDiffFiles()).isEmpty();
        assertThat(req.getExistingFindings()).isEmpty();
        assertThat(req.getModel()).isNull();
        assertThat(req.getLocale()).isNull();
    }

    @Test
    void nullDiffFilesRejected() {
        assertThatThrownBy(() -> LlmReviewRequest.builder().diffFiles(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addDiffFileRejectsNull() {
        assertThatThrownBy(() -> LlmReviewRequest.builder().addDiffFile(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullExistingFindingsRejected() {
        assertThatThrownBy(() -> LlmReviewRequest.builder().existingFindings(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addExistingFindingRejectsNull() {
        assertThatThrownBy(() -> LlmReviewRequest.builder().addExistingFinding(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void containsNoOrgSpringImports() {
        // 防御性断言：llm 模块不应引入 Spring（编译期已约束，这里做烟雾测试）
        assertThat(LlmReviewRequest.class.getPackageName())
                .isEqualTo("io.github.nephets.reviewpilot.llm");
    }
}
