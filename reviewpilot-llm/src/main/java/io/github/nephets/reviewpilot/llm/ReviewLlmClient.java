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
package io.github.nephets.reviewpilot.llm;

import io.github.nephets.reviewpilot.contract.DiffFile;
import io.github.nephets.reviewpilot.contract.Finding;

import java.util.List;

/**
 * 评审用 LLM 客户端（SPI）。
 *
 * <p>设计上刻意保持最小面：一次非流式调用，输入 {@link LlmReviewRequest}，输出 {@link LlmReviewResponse}（归一化后的评论与发现）。 默认实现
 * {@link MockReviewLlmClient} 是确定性、离线的；真实 HTTP 适配器见 {@link StubHttpReviewLlmClient}（仅 STUB，默认不接线）。
 */
public interface ReviewLlmClient {

    /**
     * 供应商标识，与配置中的 providerId 对应。
     *
     * @return 供应商标识
     */
    String providerId();

    /**
     * 发起一次评审调用。
     *
     * <p>实现约定：不得把底层 HTTP/传输异常原样抛出，必须经翻译后抛出 {@link LlmClientException}。
     *
     * @param request 统一请求（含 diff 与既有发现）
     * @return 统一响应（评论与发现均已归一化）
     * @throws LlmClientException 调用失败
     */
    LlmReviewResponse review(LlmReviewRequest request);

    /** 是否健康（能否连通）。供健康检查与诊断使用。 */
    default boolean healthy() {
        return true;
    }

    /** 适配器名，写入日志便于定位是谁在处理。 */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * 由 diff 列表与既有发现快速构造请求。
     *
     * @param diffFiles 待评审 diff
     * @param existingFindings 既有发现（静态检查产出），可为空
     * @return 请求
     */
    static LlmReviewRequest request(List<DiffFile> diffFiles, List<Finding> existingFindings) {
        return LlmReviewRequest.builder()
                .diffFiles(diffFiles)
                .existingFindings(existingFindings)
                .build();
    }
}
