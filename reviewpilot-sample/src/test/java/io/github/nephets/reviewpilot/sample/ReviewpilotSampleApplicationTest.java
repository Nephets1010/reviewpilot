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

import io.github.nephets.reviewpilot.core.ReviewPipeline;
import io.github.nephets.reviewpilot.core.ReviewWorkflowEngine;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReviewpilotSampleApplicationTest {

    @Autowired private ReviewWorkflowEngine engine;

    @Autowired private ReviewPipeline pipeline;

    @Test
    void contextLoads() {
        assertThat(engine).isNotNull();
        assertThat(pipeline).isNotNull();
        assertThat(pipeline.getPipelineId()).isEqualTo("default-review");
    }

    @Test
    void demoTenantHasRunsAfterStartup() {
        // CommandLineRunner 已在启动时跑过两条路径，acme 租户下应有运行实例
        assertThat(engine.findAll(ReviewpilotSampleApplication.TENANT)).isNotEmpty();
    }
}
