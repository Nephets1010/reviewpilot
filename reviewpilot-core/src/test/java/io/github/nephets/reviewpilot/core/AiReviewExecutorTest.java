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
import io.github.nephets.reviewpilot.contract.FindingSource;
import io.github.nephets.reviewpilot.contract.ReviewRunId;
import io.github.nephets.reviewpilot.contract.RuleConfig;
import io.github.nephets.reviewpilot.llm.MockReviewLlmClient;
import io.github.nephets.reviewpilot.llm.ReviewLlmClient;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiReviewExecutorTest {

    private static StageContext contextWithDiffs(List<DiffFile> files) {
        StageContext ctx =
                new StageContext(
                        ReviewRunId.of("r1"),
                        "acme",
                        "p1",
                        Clock.fixed(Instant.EPOCH, java.time.ZoneOffset.UTC),
                        ReviewRequest.builder().diffFiles(files).build(),
                        RuleConfig.defaultConfig());
        ctx.setDiffFiles(files);
        return ctx;
    }

    private static DiffFile secretFile() {
        return DiffFile.builder()
                .path("A.java")
                .status(DiffFile.FileStatus.ADDED)
                .language("java")
                .addChunk(
                        CodeChunk.builder()
                                .filePath("A.java")
                                .startLine(1)
                                .endLine(1)
                                .content("String password = \"x\";")
                                .build())
                .build();
    }

    @Test
    void producesFindingsAndCommentsFromMock() {
        StageContext ctx = contextWithDiffs(List.of(secretFile()));
        StageResult result = new AiReviewExecutor(new MockReviewLlmClient()).execute(ctx);
        assertThat(result.getStatus()).isEqualTo(StageStatus.SUCCEEDED);
        assertThat(ctx.getFindings()).isNotEmpty();
        assertThat(ctx.getLlmComments()).isNotEmpty();
        assertThat(ctx.getFindings()).allMatch(f -> f.getSource() == FindingSource.AI);
    }

    @Test
    void nullClientRejected() {
        assertThatThrownBy(() -> new AiReviewExecutor(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void stageIdMatchesEnum() {
        assertThat(AiReviewExecutor.stageId())
                .isEqualTo(io.github.nephets.reviewpilot.contract.ReviewStage.AI_REVIEW.id());
    }

    @Test
    void clientReviewThrowingPropagates() {
        ReviewLlmClient failing =
                new ReviewLlmClient() {
                    @Override
                    public String providerId() {
                        return "boom";
                    }

                    @Override
                    public io.github.nephets.reviewpilot.llm.LlmReviewResponse review(
                            io.github.nephets.reviewpilot.llm.LlmReviewRequest request) {
                        throw new io.github.nephets.reviewpilot.llm.LlmClientException("down");
                    }
                };
        StageContext ctx = contextWithDiffs(List.of(secretFile()));
        // executor itself does not catch; engine retry handles it. Just ensure it throws.
        try {
            new AiReviewExecutor(failing).execute(ctx);
            org.assertj.core.api.Assertions.fail("expected exception");
        } catch (io.github.nephets.reviewpilot.llm.LlmClientException e) {
            assertThat(e.getMessage()).isEqualTo("down");
        }
    }
}
