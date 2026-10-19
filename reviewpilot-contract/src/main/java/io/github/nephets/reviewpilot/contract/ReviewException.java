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

/**
 * reviewpilot 所有受检性意外的统一基类（非受检）。
 *
 * <p>引擎、阶段执行器、门禁等抛出的业务异常都继承之，便于调用方按基类统一捕获。
 */
public class ReviewException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 构造异常。 */
    public ReviewException(String message) {
        super(message);
    }

    /** 构造异常（带原因）。 */
    public ReviewException(String message, Throwable cause) {
        super(message, cause);
    }
}
