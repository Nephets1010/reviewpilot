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

/**
 * 发现的严重度，从高到低：BLOCKER &gt; CRITICAL &gt; MAJOR &gt; MINOR &gt; INFO。
 *
 * <p>门禁按严重度计数，严重度越高越可能导致整次评审被门禁拦截。
 */
public enum ReviewSeverity {

    /** 必须修复，否则阻断合并。 */
    BLOCKER(5),

    /** 严重问题，应当修复。 */
    CRITICAL(4),

    /** 一般问题。 */
    MAJOR(3),

    /** 轻微问题。 */
    MINOR(2),

    /** 提示性信息。 */
    INFO(1);

    private final int weight;

    ReviewSeverity(int weight) {
        this.weight = weight;
    }

    /**
     * 返回严重度权重，数值越大越严重。
     *
     * @return 权重
     */
    public int weight() {
        return weight;
    }

    /**
     * 判断本严重度是否不低于给定严重度（即“至少这么严重”）。
     *
     * @param other 参照严重度
     * @return 本严重度权重 &gt;= other 权重时返回 {@code true}
     */
    public boolean atLeast(ReviewSeverity other) {
        return other != null && this.weight >= other.weight;
    }
}
