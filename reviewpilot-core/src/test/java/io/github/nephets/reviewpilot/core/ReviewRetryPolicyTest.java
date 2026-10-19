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

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReviewRetryPolicyTest {

    @Test
    void noneAllowsOnlySingleAttempt() {
        ReviewRetryPolicy p = ReviewRetryPolicy.none();
        assertThat(p.getMaxAttempts()).isEqualTo(1);
        assertThat(p.allows(1)).isFalse();
        assertThat(p.backoffFor(1)).isEqualTo(Duration.ZERO);
    }

    @Test
    void fixedBackoffReturnsSameDuration() {
        ReviewRetryPolicy p = ReviewRetryPolicy.fixed(3, Duration.ofMillis(50));
        assertThat(p.getMode()).isEqualTo(ReviewRetryPolicy.BackoffMode.FIXED);
        assertThat(p.allows(1)).isTrue();
        assertThat(p.allows(2)).isTrue();
        assertThat(p.allows(3)).isFalse();
        assertThat(p.backoffFor(1)).isEqualTo(Duration.ofMillis(50));
        assertThat(p.backoffFor(2)).isEqualTo(Duration.ofMillis(50));
    }

    @Test
    void exponentialBackoffGrows() {
        ReviewRetryPolicy p = ReviewRetryPolicy.exponential(5, Duration.ofMillis(10), 2.0);
        assertThat(p.getMode()).isEqualTo(ReviewRetryPolicy.BackoffMode.EXPONENTIAL);
        assertThat(p.backoffFor(1)).isEqualTo(Duration.ofMillis(10));
        assertThat(p.backoffFor(2)).isEqualTo(Duration.ofMillis(20));
        assertThat(p.backoffFor(3)).isEqualTo(Duration.ofMillis(40));
    }

    @Test
    void backoffCapsAtOneHour() {
        ReviewRetryPolicy p = ReviewRetryPolicy.exponential(100, Duration.ofMillis(1), 1000.0);
        Duration capped = p.backoffFor(20);
        assertThat(capped.toMillis()).isLessThanOrEqualTo(Duration.ofHours(1).toMillis());
    }

    @Test
    void backoffZeroWhenAttemptLessThanOne() {
        assertThat(ReviewRetryPolicy.fixed(3, Duration.ofMillis(10)).backoffFor(0))
                .isEqualTo(Duration.ZERO);
    }

    @Test
    void ofRejectsBadArguments() {
        assertThatThrownBy(
                        () ->
                                ReviewRetryPolicy.of(
                                        0, Duration.ZERO, ReviewRetryPolicy.BackoffMode.FIXED, 1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                ReviewRetryPolicy.of(
                                        2, null, ReviewRetryPolicy.BackoffMode.FIXED, 1.0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(
                        () ->
                                ReviewRetryPolicy.of(
                                        2,
                                        Duration.ofMillis(-1),
                                        ReviewRetryPolicy.BackoffMode.FIXED,
                                        1.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ReviewRetryPolicy.of(2, Duration.ZERO, null, 1.0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(
                        () ->
                                ReviewRetryPolicy.of(
                                        2, Duration.ZERO, ReviewRetryPolicy.BackoffMode.FIXED, 0.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalsAndHashCode() {
        ReviewRetryPolicy a = ReviewRetryPolicy.fixed(3, Duration.ofMillis(10));
        ReviewRetryPolicy b = ReviewRetryPolicy.fixed(3, Duration.ofMillis(10));
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(ReviewRetryPolicy.fixed(4, Duration.ofMillis(10)));
    }
}
