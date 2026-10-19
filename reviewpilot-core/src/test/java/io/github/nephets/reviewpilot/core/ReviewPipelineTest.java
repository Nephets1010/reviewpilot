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

import io.github.nephets.reviewpilot.contract.RuleConfig;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewPipelineTest {

    private static ReviewStageDef stage(String id, String... deps) {
        ReviewStageDef.Builder b =
                ReviewStageDef.builder().stageId(id).executor(ctx -> StageResult.success());
        for (String d : deps) {
            b.dependsOn(d);
        }
        return b.build();
    }

    @Test
    void topologicalOrderRespectsDependencies() {
        ReviewPipeline pipeline =
                ReviewPipeline.builder()
                        .pipelineId("p")
                        .addStage(stage("a"))
                        .addStage(stage("b", "a"))
                        .addStage(stage("c", "a", "b"))
                        .ruleConfig(RuleConfig.defaultConfig())
                        .build();
        List<String> order =
                pipeline.topologicalOrder().stream().map(ReviewStageDef::getStageId).toList();
        assertThat(order).containsExactly("a", "b", "c");
    }

    @Test
    void buildTriggersTopologicalValidation() {
        ReviewPipeline.builder()
                .pipelineId("p")
                .addStage(stage("a"))
                .addStage(stage("b", "a"))
                .ruleConfig(RuleConfig.defaultConfig())
                .build();
    }

    @Test
    void cycleDetected() {
        assertThatThrownBy(
                        () ->
                                ReviewPipeline.builder()
                                        .pipelineId("p")
                                        .addStage(stage("a", "b"))
                                        .addStage(stage("b", "a"))
                                        .ruleConfig(RuleConfig.defaultConfig())
                                        .build())
                .isInstanceOf(
                        io.github.nephets.reviewpilot.contract.InvalidReviewDefinitionException
                                .class)
                .hasMessageContaining("环依赖");
    }

    @Test
    void orphanDependencyDetected() {
        assertThatThrownBy(
                        () ->
                                ReviewPipeline.builder()
                                        .pipelineId("p")
                                        .addStage(stage("a", "missing"))
                                        .ruleConfig(RuleConfig.defaultConfig())
                                        .build())
                .isInstanceOf(
                        io.github.nephets.reviewpilot.contract.InvalidReviewDefinitionException
                                .class)
                .hasMessageContaining("不存在的阶段");
    }

    @Test
    void duplicateStageIdRejectedAtBuild() {
        assertThatThrownBy(
                        () ->
                                ReviewPipeline.builder()
                                        .pipelineId("p")
                                        .addStage(stage("a"))
                                        .addStage(stage("a"))
                                        .ruleConfig(RuleConfig.defaultConfig())
                                        .build())
                .isInstanceOf(
                        io.github.nephets.reviewpilot.contract.InvalidReviewDefinitionException
                                .class)
                .hasMessageContaining("重复");
    }

    @Test
    void missingPipelineIdRejected() {
        assertThatThrownBy(
                        () ->
                                ReviewPipeline.builder()
                                        .addStage(stage("a"))
                                        .ruleConfig(RuleConfig.defaultConfig())
                                        .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noStagesRejected() {
        assertThatThrownBy(
                        () ->
                                ReviewPipeline.builder()
                                        .pipelineId("p")
                                        .ruleConfig(RuleConfig.defaultConfig())
                                        .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeVersionRejected() {
        assertThatThrownBy(
                        () ->
                                ReviewPipeline.builder()
                                        .pipelineId("p")
                                        .version(0)
                                        .addStage(stage("a"))
                                        .ruleConfig(RuleConfig.defaultConfig())
                                        .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blankStageRejectedInStagesList() {
        assertThatThrownBy(
                        () ->
                                ReviewPipeline.builder()
                                        .pipelineId("p")
                                        .stages(List.of())
                                        .ruleConfig(RuleConfig.defaultConfig())
                                        .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gettersReturnValues() {
        ReviewPipeline pipeline =
                ReviewPipeline.builder()
                        .pipelineId("p")
                        .name("Pipeline")
                        .version(2)
                        .addStage(stage("a"))
                        .ruleConfig(RuleConfig.defaultConfig())
                        .build();
        assertThat(pipeline.getPipelineId()).isEqualTo("p");
        assertThat(pipeline.getName()).isEqualTo("Pipeline");
        assertThat(pipeline.getVersion()).isEqualTo(2);
        assertThat(pipeline.size()).isEqualTo(1);
        assertThat(pipeline.stage("a")).isPresent();
        assertThat(pipeline.stage("z")).isEmpty();
        assertThat(pipeline.getRuleConfig()).isEqualTo(RuleConfig.defaultConfig());
    }
}
