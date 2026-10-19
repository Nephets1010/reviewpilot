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
package io.github.nephets.reviewpilot.sample;

import io.github.nephets.reviewpilot.contract.CodeChunk;
import io.github.nephets.reviewpilot.contract.DiffFile;
import io.github.nephets.reviewpilot.core.ReviewPipeline;
import io.github.nephets.reviewpilot.core.ReviewRequest;
import io.github.nephets.reviewpilot.core.ReviewRunInstance;
import io.github.nephets.reviewpilot.core.ReviewTenant;
import io.github.nephets.reviewpilot.core.ReviewWorkflowEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * 示例应用：用离线 Mock 客户端端到端跑通默认五阶段流水线，并暴露 {@code /reviewpilot/runs} 接口。
 *
 * <p>演示两条路径：一份干净的 diff 正常跑完；一份含硬编码密码的 diff 触发安全 BLOCKER，被门禁拦截。
 */
@SpringBootApplication
public class ReviewpilotSampleApplication {

    private static final Logger log = LoggerFactory.getLogger(ReviewpilotSampleApplication.class);

    /** 演示用的租户。 */
    public static final String TENANT = "acme";

    /**
     * 入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ReviewpilotSampleApplication.class, args);
    }

    /**
     * 启动后演示两条路径：干净 diff 跑通、危险 diff 被门禁拦截。
     *
     * @param engine 评审工作流引擎
     * @param pipeline 默认流水线
     * @return 启动后执行的演示任务
     */
    @Bean
    public CommandLineRunner reviewpilotDemo(ReviewWorkflowEngine engine, ReviewPipeline pipeline) {
        return args -> {
            log.info("reviewpilotDemo 启动示例演示: tenant={}", TENANT);
            ReviewTenant.runWith(
                    TENANT,
                    () -> {
                        cleanRun(engine, pipeline);
                        gatedRun(engine, pipeline);
                    });
            log.info("=== 示例结束，接口见 POST /reviewpilot/runs ===");
        };
    }

    private static void cleanRun(ReviewWorkflowEngine engine, ReviewPipeline pipeline) {
        ReviewRequest request = ReviewRequest.scripted("CleanDemo");
        String runId = engine.start(pipeline, TENANT, request);
        ReviewRunInstance instance = engine.find(TENANT, runId).orElseThrow();
        log.info(
                "[1] 干净 diff 跑通：status={}，发现数={}，阶段={}",
                instance.getStatus(),
                instance.getFindings().size(),
                stageSummary(instance));
    }

    private static void gatedRun(ReviewWorkflowEngine engine, ReviewPipeline pipeline) {
        DiffFile diff =
                DiffFile.builder()
                        .path("src/main/java/io/github/nephets/demo/Secret.java")
                        .status(DiffFile.FileStatus.ADDED)
                        .language("java")
                        .addChunk(
                                CodeChunk.builder()
                                        .filePath(
                                                "src/main/java/io/github/nephets/demo/Secret.java")
                                        .startLine(1)
                                        .endLine(1)
                                        .content("String password = \"hunter2\";")
                                        .build())
                        .build();
        ReviewRequest request =
                ReviewRequest.builder().repoRef("demo").diffFiles(List.of(diff)).build();
        String runId = engine.start(pipeline, TENANT, request);
        ReviewRunInstance instance = engine.find(TENANT, runId).orElseThrow();
        log.info(
                "[2] 危险 diff 被门禁：status={}，gated={}，reason={}，发现数={}",
                instance.getStatus(),
                instance.isGated(),
                instance.getGateReason(),
                instance.getFindings().size());
    }

    private static String stageSummary(ReviewRunInstance instance) {
        StringBuilder sb = new StringBuilder();
        instance.getStageStatuses()
                .forEach(
                        (id, status) ->
                                sb.append(id).append('=').append(status.name()).append(' '));
        return sb.toString().trim();
    }
}
