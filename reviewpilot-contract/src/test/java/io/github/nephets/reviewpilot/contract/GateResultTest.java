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

class GateResultTest {

    @Test
    void defaultPassedTrue() {
        GateResult r = GateResult.builder().build();
        assertThat(r.isPassed()).isTrue();
        assertThat(r.getBlockerCount()).isEqualTo(0);
        assertThat(r.getCriticalCount()).isEqualTo(0);
        assertThat(r.getReason()).isNull();
    }

    @Test
    void retainsAllFields() {
        GateResult r =
                GateResult.builder()
                        .passed(false)
                        .blockerCount(2)
                        .criticalCount(1)
                        .blockerThreshold(1)
                        .criticalThreshold(3)
                        .reason("too many blockers")
                        .build();
        assertThat(r.isPassed()).isFalse();
        assertThat(r.getBlockerCount()).isEqualTo(2);
        assertThat(r.getCriticalCount()).isEqualTo(1);
        assertThat(r.getBlockerThreshold()).isEqualTo(1);
        assertThat(r.getCriticalThreshold()).isEqualTo(3);
        assertThat(r.getReason()).isEqualTo("too many blockers");
    }

    @Test
    void equalsAndHashCode() {
        GateResult a = GateResult.builder().passed(true).blockerCount(1).build();
        GateResult b = GateResult.builder().passed(true).blockerCount(1).build();
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(GateResult.builder().passed(false).blockerCount(1).build());
    }
}
