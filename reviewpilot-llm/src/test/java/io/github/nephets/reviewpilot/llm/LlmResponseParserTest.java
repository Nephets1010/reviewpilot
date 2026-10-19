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

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmResponseParserTest {

    @Test
    void parsesWellFormedLine() {
        List<LlmReviewComment> comments = LlmResponseParser.parse("src/A.java:12 BLOCKER: 存在硬编码密钥");
        assertThat(comments).hasSize(1);
        LlmReviewComment c = comments.get(0);
        assertThat(c.getFilePath()).isEqualTo("src/A.java");
        assertThat(c.getLine()).isEqualTo(12);
        assertThat(c.getSeverity()).isEqualTo(ReviewSeverity.BLOCKER);
        assertThat(c.getMessage()).isEqualTo("存在硬编码密钥");
        assertThat(c.getKind()).isEqualTo(LlmReviewComment.CommentKind.ISSUE);
    }

    @Test
    void parsesSuggestionAfterArrow() {
        List<LlmReviewComment> comments =
                LlmResponseParser.parse("src/A.java:5 MAJOR: 避免使用 SELECT * => 显式列出字段");
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).getMessage()).isEqualTo("避免使用 SELECT *");
        assertThat(comments.get(0).getSuggestion()).isEqualTo("显式列出字段");
    }

    @Test
    void ignoresBlankLines() {
        List<LlmReviewComment> comments =
                LlmResponseParser.parse("\n\n  \nsrc/A.java:1 MINOR: 风格问题\n\n");
        assertThat(comments).hasSize(1);
    }

    @Test
    void ignoresUnrecognizedLines() {
        List<LlmReviewComment> comments = LlmResponseParser.parse("这不是一条合法记录\n又一行乱七八糟的\n");
        assertThat(comments).isEmpty();
    }

    @Test
    void handlesFileMarkerLine() {
        // FILE: 标记行本身不产出评论，但解析不应崩溃
        List<LlmReviewComment> comments =
                LlmResponseParser.parse("FILE: src/A.java\nsrc/A.java:3 INFO: 提示");
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).getFilePath()).isEqualTo("src/A.java");
    }

    @Test
    void nullInputReturnsEmpty() {
        assertThat(LlmResponseParser.parse(null)).isEmpty();
    }

    @Test
    void blankInputReturnsEmpty() {
        assertThat(LlmResponseParser.parse("   ")).isEmpty();
    }

    @Test
    void allSeveritiesParsed() {
        String raw =
                String.join(
                        "\n",
                        "a.java:1 BLOCKER: b",
                        "a.java:2 CRITICAL: c",
                        "a.java:3 MAJOR: m",
                        "a.java:4 MINOR: n",
                        "a.java:5 INFO: i");
        List<LlmReviewComment> comments = LlmResponseParser.parse(raw);
        assertThat(comments).hasSize(5);
        assertThat(comments.stream().map(LlmReviewComment::getSeverity))
                .containsExactlyInAnyOrder(
                        ReviewSeverity.BLOCKER,
                        ReviewSeverity.CRITICAL,
                        ReviewSeverity.MAJOR,
                        ReviewSeverity.MINOR,
                        ReviewSeverity.INFO);
    }

    @Test
    void multipleLinesAggregatedInOrder() {
        String raw = String.join("\n", "a.java:10 MAJOR: x => fix", "b.java:20 MINOR: y");
        List<LlmReviewComment> comments = LlmResponseParser.parse(raw);
        assertThat(comments).hasSize(2);
        assertThat(comments.get(0).getFilePath()).isEqualTo("a.java");
        assertThat(comments.get(1).getFilePath()).isEqualTo("b.java");
    }

    @Test
    void whitespaceAroundFieldsTrimmed() {
        List<LlmReviewComment> comments =
                LlmResponseParser.parse("   src/A.java:7   MINOR :  描述   ");
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).getFilePath()).isEqualTo("src/A.java");
        assertThat(comments.get(0).getLine()).isEqualTo(7);
        assertThat(comments.get(0).getMessage()).isEqualTo("描述");
    }
}
