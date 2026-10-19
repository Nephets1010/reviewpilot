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

import java.util.Objects;

/**
 * 规则门禁判定结果（不可变值对象）。
 *
 * <p>由 {@link RuleConfig#evaluate} 产出，记录是否通过、各严重度计数与使用的阈值， 以及未通过时的原因说明。
 */
public final class GateResult {

    private final boolean passed;
    private final int blockerCount;
    private final int criticalCount;
    private final int blockerThreshold;
    private final int criticalThreshold;
    private final String reason;

    private GateResult(Builder b) {
        this.passed = b.passed;
        this.blockerCount = b.blockerCount;
        this.criticalCount = b.criticalCount;
        this.blockerThreshold = b.blockerThreshold;
        this.criticalThreshold = b.criticalThreshold;
        this.reason = b.reason;
    }

    /** 返回构造器。 */
    public static Builder builder() {
        return new Builder();
    }

    /** 返回门禁是否通过。 */
    public boolean isPassed() {
        return passed;
    }

    /** 返回 BLOCKER 数量。 */
    public int getBlockerCount() {
        return blockerCount;
    }

    /** 返回 CRITICAL 数量。 */
    public int getCriticalCount() {
        return criticalCount;
    }

    /** 返回 BLOCKER 阈值。 */
    public int getBlockerThreshold() {
        return blockerThreshold;
    }

    /** 返回 CRITICAL 阈值。 */
    public int getCriticalThreshold() {
        return criticalThreshold;
    }

    /** 返回未通过原因（通过时可能为空）。 */
    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof GateResult)) {
            return false;
        }
        GateResult that = (GateResult) other;
        return passed == that.passed
                && blockerCount == that.blockerCount
                && criticalCount == that.criticalCount
                && blockerThreshold == that.blockerThreshold
                && criticalThreshold == that.criticalThreshold
                && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                passed, blockerCount, criticalCount, blockerThreshold, criticalThreshold, reason);
    }

    @Override
    public String toString() {
        return "GateResult{passed="
                + passed
                + ", blockerCount="
                + blockerCount
                + ", reason="
                + reason
                + '}';
    }

    /** {@link GateResult} 构造器。 */
    public static final class Builder {

        private boolean passed = true;
        private int blockerCount;
        private int criticalCount;
        private int blockerThreshold;
        private int criticalThreshold;
        private String reason;

        private Builder() {}

        /** 设置是否通过。 */
        public Builder passed(boolean passed) {
            this.passed = passed;
            return this;
        }

        /** 设置 BLOCKER 数量。 */
        public Builder blockerCount(int blockerCount) {
            this.blockerCount = blockerCount;
            return this;
        }

        /** 设置 CRITICAL 数量。 */
        public Builder criticalCount(int criticalCount) {
            this.criticalCount = criticalCount;
            return this;
        }

        /** 设置 BLOCKER 阈值。 */
        public Builder blockerThreshold(int blockerThreshold) {
            this.blockerThreshold = blockerThreshold;
            return this;
        }

        /** 设置 CRITICAL 阈值。 */
        public Builder criticalThreshold(int criticalThreshold) {
            this.criticalThreshold = criticalThreshold;
            return this;
        }

        /** 设置原因说明。 */
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        /** 构造门禁结果。 */
        public GateResult build() {
            Builder filled = new Builder();
            filled.passed = passed;
            filled.blockerCount = blockerCount;
            filled.criticalCount = criticalCount;
            filled.blockerThreshold = blockerThreshold;
            filled.criticalThreshold = criticalThreshold;
            filled.reason = reason;
            return new GateResult(filled);
        }
    }
}
