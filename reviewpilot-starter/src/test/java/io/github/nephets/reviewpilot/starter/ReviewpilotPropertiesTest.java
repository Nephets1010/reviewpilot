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
package io.github.nephets.reviewpilot.starter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewpilotPropertiesTest {

    @Test
    void defaultsAreSensible() {
        ReviewpilotProperties p = new ReviewpilotProperties();
        assertThat(p.getTenantHeader()).isEqualTo("X-Tenant-Id");
        assertThat(p.getDefaultTenantId()).isEqualTo("default");
        assertThat(p.getSnapshotLimit()).isEqualTo(50);
    }

    @Test
    void resolvePrefersExplicitTenant() {
        ReviewpilotProperties p = new ReviewpilotProperties();
        p.setDefaultTenantId("fallback");
        assertThat(ReviewpilotProperties.resolveTenantId("acme", p)).isEqualTo("acme");
    }

    @Test
    void resolveFallsBackToConfiguredDefault() {
        ReviewpilotProperties p = new ReviewpilotProperties();
        p.setDefaultTenantId("fallback");
        assertThat(ReviewpilotProperties.resolveTenantId(null, p)).isEqualTo("fallback");
        assertThat(ReviewpilotProperties.resolveTenantId("  ", p)).isEqualTo("fallback");
    }

    @Test
    void resolveFallsBackToGlobalDefault() {
        ReviewpilotProperties p = new ReviewpilotProperties();
        p.setDefaultTenantId("   ");
        assertThat(ReviewpilotProperties.resolveTenantId(null, p)).isEqualTo("default");
    }

    @Test
    void resolveWithNullPropertiesUsesGlobalDefault() {
        assertThat(ReviewpilotProperties.resolveTenantId(null, null)).isEqualTo("default");
        assertThat(ReviewpilotProperties.resolveTenantId("acme", null)).isEqualTo("acme");
    }

    @Test
    void blankTenantHeaderRevertsToDefault() {
        ReviewpilotProperties p = new ReviewpilotProperties();
        p.setTenantHeader("");
        assertThat(p.getTenantHeader()).isEqualTo("X-Tenant-Id");
        p.setTenantHeader(null);
        assertThat(p.getTenantHeader()).isEqualTo("X-Tenant-Id");
    }

    @Test
    void nonBlankTenantHeaderIsKept() {
        ReviewpilotProperties p = new ReviewpilotProperties();
        p.setTenantHeader("X-Org-Id");
        assertThat(p.getTenantHeader()).isEqualTo("X-Org-Id");
    }

    @Test
    void blankDefaultTenantIdIsKeptNotCorrected() {
        // 设计上不修正，使用处会走兜底并留日志
        ReviewpilotProperties p = new ReviewpilotProperties();
        p.setDefaultTenantId("  ");
        assertThat(p.getDefaultTenantId()).isEqualTo("  ");
    }

    @Test
    void negativeSnapshotLimitRevertsToDefault() {
        ReviewpilotProperties p = new ReviewpilotProperties();
        p.setSnapshotLimit(0);
        assertThat(p.getSnapshotLimit()).isEqualTo(50);
        p.setSnapshotLimit(-5);
        assertThat(p.getSnapshotLimit()).isEqualTo(50);
    }

    @Test
    void positiveSnapshotLimitIsKept() {
        ReviewpilotProperties p = new ReviewpilotProperties();
        p.setSnapshotLimit(10);
        assertThat(p.getSnapshotLimit()).isEqualTo(10);
    }

    @Test
    void constantsAreExposed() {
        assertThat(ReviewpilotProperties.DEFAULT_TENANT_ID).isEqualTo("default");
        assertThat(ReviewpilotProperties.DEFAULT_TENANT_HEADER).isEqualTo("X-Tenant-Id");
        assertThat(ReviewpilotProperties.DEFAULT_SNAPSHOT_LIMIT).isEqualTo(50);
    }
}
