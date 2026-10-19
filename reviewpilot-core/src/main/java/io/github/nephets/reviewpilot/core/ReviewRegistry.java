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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 流水线注册表：按 id 保存 {@link ReviewPipeline} 定义，供引擎按 id 触发运行。
 *
 * <p>重复注册同一 id 会覆盖，日志会记录覆盖以便排查“定义被悄悄替换”的问题。
 */
public final class ReviewRegistry {

    private static final Logger log = LoggerFactory.getLogger(ReviewRegistry.class);

    private final Map<String, ReviewPipeline> pipelines = new LinkedHashMap<>();

    /** 注册（或覆盖）一条流水线定义。 */
    public void register(ReviewPipeline pipeline) {
        Objects.requireNonNull(pipeline, "pipeline");
        String id = pipeline.getPipelineId();
        if (pipelines.containsKey(id)) {
            log.warn("register 覆盖已存在的流水线定义: pipelineId={}", id);
        }
        pipelines.put(id, pipeline);
    }

    /** 按 id 查找流水线。 */
    public Optional<ReviewPipeline> find(String pipelineId) {
        return Optional.ofNullable(pipelines.get(pipelineId));
    }

    /** 是否已注册某 id。 */
    public boolean contains(String pipelineId) {
        return pipelines.containsKey(pipelineId);
    }

    /** 已注册数量。 */
    public int size() {
        return pipelines.size();
    }

    /**
     * 按 id 取出流水线，未注册时抛错。
     *
     * @param pipelineId 流水线 id
     * @return 流水线定义
     * @throws InvalidReviewDefinitionException 未注册
     */
    public ReviewPipeline require(String pipelineId) {
        ReviewPipeline p = pipelines.get(pipelineId);
        if (p == null) {
            throw new InvalidReviewDefinitionException("流水线未注册：" + pipelineId);
        }
        return p;
    }

    /** 清空全部注册。 */
    public void clear() {
        pipelines.clear();
    }
}
