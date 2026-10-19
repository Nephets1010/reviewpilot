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
package io.github.nephets.reviewpilot.starter;

import io.github.nephets.reviewpilot.core.InMemoryReviewRunStore;
import io.github.nephets.reviewpilot.core.ReviewPipelines;
import io.github.nephets.reviewpilot.core.ReviewRegistry;
import io.github.nephets.reviewpilot.core.ReviewWorkflowEngine;
import io.github.nephets.reviewpilot.llm.MockReviewLlmClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ReviewpilotProperties properties = new ReviewpilotProperties();
        ReviewRegistry registry = new ReviewRegistry();
        registry.register(ReviewPipelines.defaultPipeline(new MockReviewLlmClient()));
        ReviewWorkflowEngine engine =
                new ReviewWorkflowEngine(
                        new InMemoryReviewRunStore(), registry, Clock.systemUTC(), 50);
        ReviewController controller = new ReviewController(engine, registry, properties);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void startDefaultPipelineReturnsRun() throws Exception {
        mockMvc.perform(post("/reviewpilot/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.runId").exists())
                .andExpect(jsonPath("$.snapshotVersions").isArray());
    }

    @Test
    void startWithExplicitPipelineId() throws Exception {
        mockMvc.perform(
                        post("/reviewpilot/runs")
                                .contentType("application/json")
                                .content("{\"pipelineId\":\"default-review\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pipelineId").value("default-review"));
    }

    @Test
    void startUnknownPipelineReturns404() throws Exception {
        mockMvc.perform(
                        post("/reviewpilot/runs")
                                .contentType("application/json")
                                .content("{\"pipelineId\":\"nope\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getMissingRunReturns404() throws Exception {
        mockMvc.perform(get("/reviewpilot/runs/does-not-exist")).andExpect(status().isNotFound());
    }

    @Test
    void fullLifecycleStartThenGetThenRollback() throws Exception {
        String runId =
                mockMvc.perform(post("/reviewpilot/runs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
                        .replaceAll(".*\"runId\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(get("/reviewpilot/runs/" + runId)).andExpect(status().isOk());
        mockMvc.perform(post("/reviewpilot/runs/" + runId + "/rollback").param("version", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void rollbackMissingRunReturns404() throws Exception {
        mockMvc.perform(post("/reviewpilot/runs/ghost/rollback").param("version", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rollbackInvalidVersionReturns400() throws Exception {
        String runId =
                mockMvc.perform(post("/reviewpilot/runs"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
                        .replaceAll(".*\"runId\":\"([^\"]+)\".*", "$1");
        // 使用一个极大的版本号触发 IllegalArgumentException
        mockMvc.perform(post("/reviewpilot/runs/" + runId + "/rollback").param("version", "9999"))
                .andExpect(status().isBadRequest());
    }
}
