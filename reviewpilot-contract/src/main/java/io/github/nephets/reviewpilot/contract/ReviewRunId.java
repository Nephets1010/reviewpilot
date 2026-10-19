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
package io.github.nephets.reviewpilot.contract;

import java.util.Objects;
import java.util.UUID;

/**
 * 评审运行标识，包装一个不可为空的运行 id 字符串。
 *
 * <p>作为值对象，只做校验与展示，不含业务行为。空串 / 全空白会被拒绝；构造时会去掉首尾空白。
 */
public final class ReviewRunId {

    private static final String PREFIX = "run-";

    private final String value;

    private ReviewRunId(String value) {
        this.value = value;
    }

    /**
     * 用给定字符串构造运行标识。
     *
     * @param value 运行 id，不可为空或全空白
     * @return 运行标识
     * @throws IllegalArgumentException value 为空或全空白
     */
    public static ReviewRunId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ReviewRunId 不可为空");
        }
        return new ReviewRunId(value.trim());
    }

    /**
     * 生成一个全新的运行标识（带 {@code run-} 前缀的 UUID）。
     *
     * @return 运行标识
     */
    public static ReviewRunId generate() {
        return new ReviewRunId(PREFIX + UUID.randomUUID());
    }

    /** 返回底层字符串。 */
    public String asString() {
        return value;
    }

    /** 返回底层字符串（同 {@link #asString()}）。 */
    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ReviewRunId)) {
            return false;
        }
        return value.equals(((ReviewRunId) other).value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
