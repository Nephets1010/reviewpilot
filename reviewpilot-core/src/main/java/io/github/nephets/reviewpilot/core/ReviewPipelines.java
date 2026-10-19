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
import io.github.nephets.reviewpilot.llm.ReviewLlmClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 默认流水线工厂：把内置阶段拼成标准 DAG。
 *
 * <p>默认顺序为 {@code FETCH_DIFF → STATIC_ANALYSIS → AI_REVIEW → AGGREGATE → REPORT}， 其中 STATIC 与 AI
 * 都依赖 FETCH，AGGREGATE 依赖 STATIC 与 AI，REPORT 依赖 AGGREGATE。 这正好是一个 DAG：STATIC 与 AI
 * 可并行（引擎当前按拓扑序串行推进），REPORT 必须等聚合完成。
 */
public final class ReviewPipelines {

    private static final Logger log = LoggerFactory.getLogger(ReviewPipelines.class);

    private ReviewPipelines() {}

    /**
     * 构造默认五阶段流水线。
     *
     * @param client AI 评审客户端（如 {@code MockReviewLlmClient}），不可为 {@code null}
     * @return 流水线定义
     */
    public static ReviewPipeline defaultPipeline(ReviewLlmClient client) {
        Objects.requireNonNull(client, "client");
        ReviewStageDef fetch =
                ReviewStageDef.builder()
                        .stageId(ReviewStage.FETCH_DIFF.id())
                        .name("拉取 Diff")
                        .executor(new FetchDiffExecutor())
                        .build();
        ReviewStageDef staticAnalysis =
                ReviewStageDef.builder()
                        .stageId(ReviewStage.STATIC_ANALYSIS.id())
                        .name("静态检查")
                        .dependsOn(ReviewStage.FETCH_DIFF.id())
                        .executor(new StaticAnalysisExecutor())
                        .build();
        ReviewStageDef aiReview =
                ReviewStageDef.builder()
                        .stageId(ReviewStage.AI_REVIEW.id())
                        .name("AI 评审")
                        .dependsOn(ReviewStage.FETCH_DIFF.id())
                        .executor(new AiReviewExecutor(client))
                        .build();
        ReviewStageDef aggregate =
                ReviewStageDef.builder()
                        .stageId(ReviewStage.AGGREGATE.id())
                        .name("聚合")
                        .dependsOn(ReviewStage.STATIC_ANALYSIS.id())
                        .dependsOn(ReviewStage.AI_REVIEW.id())
                        .executor(new AggregateExecutor())
                        .build();
        ReviewStageDef report =
                ReviewStageDef.builder()
                        .stageId(ReviewStage.REPORT.id())
                        .name("报告")
                        .dependsOn(ReviewStage.AGGREGATE.id())
                        .executor(new ReportExecutor())
                        .build();
        ReviewPipeline pipeline =
                ReviewPipeline.builder()
                        .pipelineId("default-review")
                        .name("默认评审流水线")
                        .version(1)
                        .addStage(fetch)
                        .addStage(staticAnalysis)
                        .addStage(aiReview)
                        .addStage(aggregate)
                        .addStage(report)
                        .ruleConfig(RuleConfig.defaultConfig())
                        .build();
        log.debug("defaultPipeline 构造默认流水线: pipelineId={}", pipeline.getPipelineId());
        return pipeline;
    }

    /**
     * 构造默认五阶段流水线，并允许覆盖规则配置（用于门禁相关测试）。
     *
     * @param client AI 评审客户端
     * @param ruleConfig 规则配置
     * @return 流水线定义
     */
    public static ReviewPipeline defaultPipeline(ReviewLlmClient client, RuleConfig ruleConfig) {
        Objects.requireNonNull(ruleConfig, "ruleConfig");
        return withRuleConfig(defaultPipeline(client), ruleConfig);
    }

    /**
     * 复制一份流水线但替换其规则配置。
     *
     * @param source 源流水线
     * @param ruleConfig 规则配置
     * @return 新流水线
     */
    public static ReviewPipeline withRuleConfig(ReviewPipeline source, RuleConfig ruleConfig) {
        return ReviewPipeline.builder()
                .pipelineId(source.getPipelineId())
                .name(source.getName())
                .version(source.getVersion())
                .stages(source.getStages())
                .ruleConfig(ruleConfig)
                .build();
    }
}
