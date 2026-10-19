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
package io.github.nephets.reviewpilot.contract;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LlmReviewCommentTest {

    @Test
    void defaultsToMajorSuggestion() {
        LlmReviewComment c = LlmReviewComment.builder().message("look").build();
        assertThat(c.getSeverity()).isEqualTo(ReviewSeverity.MAJOR);
        assertThat(c.getKind()).isEqualTo(LlmReviewComment.CommentKind.SUGGESTION);
        assertThat(c.getLine()).isEqualTo(0);
        assertThat(c.getId()).isNotBlank();
    }

    @Test
    void nullSeverityFallsBack() {
        assertThat(LlmReviewComment.builder().severity(null).build().getSeverity())
                .isEqualTo(ReviewSeverity.MAJOR);
    }

    @Test
    void nullKindFallsBackToSuggestion() {
        assertThat(LlmReviewComment.builder().kind(null).build().getKind())
                .isEqualTo(LlmReviewComment.CommentKind.SUGGESTION);
    }

    @Test
    void nullMessageBecomesEmpty() {
        assertThat(LlmReviewComment.builder().message(null).build().getMessage()).isEqualTo("");
    }

    @Test
    void allFieldsSet() {
        LlmReviewComment c =
                LlmReviewComment.builder()
                        .id("c-1")
                        .filePath("A.java")
                        .line(7)
                        .severity(ReviewSeverity.CRITICAL)
                        .message("bug")
                        .suggestion("fix it")
                        .model("mock-1")
                        .kind(LlmReviewComment.CommentKind.ISSUE)
                        .raw("raw text")
                        .build();
        assertThat(c.getId()).isEqualTo("c-1");
        assertThat(c.getFilePath()).isEqualTo("A.java");
        assertThat(c.getLine()).isEqualTo(7);
        assertThat(c.getSeverity()).isEqualTo(ReviewSeverity.CRITICAL);
        assertThat(c.getMessage()).isEqualTo("bug");
        assertThat(c.getSuggestion()).isEqualTo("fix it");
        assertThat(c.getModel()).isEqualTo("mock-1");
        assertThat(c.getKind()).isEqualTo(LlmReviewComment.CommentKind.ISSUE);
        assertThat(c.getRaw()).isEqualTo("raw text");
    }

    @Test
    void fourKindsPresent() {
        assertThat(LlmReviewComment.CommentKind.values())
                .containsExactlyInAnyOrder(
                        LlmReviewComment.CommentKind.ISSUE,
                        LlmReviewComment.CommentKind.SUGGESTION,
                        LlmReviewComment.CommentKind.NIT,
                        LlmReviewComment.CommentKind.PRAISE);
    }

    @Test
    void equalsAndHashCode() {
        LlmReviewComment a =
                LlmReviewComment.builder()
                        .id("c-1")
                        .message("x")
                        .kind(LlmReviewComment.CommentKind.NIT)
                        .build();
        LlmReviewComment b =
                LlmReviewComment.builder()
                        .id("c-1")
                        .message("x")
                        .kind(LlmReviewComment.CommentKind.NIT)
                        .build();
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
