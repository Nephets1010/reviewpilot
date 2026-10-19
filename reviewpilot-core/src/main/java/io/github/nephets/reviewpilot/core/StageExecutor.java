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
 * 阶段执行器（SPI）。
 *
 * <p>每个阶段的具体行为由实现决定：读取 {@link StageContext} 里的 diff / 前置发现，向上下文累加本阶段的产物（发现、评论、检查结果）。
 * 引擎负责调度、重试与落存储，不关心阶段内部逻辑。
 */
@FunctionalInterface
public interface StageExecutor {

    /**
     * 执行阶段。
     *
     * @param ctx 运行上下文（含 diff、累计发现与共享输出）
     * @return 执行结果；返回 {@code null} 时引擎按失败处理
     * @throws RuntimeException 任意异常都会走重试 / 失败路径
     */
    StageResult execute(StageContext ctx);
}
