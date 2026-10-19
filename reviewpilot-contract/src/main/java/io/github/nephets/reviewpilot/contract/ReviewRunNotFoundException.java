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

/** 按租户 + 运行 id 找不到对应的运行实例。 */
public class ReviewRunNotFoundException extends ReviewException {

    private static final long serialVersionUID = 1L;

    /** 构造异常。 */
    public ReviewRunNotFoundException(String message) {
        super(message);
    }

    /** 构造异常（带原因）。 */
    public ReviewRunNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
