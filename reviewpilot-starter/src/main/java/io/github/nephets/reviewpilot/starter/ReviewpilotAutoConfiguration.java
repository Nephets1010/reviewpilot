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
import io.github.nephets.reviewpilot.llm.MockReviewLlmClient;
import io.github.nephets.reviewpilot.llm.ReviewLlmClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * reviewpilot 自动装配：注册引擎、存储、注册表、默认 Mock LLM 客户端与默认流水线。
 *
 * <p>所有 Bean 都带 {@code @ConditionalOnMissingBean}，应用可以用自己的实现整体替换。 Web 相关 Bean（租户过滤器与运行接口）只在 Servlet
 * Web 应用下装配。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ReviewpilotProperties.class)
public class ReviewpilotAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ReviewpilotAutoConfiguration.class);

    /**
     * 运行实例存储。
     *
     * @return 内存实现
     */
    @Bean
    @ConditionalOnMissingBean(ReviewRunStore.class)
    public ReviewRunStore reviewpilotRunStore() {
        log.info("reviewpilotRunStore 装配内存运行存储");
        return new InMemoryReviewRunStore();
    }

    /**
     * 流水线定义注册表。
     *
     * @return 空注册表，应用自行注册流水线
     */
    @Bean
    @ConditionalOnMissingBean(ReviewRegistry.class)
    public ReviewRegistry reviewpilotRegistry() {
        log.info("reviewpilotRegistry 装配流水线注册表");
        return new ReviewRegistry();
    }

    /**
     * 大模型客户端（默认离线 Mock，不联网）。
     *
     * @return Mock 实现
     */
    @Bean
    @ConditionalOnMissingBean(ReviewLlmClient.class)
    public ReviewLlmClient reviewpilotLlmClient() {
        log.info("reviewpilotLlmClient 装配离线 Mock 客户端（providerId=mock）");
        return new MockReviewLlmClient();
    }

    /**
     * 默认五阶段流水线（依赖上面的 Mock 客户端）。
     *
     * @param client 大模型客户端
     * @return 默认流水线
     */
    @Bean
    @ConditionalOnMissingBean(name = "reviewpilotDefaultPipeline")
    public ReviewPipeline reviewpilotDefaultPipeline(ReviewLlmClient client) {
        log.info("reviewpilotDefaultPipeline 装配默认评审流水线");
        return io.github.nephets.reviewpilot.core.ReviewPipelines.defaultPipeline(client);
    }

    /**
     * 工作流引擎。
     *
     * @param store 运行实例存储
     * @param registry 流水线定义注册表
     * @param properties 配置属性，提供快照保留上限
     * @return 引擎
     */
    @Bean
    @ConditionalOnMissingBean(ReviewWorkflowEngine.class)
    public ReviewWorkflowEngine reviewpilotEngine(
            ReviewRunStore store, ReviewRegistry registry, ReviewpilotProperties properties) {
        log.info("reviewpilotEngine 装配评审工作流引擎: snapshotLimit={}", properties.getSnapshotLimit());
        return new ReviewWorkflowEngine(
                store, registry, Clock.systemUTC(), properties.getSnapshotLimit());
    }

    /** Web 环境下的 Bean：租户过滤器与 {@code /reviewpilot/runs} 接口。 */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public static class ReviewpilotWebConfiguration {

        private static final Logger log =
                LoggerFactory.getLogger(ReviewpilotWebConfiguration.class);

        /**
         * 按请求头写入租户上下文的过滤器。
         *
         * @param properties 配置属性
         * @return 过滤器
         */
        @Bean
        @ConditionalOnMissingBean(ReviewpilotTenantFilter.class)
        public ReviewpilotTenantFilter reviewpilotTenantFilter(ReviewpilotProperties properties) {
            log.info("reviewpilotTenantFilter 装配租户过滤器");
            return new ReviewpilotTenantFilter(properties);
        }

        /**
         * 运行接口控制器。
         *
         * @param engine 评审工作流引擎
         * @param registry 流水线定义注册表
         * @param properties 配置属性
         * @return 控制器
         */
        @Bean
        @ConditionalOnMissingBean(ReviewController.class)
        public ReviewController reviewpilotController(
                ReviewWorkflowEngine engine,
                ReviewRegistry registry,
                ReviewpilotProperties properties) {
            log.info("reviewpilotController 装配运行接口控制器");
            return new ReviewController(engine, registry, properties);
        }
    }
}
