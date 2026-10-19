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
package io.github.nephets.reviewpilot.llm;

import io.github.nephets.reviewpilot.contract.DiffFile;
import io.github.nephets.reviewpilot.contract.Finding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 提示词构造器：把 {@link LlmReviewRequest} 的 diff 与既有发现拼成一段确定性的评审提示词。
 *
 * <p>为什么要单独成类：提示词结构（diff 文本、既有发现摘要、输出格式约定）会随模型演进调整，
 * 把它从客户端实现里拆出来，真实适配器与测试都能复用同一套拼装逻辑，也方便对“构造出的提示词”做断言。
 */
public final class LlmPromptBuilder {

    private static final Logger log = LoggerFactory.getLogger(LlmPromptBuilder.class);

    private LlmPromptBuilder() {}

    /**
     * 由请求构造提示词。
     *
     * @param request 评审请求
     * @return 提示词文本；请求为空时返回兜底说明
     */
    public static String build(LlmReviewRequest request) {
        if (request == null) {
            log.warn("build 请求为 null，返回兜底提示词");
            return "（无评审请求）";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("你是一名资深代码评审专家。请评审以下 diff，指出缺陷、安全隐患与改进点。\n");
        if (request.getLocale() != null) {
            sb.append("请使用 ").append(request.getLocale()).append(" 作答。\n");
        }
        if (request.getModel() != null) {
            sb.append("目标模型：").append(request.getModel()).append("。\n");
        }
        List<DiffFile> files = request.getDiffFiles();
        sb.append("=== DIFF 开始（共 ").append(files.size()).append(" 个文件）===\n");
        for (DiffFile file : files) {
            sb.append("文件：")
                    .append(file.getPath())
                    .append(" (")
                    .append(file.getStatus())
                    .append(")\n");
            for (var chunk : file.getChunks()) {
                if (chunk.getHunkHeader() != null) {
                    sb.append(chunk.getHunkHeader()).append("\n");
                }
                sb.append(chunk.getContent()).append("\n");
            }
        }
        sb.append("=== DIFF 结束 ===\n");
        List<Finding> existing = request.getExistingFindings();
        if (!existing.isEmpty()) {
            sb.append("已有静态检查发现 ").append(existing.size()).append(" 条，请在此基础上补充 AI 视角：\n");
            for (Finding f : existing) {
                sb.append("- [")
                        .append(f.getSeverity())
                        .append("] ")
                        .append(f.getFilePath())
                        .append(":")
                        .append(f.getLine())
                        .append(" ")
                        .append(f.getMessage())
                        .append("\n");
            }
        }
        return sb.toString();
    }
}
