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

/**
 * 单个阶段在一次运行中的状态。状态机只做「记录新状态 + 落存储」，没有回退单步状态的入口，整体回退一律走快照版本链。
 *
 * <p><b>为什么 SKIPPED 不算失败：</b>条件阶段（conditional stage）在谓词为假时跳过，是预期行为，不应让整次运行失败，也不应阻断下游阶段。因此 SKIPPED 与
 * SUCCEEDED 一样是可继续的终态。
 */
public enum StageStatus {

    /** 已创建但尚未开始。 */
    PENDING,

    /** 正在执行。 */
    RUNNING,

    /** 执行成功。 */
    SUCCEEDED,

    /** 执行失败（含重试预算耗尽）。 */
    FAILED,

    /** 条件为假被跳过（预期行为，非失败）。 */
    SKIPPED,

    /** 依赖的前置阶段失败，本阶段无法执行。 */
    BLOCKED;

    /**
     * 判断状态是否「可继续」（SUCCEEDED 或 SKIPPED），下游阶段可据此决定是否能推进。
     *
     * @param status 状态，允许 {@code null}
     * @return 可继续返回 {@code true}
     */
    public static boolean isContinueable(StageStatus status) {
        return status == SUCCEEDED || status == SKIPPED;
    }

    /**
     * 判断状态是否为终态（不会再被状态机改写）。
     *
     * @param status 状态，允许 {@code null}
     * @return 终态返回 {@code true}
     */
    public static boolean isTerminal(StageStatus status) {
        if (status == null) {
            return false;
        }
        return status == SUCCEEDED || status == FAILED || status == SKIPPED || status == BLOCKED;
    }
}
