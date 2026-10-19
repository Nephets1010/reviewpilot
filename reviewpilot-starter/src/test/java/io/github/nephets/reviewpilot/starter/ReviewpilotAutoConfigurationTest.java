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
import io.github.nephets.reviewpilot.core.ReviewPipeline;
import io.github.nephets.reviewpilot.core.ReviewRegistry;
import io.github.nephets.reviewpilot.core.ReviewRunStore;
import io.github.nephets.reviewpilot.core.ReviewWorkflowEngine;
import io.github.nephets.reviewpilot.llm.ReviewLlmClient;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewpilotAutoConfigurationTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ReviewpilotAutoConfiguration.class));

    private final WebApplicationContextRunner webRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ReviewpilotAutoConfiguration.class));

    @Test
    void registersCoreBeans() {
        runner.run(
                context -> {
                    assertThat(context).hasSingleBean(ReviewRunStore.class);
                    assertThat(context).hasSingleBean(ReviewRegistry.class);
                    assertThat(context).hasSingleBean(ReviewLlmClient.class);
                    assertThat(context).hasSingleBean(ReviewPipeline.class);
                    assertThat(context).hasSingleBean(ReviewWorkflowEngine.class);
                    assertThat(context).hasSingleBean(ReviewpilotProperties.class);
                });
    }

    @Test
    void registersWebBeansInServletContext() {
        webRunner.run(
                context -> {
                    assertThat(context).hasSingleBean(ReviewpilotTenantFilter.class);
                    assertThat(context).hasSingleBean(ReviewController.class);
                    assertThat(context).hasSingleBean(ReviewWorkflowEngine.class);
                });
    }

    @Test
    void respectsCustomBeans() {
        ReviewWorkflowEngine custom = new ReviewWorkflowEngine(new InMemoryReviewRunStore());
        runner.withBean(ReviewWorkflowEngine.class, () -> custom)
                .run(
                        context ->
                                assertThat(context)
                                        .getBean(ReviewWorkflowEngine.class)
                                        .isSameAs(custom));
    }

    @Test
    void defaultPipelineIdIsDefaultReview() {
        runner.run(
                context -> {
                    ReviewPipeline pipeline = context.getBean(ReviewPipeline.class);
                    assertThat(pipeline.getPipelineId()).isEqualTo("default-review");
                });
    }
}
