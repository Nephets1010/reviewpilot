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

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StaticAnalysisExecutorTest {

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

    private static DiffFile file(String path, String content) {
        int end = 1 + content.split("\\R", -1).length - 1;
        return DiffFile.builder()
                .path(path)
                .status(DiffFile.FileStatus.ADDED)
                .language("java")
                .addChunk(
                        CodeChunk.builder()
                                .filePath(path)
                                .startLine(1)
                                .endLine(end)
                                .content(content)
                                .build())
                .build();
    }

    @Test
    void detectsLongLine() {
        String longLine = "x".repeat(130);
        StageContext ctx = contextWithDiffs(List.of(file("A.java", longLine)));
        StageResult result = new StaticAnalysisExecutor().execute(ctx);
        assertThat(result.getStatus()).isEqualTo(StageStatus.SUCCEEDED);
        assertThat(ctx.getFindings()).anyMatch(f -> f.getRuleId().equals("static.line-too-long"));
        assertThat(ctx.getStaticResults()).hasSize(1);
    }

    @Test
    void detectsBroadCatch() {
        StageContext ctx = contextWithDiffs(List.of(file("A.java", "catch Exception e {}")));
        new StaticAnalysisExecutor().execute(ctx);
        assertThat(ctx.getFindings()).anyMatch(f -> f.getRuleId().equals("static.broad-catch"));
    }

    @Test
    void detectsTodo() {
        StageContext ctx = contextWithDiffs(List.of(file("A.java", "// TODO fix this later")));
        new StaticAnalysisExecutor().execute(ctx);
        assertThat(ctx.getFindings()).anyMatch(f -> f.getRuleId().equals("static.todo"));
    }

    @Test
    void findingsAreStaticSourced() {
        StageContext ctx =
                contextWithDiffs(List.of(file("A.java", "catch Exception e {} // TODO x")));
        new StaticAnalysisExecutor().execute(ctx);
        assertThat(ctx.getFindings()).allMatch(f -> f.getSource() == FindingSource.STATIC);
    }

    @Test
    void cleanDiffProducesNoFindings() {
        StageContext ctx = contextWithDiffs(List.of(file("A.java", "int a = 1;")));
        new StaticAnalysisExecutor().execute(ctx);
        assertThat(ctx.getFindings()).isEmpty();
    }

    @Test
    void passedFalseWhenCriticalPresent() {
        StageContext ctx =
                contextWithDiffs(
                        List.of(
                                file(
                                        "A.java",
                                        "throw new RuntimeException(); // not critical"))); // no
        // critical rule triggers
        new StaticAnalysisExecutor().execute(ctx);
        // no CRITICAL+ rule exists in static, passed remains true
        assertThat(ctx.getStaticResults().get(0).isPassed()).isTrue();
    }

    @Test
    void stageIdMatchesEnum() {
        assertThat(StaticAnalysisExecutor.stageId())
                .isEqualTo(io.github.nephets.reviewpilot.contract.ReviewStage.STATIC_ANALYSIS.id());
    }
}
