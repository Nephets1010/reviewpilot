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
import io.github.nephets.reviewpilot.contract.ReviewRunStatus;
import io.github.nephets.reviewpilot.contract.RuleConfig;
import io.github.nephets.reviewpilot.llm.MockReviewLlmClient;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewWorkflowEngineTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, java.time.ZoneOffset.UTC);

    /** 记录退避调用的 Sleeper。 */
    static final class RecordingSleeper implements ReviewWorkflowEngine.Sleeper {
        final AtomicInteger calls = new AtomicInteger();
        Duration last;

        @Override
        public void sleep(Duration duration) {
            calls.incrementAndGet();
            last = duration;
        }
    }

    private static ReviewWorkflowEngine engine(RecordingSleeper sleeper) {
        return new ReviewWorkflowEngine(
                new InMemoryReviewRunStore(), new ReviewRegistry(), CLOCK, 50, sleeper);
    }

    private static DiffFile secretDiff() {
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

    private static DiffFile cleanDiff() {
        return DiffFile.builder()
                .path("A.java")
                .status(DiffFile.FileStatus.ADDED)
                .language("java")
                .addChunk(
                        CodeChunk.builder()
                                .filePath("A.java")
                                .startLine(1)
                                .endLine(1)
                                .content("int add(int a, int b) { return a + b; }")
                                .build())
                .build();
    }

    @Test
    void fullPipelineRunsToSuccessWithoutFindings() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        ReviewPipeline pipeline = ReviewPipelines.defaultPipeline(new MockReviewLlmClient());
        String runId =
                engine.start(
                        pipeline,
                        "acme",
                        ReviewRequest.builder().diffFiles(List.of(cleanDiff())).build());
        var instance = engine.find("acme", runId).orElseThrow();
        assertThat(instance.getStatus()).isEqualTo(ReviewRunStatus.SUCCEEDED);
        assertThat(instance.isGated()).isFalse();
        assertThat(instance.getReport().getStatus()).isEqualTo(ReviewRunStatus.SUCCEEDED);
    }

    @Test
    void fullPipelineWithBlockerIsGated() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        ReviewPipeline pipeline = ReviewPipelines.defaultPipeline(new MockReviewLlmClient());
        String runId =
                engine.start(
                        pipeline,
                        "acme",
                        ReviewRequest.builder().diffFiles(List.of(secretDiff())).build());
        var instance = engine.find("acme", runId).orElseThrow();
        assertThat(instance.getStatus()).isEqualTo(ReviewRunStatus.GATED);
        assertThat(instance.isGated()).isTrue();
        assertThat(instance.getFindings())
                .anyMatch(
                        f ->
                                f.getSeverity()
                                        == io.github.nephets.reviewpilot.contract.ReviewSeverity
                                                .BLOCKER);
    }

    @Test
    void gateFailsOnBlockerCountThreshold() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        RuleConfig rule = RuleConfig.builder().blockerThreshold(2).build();
        ReviewPipeline pipeline = ReviewPipelines.defaultPipeline(new MockReviewLlmClient(), rule);
        String runId =
                engine.start(
                        pipeline,
                        "acme",
                        ReviewRequest.builder().diffFiles(List.of(secretDiff())).build());
        var instance = engine.find("acme", runId).orElseThrow();
        // 仅 1 个 BLOCKER，未达阈值 2 → 通过
        assertThat(instance.isGated()).isFalse();
        assertThat(instance.getStatus()).isEqualTo(ReviewRunStatus.SUCCEEDED);
    }

    @Test
    void stageSkippedWhenConditionFalse() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        ReviewPipeline pipeline =
                ReviewPipeline.builder()
                        .pipelineId("cond")
                        .addStage(
                                ReviewStageDef.builder()
                                        .stageId("fetch")
                                        .executor(
                                                ctx -> {
                                                    ctx.setDiffFiles(List.of(cleanDiff()));
                                                    return StageResult.success();
                                                })
                                        .build())
                        .addStage(
                                ReviewStageDef.builder()
                                        .stageId("optional")
                                        .dependsOn("fetch")
                                        .condition(ctx -> false)
                                        .executor(ctx -> StageResult.failure("should-not-run"))
                                        .build())
                        .addStage(
                                ReviewStageDef.builder()
                                        .stageId("report")
                                        .dependsOn("optional")
                                        .executor(ctx -> StageResult.success())
                                        .build())
                        .ruleConfig(RuleConfig.defaultConfig())
                        .build();
        String runId = engine.start(pipeline, "acme", ReviewRequest.builder().build());
        var instance = engine.find("acme", runId).orElseThrow();
        assertThat(instance.stageStatus("optional")).isEqualTo(StageStatus.SKIPPED);
        assertThat(instance.stageStatus("report")).isEqualTo(StageStatus.SUCCEEDED);
        assertThat(instance.getStatus()).isEqualTo(ReviewRunStatus.SUCCEEDED);
    }

    @Test
    void downstreamRunsEvenIfDependencySkipped() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        ReviewPipeline pipeline =
                ReviewPipeline.builder()
                        .pipelineId("cond2")
                        .addStage(
                                ReviewStageDef.builder()
                                        .stageId("fetch")
                                        .executor(ctx -> StageResult.success())
                                        .build())
                        .addStage(
                                ReviewStageDef.builder()
                                        .stageId("optional")
                                        .dependsOn("fetch")
                                        .condition(ctx -> false)
                                        .executor(ctx -> StageResult.failure("x"))
                                        .build())
                        .addStage(
                                ReviewStageDef.builder()
                                        .stageId("report")
                                        .dependsOn("optional")
                                        .executor(ctx -> StageResult.success())
                                        .build())
                        .ruleConfig(RuleConfig.defaultConfig())
                        .build();
        String runId = engine.start(pipeline, "acme", ReviewRequest.builder().build());
        var instance = engine.find("acme", runId).orElseThrow();
        assertThat(instance.stageStatus("report")).isEqualTo(StageStatus.SUCCEEDED);
    }

    @Test
    void retryBackoffHappensThenFails() {
        RecordingSleeper sleeper = new RecordingSleeper();
        ReviewWorkflowEngine engine = engine(sleeper);
        ReviewPipeline pipeline =
                ReviewPipeline.builder()
                        .pipelineId("retry")
                        .addStage(
                                ReviewStageDef.builder()
                                        .stageId("flaky")
                                        .executor(ctx -> StageResult.failure("always"))
                                        .retryPolicy(
                                                ReviewRetryPolicy.fixed(3, Duration.ofMillis(10)))
                                        .build())
                        .ruleConfig(RuleConfig.disabled())
                        .build();
        String runId = engine.start(pipeline, "acme", ReviewRequest.builder().build());
        var instance = engine.find("acme", runId).orElseThrow();
        assertThat(instance.getStatus()).isEqualTo(ReviewRunStatus.FAILED);
        // maxAttempts=3 → 退避 2 次
        assertThat(sleeper.calls.get()).isEqualTo(2);
        assertThat(sleeper.last).isEqualTo(Duration.ofMillis(10));
    }

    @Test
    void retrySucceedsAfterOneFailure() {
        RecordingSleeper sleeper = new RecordingSleeper();
        ReviewWorkflowEngine engine = engine(sleeper);
        AtomicInteger attempts = new AtomicInteger();
        ReviewPipeline pipeline =
                ReviewPipeline.builder()
                        .pipelineId("retry-ok")
                        .addStage(
                                ReviewStageDef.builder()
                                        .stageId("flaky")
                                        .executor(
                                                ctx ->
                                                        attempts.incrementAndGet() < 2
                                                                ? StageResult.failure("first")
                                                                : StageResult.success("recovered"))
                                        .retryPolicy(
                                                ReviewRetryPolicy.fixed(3, Duration.ofMillis(5)))
                                        .build())
                        .ruleConfig(RuleConfig.disabled())
                        .build();
        String runId = engine.start(pipeline, "acme", ReviewRequest.builder().build());
        var instance = engine.find("acme", runId).orElseThrow();
        assertThat(instance.getStatus()).isEqualTo(ReviewRunStatus.SUCCEEDED);
        assertThat(sleeper.calls.get()).isEqualTo(1);
    }

    @Test
    void exponentialBackoffSequence() {
        RecordingSleeper sleeper = new RecordingSleeper();
        ReviewWorkflowEngine engine = engine(sleeper);
        ReviewPipeline pipeline =
                ReviewPipeline.builder()
                        .pipelineId("retry-exp")
                        .addStage(
                                ReviewStageDef.builder()
                                        .stageId("flaky")
                                        .executor(ctx -> StageResult.failure("always"))
                                        .retryPolicy(
                                                ReviewRetryPolicy.exponential(
                                                        4, Duration.ofMillis(10), 2.0))
                                        .build())
                        .ruleConfig(RuleConfig.disabled())
                        .build();
        engine.start(pipeline, "acme", ReviewRequest.builder().build());
        // maxAttempts=4 → 3 次退避：10, 20, 40
        assertThat(sleeper.calls.get()).isEqualTo(3);
        assertThat(sleeper.last).isEqualTo(Duration.ofMillis(40));
    }

    @Test
    void snapshotRollbackClearsFindings() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        ReviewPipeline pipeline = ReviewPipelines.defaultPipeline(new MockReviewLlmClient());
        String runId =
                engine.start(
                        pipeline,
                        "acme",
                        ReviewRequest.builder().diffFiles(List.of(secretDiff())).build());
        var instance = engine.find("acme", runId).orElseThrow();
        assertThat(instance.getStatus()).isEqualTo(ReviewRunStatus.GATED);
        int before = instance.getSnapshots().size();
        engine.rollbackTo("acme", runId, 1);
        var after = engine.find("acme", runId).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(ReviewRunStatus.ROLLED_BACK);
        assertThat(after.getFindings()).isEmpty();
        assertThat(after.getSnapshots().size()).isLessThan(before);
        assertThat(after.isGated()).isFalse();
    }

    @Test
    void rollbackToInvalidVersionThrows() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        ReviewPipeline pipeline = ReviewPipelines.defaultPipeline(new MockReviewLlmClient());
        String runId =
                engine.start(
                        pipeline,
                        "acme",
                        ReviewRequest.builder().diffFiles(List.of(cleanDiff())).build());
        assertThatThrownBy(() -> engine.rollbackTo("acme", runId, 999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("快照版本不存在");
    }

    @Test
    void rollbackToMissingRunThrows() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        assertThatThrownBy(() -> engine.rollbackTo("acme", "nope", 1))
                .isInstanceOf(
                        io.github.nephets.reviewpilot.contract.ReviewRunNotFoundException.class);
    }

    @Test
    void resumeAfterRollbackReRunsToCompletion() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        ReviewPipeline pipeline = ReviewPipelines.defaultPipeline(new MockReviewLlmClient());
        String runId =
                engine.start(
                        pipeline,
                        "acme",
                        ReviewRequest.builder().diffFiles(List.of(cleanDiff())).build());
        engine.rollbackTo("acme", runId, 1);
        var rolled = engine.find("acme", runId).orElseThrow();
        assertThat(rolled.getStatus()).isEqualTo(ReviewRunStatus.ROLLED_BACK);
        engine.resume("acme", runId);
        var resumed = engine.find("acme", runId).orElseThrow();
        assertThat(resumed.getStatus()).isEqualTo(ReviewRunStatus.SUCCEEDED);
    }

    @Test
    void resumeOnTerminalRunIsNoOp() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        ReviewPipeline pipeline = ReviewPipelines.defaultPipeline(new MockReviewLlmClient());
        String runId =
                engine.start(
                        pipeline,
                        "acme",
                        ReviewRequest.builder().diffFiles(List.of(cleanDiff())).build());
        var before = engine.find("acme", runId).orElseThrow().getSnapshots().size();
        engine.resume("acme", runId);
        var after = engine.find("acme", runId).orElseThrow().getSnapshots().size();
        assertThat(after).isEqualTo(before);
    }

    @Test
    void multiTenantIsolation() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        ReviewPipeline pipeline = ReviewPipelines.defaultPipeline(new MockReviewLlmClient());
        String r1 =
                engine.start(
                        pipeline,
                        "acme",
                        ReviewRequest.builder().diffFiles(List.of(cleanDiff())).build());
        String r2 =
                engine.start(
                        pipeline,
                        "globex",
                        ReviewRequest.builder().diffFiles(List.of(cleanDiff())).build());
        assertThat(engine.find("acme", r1)).isPresent();
        assertThat(engine.find("globex", r2)).isPresent();
        // 跨租户不可见
        assertThat(engine.find("acme", r2)).isEmpty();
        assertThat(engine.find("globex", r1)).isEmpty();
        assertThat(engine.findAll("acme")).hasSize(1);
        assertThat(engine.findAll("globex")).hasSize(1);
    }

    @Test
    void findByStatusScopedToTenant() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        ReviewPipeline pipeline = ReviewPipelines.defaultPipeline(new MockReviewLlmClient());
        engine.start(
                pipeline, "acme", ReviewRequest.builder().diffFiles(List.of(cleanDiff())).build());
        engine.start(
                pipeline,
                "globex",
                ReviewRequest.builder().diffFiles(List.of(secretDiff())).build());
        assertThat(engine.findByStatus("acme", ReviewRunStatus.SUCCEEDED)).hasSize(1);
        assertThat(engine.findByStatus("globex", ReviewRunStatus.GATED)).hasSize(1);
    }

    @Test
    void staticAndAiFindingsCombined() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        // 同时触发静态（长行）与 AI（password）
        String longLine = "String password = \"" + "x".repeat(120) + "\";";
        DiffFile file =
                DiffFile.builder()
                        .path("A.java")
                        .status(DiffFile.FileStatus.ADDED)
                        .language("java")
                        .addChunk(
                                CodeChunk.builder()
                                        .filePath("A.java")
                                        .startLine(1)
                                        .endLine(1)
                                        .content(longLine)
                                        .build())
                        .build();
        String runId =
                engine.start(
                        ReviewPipelines.defaultPipeline(new MockReviewLlmClient()),
                        "acme",
                        ReviewRequest.builder().diffFiles(List.of(file)).build());
        var instance = engine.find("acme", runId).orElseThrow();
        assertThat(instance.getFindings()).anyMatch(f -> f.getSource() == FindingSource.STATIC);
        assertThat(instance.getFindings()).anyMatch(f -> f.getSource() == FindingSource.AI);
    }

    @Test
    void startByPipelineIdRequiresRegistration() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        assertThatThrownBy(
                        () ->
                                engine.start(
                                        "not-registered", "acme", ReviewRequest.builder().build()))
                .isInstanceOf(
                        io.github.nephets.reviewpilot.contract.InvalidReviewDefinitionException
                                .class);
    }

    @Test
    void startRegistersPipelineAndRuns() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        ReviewPipeline pipeline = ReviewPipelines.defaultPipeline(new MockReviewLlmClient());
        engine.getRegistry().register(pipeline);
        String runId =
                engine.start(
                        "default-review",
                        "acme",
                        ReviewRequest.builder().diffFiles(List.of(cleanDiff())).build());
        assertThat(engine.find("acme", runId)).isPresent();
    }

    @Test
    void nullPipelineRejected() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        assertThatThrownBy(
                        () ->
                                engine.start(
                                        (ReviewPipeline) null,
                                        "acme",
                                        ReviewRequest.builder().build()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullRequestRejected() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        assertThatThrownBy(
                        () ->
                                engine.start(
                                        ReviewPipelines.defaultPipeline(new MockReviewLlmClient()),
                                        "acme",
                                        null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void engineConstructorRejectsNullStore() {
        assertThatThrownBy(() -> new ReviewWorkflowEngine(null, new ReviewRegistry(), CLOCK, 50))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void engineConstructorRejectsBadSnapshotLimit() {
        assertThatThrownBy(
                        () ->
                                new ReviewWorkflowEngine(
                                        new InMemoryReviewRunStore(),
                                        new ReviewRegistry(),
                                        CLOCK,
                                        0,
                                        new RecordingSleeper()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void snapshotsAreVersionedChain() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        ReviewPipeline pipeline = ReviewPipelines.defaultPipeline(new MockReviewLlmClient());
        String runId =
                engine.start(
                        pipeline,
                        "acme",
                        ReviewRequest.builder().diffFiles(List.of(cleanDiff())).build());
        var instance = engine.find("acme", runId).orElseThrow();
        assertThat(instance.latestSnapshotVersion()).isGreaterThan(0);
        assertThat(instance.getSnapshots()).extracting(RunSnapshot::getVersion).isSorted();
    }

    @Test
    void useDefaultConstructorBuildsEngine() {
        ReviewWorkflowEngine engine = new ReviewWorkflowEngine(new InMemoryReviewRunStore());
        assertThat(engine.getRunStore()).isNotNull();
        assertThat(engine.getRegistry()).isNotNull();
    }

    @Test
    void closeIsSafe() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        engine.close();
    }

    @Test
    void tenantIdBlankFallsBackToDefault() {
        ReviewWorkflowEngine engine = engine(new RecordingSleeper());
        ReviewPipeline pipeline = ReviewPipelines.defaultPipeline(new MockReviewLlmClient());
        String runId =
                engine.start(
                        pipeline,
                        "  ",
                        ReviewRequest.builder().diffFiles(List.of(cleanDiff())).build());
        var instance = engine.find("default", runId).orElseThrow();
        assertThat(instance.getTenantId()).isEqualTo("default");
    }
}
