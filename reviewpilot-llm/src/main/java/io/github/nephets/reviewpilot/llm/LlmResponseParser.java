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

import io.github.nephets.reviewpilot.contract.LlmReviewComment;
import io.github.nephets.reviewpilot.contract.ReviewSeverity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LLM 原始响应的解析器（用于真实 HTTP 适配器，Mock 不依赖它）。
 *
 * <p>约定输入为若干行，每行形如：{@code 路径:行号 严重度: 说明 [=> 建议]}。 无法识别的行被忽略，保证“脏数据”不会让解析整体崩溃。
 */
public final class LlmResponseParser {

    private static final Logger log = LoggerFactory.getLogger(LlmResponseParser.class);

    private static final Pattern LINE_PATTERN =
            Pattern.compile(
                    "^(?<path>[^:\\s][^:]*):(?<line>\\d+)\\s+(?<sev>BLOCKER|CRITICAL|MAJOR|MINOR|INFO)\\s*:\\s*(?<msg>.*)$");

    private LlmResponseParser() {}

    /**
     * 解析原始文本为评论列表。
     *
     * @param raw 模型原始输出，为 {@code null} 或空时返回空列表
     * @return 评论列表（不会为 {@code null}）
     */
    public static List<LlmReviewComment> parse(String raw) {
        List<LlmReviewComment> comments = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return comments;
        }
        String currentFile = null;
        for (String rawLine : raw.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher m = LINE_PATTERN.matcher(line);
            if (m.matches()) {
                String path = m.group("path").trim();
                int ln = Integer.parseInt(m.group("line"));
                ReviewSeverity sev = ReviewSeverity.valueOf(m.group("sev"));
                String msgPart = m.group("msg");
                String message = msgPart;
                String suggestion = null;
                int arrow = msgPart.indexOf("=>");
                if (arrow >= 0) {
                    message = msgPart.substring(0, arrow).trim();
                    suggestion = msgPart.substring(arrow + 2).trim();
                }
                comments.add(
                        LlmReviewComment.builder()
                                .filePath(path)
                                .line(ln)
                                .severity(sev)
                                .message(message)
                                .suggestion(suggestion)
                                .kind(LlmReviewComment.CommentKind.ISSUE)
                                .build());
            } else if (line.startsWith("FILE:")) {
                currentFile = line.substring("FILE:".length()).trim();
            } else {
                log.debug("parse 跳过无法识别的行: {}", line);
            }
        }
        return comments;
    }
}
