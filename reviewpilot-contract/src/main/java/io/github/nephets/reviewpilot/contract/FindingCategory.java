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
 * 发现的归类。
 *
 * <p>归类用于报告分组与统计；门禁与阶段逻辑只依赖 {@link ReviewSeverity} 与来源， 不依赖具体归类，因此新增归类不需要改引擎。
 */
public enum FindingCategory {

    /** 功能性缺陷 / 潜在 bug。 */
    BUG,

    /** 安全漏洞或风险。 */
    SECURITY,

    /** 性能问题。 */
    PERFORMANCE,

    /** 代码风格。 */
    STYLE,

    /** 可维护性。 */
    MAINTAINABILITY,

    /** 测试相关。 */
    TESTING,

    /** 文档 / 注释。 */
    DOCUMENTATION,

    /** 其它。 */
    OTHER
}
