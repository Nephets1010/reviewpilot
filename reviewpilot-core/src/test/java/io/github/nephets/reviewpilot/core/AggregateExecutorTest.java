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
import io.github.nephets.reviewpilot.contract.ReviewRunId;
import io.github.nephets.reviewpilot.contract.RuleConfig;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AggregateExecutorTest {

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
    void dedupesById() {
        StageContext ctx = context();
        ctx.addFinding(Finding.builder().id("a").message("1").build());
        ctx.addFinding(Finding.builder().id("a").message("2").build());
        ctx.addFinding(Finding.builder().id("b").message("3").build());
        StageResult result = new AggregateExecutor().execute(ctx);
        assertThat(result.getStatus()).isEqualTo(StageStatus.SUCCEEDED);
        assertThat(ctx.getFindings()).hasSize(2);
    }

    @Test
    void preservesOrder() {
        StageContext ctx = context();
        ctx.addFinding(Finding.builder().id("b").message("b").build());
        ctx.addFinding(Finding.builder().id("a").message("a").build());
        new AggregateExecutor().execute(ctx);
        assertThat(ctx.getFindings().stream().map(Finding::getId).toList())
                .containsExactly("b", "a");
    }

    @Test
    void stageIdMatchesEnum() {
        assertThat(AggregateExecutor.stageId())
                .isEqualTo(io.github.nephets.reviewpilot.contract.ReviewStage.AGGREGATE.id());
    }
}
