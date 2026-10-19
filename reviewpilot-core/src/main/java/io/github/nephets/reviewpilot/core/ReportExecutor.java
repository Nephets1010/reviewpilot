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

import io.github.nephets.reviewpilot.contract.ReviewReport;
import io.github.nephets.reviewpilot.contract.ReviewRunStatus;
import io.github.nephets.reviewpilot.contract.ReviewSeverity;
import io.github.nephets.reviewpilot.contract.ReviewStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 内置阶段执行器：汇总成最终 {@link ReviewReport} 并写入上下文。
 *
 * <p>报告状态依据门禁结果（聚合阶段之后由引擎写入上下文）决定：门禁未通过则为 {@link ReviewRunStatus#GATED}，否则 {@link
 * ReviewRunStatus#SUCCEEDED}。摘要由严重度计数拼成，便于直接在接口/日志中阅读。
 */
public final class ReportExecutor implements StageExecutor {

    private static final Logger log = LoggerFactory.getLogger(ReportExecutor.class);

    @Override
    public StageResult execute(StageContext ctx) {
        ReviewRunStatus status =
                (ctx.getGateResult() != null && !ctx.getGateResult().isPassed())
                        ? ReviewRunStatus.GATED
                        : ReviewRunStatus.SUCCEEDED;
        long blockers =
                ctx.getFindings().stream()
                        .filter(f -> f.getSeverity() == ReviewSeverity.BLOCKER)
                        .count();
        long criticals =
                ctx.getFindings().stream()
                        .filter(f -> f.getSeverity() == ReviewSeverity.CRITICAL)
                        .count();
        String summary =
                String.format(
                        "发现 %d 条，其中 BLOCKER %d / CRITICAL %d；门禁%s。",
                        ctx.getFindings().size(),
                        blockers,
                        criticals,
                        status == ReviewRunStatus.GATED ? "未通过" : "通过");
        ReviewReport report =
                ReviewReport.builder()
                        .runId(ctx.getRunId())
                        .tenantId(ctx.getTenantId())
                        .pipelineId(ctx.getPipelineId())
                        .status(status)
                        .generatedAt(ctx.now())
                        .findings(ctx.getFindings())
                        .gate(ctx.getGateResult())
                        .summary(summary)
                        .build();
        ctx.setReport(report);
        log.debug("execute 报告生成: runId={}, status={}", ctx.getRunId(), status);
        return StageResult.success("report status=" + status);
    }

    /** 返回阶段 id（{@link ReviewStage#REPORT}）。 */
    public static String stageId() {
        return ReviewStage.REPORT.id();
    }
}
