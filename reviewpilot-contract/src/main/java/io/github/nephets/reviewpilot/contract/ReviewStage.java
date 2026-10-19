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

import java.util.Optional;

/**
 * 流水线内置阶段标识。
 *
 * <p>默认流水线顺序为 {@code FETCH_DIFF → STATIC_ANALYSIS → AI_REVIEW → AGGREGATE → REPORT}。 引擎本身支持任意
 * DAG，阶段 id 由 {@link #id()} 暴露，便于在 {@code ReviewStageDef} 中引用依赖。
 */
public enum ReviewStage {

    /** 拉取待评审 diff。 */
    FETCH_DIFF("fetch-diff"),

    /** 静态检查。 */
    STATIC_ANALYSIS("static-analysis"),

    /** AI 评审。 */
    AI_REVIEW("ai-review"),

    /** 聚合静态发现与 AI 发现。 */
    AGGREGATE("aggregate"),

    /** 生成报告并落地门禁结果。 */
    REPORT("report");

    private final String id;

    ReviewStage(String id) {
        this.id = id;
    }

    /** 返回阶段 id（用于 DAG 依赖引用）。 */
    public String id() {
        return id;
    }

    /**
     * 按 id 查找阶段。
     *
     * @param id 阶段 id
     * @return 匹配的阶段，未找到返回 {@link Optional#empty()}
     */
    public static Optional<ReviewStage> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        for (ReviewStage stage : values()) {
            if (stage.id.equals(id)) {
                return Optional.of(stage);
            }
        }
        return Optional.empty();
    }
}
