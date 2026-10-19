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

class LlmPromptBuilderTest {

    private static DiffFile file(String path, String content) {
        int end = content.split("\\R", -1).length;
        return DiffFile.builder()
                .path(path)
                .status(DiffFile.FileStatus.MODIFIED)
                .language("java")
                .addChunk(
                        CodeChunk.builder()
                                .filePath(path)
                                .startLine(1)
                                .endLine(end)
                                .hunkHeader("@@ -1,2 +1,2 @@")
                                .content(content)
                                .build())
                .build();
    }

    @Test
    void buildsPromptWithDiff() {
        String prompt =
                LlmPromptBuilder.build(
                        LlmReviewRequest.builder()
                                .diffFiles(List.of(file("A.java", "int x=1;")))
                                .build());
        assertThat(prompt).contains("资深代码评审专家");
        assertThat(prompt).contains("A.java");
        assertThat(prompt).contains("int x=1;");
        assertThat(prompt).contains("DIFF 开始");
        assertThat(prompt).contains("DIFF 结束");
        assertThat(prompt).contains("MODIFIED");
    }

    @Test
    void includesLocaleWhenSet() {
        String prompt =
                LlmPromptBuilder.build(
                        LlmReviewRequest.builder()
                                .diffFiles(List.of(file("A.java", "x")))
                                .locale("zh-CN")
                                .build());
        assertThat(prompt).contains("zh-CN");
    }

    @Test
    void includesModelWhenSet() {
        String prompt =
                LlmPromptBuilder.build(
                        LlmReviewRequest.builder()
                                .diffFiles(List.of(file("A.java", "x")))
                                .model("gpt-4o")
                                .build());
        assertThat(prompt).contains("gpt-4o");
    }

    @Test
    void includesExistingFindings() {
        Finding f =
                Finding.builder()
                        .severity(io.github.nephets.reviewpilot.contract.ReviewSeverity.MAJOR)
                        .filePath("A.java")
                        .line(3)
                        .message("样例发现")
                        .build();
        String prompt =
                LlmPromptBuilder.build(
                        LlmReviewRequest.builder()
                                .diffFiles(List.of(file("A.java", "x")))
                                .existingFindings(List.of(f))
                                .build());
        assertThat(prompt).contains("已有静态检查发现");
        assertThat(prompt).contains("样例发现");
    }

    @Test
    void noExistingFindingsOmitsSection() {
        String prompt =
                LlmPromptBuilder.build(
                        LlmReviewRequest.builder().diffFiles(List.of(file("A.java", "x"))).build());
        assertThat(prompt).doesNotContain("已有静态检查发现");
    }

    @Test
    void hunkHeaderIncluded() {
        String prompt =
                LlmPromptBuilder.build(
                        LlmReviewRequest.builder().diffFiles(List.of(file("A.java", "x"))).build());
        assertThat(prompt).contains("@@ -1,2 +1,2 @@");
    }

    @Test
    void nullRequestReturnsFallback() {
        assertThat(LlmPromptBuilder.build(null)).isEqualTo("（无评审请求）");
    }

    @Test
    void emptyDiffStillBuildsHeader() {
        String prompt = LlmPromptBuilder.build(LlmReviewRequest.builder().build());
        assertThat(prompt).contains("共 0 个文件");
    }
}
