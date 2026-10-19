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

import java.util.List;
import java.util.Objects;

/**
 * 规则门禁配置（不可变值对象），同时提供 {@link #evaluate} 把发现列表判定成 {@link GateResult}。
 *
 * <p><b>为什么门禁放在聚合之后、由统一配置判定：</b>门禁需要「全局视角」——单条发现可能是 INFO，但聚合后 BLOCKER 总数越过阈值就该整次失败。 把阈值与判定逻辑固化进
 * {@code RuleConfig}，引擎只负责在聚合后调用一次，避免把规则散落到各阶段。
 */
public final class RuleConfig {

    private final boolean gatingEnabled;
    private final int blockerThreshold;
    private final int criticalThreshold;

    private RuleConfig(Builder b) {
        this.gatingEnabled = b.gatingEnabled;
        this.blockerThreshold = b.blockerThreshold;
        this.criticalThreshold = b.criticalThreshold;
    }

    /** 返回默认配置：门禁开启，BLOCKER 阈值 1（存在即拦截），CRITICAL 阈值 0（不按 CRITICAL 拦截）。 */
    public static RuleConfig defaultConfig() {
        return builder().build();
    }

    /** 返回关闭门禁的配置。 */
    public static RuleConfig disabled() {
        return builder().gatingEnabled(false).build();
    }

    /** 返回构造器。 */
    public static Builder builder() {
        return new Builder();
    }

    /** 门禁是否开启。 */
    public boolean isGatingEnabled() {
        return gatingEnabled;
    }

    /** 返回 BLOCKER 阈值：聚合后 BLOCKER 数量 ≥ 该值即拦截。 */
    public int getBlockerThreshold() {
        return blockerThreshold;
    }

    /** 返回 CRITICAL 阈值：&gt; 0 时聚合后 CRITICAL 数量 ≥ 该值即拦截。 */
    public int getCriticalThreshold() {
        return criticalThreshold;
    }

    /**
     * 用本配置判定一组发现。
     *
     * @param findings 聚合后的发现列表，为 {@code null} 时按空表处理
     * @return 门禁结果；门禁关闭时永远通过
     */
    public GateResult evaluate(List<Finding> findings) {
        int blockers = 0;
        int criticals = 0;
        if (findings != null) {
            for (Finding f : findings) {
                if (f.getSeverity() == ReviewSeverity.BLOCKER) {
                    blockers++;
                } else if (f.getSeverity() == ReviewSeverity.CRITICAL) {
                    criticals++;
                }
            }
        }
        boolean passed = true;
        String reason = null;
        if (gatingEnabled) {
            if (blockers >= blockerThreshold) {
                passed = false;
                reason = "BLOCKER 数量 " + blockers + " 达到/超过阈值 " + blockerThreshold + "，评审被门禁拦截";
            } else if (criticalThreshold > 0 && criticals >= criticalThreshold) {
                passed = false;
                reason = "CRITICAL 数量 " + criticals + " 达到/超过阈值 " + criticalThreshold + "，评审被门禁拦截";
            }
        }
        return GateResult.builder()
                .passed(passed)
                .blockerCount(blockers)
                .criticalCount(criticals)
                .blockerThreshold(blockerThreshold)
                .criticalThreshold(criticalThreshold)
                .reason(reason)
                .build();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RuleConfig)) {
            return false;
        }
        RuleConfig that = (RuleConfig) other;
        return gatingEnabled == that.gatingEnabled
                && blockerThreshold == that.blockerThreshold
                && criticalThreshold == that.criticalThreshold;
    }

    @Override
    public int hashCode() {
        return Objects.hash(gatingEnabled, blockerThreshold, criticalThreshold);
    }

    @Override
    public String toString() {
        return "RuleConfig{gatingEnabled="
                + gatingEnabled
                + ", blockerThreshold="
                + blockerThreshold
                + ", criticalThreshold="
                + criticalThreshold
                + '}';
    }

    /** {@link RuleConfig} 构造器。 */
    public static final class Builder {

        private boolean gatingEnabled = true;
        private int blockerThreshold = 1;
        private int criticalThreshold = 0;

        private Builder() {}

        /** 设置门禁是否开启，默认开启。 */
        public Builder gatingEnabled(boolean gatingEnabled) {
            this.gatingEnabled = gatingEnabled;
            return this;
        }

        /** 设置 BLOCKER 阈值，必须 &gt;= 0，默认 1。 */
        public Builder blockerThreshold(int blockerThreshold) {
            this.blockerThreshold = blockerThreshold;
            return this;
        }

        /** 设置 CRITICAL 阈值，必须 &gt;= 0，默认 0（不按 CRITICAL 拦截）。 */
        public Builder criticalThreshold(int criticalThreshold) {
            this.criticalThreshold = criticalThreshold;
            return this;
        }

        /** 构造规则配置。 */
        public RuleConfig build() {
            if (blockerThreshold < 0) {
                throw new IllegalArgumentException(
                        "blockerThreshold 必须 >= 0，当前：" + blockerThreshold);
            }
            if (criticalThreshold < 0) {
                throw new IllegalArgumentException(
                        "criticalThreshold 必须 >= 0，当前：" + criticalThreshold);
            }
            Builder filled = new Builder();
            filled.gatingEnabled = gatingEnabled;
            filled.blockerThreshold = blockerThreshold;
            filled.criticalThreshold = criticalThreshold;
            return new RuleConfig(filled);
        }
    }
}
