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

import io.github.nephets.reviewpilot.contract.ReviewRunStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 内存版 {@link ReviewRunStore}：按 “租户 id + 运行 id” 存进有界 {@link LinkedHashMap}。
 *
 * <p>保存的是实例引用本身，引擎就地修改后不需要再次 put 也能查到最新状态；进程重启即丢失，用于测试与单机场景。 超出容量上限时按 LRU 淘汰最旧条目。
 */
public final class InMemoryReviewRunStore implements ReviewRunStore {

    private static final Logger log = LoggerFactory.getLogger(InMemoryReviewRunStore.class);

    private static final String KEY_SEPARATOR = "::";

    /** 默认容量上限：超出时淘汰最旧条目。 */
    private static final int DEFAULT_MAX_CAPACITY = 1_000_000;

    private final int maxCapacity;

    private final Map<String, ReviewRunInstance> instances;

    /** 使用默认容量上限构造。 */
    public InMemoryReviewRunStore() {
        this(DEFAULT_MAX_CAPACITY);
    }

    /**
     * 使用指定容量上限构造。
     *
     * @param maxCapacity 容量上限，必须 &gt;= 1
     * @throws IllegalArgumentException 当 {@code maxCapacity} &lt; 1 时
     */
    public InMemoryReviewRunStore(int maxCapacity) {
        if (maxCapacity < 1) {
            throw new IllegalArgumentException("maxCapacity 必须 >= 1，当前：" + maxCapacity);
        }
        this.maxCapacity = maxCapacity;
        this.instances =
                new LinkedHashMap<>(16, 0.75f, true) {
                    @Override
                    protected boolean removeEldestEntry(
                            Map.Entry<String, ReviewRunInstance> eldest) {
                        return size() > InMemoryReviewRunStore.this.maxCapacity;
                    }
                };
    }

    @Override
    public synchronized void save(ReviewRunInstance instance) {
        String k = key(instance.getTenantId(), instance.getRunId().asString());
        boolean evicting = !instances.containsKey(k) && instances.size() >= maxCapacity;
        if (evicting) {
            log.info(
                    "save 已达到容量上限，将按 LRU 淘汰最旧条目: tenantId={}, maxCapacity={}",
                    instance.getTenantId(),
                    maxCapacity);
        }
        instances.put(k, Objects.requireNonNull(instance, "instance"));
    }

    @Override
    public synchronized Optional<ReviewRunInstance> findById(String tenantId, String runId) {
        if (tenantId == null || runId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(instances.get(key(tenantId, runId)));
    }

    @Override
    public synchronized List<ReviewRunInstance> findByStatus(
            String tenantId, ReviewRunStatus status) {
        List<ReviewRunInstance> result = new ArrayList<>();
        if (status == null) {
            return result;
        }
        for (ReviewRunInstance instance : ofTenant(tenantId)) {
            if (status == instance.getStatus()) {
                result.add(instance);
            }
        }
        return result;
    }

    @Override
    public synchronized List<ReviewRunInstance> findAll(String tenantId) {
        return new ArrayList<>(ofTenant(tenantId));
    }

    private List<ReviewRunInstance> ofTenant(String tenantId) {
        List<ReviewRunInstance> result = new ArrayList<>();
        if (tenantId == null) {
            return result;
        }
        for (ReviewRunInstance instance : instances.values()) {
            if (tenantId.equals(instance.getTenantId())) {
                result.add(instance);
            }
        }
        return result;
    }

    private static String key(String tenantId, String runId) {
        return tenantId + KEY_SEPARATOR + runId;
    }
}
