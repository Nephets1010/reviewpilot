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

class ReviewRunIdTest {

    @Test
    void ofWrapsValue() {
        ReviewRunId id = ReviewRunId.of("run-123");
        assertThat(id.asString()).isEqualTo("run-123");
    }

    @Test
    void generateProducesUniqueNonBlank() {
        ReviewRunId a = ReviewRunId.generate();
        ReviewRunId b = ReviewRunId.generate();
        assertThat(a.asString()).isNotBlank();
        assertThat(b.asString()).isNotBlank();
        assertThat(a.asString()).isNotEqualTo(b.asString());
    }

    @Test
    void equalWhenSameValue() {
        assertThat(ReviewRunId.of("x")).isEqualTo(ReviewRunId.of("x"));
        assertThat(ReviewRunId.of("x")).hasSameHashCodeAs(ReviewRunId.of("x"));
        assertThat(ReviewRunId.of("x")).isNotEqualTo(ReviewRunId.of("y"));
    }

    @Test
    void notEqualToNullAndOtherType() {
        assertThat(ReviewRunId.of("x")).isNotEqualTo(null);
        assertThat(ReviewRunId.of("x")).isNotEqualTo("x");
    }

    @Test
    void sameReferenceEqual() {
        ReviewRunId id = ReviewRunId.of("z");
        assertThat(id).isEqualTo(id);
    }
}
