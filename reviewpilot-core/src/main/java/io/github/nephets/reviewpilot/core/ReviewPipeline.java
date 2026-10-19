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

import io.github.nephets.reviewpilot.contract.InvalidReviewDefinitionException;
import io.github.nephets.reviewpilot.contract.RuleConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 评审流水线定义（DAG）：一组阶段 + 规则配置 + 版本号。
 *
 * <p><b>为什么阶段组成 DAG 而不是写死的线性列表：</b>静态检查、安全扫描、依赖审计往往互相独立可并行， 而“聚合 /
 * 报告”依赖它们全部完成。把阶段固化为一条直线会把这种天然结构压平。DAG 让引擎只认依赖边， 线性只是 DAG 的特例。{@link #topologicalOrder()}
 * 负责把依赖关系排成可执行顺序，并检测环。
 */
public final class ReviewPipeline {

    private static final Logger log = LoggerFactory.getLogger(ReviewPipeline.class);

    private final String pipelineId;
    private final String name;
    private final int version;
    private final List<ReviewStageDef> stages;
    private final RuleConfig ruleConfig;

    private ReviewPipeline(Builder b) {
        this.pipelineId = b.pipelineId;
        this.name = b.name;
        this.version = b.version;
        this.stages = List.copyOf(b.stages);
        this.ruleConfig = b.ruleConfig == null ? RuleConfig.defaultConfig() : b.ruleConfig;
    }

    /** 返回构造器。 */
    public static Builder builder() {
        return new Builder();
    }

    /** 返回流水线 id。 */
    public String getPipelineId() {
        return pipelineId;
    }

    /** 返回流水线名称。 */
    public String getName() {
        return name;
    }

    /** 返回版本号。 */
    public int getVersion() {
        return version;
    }

    /** 返回阶段列表（不可修改）。 */
    public List<ReviewStageDef> getStages() {
        return stages;
    }

    /** 返回规则配置。 */
    public RuleConfig getRuleConfig() {
        return ruleConfig;
    }

    /** 返回阶段数量。 */
    public int size() {
        return stages.size();
    }

    /** 按阶段 id 查找。 */
    public Optional<ReviewStageDef> stage(String stageId) {
        for (ReviewStageDef s : stages) {
            if (s.getStageId().equals(stageId)) {
                return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    /**
     * 返回拓扑有序的阶段列表（依赖者总在依赖之后）。
     *
     * @return 可执行顺序的阶段列表
     * @throws InvalidReviewDefinitionException 存在环依赖或悬空依赖
     */
    public List<ReviewStageDef> topologicalOrder() {
        Map<String, ReviewStageDef> byId = new HashMap<>();
        for (ReviewStageDef s : stages) {
            byId.put(s.getStageId(), s);
        }
        for (ReviewStageDef s : stages) {
            for (String dep : s.getDependsOn()) {
                if (!byId.containsKey(dep)) {
                    throw new InvalidReviewDefinitionException(
                            "阶段 " + s.getStageId() + " 依赖了不存在的阶段 " + dep);
                }
            }
        }
        Map<String, Integer> indegree = new LinkedHashMap<>();
        for (ReviewStageDef s : stages) {
            indegree.put(s.getStageId(), 0);
        }
        for (ReviewStageDef s : stages) {
            for (String dep : s.getDependsOn()) {
                indegree.merge(s.getStageId(), 1, Integer::sum);
            }
        }
        List<ReviewStageDef> order = new ArrayList<>();
        List<ReviewStageDef> ready = new ArrayList<>();
        for (ReviewStageDef s : stages) {
            if (indegree.get(s.getStageId()) == 0) {
                ready.add(s);
            }
        }
        while (!ready.isEmpty()) {
            ReviewStageDef current = ready.remove(0);
            order.add(current);
            for (ReviewStageDef s : stages) {
                if (s.getDependsOn().contains(current.getStageId())) {
                    int left = indegree.merge(s.getStageId(), -1, Integer::sum);
                    if (left == 0) {
                        ready.add(s);
                    }
                }
            }
        }
        if (order.size() != stages.size()) {
            throw new InvalidReviewDefinitionException("流水线存在环依赖，无法拓扑排序：pipelineId=" + pipelineId);
        }
        return order;
    }

    @Override
    public String toString() {
        return "ReviewPipeline{pipelineId='"
                + pipelineId
                + "', version="
                + version
                + ", stages="
                + stages.size()
                + '}';
    }

    /** {@link ReviewPipeline} 构造器。 */
    public static final class Builder {

        private String pipelineId;
        private String name;
        private int version = 1;
        private final List<ReviewStageDef> stages = new ArrayList<>();
        private RuleConfig ruleConfig;

        private Builder() {}

        /** 设置流水线 id，不可为空。 */
        public Builder pipelineId(String pipelineId) {
            this.pipelineId = pipelineId;
            return this;
        }

        /** 设置名称；未设置时回退为 id。 */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /** 设置版本号，必须 &gt;= 1。 */
        public Builder version(int version) {
            this.version = version;
            return this;
        }

        /** 追加一个阶段。 */
        public Builder addStage(ReviewStageDef stage) {
            if (stage == null) {
                throw new IllegalArgumentException("stage 不可为 null");
            }
            stages.add(stage);
            return this;
        }

        /** 覆盖设置阶段列表。 */
        public Builder stages(List<ReviewStageDef> stages) {
            if (stages == null) {
                throw new IllegalArgumentException("stages 不可为 null");
            }
            this.stages.clear();
            this.stages.addAll(stages);
            return this;
        }

        /** 设置规则配置（为 {@code null} 时使用默认）。 */
        public Builder ruleConfig(RuleConfig ruleConfig) {
            this.ruleConfig = ruleConfig;
            return this;
        }

        /** 构造流水线。 */
        public ReviewPipeline build() {
            if (pipelineId == null || pipelineId.isBlank()) {
                log.error("build 流水线缺少 pipelineId");
                throw new IllegalArgumentException("pipelineId 不可为空");
            }
            if (stages.isEmpty()) {
                log.error("build 流水线没有阶段: pipelineId={}", pipelineId);
                throw new IllegalArgumentException("流水线至少需要一个阶段");
            }
            if (version < 1) {
                throw new IllegalArgumentException("version 必须 >= 1，当前值：" + version);
            }
            Map<String, Boolean> seen = new LinkedHashMap<>();
            for (ReviewStageDef s : stages) {
                if (seen.putIfAbsent(s.getStageId(), Boolean.TRUE) != null) {
                    throw new InvalidReviewDefinitionException("阶段 id 重复：" + s.getStageId());
                }
            }
            Builder filled = new Builder();
            filled.pipelineId = pipelineId.trim();
            filled.name = (name == null || name.isBlank()) ? filled.pipelineId : name;
            filled.version = version;
            filled.stages.addAll(stages);
            filled.ruleConfig = ruleConfig;
            ReviewPipeline built = new ReviewPipeline(filled);
            // 提前触发一次拓扑排序以暴露环/悬空依赖，而不是等到运行时才失败。
            built.topologicalOrder();
            log.debug(
                    "build 构造流水线: pipelineId={}, version={}, stageCount={}",
                    built.getPipelineId(),
                    built.getVersion(),
                    built.size());
            return built;
        }
    }
}
