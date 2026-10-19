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
 * 发现的来源：静态检查（STATIC）或 AI 评审（AI）。
 *
 * <p>聚合阶段把两个来源的发现合并成一个列表，报告再按来源分组展示。
 */
public enum FindingSource {

    /** 静态检查工具产出。 */
    STATIC,

    /** AI 评审产出。 */
    AI
}
