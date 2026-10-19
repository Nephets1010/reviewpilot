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

import io.github.nephets.reviewpilot.contract.CodeChunk;
import io.github.nephets.reviewpilot.contract.DiffFile;
import io.github.nephets.reviewpilot.contract.ReviewStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * 内置阶段执行器：拉取待评审 diff。
 *
 * <p>优先使用请求中的内联 diff（便于测试注入确定性内容），未提供时回退到一份确定性「干净」脚本化 diff， 保证离线、可断言。产出的 diff 写入上下文供后续静态检查与 AI
 * 评审消费。
 */
public final class FetchDiffExecutor implements StageExecutor {

    private static final Logger log = LoggerFactory.getLogger(FetchDiffExecutor.class);

    private final List<DiffFile> fallbackDiff;

    /** 使用默认「干净」脚本化 diff 构造。 */
    public FetchDiffExecutor() {
        this(defaultScriptedDiff());
    }

    /**
     * 使用指定兜底 diff 构造。
     *
     * @param fallbackDiff 兜底 diff，为 {@code null} 或空时退化为空列表
     */
    public FetchDiffExecutor(List<DiffFile> fallbackDiff) {
        this.fallbackDiff = fallbackDiff == null ? List.of() : List.copyOf(fallbackDiff);
    }

    @Override
    public StageResult execute(StageContext ctx) {
        List<DiffFile> diffs =
                ctx.getRequest().hasInlineDiff() ? ctx.getRequest().getDiffFiles() : fallbackDiff;
        ctx.setDiffFiles(diffs);
        log.debug("execute 拉取 diff: runId={}, fileCount={}", ctx.getRunId(), diffs.size());
        return StageResult.success("fetched " + diffs.size() + " files");
    }

    /** 返回阶段 id（{@link ReviewStage#FETCH_DIFF}）。 */
    public static String stageId() {
        return ReviewStage.FETCH_DIFF.id();
    }

    static List<DiffFile> defaultScriptedDiff() {
        DiffFile file =
                DiffFile.builder()
                        .path("src/main/java/io/github/nephets/demo/Demo.java")
                        .status(DiffFile.FileStatus.ADDED)
                        .language("java")
                        .addChunk(
                                CodeChunk.builder()
                                        .filePath("src/main/java/io/github/nephets/demo/Demo.java")
                                        .startLine(10)
                                        .endLine(14)
                                        .content(
                                                "public int add(int a, int b) {\n"
                                                        + "    // a clean helper\n"
                                                        + "    return a + b;\n"
                                                        + "}")
                                        .build())
                        .build();
        return List.of(file);
    }

    /** 供测试构造确定性 diff 的工具（避免重复样板）。 */
    static DiffFile scriptedChunk(String path, int startLine, String content) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(content, "content");
        int endLine = startLine + content.split("\\R", -1).length - 1;
        return DiffFile.builder()
                .path(path)
                .status(DiffFile.FileStatus.MODIFIED)
                .language("java")
                .addChunk(
                        CodeChunk.builder()
                                .filePath(path)
                                .startLine(startLine)
                                .endLine(endLine)
                                .content(content)
                                .build())
                .build();
    }
}
