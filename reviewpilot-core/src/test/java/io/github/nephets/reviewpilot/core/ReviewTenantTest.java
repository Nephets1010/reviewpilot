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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewTenantTest {

    @AfterEach
    void clear() {
        ReviewTenant.clear();
    }

    @Test
    void defaultIsDefaultTenant() {
        assertThat(ReviewTenant.isDefault()).isTrue();
        assertThat(ReviewTenant.current()).isEqualTo(ReviewTenant.DEFAULT_TENANT_ID);
    }

    @Test
    void setChangesCurrent() {
        ReviewTenant.set("acme");
        assertThat(ReviewTenant.current()).isEqualTo("acme");
        assertThat(ReviewTenant.isDefault()).isFalse();
    }

    @Test
    void setBlankFallsBackToDefault() {
        ReviewTenant.set("  ");
        assertThat(ReviewTenant.current()).isEqualTo(ReviewTenant.DEFAULT_TENANT_ID);
    }

    @Test
    void runWithRestoresPrevious() {
        ReviewTenant.set("base");
        ReviewTenant.runWith("acme", () -> assertThat(ReviewTenant.current()).isEqualTo("acme"));
        assertThat(ReviewTenant.current()).isEqualTo("base");
    }

    @Test
    void runWithBlankFallsBackInsideBlock() {
        ReviewTenant.clear();
        ReviewTenant.runWith(
                null,
                () -> assertThat(ReviewTenant.current()).isEqualTo(ReviewTenant.DEFAULT_TENANT_ID));
    }

    @Test
    void resolveBlankFallsBack() {
        assertThat(ReviewTenant.resolve(null)).isEqualTo(ReviewTenant.DEFAULT_TENANT_ID);
        assertThat(ReviewTenant.resolve("  ")).isEqualTo(ReviewTenant.DEFAULT_TENANT_ID);
        assertThat(ReviewTenant.resolve("acme")).isEqualTo("acme");
    }

    @Test
    void clearRemovesThreadLocal() {
        ReviewTenant.set("acme");
        ReviewTenant.clear();
        assertThat(ReviewTenant.current()).isEqualTo(ReviewTenant.DEFAULT_TENANT_ID);
        assertThat(ReviewTenant.isDefault()).isTrue();
    }
}
