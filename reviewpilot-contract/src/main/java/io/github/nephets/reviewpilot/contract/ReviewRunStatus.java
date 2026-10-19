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
 * 一次评审运行（ReviewRunInstance）的整体状态。
 *
 * <p><b>为什么 {@link #GATED} 与 {@link #ROLLED_BACK} 也是终态却仍可被显式 resume：</b>{@link #isTerminal} 回答的是
 * “引擎还会不会自发推进这个运行”，而 resume 是显式人工指令。门禁失败（GATED）与回退（ROLLED_BACK）都表示自动路径到此为止， 但允许人工重新裁决或重跑。
 */
public enum ReviewRunStatus {

    /** 已创建，尚未开始执行。 */
    PENDING,

    /** 正在推进阶段。 */
    RUNNING,

    /** 所有阶段执行成功，且规则门禁通过。 */
    SUCCEEDED,

    /** 有阶段执行失败，或依赖失败导致下游被跳过。 */
    FAILED,

    /** 所有阶段完成，但规则门禁未通过（例如 BLOCKER 数量越界）。 */
    GATED,

    /** 已按快照回退到某个历史版本。 */
    ROLLED_BACK;

    /**
     * 判断状态是否为终态（不会再被状态机自发改写）。
     *
     * @param status 待判断的状态，允许为 {@code null}
     * @return 终态返回 {@code true}；{@code null} 返回 {@code false}
     */
    public static boolean isTerminal(ReviewRunStatus status) {
        if (status == null) {
            return false;
        }
        return status == SUCCEEDED || status == FAILED || status == GATED || status == ROLLED_BACK;
    }
}
