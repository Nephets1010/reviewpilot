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

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewStageDefTest {

    private static StageExecutor noop() {
        return ctx -> StageResult.success();
    }

    @Test
    void buildsWithDefaultsAndNoCondition() {
        ReviewStageDef def = ReviewStageDef.builder().stageId("s1").executor(noop()).build();
        assertThat(def.getStageId()).isEqualTo("s1");
        assertThat(def.getName()).isEqualTo("s1");
        assertThat(def.getDependsOn()).isEmpty();
        assertThat(def.hasCondition()).isFalse();
        assertThat(def.getCondition()).isNull();
        assertThat(def.getRetryPolicy()).isEqualTo(ReviewRetryPolicy.none());
    }

    @Test
    void nameFallsBackToStageId() {
        assertThat(ReviewStageDef.builder().stageId("s1").executor(noop()).build().getName())
                .isEqualTo("s1");
    }

    @Test
    void explicitNameUsed() {
        assertThat(
                        ReviewStageDef.builder()
                                .stageId("s1")
                                .name("Stage One")
                                .executor(noop())
                                .build()
                                .getName())
                .isEqualTo("Stage One");
    }

    @Test
    void dependsOnAccumulates() {
        ReviewStageDef def =
                ReviewStageDef.builder()
                        .stageId("s2")
                        .dependsOn("s0")
                        .dependsOn("a")
                        .executor(noop())
                        .build();
        assertThat(def.getDependsOn()).containsExactlyInAnyOrder("s0", "a");
    }

    @Test
    void dependsOnSetOverrides() {
        ReviewStageDef def =
                ReviewStageDef.builder()
                        .stageId("s2")
                        .dependsOn(Set.of("x", "y"))
                        .executor(noop())
                        .build();
        assertThat(def.getDependsOn()).containsExactlyInAnyOrder("x", "y");
    }

    @Test
    void blankDependsOnRejected() {
        assertThatThrownBy(
                        () ->
                                ReviewStageDef.builder()
                                        .stageId("s1")
                                        .dependsOn(" ")
                                        .executor(noop()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullExecutorRejected() {
        assertThatThrownBy(() -> ReviewStageDef.builder().stageId("s1").build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("executor");
    }

    @Test
    void blankStageIdRejected() {
        assertThatThrownBy(() -> ReviewStageDef.builder().stageId(" ").executor(noop()).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void selfDependencyRejected() {
        assertThatThrownBy(
                        () ->
                                ReviewStageDef.builder()
                                        .stageId("s1")
                                        .dependsOn("s1")
                                        .executor(noop())
                                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("自己");
    }

    @Test
    void conditionIsRetained() {
        StageCondition cond = ctx -> true;
        ReviewStageDef def =
                ReviewStageDef.builder().stageId("s1").condition(cond).executor(noop()).build();
        assertThat(def.hasCondition()).isTrue();
        assertThat(def.getCondition()).isEqualTo(cond);
    }

    @Test
    void retryPolicyIsRetained() {
        ReviewRetryPolicy policy = ReviewRetryPolicy.fixed(3, java.time.Duration.ofMillis(5));
        ReviewStageDef def =
                ReviewStageDef.builder().stageId("s1").retryPolicy(policy).executor(noop()).build();
        assertThat(def.getRetryPolicy()).isEqualTo(policy);
    }

    @Test
    void nullRetryPolicyFallsBackToNone() {
        assertThat(
                        ReviewStageDef.builder()
                                .stageId("s1")
                                .retryPolicy(null)
                                .executor(noop())
                                .build()
                                .getRetryPolicy())
                .isEqualTo(ReviewRetryPolicy.none());
    }
}
