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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleConfigTest {

    private static Finding blocker() {
        return Finding.builder().id("b").severity(ReviewSeverity.BLOCKER).message("b").build();
    }

    private static Finding critical() {
        return Finding.builder().id("c").severity(ReviewSeverity.CRITICAL).message("c").build();
    }

    @Test
    void defaultConfigGatingEnabledWithBlockerThresholdOne() {
        RuleConfig c = RuleConfig.defaultConfig();
        assertThat(c.isGatingEnabled()).isTrue();
        assertThat(c.getBlockerThreshold()).isEqualTo(1);
        assertThat(c.getCriticalThreshold()).isEqualTo(0);
    }

    @Test
    void disabledConfigHasGatingOff() {
        assertThat(RuleConfig.disabled().isGatingEnabled()).isFalse();
    }

    @Test
    void passesWhenNoBlockerUnderDefault() {
        GateResult g =
                RuleConfig.defaultConfig()
                        .evaluate(
                                List.of(
                                        Finding.builder()
                                                .id("m")
                                                .severity(ReviewSeverity.MAJOR)
                                                .build()));
        assertThat(g.isPassed()).isTrue();
        assertThat(g.getReason()).isNull();
        assertThat(g.getBlockerCount()).isEqualTo(0);
        assertThat(g.getCriticalCount()).isEqualTo(0);
    }

    @Test
    void failsWhenBlockerReachesThreshold() {
        GateResult g = RuleConfig.defaultConfig().evaluate(List.of(blocker()));
        assertThat(g.isPassed()).isFalse();
        assertThat(g.getBlockerCount()).isEqualTo(1);
        assertThat(g.getReason()).contains("BLOCKER");
    }

    @Test
    void blockerBelowThresholdPasses() {
        RuleConfig c = RuleConfig.builder().blockerThreshold(3).build();
        GateResult g = c.evaluate(List.of(blocker(), blocker()));
        assertThat(g.isPassed()).isTrue();
    }

    @Test
    void blockerAtThresholdFails() {
        RuleConfig c = RuleConfig.builder().blockerThreshold(2).build();
        GateResult g = c.evaluate(List.of(blocker(), blocker()));
        assertThat(g.isPassed()).isFalse();
    }

    @Test
    void criticalAboveZeroThresholdFails() {
        RuleConfig c = RuleConfig.builder().criticalThreshold(2).build();
        GateResult g = c.evaluate(List.of(critical(), critical()));
        assertThat(g.isPassed()).isFalse();
        assertThat(g.getReason()).contains("CRITICAL");
        assertThat(g.getCriticalCount()).isEqualTo(2);
    }

    @Test
    void criticalBelowThresholdPasses() {
        RuleConfig c = RuleConfig.builder().criticalThreshold(2).build();
        GateResult g = c.evaluate(List.of(critical()));
        assertThat(g.isPassed()).isTrue();
    }

    @Test
    void zeroCriticalThresholdNeverBlocksOnCritical() {
        RuleConfig c = RuleConfig.builder().criticalThreshold(0).build();
        GateResult g = c.evaluate(List.of(critical(), critical()));
        assertThat(g.isPassed()).isTrue();
    }

    @Test
    void blockerTakesPrecedenceOverCriticalInReason() {
        RuleConfig c = RuleConfig.builder().blockerThreshold(1).criticalThreshold(1).build();
        GateResult g = c.evaluate(List.of(blocker(), critical()));
        assertThat(g.isPassed()).isFalse();
        assertThat(g.getReason()).contains("BLOCKER");
    }

    @Test
    void gatingDisabledAlwaysPassesEvenWithBlocker() {
        GateResult g = RuleConfig.disabled().evaluate(List.of(blocker(), blocker(), blocker()));
        assertThat(g.isPassed()).isTrue();
        assertThat(g.getBlockerCount()).isEqualTo(3);
        assertThat(g.getReason()).isNull();
    }

    @Test
    void nullFindingsTreatedAsEmpty() {
        GateResult g = RuleConfig.defaultConfig().evaluate(null);
        assertThat(g.isPassed()).isTrue();
        assertThat(g.getBlockerCount()).isEqualTo(0);
    }

    @Test
    void negativeBlockerThresholdRejected() {
        assertThatThrownBy(() -> RuleConfig.builder().blockerThreshold(-1).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void negativeCriticalThresholdRejected() {
        assertThatThrownBy(() -> RuleConfig.builder().criticalThreshold(-1).build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalsAndHashCode() {
        RuleConfig a = RuleConfig.defaultConfig();
        RuleConfig b = RuleConfig.builder().build();
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(RuleConfig.builder().gatingEnabled(false).build());
    }

    @Test
    void toStringContainsFields() {
        assertThat(RuleConfig.defaultConfig().toString()).contains("gatingEnabled");
    }
}
