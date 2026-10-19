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

class ReviewSeverityTest {

    @Test
    void weightIsMonotonicFromBlockerToInfo() {
        assertThat(ReviewSeverity.BLOCKER.weight()).isGreaterThan(ReviewSeverity.CRITICAL.weight());
        assertThat(ReviewSeverity.CRITICAL.weight()).isGreaterThan(ReviewSeverity.MAJOR.weight());
        assertThat(ReviewSeverity.MAJOR.weight()).isGreaterThan(ReviewSeverity.MINOR.weight());
        assertThat(ReviewSeverity.MINOR.weight()).isGreaterThan(ReviewSeverity.INFO.weight());
    }

    @Test
    void blockerAtLeastEverything() {
        assertThat(ReviewSeverity.BLOCKER.atLeast(ReviewSeverity.BLOCKER)).isTrue();
        assertThat(ReviewSeverity.BLOCKER.atLeast(ReviewSeverity.CRITICAL)).isTrue();
        assertThat(ReviewSeverity.BLOCKER.atLeast(ReviewSeverity.INFO)).isTrue();
    }

    @Test
    void infoAtLeastNothingExceptInfo() {
        assertThat(ReviewSeverity.INFO.atLeast(ReviewSeverity.INFO)).isTrue();
        assertThat(ReviewSeverity.INFO.atLeast(ReviewSeverity.MINOR)).isFalse();
        assertThat(ReviewSeverity.INFO.atLeast(ReviewSeverity.BLOCKER)).isFalse();
    }

    @Test
    void atLeastNullIsFalse() {
        assertThat(ReviewSeverity.MAJOR.atLeast(null)).isFalse();
    }

    @Test
    void majorAtLeastMajorButNotBlocker() {
        assertThat(ReviewSeverity.MAJOR.atLeast(ReviewSeverity.MAJOR)).isTrue();
        assertThat(ReviewSeverity.MAJOR.atLeast(ReviewSeverity.CRITICAL)).isFalse();
        assertThat(ReviewSeverity.MAJOR.atLeast(ReviewSeverity.MINOR)).isTrue();
    }

    @Test
    void criticalAtLeastMajorAndMinor() {
        assertThat(ReviewSeverity.CRITICAL.atLeast(ReviewSeverity.MAJOR)).isTrue();
        assertThat(ReviewSeverity.CRITICAL.atLeast(ReviewSeverity.MINOR)).isTrue();
    }

    @Test
    void valuesContainAllFive() {
        assertThat(ReviewSeverity.values())
                .containsExactlyInAnyOrder(
                        ReviewSeverity.BLOCKER,
                        ReviewSeverity.CRITICAL,
                        ReviewSeverity.MAJOR,
                        ReviewSeverity.MINOR,
                        ReviewSeverity.INFO);
    }
}
