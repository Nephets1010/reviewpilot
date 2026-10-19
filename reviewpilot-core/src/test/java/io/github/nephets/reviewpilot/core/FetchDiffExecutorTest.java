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
import io.github.nephets.reviewpilot.contract.ReviewRunId;
import io.github.nephets.reviewpilot.contract.RuleConfig;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FetchDiffExecutorTest {

    private static StageContext contextWithRequest(ReviewRequest request) {
        return new StageContext(
                ReviewRunId.of("r1"),
                "acme",
                "p1",
                Clock.fixed(Instant.EPOCH, java.time.ZoneOffset.UTC),
                request,
                RuleConfig.defaultConfig());
    }

    @Test
    void usesInlineDiffWhenProvided() {
        DiffFile file = DiffFile.builder().path("A.java").addChunk(chunk("A.java", 1, "x")).build();
        StageContext ctx =
                contextWithRequest(ReviewRequest.builder().diffFiles(List.of(file)).build());
        StageResult result = new FetchDiffExecutor().execute(ctx);
        assertThat(result.getStatus()).isEqualTo(StageStatus.SUCCEEDED);
        assertThat(ctx.getDiffFiles()).containsExactly(file);
    }

    @Test
    void fallsBackToScriptedDiffWhenNoInline() {
        StageContext ctx = contextWithRequest(ReviewRequest.builder().build());
        StageResult result = new FetchDiffExecutor().execute(ctx);
        assertThat(result.getStatus()).isEqualTo(StageStatus.SUCCEEDED);
        assertThat(ctx.getDiffFiles()).isNotEmpty();
    }

    @Test
    void nullFallbackDiffYieldsEmpty() {
        StageContext ctx = contextWithRequest(ReviewRequest.builder().build());
        StageResult result = new FetchDiffExecutor((List<DiffFile>) null).execute(ctx);
        assertThat(ctx.getDiffFiles()).isEmpty();
        assertThat(result.getStatus()).isEqualTo(StageStatus.SUCCEEDED);
    }

    @Test
    void stageIdMatchesEnum() {
        assertThat(FetchDiffExecutor.stageId())
                .isEqualTo(io.github.nephets.reviewpilot.contract.ReviewStage.FETCH_DIFF.id());
    }

    @Test
    void scriptedChunkHelperBuildsModifiedFile() {
        DiffFile file = FetchDiffExecutor.scriptedChunk("B.java", 3, "line1\nline2");
        assertThat(file.getStatus()).isEqualTo(DiffFile.FileStatus.MODIFIED);
        assertThat(file.getChunks()).hasSize(1);
    }

    private static CodeChunk chunk(String path, int start, String content) {
        int end = start + content.split("\\R", -1).length - 1;
        return CodeChunk.builder()
                .filePath(path)
                .startLine(start)
                .endLine(end)
                .content(content)
                .build();
    }
}
