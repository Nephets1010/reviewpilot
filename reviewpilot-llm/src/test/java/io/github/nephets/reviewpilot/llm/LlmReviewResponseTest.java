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

import io.github.nephets.reviewpilot.contract.Finding;
import io.github.nephets.reviewpilot.contract.LlmReviewComment;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmReviewResponseTest {

    private static LlmReviewComment comment(String id) {
        return LlmReviewComment.builder().id(id).filePath("A.java").line(1).message("m").build();
    }

    @Test
    void builderSetsAllFields() {
        Finding f = Finding.builder().message("m").build();
        LlmReviewResponse r =
                LlmReviewResponse.builder()
                        .addComment(comment("c1"))
                        .addFinding(f)
                        .rawText("raw")
                        .model("m1")
                        .build();
        assertThat(r.getComments()).hasSize(1);
        assertThat(r.getFindings()).hasSize(1);
        assertThat(r.getRawText()).isEqualTo("raw");
        assertThat(r.getModel()).isEqualTo("m1");
    }

    @Test
    void listsAreImmutableCopies() {
        LlmReviewResponse.Builder b =
                LlmReviewResponse.builder()
                        .addComment(comment("c1"))
                        .addFinding(Finding.builder().build());
        LlmReviewResponse r = b.build();
        assertThat(r.getComments()).isNotEmpty();
        assertThat(r.getFindings()).isNotEmpty();
        assertThatThrownBy(() -> r.getComments().add(comment("c2")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void commentsSetterOverrides() {
        LlmReviewResponse r =
                LlmReviewResponse.builder()
                        .addComment(comment("c1"))
                        .comments(List.of(comment("c2")))
                        .build();
        assertThat(r.getComments()).hasSize(1);
        assertThat(r.getComments().get(0).getId()).isEqualTo("c2");
    }

    @Test
    void findingsSetterOverrides() {
        LlmReviewResponse r =
                LlmReviewResponse.builder()
                        .addFinding(Finding.builder().build())
                        .findings(List.of(Finding.builder().message("only").build()))
                        .build();
        assertThat(r.getFindings()).hasSize(1);
        assertThat(r.getFindings().get(0).getMessage()).isEqualTo("only");
    }

    @Test
    void addCommentRejectsNull() {
        assertThatThrownBy(() -> LlmReviewResponse.builder().addComment(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void commentsRejectsNull() {
        assertThatThrownBy(() -> LlmReviewResponse.builder().comments(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void addFindingRejectsNull() {
        assertThatThrownBy(() -> LlmReviewResponse.builder().addFinding(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findingsRejectsNull() {
        assertThatThrownBy(() -> LlmReviewResponse.builder().findings(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toStringMentionsCounts() {
        LlmReviewResponse r =
                LlmReviewResponse.builder()
                        .addComment(comment("c1"))
                        .addFinding(Finding.builder().build())
                        .model("m1")
                        .build();
        assertThat(r.toString()).contains("comments=1");
        assertThat(r.toString()).contains("findings=1");
    }
}
