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

import io.github.nephets.reviewpilot.contract.ReviewRunStatus;
import io.github.nephets.reviewpilot.core.ReviewPipeline;
import io.github.nephets.reviewpilot.core.ReviewRegistry;
import io.github.nephets.reviewpilot.core.ReviewRequest;
import io.github.nephets.reviewpilot.core.ReviewRunInstance;
import io.github.nephets.reviewpilot.core.ReviewWorkflowEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * reviewpilot 的运行接口：启动、查询与回退。
 *
 * <p>租户 id 取自 {@link ReviewpilotTenantFilter} 按请求头写入的上下文，缺失时回退到 {@code
 * reviewpilot.default-tenant-id}。
 */
@RestController
@RequestMapping("/reviewpilot/runs")
public class ReviewController {

    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewWorkflowEngine engine;
    private final ReviewRegistry registry;
    private final ReviewpilotProperties properties;

    /**
     * 构造控制器。
     *
     * @param engine 评审工作流引擎
     * @param registry 流水线定义注册表
     * @param properties 配置属性
     */
    public ReviewController(
            ReviewWorkflowEngine engine,
            ReviewRegistry registry,
            ReviewpilotProperties properties) {
        this.engine = engine;
        this.registry = registry;
        this.properties = properties;
    }

    /**
     * 启动一次运行。请求体：{@code {"pipelineId": "...", "repoRef": "...", "headRef": "..."}}； 省略 pipelineId
     * 时使用默认流水线。
     *
     * @param body 请求体
     * @return 运行视图；流水线未注册返回 404
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> start(
            @RequestBody(required = false) Map<String, Object> body) {
        String pipelineId = text(body, "pipelineId");
        String tenantId = tenant();
        log.info("start 收到启动请求: pipelineId={}, tenantId={}", pipelineId, tenantId);
        ReviewPipeline pipeline;
        if (pipelineId == null || pipelineId.isBlank()) {
            pipeline = registry.find("default-review").orElse(null);
            if (pipeline == null) {
                log.warn("start 默认流水线未注册");
                return notFound("默认流水线未注册");
            }
        } else {
            pipeline = registry.find(pipelineId.trim()).orElse(null);
            if (pipeline == null) {
                log.warn("start 流水线未注册: pipelineId={}", pipelineId);
                return notFound("流水线未注册：" + pipelineId);
            }
        }
        ReviewRequest request = toRequest(body);
        String runId = engine.start(pipeline, tenantId, request);
        log.info(
                "start 运行已启动: tenantId={}, pipelineId={}, runId={}",
                tenantId,
                pipeline.getPipelineId(),
                runId);
        return ok(describe(engine.find(tenantId, runId).orElseThrow()));
    }

    /**
     * 查询一次运行的状态、发现与快照版本。
     *
     * @param runId 运行 id
     * @return 运行视图；不存在返回 404
     */
    @GetMapping("/{runId}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String runId) {
        String tenantId = tenant();
        log.info("get 查询运行: tenantId={}, runId={}", tenantId, runId);
        Optional<ReviewRunInstance> found = engine.find(tenantId, runId);
        if (found.isEmpty()) {
            log.warn("get 运行不存在: tenantId={}, runId={}", tenantId, runId);
            return notFound("运行不存在：" + runId);
        }
        return ok(describe(found.orElseThrow()));
    }

    /**
     * 回退到指定快照版本。
     *
     * @param runId 运行 id
     * @param version 快照版本号
     * @return 运行视图
     */
    @PostMapping("/{runId}/rollback")
    public ResponseEntity<Map<String, Object>> rollback(
            @PathVariable String runId, @RequestParam("version") int version) {
        String tenantId = tenant();
        log.info("rollback 收到回退请求: tenantId={}, runId={}, version={}", tenantId, runId, version);
        if (engine.find(tenantId, runId).isEmpty()) {
            log.warn("rollback 运行不存在: tenantId={}, runId={}", tenantId, runId);
            return notFound("运行不存在：" + runId);
        }
        try {
            engine.rollbackTo(tenantId, runId, version);
        } catch (IllegalArgumentException e) {
            log.warn(
                    "rollback 参数非法: runId={}, version={}, message={}",
                    runId,
                    version,
                    e.getMessage());
            return badRequest(e.getMessage());
        }
        return ok(describe(engine.find(tenantId, runId).orElseThrow()));
    }

    private Map<String, Object> describe(ReviewRunInstance instance) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("runId", instance.getRunId().asString());
        view.put("tenantId", instance.getTenantId());
        view.put("pipelineId", instance.getPipelineId());
        view.put("pipelineVersion", instance.getPipelineVersion());
        view.put("status", instance.getStatus().name());
        view.put("gated", instance.isGated());
        view.put("gateReason", instance.getGateReason());
        view.put("stageStatuses", new LinkedHashMap<>(instance.getStageStatuses()));
        view.put("findingCount", instance.getFindings().size());
        view.put("snapshotVersions", snapshotVersions(instance));
        if (instance.getStatus() == ReviewRunStatus.FAILED) {
            view.put("error", instance.getError());
        }
        if (instance.getReport() != null) {
            view.put("report", describeReport(instance));
        } else {
            view.put("report", null);
        }
        return view;
    }

    private Map<String, Object> describeReport(ReviewRunInstance instance) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("status", instance.getReport().getStatus().name());
        report.put("findingCount", instance.getReport().totalFindings());
        report.put("hasBlocker", instance.getReport().hasBlocker());
        report.put("summary", instance.getReport().getSummary());
        return report;
    }

    private static List<Integer> snapshotVersions(ReviewRunInstance instance) {
        List<Integer> versions = new ArrayList<>();
        instance.getSnapshots().forEach(s -> versions.add(s.getVersion()));
        return versions;
    }

    private ReviewRequest toRequest(Map<String, Object> body) {
        if (body == null) {
            return ReviewRequest.scripted("demo");
        }
        ReviewRequest.Builder builder = ReviewRequest.builder();
        Object repoRef = body.get("repoRef");
        if (repoRef != null) {
            builder.repoRef(String.valueOf(repoRef));
        }
        Object baseRef = body.get("baseRef");
        if (baseRef != null) {
            builder.baseRef(String.valueOf(baseRef));
        }
        Object headRef = body.get("headRef");
        if (headRef != null) {
            builder.headRef(String.valueOf(headRef));
        }
        return builder.build();
    }

    private String tenant() {
        String tenantId =
                ReviewpilotProperties.resolveTenantId(
                        io.github.nephets.reviewpilot.core.ReviewTenant.current(), properties);
        log.debug("tenant 解析租户: tenantId={}", tenantId);
        return tenantId;
    }

    private static String text(Map<String, Object> body, String key) {
        if (body == null) {
            return null;
        }
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static ResponseEntity<Map<String, Object>> ok(Map<String, Object> body) {
        return ResponseEntity.ok(body);
    }

    private static ResponseEntity<Map<String, Object>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    private static ResponseEntity<Map<String, Object>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", message));
    }
}
