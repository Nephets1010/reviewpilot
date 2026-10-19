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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewReportTest {

    @Test
    void defaultStatusPendingAndEpoch() {
        ReviewReport r = ReviewReport.builder().runId(ReviewRunId.of("r1")).build();
        assertThat(r.getStatus()).isEqualTo(ReviewRunStatus.PENDING);
        assertThat(r.getGeneratedAt()).isEqualTo(Instant.EPOCH);
        assertThat(r.getFindings()).isEmpty();
    }

    @Test
    void retainsAllFields() {
        ReviewRunId id = ReviewRunId.of("r1");
        Finding f = Finding.builder().id("f").severity(ReviewSeverity.BLOCKER).build();
        GateResult g = GateResult.builder().passed(false).blockerCount(1).build();
        Instant now = Instant.now();
        ReviewReport r =
                ReviewReport.builder()
                        .runId(id)
                        .tenantId("acme")
                        .pipelineId("p1")
                        .status(ReviewRunStatus.GATED)
                        .generatedAt(now)
                        .addFinding(f)
                        .gate(g)
                        .summary("needs work")
                        .build();
        assertThat(r.getRunId()).isEqualTo(id);
        assertThat(r.getTenantId()).isEqualTo("acme");
        assertThat(r.getPipelineId()).isEqualTo("p1");
        assertThat(r.getStatus()).isEqualTo(ReviewRunStatus.GATED);
        assertThat(r.getGeneratedAt()).isEqualTo(now);
        assertThat(r.getFindings()).containsExactly(f);
        assertThat(r.getGate()).isEqualTo(g);
        assertThat(r.getSummary()).isEqualTo("needs work");
    }

    @Test
    void totalFindingsAndCountBySeverity() {
        ReviewReport r =
                ReviewReport.builder()
                        .runId(ReviewRunId.of("r1"))
                        .addFinding(
                                Finding.builder().id("a").severity(ReviewSeverity.BLOCKER).build())
                        .addFinding(
                                Finding.builder().id("b").severity(ReviewSeverity.BLOCKER).build())
                        .addFinding(
                                Finding.builder().id("c").severity(ReviewSeverity.MAJOR).build())
                        .build();
        assertThat(r.totalFindings()).isEqualTo(3);
        assertThat(r.countBySeverity(ReviewSeverity.BLOCKER)).isEqualTo(2);
        assertThat(r.countBySeverity(ReviewSeverity.MAJOR)).isEqualTo(1);
        assertThat(r.countBySeverity(ReviewSeverity.INFO)).isEqualTo(0);
    }

    @Test
    void hasBlockerTrueAndFalse() {
        ReviewReport with =
                ReviewReport.builder()
                        .runId(ReviewRunId.of("r1"))
                        .addFinding(
                                Finding.builder().id("a").severity(ReviewSeverity.BLOCKER).build())
                        .build();
        ReviewReport without =
                ReviewReport.builder()
                        .runId(ReviewRunId.of("r1"))
                        .addFinding(
                                Finding.builder().id("a").severity(ReviewSeverity.MINOR).build())
                        .build();
        assertThat(with.hasBlocker()).isTrue();
        assertThat(without.hasBlocker()).isFalse();
    }

    @Test
    void nullSeverityInCountIsSafe() {
        assertThatThrownBy(
                        () ->
                                ReviewReport.builder()
                                        .runId(ReviewRunId.of("r1"))
                                        .build()
                                        .countBySeverity(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullRunIdRejected() {
        assertThatThrownBy(() -> ReviewReport.builder().build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("runId");
    }

    @Test
    void findingsImmutable() {
        java.util.List<Finding> src = new java.util.ArrayList<>();
        src.add(Finding.builder().id("a").build());
        ReviewReport r = ReviewReport.builder().runId(ReviewRunId.of("r1")).findings(src).build();
        src.add(Finding.builder().id("b").build());
        assertThat(r.getFindings()).hasSize(1);
    }

    @Test
    void equalsAndHashCode() {
        ReviewRunId id = ReviewRunId.of("r1");
        ReviewReport a = ReviewReport.builder().runId(id).build();
        ReviewReport b = ReviewReport.builder().runId(id).build();
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}
