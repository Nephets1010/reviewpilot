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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaticCheckResultTest {

    @Test
    void buildsWithDefaultsPassedTrue() {
        StaticCheckResult r = StaticCheckResult.builder().toolName("checkstyle").build();
        assertThat(r.getToolName()).isEqualTo("checkstyle");
        assertThat(r.isPassed()).isTrue();
        assertThat(r.getFindings()).isEmpty();
        assertThat(r.getFilePath()).isNull();
    }

    @Test
    void retainsFindingsAndSummary() {
        Finding f = Finding.builder().id("f-1").message("x").build();
        StaticCheckResult r =
                StaticCheckResult.builder()
                        .toolName("pmd")
                        .filePath("A.java")
                        .addFinding(f)
                        .passed(false)
                        .summary("2 issues")
                        .build();
        assertThat(r.getFindings()).containsExactly(f);
        assertThat(r.isPassed()).isFalse();
        assertThat(r.getFilePath()).isEqualTo("A.java");
        assertThat(r.getSummary()).isEqualTo("2 issues");
    }

    @Test
    void findingsAreImmutable() {
        java.util.List<Finding> src = new java.util.ArrayList<>();
        src.add(Finding.builder().id("f-1").message("x").build());
        StaticCheckResult r = StaticCheckResult.builder().toolName("t").findings(src).build();
        src.add(Finding.builder().id("f-2").message("y").build());
        assertThat(r.getFindings()).hasSize(1);
    }

    @Test
    void nullFindingsRejected() {
        assertThatThrownBy(() -> StaticCheckResult.builder().toolName("t").findings(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullFindingInAddRejected() {
        assertThatThrownBy(() -> StaticCheckResult.builder().toolName("t").addFinding(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finding");
    }

    @Test
    void blankToolNameRejected() {
        assertThatThrownBy(() -> StaticCheckResult.builder().toolName(" ").build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalsAndHashCode() {
        StaticCheckResult a = StaticCheckResult.builder().toolName("t").build();
        StaticCheckResult b = StaticCheckResult.builder().toolName("t").build();
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
