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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StageResultTest {

    @Test
    void successIsContinueable() {
        StageResult r = StageResult.success();
        assertThat(r.getStatus()).isEqualTo(StageStatus.SUCCEEDED);
        assertThat(r.isContinueable()).isTrue();
        assertThat(r.getMessage()).isEmpty();
    }

    @Test
    void successWithMessage() {
        StageResult r = StageResult.success("done");
        assertThat(r.getStatus()).isEqualTo(StageStatus.SUCCEEDED);
        assertThat(r.getMessage()).isEqualTo("done");
    }

    @Test
    void failureIsNotContinueable() {
        StageResult r = StageResult.failure("boom");
        assertThat(r.getStatus()).isEqualTo(StageStatus.FAILED);
        assertThat(r.isContinueable()).isFalse();
        assertThat(r.getMessage()).isEqualTo("boom");
    }

    @Test
    void skippedIsContinueable() {
        StageResult r = StageResult.skipped("condition false");
        assertThat(r.getStatus()).isEqualTo(StageStatus.SKIPPED);
        assertThat(r.isContinueable()).isTrue();
    }

    @Test
    void nullMessageBecomesEmpty() {
        assertThat(StageResult.success(null).getMessage()).isEmpty();
    }
}
