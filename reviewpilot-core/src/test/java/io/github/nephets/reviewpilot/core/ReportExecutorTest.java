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

import io.github.nephets.reviewpilot.contract.Finding;
import io.github.nephets.reviewpilot.contract.GateResult;
import io.github.nephets.reviewpilot.contract.ReviewRunId;
import io.github.nephets.reviewpilot.contract.ReviewRunStatus;
import io.github.nephets.reviewpilot.contract.RuleConfig;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ReportExecutorTest {

    private static StageContext context() {
        return new StageContext(
                ReviewRunId.of("r1"),
                "acme",
                "p1",
                Clock.fixed(Instant.EPOCH, java.time.ZoneOffset.UTC),
                ReviewRequest.scripted("demo"),
                RuleConfig.defaultConfig());
    }

    @Test
    void reportSucceededWhenGatePassed() {
        StageContext ctx = context();
        ctx.setGateResult(GateResult.builder().passed(true).build());
        new ReportExecutor().execute(ctx);
        assertThat(ctx.getReport().getStatus()).isEqualTo(ReviewRunStatus.SUCCEEDED);
        assertThat(ctx.getReport().getSummary()).contains("门禁通过");
    }

    @Test
    void reportGatedWhenGateFailed() {
        StageContext ctx = context();
        ctx.setGateResult(GateResult.builder().passed(false).blockerCount(1).build());
        new ReportExecutor().execute(ctx);
        assertThat(ctx.getReport().getStatus()).isEqualTo(ReviewRunStatus.GATED);
        assertThat(ctx.getReport().getSummary()).contains("门禁未通过");
    }

    @Test
    void reportCountsBlockerAndCritical() {
        StageContext ctx = context();
        ctx.addFinding(
                Finding.builder()
                        .id("b")
                        .severity(io.github.nephets.reviewpilot.contract.ReviewSeverity.BLOCKER)
                        .message("b")
                        .build());
        ctx.addFinding(
                Finding.builder()
                        .id("c")
                        .severity(io.github.nephets.reviewpilot.contract.ReviewSeverity.CRITICAL)
                        .message("c")
                        .build());
        new ReportExecutor().execute(ctx);
        assertThat(ctx.getReport().getSummary()).contains("BLOCKER 1").contains("CRITICAL 1");
    }

    @Test
    void stageIdMatchesEnum() {
        assertThat(ReportExecutor.stageId())
                .isEqualTo(io.github.nephets.reviewpilot.contract.ReviewStage.REPORT.id());
    }
}
