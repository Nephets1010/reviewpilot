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

import java.util.Optional;

/**
 * 多租户上下文（轻量，仅一个贯穿引擎的租户 id）。
 *
 * <p>引擎的主要方法都要求显式传入 tenantId（store-first 隔离的硬性契约）， 此类仅作为“调用方不方便逐层传参时”的便利：在 {@link #runWith} 块内可用
 * {@link #current()} 取到当前租户。默认租户为 {@value #DEFAULT_TENANT_ID}。
 */
public final class ReviewTenant {

    /** 默认租户 id。 */
    public static final String DEFAULT_TENANT_ID = "default";

    private static final ThreadLocal<String> CURRENT =
            ThreadLocal.withInitial(() -> DEFAULT_TENANT_ID);

    private ReviewTenant() {}

    /** 返回当前线程的租户 id（不会为 {@code null}）。 */
    public static String current() {
        return CURRENT.get();
    }

    /**
     * 直接设置当前线程的租户（空白处回退默认租户）。
     *
     * <p>与 {@link #runWith} 的区别：不负责还原旧值，调用方需自行在 {@code finally} 中 {@link #clear()}。 Web
     * 过滤器逐请求设置/清除时用它最省事。
     *
     * @param tenantId 租户 id
     */
    public static void set(String tenantId) {
        CURRENT.set(resolve(tenantId));
    }

    /**
     * 在块内设置当前租户，结束后还原。
     *
     * @param tenantId 租户 id，为 {@code null}/空白时按默认租户
     * @param action 要执行的动作
     */
    public static void runWith(String tenantId, Runnable action) {
        String previous = CURRENT.get();
        CURRENT.set(resolve(tenantId));
        try {
            action.run();
        } finally {
            CURRENT.set(previous);
        }
    }

    /**
     * 解析租户 id，空白/空值回退默认租户。
     *
     * @param tenantId 候选租户 id
     * @return 非空租户 id
     */
    public static String resolve(String tenantId) {
        return (tenantId == null || tenantId.isBlank()) ? DEFAULT_TENANT_ID : tenantId.trim();
    }

    /** 清除当前线程的租户（一般测试用）。 */
    public static void clear() {
        CURRENT.remove();
    }

    /** 当前是否处于默认租户。 */
    public static boolean isDefault() {
        return DEFAULT_TENANT_ID.equals(CURRENT.get());
    }

    /** 仅供测试断言当前线程租户。 */
    static Optional<String> peek() {
        return Optional.ofNullable(CURRENT.get());
    }
}
