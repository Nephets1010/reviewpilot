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
 * 阶段执行前的条件谓词。
 *
 * <p><b>为什么需要它：</b>不是所有阶段对每次提交都有意义（例如只在改动含 SQL 时才跑 SQL 审核）。 条件为假时引擎把阶段记为 {@link
 * StageStatus#SKIPPED}，不视为失败，也不阻断下游。条件在运行期对 {@link StageContext} 求值， 因此可以基于已拉取的 diff、既有发现等动态判断。
 */
@FunctionalInterface
public interface StageCondition {

    /**
     * 判断阶段是否应执行。
     *
     * @param ctx 运行上下文
     * @return {@code true} 表示执行，{@code false} 表示跳过
     */
    boolean test(StageContext ctx);
}
