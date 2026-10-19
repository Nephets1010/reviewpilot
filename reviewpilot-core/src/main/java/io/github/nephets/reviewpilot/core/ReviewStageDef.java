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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 阶段定义：一个阶段“怎么执行、依赖谁、何时跳过、失败怎么重试”。
 *
 * <p>用 {@link #builder()} 构造。依赖以阶段 id 集合表示，构成 DAG；条件谓词决定运行期是否跳过； 重试策略决定失败后的退避与预算。
 */
public final class ReviewStageDef {

    private static final Logger log = LoggerFactory.getLogger(ReviewStageDef.class);

    private final String stageId;
    private final String name;
    private final Set<String> dependsOn;
    private final StageCondition condition;
    private final StageExecutor executor;
    private final ReviewRetryPolicy retryPolicy;

    private ReviewStageDef(Builder b) {
        this.stageId = b.stageId;
        this.name = b.name;
        this.dependsOn = Set.copyOf(b.dependsOn);
        this.condition = b.condition;
        this.executor = b.executor;
        this.retryPolicy = b.retryPolicy;
    }

    /** 返回构造器。 */
    public static Builder builder() {
        return new Builder();
    }

    /** 返回阶段 id（DAG 依赖引用）。 */
    public String getStageId() {
        return stageId;
    }

    /** 返回阶段名称。 */
    public String getName() {
        return name;
    }

    /** 返回依赖的阶段 id 集合（不可修改）。 */
    public Set<String> getDependsOn() {
        return dependsOn;
    }

    /** 返回条件谓词（可能为 {@code null}，表示总是执行）。 */
    public StageCondition getCondition() {
        return condition;
    }

    /** 返回阶段执行器（可能为 {@code null}，由引擎视情况拒绝）。 */
    public StageExecutor getExecutor() {
        return executor;
    }

    /** 返回重试策略。 */
    public ReviewRetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    /** 是否有条件谓词。 */
    public boolean hasCondition() {
        return condition != null;
    }

    @Override
    public String toString() {
        return "ReviewStageDef{stageId='" + stageId + "', dependsOn=" + dependsOn + '}';
    }

    /** {@link ReviewStageDef} 构造器。 */
    public static final class Builder {

        private String stageId;
        private String name;
        private final Set<String> dependsOn = new LinkedHashSet<>();
        private StageCondition condition;
        private StageExecutor executor;
        private ReviewRetryPolicy retryPolicy = ReviewRetryPolicy.none();

        private Builder() {}

        /** 设置阶段 id，不可为空。 */
        public Builder stageId(String stageId) {
            this.stageId = stageId;
            return this;
        }

        /** 设置阶段名称；未设置时回退为阶段 id。 */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** 追加一个依赖的阶段 id。 */
        public Builder dependsOn(String stageId) {
            if (stageId == null || stageId.isBlank()) {
                throw new IllegalArgumentException("dependsOn 的 stageId 不可为空");
            }
            dependsOn.add(stageId.trim());
            return this;
        }

        /** 覆盖设置依赖集合。 */
        public Builder dependsOn(Set<String> stageIds) {
            dependsOn.clear();
            if (stageIds != null) {
                dependsOn.addAll(stageIds);
            }
            return this;
        }

        /** 设置条件谓词（为 {@code null} 表示无条件执行）。 */
        public Builder condition(StageCondition condition) {
            this.condition = condition;
            return this;
        }

        /** 设置执行器（不可为 {@code null}）。 */
        public Builder executor(StageExecutor executor) {
            this.executor = executor;
            return this;
        }

        /** 设置重试策略；未设置时为 {@link ReviewRetryPolicy#none()}。 */
        public Builder retryPolicy(ReviewRetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy == null ? ReviewRetryPolicy.none() : retryPolicy;
            return this;
        }

        /** 构造阶段定义。 */
        public ReviewStageDef build() {
            if (stageId == null || stageId.isBlank()) {
                log.error("build 阶段定义缺少 stageId");
                throw new IllegalArgumentException("stageId 不可为空");
            }
            if (dependsOn.contains(stageId)) {
                throw new IllegalArgumentException("阶段不能依赖自己：" + stageId);
            }
            if (executor == null) {
                log.error("build 阶段定义缺少执行器: stageId={}", stageId);
                throw new IllegalArgumentException("executor 不可为 null，stageId=" + stageId);
            }
            String trimmed = stageId.trim();
            Builder filled = new Builder();
            filled.stageId = trimmed;
            filled.name = (name == null || name.isBlank()) ? trimmed : name;
            filled.dependsOn.addAll(dependsOn);
            filled.condition = condition;
            filled.executor = executor;
            filled.retryPolicy = retryPolicy;
            return new ReviewStageDef(filled);
        }
    }
}
