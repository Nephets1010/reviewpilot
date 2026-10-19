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

class FindingTest {

    @Test
    void defaultSeverityIsMajorAndStatic() {
        Finding f = Finding.builder().message("x").build();
        assertThat(f.getSeverity()).isEqualTo(ReviewSeverity.MAJOR);
        assertThat(f.getSource()).isEqualTo(FindingSource.STATIC);
        assertThat(f.getCategory()).isEqualTo(FindingCategory.OTHER);
        assertThat(f.getLine()).isEqualTo(0);
        assertThat(f.getId()).isNotBlank();
    }

    @Test
    void nullSeverityFallsBackToMajor() {
        assertThat(Finding.builder().severity(null).build().getSeverity())
                .isEqualTo(ReviewSeverity.MAJOR);
    }

    @Test
    void nullSourceFallsBackToStatic() {
        assertThat(Finding.builder().source(null).build().getSource())
                .isEqualTo(FindingSource.STATIC);
    }

    @Test
    void nullCategoryFallsBackToOther() {
        assertThat(Finding.builder().category(null).build().getCategory())
                .isEqualTo(FindingCategory.OTHER);
    }

    @Test
    void nullMessageBecomesEmpty() {
        assertThat(Finding.builder().message(null).build().getMessage()).isEqualTo("");
    }

    @Test
    void explicitIdIsKept() {
        Finding f = Finding.builder().id("f-1").message("x").build();
        assertThat(f.getId()).isEqualTo("f-1");
    }

    @Test
    void allAccessorsReturnSetValues() {
        Finding f =
                Finding.builder()
                        .id("f-9")
                        .severity(ReviewSeverity.BLOCKER)
                        .category(FindingCategory.SECURITY)
                        .source(FindingSource.AI)
                        .filePath("A.java")
                        .line(42)
                        .message("secret")
                        .ruleId("R1")
                        .suggestion("fix")
                        .code("String p = 1;")
                        .build();
        assertThat(f.getId()).isEqualTo("f-9");
        assertThat(f.getSeverity()).isEqualTo(ReviewSeverity.BLOCKER);
        assertThat(f.getCategory()).isEqualTo(FindingCategory.SECURITY);
        assertThat(f.getSource()).isEqualTo(FindingSource.AI);
        assertThat(f.getFilePath()).isEqualTo("A.java");
        assertThat(f.getLine()).isEqualTo(42);
        assertThat(f.getMessage()).isEqualTo("secret");
        assertThat(f.getRuleId()).isEqualTo("R1");
        assertThat(f.getSuggestion()).isEqualTo("fix");
        assertThat(f.getCode()).isEqualTo("String p = 1;");
    }

    @Test
    void equalsAndHashCode() {
        Finding a = Finding.builder().id("f-1").severity(ReviewSeverity.MAJOR).message("x").build();
        Finding b = Finding.builder().id("f-1").severity(ReviewSeverity.MAJOR).message("x").build();
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(Finding.builder().id("f-2").message("x").build());
    }

    @Test
    void notEqualToNullAndOtherType() {
        Finding f = Finding.builder().message("x").build();
        assertThat(f).isNotEqualTo(null);
        assertThat(f).isNotEqualTo("x");
    }
}
