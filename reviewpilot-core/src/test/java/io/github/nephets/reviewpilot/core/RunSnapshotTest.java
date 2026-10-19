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

import io.github.nephets.reviewpilot.contract.DiffFile;
import io.github.nephets.reviewpilot.contract.Finding;
import io.github.nephets.reviewpilot.contract.LlmReviewComment;
import io.github.nephets.reviewpilot.contract.ReviewRunStatus;
import io.github.nephets.reviewpilot.contract.StaticCheckResult;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RunSnapshotTest {

    @Test
    void builderCapturesFullState() {
        Map<String, StageStatus> statuses = new LinkedHashMap<>();
        statuses.put("fetch-diff", StageStatus.SUCCEEDED);
        Finding f = Finding.builder().id("f1").message("x").build();
        LlmReviewComment c = LlmReviewComment.builder().message("y").build();
        StaticCheckResult r = StaticCheckResult.builder().toolName("t").build();
        DiffFile d = DiffFile.builder().path("A.java").addChunk(chunk("A.java", 1, "x")).build();
        RunSnapshot snap =
                new RunSnapshot.Builder()
                        .version(3)
                        .status(ReviewRunStatus.RUNNING)
                        .stageStatuses(statuses)
                        .findings(List.of(f))
                        .llmComments(List.of(c))
                        .staticResults(List.of(r))
                        .diffFiles(List.of(d))
                        .timestamp(Instant.EPOCH)
                        .note("enter stage")
                        .build();
        assertThat(snap.getVersion()).isEqualTo(3);
        assertThat(snap.getStatus()).isEqualTo(ReviewRunStatus.RUNNING);
        assertThat(snap.getStageStatuses()).containsEntry("fetch-diff", StageStatus.SUCCEEDED);
        assertThat(snap.getFindings()).containsExactly(f);
        assertThat(snap.getLlmComments()).containsExactly(c);
        assertThat(snap.getStaticResults()).containsExactly(r);
        assertThat(snap.getDiffFiles()).containsExactly(d);
        assertThat(snap.getTimestamp()).isEqualTo(Instant.EPOCH);
        assertThat(snap.getNote()).isEqualTo("enter stage");
    }

    @Test
    void toStringMentionsVersion() {
        RunSnapshot snap =
                new RunSnapshot.Builder()
                        .version(1)
                        .status(ReviewRunStatus.PENDING)
                        .stageStatuses(new LinkedHashMap<>())
                        .findings(List.of())
                        .llmComments(List.of())
                        .staticResults(List.of())
                        .diffFiles(List.of())
                        .timestamp(Instant.EPOCH)
                        .note("init")
                        .build();
        assertThat(snap.toString()).contains("version=1");
    }

    private static io.github.nephets.reviewpilot.contract.CodeChunk chunk(
            String path, int start, String content) {
        int end = start + content.split("\\R", -1).length - 1;
        return io.github.nephets.reviewpilot.contract.CodeChunk.builder()
                .filePath(path)
                .startLine(start)
                .endLine(end)
                .content(content)
                .build();
    }
}
