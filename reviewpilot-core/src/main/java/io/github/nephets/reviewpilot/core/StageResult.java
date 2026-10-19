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

/**
 * 阶段执行结果：状态 + 说明文字。
 *
 * <p>阶段执行器返回后，引擎依据状态推进：{@code SUCCEEDED} / {@code SKIPPED} 视为成功继续； {@code FAILED} 触发重试（预算内）或最终失败。
 */
public final class StageResult {

    private final StageStatus status;
    private final String message;

    private StageResult(StageStatus status, String message) {
        this.status = status;
        this.message = message == null ? "" : message;
    }

    /** 成功结果。 */
    public static StageResult success() {
        return new StageResult(StageStatus.SUCCEEDED, "");
    }

    /** 成功结果（带说明）。 */
    public static StageResult success(String message) {
        return new StageResult(StageStatus.SUCCEEDED, message);
    }

    /** 失败结果。 */
    public static StageResult failure(String message) {
        return new StageResult(StageStatus.FAILED, message);
    }

    /** 跳过结果（条件为假时由引擎产生，也可由执行器主动返回）。 */
    public static StageResult skipped(String message) {
        return new StageResult(StageStatus.SKIPPED, message);
    }

    /** 返回状态。 */
    public StageStatus getStatus() {
        return status;
    }

    /** 返回说明文字。 */
    public String getMessage() {
        return message;
    }

    /** 是否成功或可继续（SUCCEEDED / SKIPPED）。 */
    public boolean isContinueable() {
        return StageStatus.isContinueable(status);
    }

    @Override
    public String toString() {
        return "StageResult{status=" + status + ", message='" + message + "'}";
    }
}
