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
package io.github.nephets.reviewpilot.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StageStatusTest {

    @Test
    void continueableAreSucceededAndSkipped() {
        assertThat(StageStatus.isContinueable(StageStatus.SUCCEEDED)).isTrue();
        assertThat(StageStatus.isContinueable(StageStatus.SKIPPED)).isTrue();
        assertThat(StageStatus.isContinueable(StageStatus.FAILED)).isFalse();
        assertThat(StageStatus.isContinueable(StageStatus.PENDING)).isFalse();
        assertThat(StageStatus.isContinueable(StageStatus.RUNNING)).isFalse();
        assertThat(StageStatus.isContinueable(StageStatus.BLOCKED)).isFalse();
    }

    @Test
    void terminalStates() {
        assertThat(StageStatus.isTerminal(StageStatus.SUCCEEDED)).isTrue();
        assertThat(StageStatus.isTerminal(StageStatus.FAILED)).isTrue();
        assertThat(StageStatus.isTerminal(StageStatus.SKIPPED)).isTrue();
        assertThat(StageStatus.isTerminal(StageStatus.BLOCKED)).isTrue();
        assertThat(StageStatus.isTerminal(StageStatus.PENDING)).isFalse();
        assertThat(StageStatus.isTerminal(StageStatus.RUNNING)).isFalse();
    }

    @Test
    void nullIsNotTerminalAndNotContinueable() {
        assertThat(StageStatus.isTerminal(null)).isFalse();
        assertThat(StageStatus.isContinueable(null)).isFalse();
    }
}
