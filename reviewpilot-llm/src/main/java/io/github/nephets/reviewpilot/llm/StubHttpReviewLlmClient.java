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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 真实 HTTP LLM 适配器的<b>桩</b>（STUB）。
 *
 * <p><b>为什么它默认不接线、也不联网：</b>任务要求「测试零联网」。这个桩只占位，说明真实适配器应实现 {@link ReviewLlmClient}、 用 {@link
 * LlmPromptBuilder} 拼请求、用 {@link LlmResponseParser} 解析响应，并把底层异常翻译为 {@link LlmClientException}。
 * 它没有被任何自动装配注册，调用 {@link #review} 会直接抛错，避免误用导致测试联网。
 */
public final class StubHttpReviewLlmClient implements ReviewLlmClient {

    private static final Logger log = LoggerFactory.getLogger(StubHttpReviewLlmClient.class);

    private final String baseUrl;

    /** 使用占位 baseUrl 构造。 */
    public StubHttpReviewLlmClient() {
        this("http://localhost:11434");
    }

    /**
     * 使用指定 baseUrl 构造。
     *
     * @param baseUrl 真实端点（仅占位，本桩不发起请求）
     */
    public StubHttpReviewLlmClient(String baseUrl) {
        this.baseUrl = baseUrl == null ? "http://localhost:11434" : baseUrl;
    }

    @Override
    public String providerId() {
        return "stub-http";
    }

    @Override
    public boolean healthy() {
        // 桩未实现真实探活，标记为不健康，避免被误当作可用实现。
        return false;
    }

    @Override
    public LlmReviewResponse review(LlmReviewRequest request) {
        log.error("review 真实 HTTP 适配器尚未实现（STUB），baseUrl={}", baseUrl);
        throw new LlmClientException("真实 HTTP LLM 适配器未接线：这是一个占位 STUB，baseUrl=" + baseUrl);
    }
}
