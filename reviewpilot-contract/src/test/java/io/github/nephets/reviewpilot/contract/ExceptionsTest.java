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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionsTest {

    @Test
    void reviewExceptionIsRuntimeException() {
        assertThat(new ReviewException("boom")).isInstanceOf(RuntimeException.class);
        assertThat(new ReviewException("boom").getMessage()).isEqualTo("boom");
    }

    @Test
    void reviewExceptionWithCause() {
        Throwable cause = new IllegalStateException("root");
        assertThat(new ReviewException("boom", cause).getCause()).isEqualTo(cause);
    }

    @Test
    void runNotFoundExceptionMessage() {
        assertThat(new ReviewRunNotFoundException("missing").getMessage()).isEqualTo("missing");
    }

    @Test
    void stageExecutionExceptionHoldsStageId() {
        StageExecutionException e = new StageExecutionException("s1", "failed");
        assertThat(e.getStageId()).isEqualTo("s1");
        assertThat(e.getMessage()).isEqualTo("failed");
    }

    @Test
    void stageExecutionExceptionWithCause() {
        Throwable cause = new RuntimeException("root");
        StageExecutionException e = new StageExecutionException("s1", "failed", cause);
        assertThat(e.getStageId()).isEqualTo("s1");
        assertThat(e.getCause()).isEqualTo(cause);
    }

    @Test
    void invalidDefinitionExceptionMessage() {
        assertThat(new InvalidReviewDefinitionException("bad def").getMessage())
                .isEqualTo("bad def");
    }

    @Test
    void hierarchy() {
        assertThat(new ReviewRunNotFoundException("x")).isInstanceOf(ReviewException.class);
        assertThat(new StageExecutionException("s", "x")).isInstanceOf(ReviewException.class);
        assertThat(new InvalidReviewDefinitionException("x")).isInstanceOf(ReviewException.class);
    }
}
