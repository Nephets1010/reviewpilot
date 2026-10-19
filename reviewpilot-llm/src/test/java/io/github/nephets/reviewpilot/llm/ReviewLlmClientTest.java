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

class ReviewLlmClientTest {

    private static final ReviewLlmClient DUMMY =
            new ReviewLlmClient() {
                @Override
                public String providerId() {
                    return "dummy";
                }

                @Override
                public LlmReviewResponse review(LlmReviewRequest request) {
                    return LlmReviewResponse.builder().build();
                }
            };

    @Test
    void staticRequestFactoryBuildsRequest() {
        DiffFile f =
                DiffFile.builder()
                        .path("A.java")
                        .status(DiffFile.FileStatus.ADDED)
                        .language("java")
                        .addChunk(
                                CodeChunk.builder()
                                        .filePath("A.java")
                                        .startLine(1)
                                        .endLine(1)
                                        .content("x")
                                        .build())
                        .build();
        Finding finding = Finding.builder().message("m").build();
        LlmReviewRequest req = ReviewLlmClient.request(List.of(f), List.of(finding));
        assertThat(req.getDiffFiles()).containsExactly(f);
        assertThat(req.getExistingFindings()).containsExactly(finding);
    }

    @Test
    void defaultHealthyIsTrue() {
        assertThat(DUMMY.healthy()).isTrue();
    }

    @Test
    void defaultNameIsNotNull() {
        assertThat(DUMMY.name()).isNotNull();
    }
}
