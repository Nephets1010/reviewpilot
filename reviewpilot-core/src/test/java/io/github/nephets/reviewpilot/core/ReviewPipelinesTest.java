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
import io.github.nephets.reviewpilot.llm.MockReviewLlmClient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewPipelinesTest {

    @Test
    void defaultPipelineHasFiveStagesInOrder() {
        ReviewPipeline p = ReviewPipelines.defaultPipeline(new MockReviewLlmClient());
        assertThat(p.getPipelineId()).isEqualTo("default-review");
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

    @Test
    void defaultPipelineUsesDefaultRuleConfig() {
        ReviewPipeline p = ReviewPipelines.defaultPipeline(new MockReviewLlmClient());
        assertThat(p.getRuleConfig()).isEqualTo(RuleConfig.defaultConfig());
    }

    @Test
    void defaultPipelineWithRuleConfigOverrides() {
        RuleConfig custom = RuleConfig.builder().blockerThreshold(5).build();
        ReviewPipeline p = ReviewPipelines.defaultPipeline(new MockReviewLlmClient(), custom);
        assertThat(p.getRuleConfig().getBlockerThreshold()).isEqualTo(5);
    }

    @Test
    void withRuleConfigCopiesStages() {
        ReviewPipeline base = ReviewPipelines.defaultPipeline(new MockReviewLlmClient());
        RuleConfig custom = RuleConfig.disabled();
        ReviewPipeline derived = ReviewPipelines.withRuleConfig(base, custom);
        assertThat(derived.size()).isEqualTo(base.size());
        assertThat(derived.getRuleConfig()).isEqualTo(custom);
        assertThat(derived.getPipelineId()).isEqualTo(base.getPipelineId());
    }

    @Test
    void nullClientRejected() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> ReviewPipelines.defaultPipeline(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullRuleConfigRejected() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> ReviewPipelines.defaultPipeline(new MockReviewLlmClient(), null))
                .isInstanceOf(NullPointerException.class);
    }
}
