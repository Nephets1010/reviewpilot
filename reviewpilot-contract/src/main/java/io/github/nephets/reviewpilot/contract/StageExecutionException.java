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

/** 某个阶段执行失败（含重试预算耗尽）。携带出错的阶段 id，便于定位。 */
public class StageExecutionException extends ReviewException {

    private static final long serialVersionUID = 1L;

    private final String stageId;

    /** 构造异常。 */
    public StageExecutionException(String stageId, String message) {
        super(message);
        this.stageId = stageId;
    }

    /** 构造异常（带原因）。 */
    public StageExecutionException(String stageId, String message, Throwable cause) {
        super(message, cause);
        this.stageId = stageId;
    }

    /** 返回出错的阶段 id。 */
    public String getStageId() {
        return stageId;
    }
}
