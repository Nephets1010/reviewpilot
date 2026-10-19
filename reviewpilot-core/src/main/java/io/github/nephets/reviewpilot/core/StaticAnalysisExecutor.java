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

import io.github.nephets.reviewpilot.contract.DiffFile;
import io.github.nephets.reviewpilot.contract.Finding;
import io.github.nephets.reviewpilot.contract.FindingCategory;
import io.github.nephets.reviewpilot.contract.FindingSource;
import io.github.nephets.reviewpilot.contract.ReviewSeverity;
import io.github.nephets.reviewpilot.contract.ReviewStage;
import io.github.nephets.reviewpilot.contract.StaticCheckResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 内置阶段执行器：确定性静态检查。
 *
 * <p>按一组固定规则扫描 diff 内容产出发现（来源 {@link FindingSource#STATIC}）：行过长、（宽泛）捕获异常、TODO 待办等。 与 {@code
 * MockReviewLlmClient} 一样刻意保持确定性，便于测试断言，不依赖任何外部工具。
 */
public final class StaticAnalysisExecutor implements StageExecutor {

    private static final Logger log = LoggerFactory.getLogger(StaticAnalysisExecutor.class);

    private static final int MAX_LINE_LENGTH = 120;

    @Override
    public StageResult execute(StageContext ctx) {
        List<Finding> findings = new ArrayList<>();
        for (DiffFile file : ctx.getDiffFiles()) {
            for (var chunk : file.getChunks()) {
                String[] lines = chunk.getContent().split("\\R", -1);
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    int lineNo = chunk.getStartLine() + i;
                    if (line.length() > MAX_LINE_LENGTH) {
                        findings.add(
                                Finding.builder()
                                        .severity(ReviewSeverity.MAJOR)
                                        .category(FindingCategory.STYLE)
                                        .source(FindingSource.STATIC)
                                        .filePath(chunk.getFilePath())
                                        .line(lineNo)
                                        .message(
                                                "行长度 " + line.length() + " 超过上限 " + MAX_LINE_LENGTH)
                                        .ruleId("static.line-too-long")
                                        .code(line.strip())
                                        .build());
                    }
                    String lower = line.toLowerCase();
                    if (lower.contains("catch")
                            && lower.contains("exception")
                            && !lower.contains("(")) {
                        findings.add(
                                Finding.builder()
                                        .severity(ReviewSeverity.MINOR)
                                        .category(FindingCategory.MAINTAINABILITY)
                                        .source(FindingSource.STATIC)
                                        .filePath(chunk.getFilePath())
                                        .line(lineNo)
                                        .message("建议捕获具体异常而非宽泛 Exception")
                                        .ruleId("static.broad-catch")
                                        .code(line.strip())
                                        .build());
                    }
                    if (lower.contains("todo") || lower.contains("fixme")) {
                        findings.add(
                                Finding.builder()
                                        .severity(ReviewSeverity.MINOR)
                                        .category(FindingCategory.MAINTAINABILITY)
                                        .source(FindingSource.STATIC)
                                        .filePath(chunk.getFilePath())
                                        .line(lineNo)
                                        .message("存在 TODO/FIXME 待办，建议跟踪处理")
                                        .ruleId("static.todo")
                                        .code(line.strip())
                                        .build());
                    }
                }
            }
        }
        StaticCheckResult result =
                StaticCheckResult.builder()
                        .toolName("reviewpilot-static")
                        .passed(
                                findings.stream()
                                        .noneMatch(
                                                f ->
                                                        f.getSeverity().weight()
                                                                >= ReviewSeverity.CRITICAL
                                                                        .weight()))
                        .findings(findings)
                        .summary("静态检查完成，发现 " + findings.size() + " 条")
                        .build();
        ctx.addStaticResult(result);
        findings.forEach(ctx::addFinding);
        log.debug("execute 静态检查完成: runId={}, findings={}", ctx.getRunId(), findings.size());
        return StageResult.success("static found " + findings.size());
    }

    /** 返回阶段 id（{@link ReviewStage#STATIC_ANALYSIS}）。 */
    public static String stageId() {
        return ReviewStage.STATIC_ANALYSIS.id();
    }
}
