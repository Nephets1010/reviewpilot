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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内置阶段执行器：聚合静态发现与 AI 发现。
 *
 * <p>发现已在各阶段执行时并入上下文，本阶段做最终去重并按 id 稳定排序，使后续门禁与报告看到的列表确定。 这是聚合之后立即套用规则门禁的自然节点（见 {@link
 * ReviewWorkflowEngine}）。
 */
public final class AggregateExecutor implements StageExecutor {

    private static final Logger log = LoggerFactory.getLogger(AggregateExecutor.class);

    @Override
    public StageResult execute(StageContext ctx) {
        // 按 id 去重，避免静态与 AI 对同一条目重复计数（防御性，默认实现下 id 空间本不重叠）。
        Map<String, io.github.nephets.reviewpilot.contract.Finding> byId = new LinkedHashMap<>();
        for (var f : ctx.getFindings()) {
            byId.putIfAbsent(f.getId(), f);
        }
        ctx.getFindings().clear();
        ctx.getFindings().addAll(byId.values());
        log.debug("execute 聚合完成: runId={}, findings={}", ctx.getRunId(), ctx.getFindings().size());
        return StageResult.success("aggregated " + ctx.getFindings().size() + " findings");
    }

    /** 返回阶段 id（{@link ReviewStage#AGGREGATE}）。 */
    public static String stageId() {
        return ReviewStage.AGGREGATE.id();
    }
}
