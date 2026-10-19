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

import io.github.nephets.reviewpilot.contract.ReviewStage;
import io.github.nephets.reviewpilot.contract.RuleConfig;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewRegistryTest {

    private static ReviewPipeline pipeline(String id) {
        return ReviewPipeline.builder()
                .pipelineId(id)
                .addStage(
                        ReviewStageDef.builder()
                                .stageId(ReviewStage.FETCH_DIFF.id())
                                .executor(ctx -> StageResult.success())
                                .build())
                .ruleConfig(RuleConfig.defaultConfig())
                .build();
    }

    @Test
    void registerAndFind() {
        ReviewRegistry registry = new ReviewRegistry();
        ReviewPipeline p = pipeline("p1");
        registry.register(p);
        assertThat(registry.size()).isEqualTo(1);
        assertThat(registry.contains("p1")).isTrue();
        assertThat(registry.find("p1")).contains(p);
    }

    @Test
    void findMissingReturnsEmpty() {
        assertThat(new ReviewRegistry().find("nope")).isEmpty();
    }

    @Test
    void requireThrowsWhenMissing() {
        ReviewRegistry registry = new ReviewRegistry();
        assertThatThrownBy(() -> registry.require("nope"))
                .isInstanceOf(
                        io.github.nephets.reviewpilot.contract.InvalidReviewDefinitionException
                                .class);
    }

    @Test
    void requireReturnsExisting() {
        ReviewRegistry registry = new ReviewRegistry();
        registry.register(pipeline("p1"));
        assertThat(registry.require("p1").getPipelineId()).isEqualTo("p1");
    }

    @Test
    void registerOverwrites() {
        ReviewRegistry registry = new ReviewRegistry();
        registry.register(pipeline("p1"));
        registry.register(pipeline("p1"));
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    void nullPipelineRejected() {
        assertThatThrownBy(() -> new ReviewRegistry().register(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void clearEmptiesRegistry() {
        ReviewRegistry registry = new ReviewRegistry();
        registry.register(pipeline("p1"));
        registry.clear();
        assertThat(registry.size()).isEqualTo(0);
        assertThat(registry.contains("p1")).isFalse();
    }

    @Test
    void defaultPipelineRegistersFiveStages() {
        ReviewRegistry registry = new ReviewRegistry();
        registry.register(
                ReviewPipelines.defaultPipeline(
                        new io.github.nephets.reviewpilot.llm.MockReviewLlmClient()));
        ReviewPipeline p = registry.require("default-review");
        assertThat(p.size()).isEqualTo(5);
        List<String> order = p.topologicalOrder().stream().map(ReviewStageDef::getStageId).toList();
        assertThat(order)
                .containsExactly(
                        ReviewStage.FETCH_DIFF.id(),
                        ReviewStage.STATIC_ANALYSIS.id(),
                        ReviewStage.AI_REVIEW.id(),
                        ReviewStage.AGGREGATE.id(),
                        ReviewStage.REPORT.id());
    }
}
