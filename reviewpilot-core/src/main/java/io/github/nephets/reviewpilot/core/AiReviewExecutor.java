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

import io.github.nephets.reviewpilot.contract.ReviewStage;
import io.github.nephets.reviewpilot.llm.LlmReviewRequest;
import io.github.nephets.reviewpilot.llm.LlmReviewResponse;
import io.github.nephets.reviewpilot.llm.ReviewLlmClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * 内置阶段执行器：调用 {@link ReviewLlmClient} 做 AI 评审。
 *
 * <p>把上下文里的 diff 与既有发现（静态检查已产出）包装成 {@link LlmReviewRequest} 交给客户端， 再把响应中的评论与发现并入上下文，供聚合与门禁使用。默认接入
 * {@code MockReviewLlmClient}（确定性、离线）， 真实 HTTP 适配器由调用方自行替换。
 */
public final class AiReviewExecutor implements StageExecutor {

    private static final Logger log = LoggerFactory.getLogger(AiReviewExecutor.class);

    private final ReviewLlmClient client;

    /**
     * 用 LLM 客户端构造。
     *
     * @param client 评审客户端，不可为 {@code null}
     */
    public AiReviewExecutor(ReviewLlmClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public StageResult execute(StageContext ctx) {
        LlmReviewRequest request =
                LlmReviewRequest.builder()
                        .diffFiles(ctx.getDiffFiles())
                        .existingFindings(ctx.getFindings())
                        .build();
        LlmReviewResponse response = client.review(request);
        response.getFindings().forEach(ctx::addFinding);
        response.getComments().forEach(ctx::addLlmComment);
        log.debug(
                "execute AI 评审完成: runId={}, comments={}, findings={}",
                ctx.getRunId(),
                response.getComments().size(),
                response.getFindings().size());
        return StageResult.success("ai produced " + response.getComments().size() + " comments");
    }

    /** 返回阶段 id（{@link ReviewStage#AI_REVIEW}）。 */
    public static String stageId() {
        return ReviewStage.AI_REVIEW.id();
    }
}
