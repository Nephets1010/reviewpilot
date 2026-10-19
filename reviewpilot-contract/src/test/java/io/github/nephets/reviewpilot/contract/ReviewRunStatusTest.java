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

class ReviewRunStatusTest {

    @Test
    void pendingAndRunningAreNotTerminal() {
        assertThat(ReviewRunStatus.isTerminal(ReviewRunStatus.PENDING)).isFalse();
        assertThat(ReviewRunStatus.isTerminal(ReviewRunStatus.RUNNING)).isFalse();
    }

    @Test
    void terminalStatesAreTerminal() {
        assertThat(ReviewRunStatus.isTerminal(ReviewRunStatus.SUCCEEDED)).isTrue();
        assertThat(ReviewRunStatus.isTerminal(ReviewRunStatus.FAILED)).isTrue();
        assertThat(ReviewRunStatus.isTerminal(ReviewRunStatus.GATED)).isTrue();
        assertThat(ReviewRunStatus.isTerminal(ReviewRunStatus.ROLLED_BACK)).isTrue();
    }

    @Test
    void nullIsNotTerminal() {
        assertThat(ReviewRunStatus.isTerminal(null)).isFalse();
    }

    @Test
    void everyValueHasNonNullName() {
        for (ReviewRunStatus s : ReviewRunStatus.values()) {
            assertThat(s.name()).isNotEmpty();
        }
    }
}
