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
import io.github.nephets.reviewpilot.contract.ReviewRunId;
import io.github.nephets.reviewpilot.contract.RuleConfig;
import io.github.nephets.reviewpilot.contract.StaticCheckResult;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StageContextTest {

    private static StageContext newContext() {
        return new StageContext(
                ReviewRunId.of("r1"),
                "acme",
                "p1",
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
                ReviewRequest.scripted("demo"),
                RuleConfig.defaultConfig());
    }

    @Test
    void nullRunIdRejected() {
        assertThatThrownBy(
                        () ->
                                new StageContext(
                                        null,
                                        "t",
                                        "p",
                                        Clock.systemUTC(),
                                        ReviewRequest.scripted("d"),
                                        RuleConfig.defaultConfig()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullClockRejected() {
        assertThatThrownBy(
                        () ->
                                new StageContext(
                                        ReviewRunId.of("r"),
                                        "t",
                                        "p",
                                        null,
                                        ReviewRequest.scripted("d"),
                                        RuleConfig.defaultConfig()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullRequestRejected() {
        assertThatThrownBy(
                        () ->
                                new StageContext(
                                        ReviewRunId.of("r"),
                                        "t",
                                        "p",
                                        Clock.systemUTC(),
                                        null,
                                        RuleConfig.defaultConfig()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullRuleConfigRejected() {
        assertThatThrownBy(
                        () ->
                                new StageContext(
                                        ReviewRunId.of("r"),
                                        "t",
                                        "p",
                                        Clock.systemUTC(),
                                        ReviewRequest.scripted("d"),
                                        null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void accessorsReturnConstructorValues() {
        StageContext ctx = newContext();
        assertThat(ctx.getRunId().asString()).isEqualTo("r1");
        assertThat(ctx.getTenantId()).isEqualTo("acme");
        assertThat(ctx.getPipelineId()).isEqualTo("p1");
        assertThat(ctx.getRequest().hasInlineDiff()).isTrue();
        assertThat(ctx.getRuleConfig()).isEqualTo(RuleConfig.defaultConfig());
    }

    @Test
    void nowUsesInjectedClock() {
        assertThat(newContext().now()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void findingsAccumulate() {
        StageContext ctx = newContext();
        Finding f = Finding.builder().id("f1").message("x").build();
        ctx.addFinding(f);
        assertThat(ctx.getFindings()).containsExactly(f);
        assertThatThrownBy(() -> ctx.addFinding(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void llmCommentsAccumulate() {
        StageContext ctx = newContext();
        LlmReviewComment c = LlmReviewComment.builder().message("x").build();
        ctx.addLlmComment(c);
        assertThat(ctx.getLlmComments()).containsExactly(c);
        assertThatThrownBy(() -> ctx.addLlmComment(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void staticResultsAccumulate() {
        StageContext ctx = newContext();
        StaticCheckResult r = StaticCheckResult.builder().toolName("t").build();
        ctx.addStaticResult(r);
        assertThat(ctx.getStaticResults()).containsExactly(r);
        assertThatThrownBy(() -> ctx.addStaticResult(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void diffFilesSetAndGet() {
        StageContext ctx = newContext();
        DiffFile file =
                DiffFile.builder().path("A.java").addChunk(scriptedChunk("A.java", 1, "x")).build();
        ctx.setDiffFiles(List.of(file));
        assertThat(ctx.getDiffFiles()).containsExactly(file);
        ctx.setDiffFiles(null);
        assertThat(ctx.getDiffFiles()).isEmpty();
    }

    @Test
    void stageOutputsRoundTrip() {
        StageContext ctx = newContext();
        ctx.putStageOutput("k", "v");
        assertThat(ctx.getStageOutput("k")).isEqualTo("v");
        assertThat(ctx.getStageOutput("missing")).isNull();
        assertThat(ctx.getStageOutputs()).containsKey("k");
    }

    @Test
    void gateResultAndReport() {
        StageContext ctx = newContext();
        assertThat(ctx.getGateResult()).isNull();
        assertThat(ctx.getReport()).isNull();
    }

    private static io.github.nephets.reviewpilot.contract.CodeChunk scriptedChunk(
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
