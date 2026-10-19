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
package io.github.nephets.reviewpilot.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewStageTest {

    @Test
    void everyStageIdIsStableString() {
        assertThat(ReviewStage.FETCH_DIFF.id()).isEqualTo("fetch-diff");
        assertThat(ReviewStage.STATIC_ANALYSIS.id()).isEqualTo("static-analysis");
        assertThat(ReviewStage.AI_REVIEW.id()).isEqualTo("ai-review");
        assertThat(ReviewStage.AGGREGATE.id()).isEqualTo("aggregate");
        assertThat(ReviewStage.REPORT.id()).isEqualTo("report");
    }

    @Test
    void fromIdResolvesKnownStages() {
        assertThat(ReviewStage.fromId("fetch-diff")).contains(ReviewStage.FETCH_DIFF);
        assertThat(ReviewStage.fromId("static-analysis")).contains(ReviewStage.STATIC_ANALYSIS);
        assertThat(ReviewStage.fromId("ai-review")).contains(ReviewStage.AI_REVIEW);
        assertThat(ReviewStage.fromId("aggregate")).contains(ReviewStage.AGGREGATE);
        assertThat(ReviewStage.fromId("report")).contains(ReviewStage.REPORT);
    }

    @Test
    void fromIdReturnsEmptyForUnknown() {
        assertThat(ReviewStage.fromId("nope")).isEmpty();
        assertThat(ReviewStage.fromId(null)).isEmpty();
    }

    @Test
    void everyStageHasNonNullId() {
        for (ReviewStage stage : ReviewStage.values()) {
            assertThat(stage.id()).isNotEmpty();
        }
    }
}
