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

import io.github.nephets.reviewpilot.core.ReviewTenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

class ReviewpilotTenantFilterTest {

    private static final Servlet NOOP_SERVLET =
            new Servlet() {
                @Override
                public void init(ServletConfig config) {}

                @Override
                public ServletConfig getServletConfig() {
                    return null;
                }

                @Override
                public void service(ServletRequest req, ServletResponse res) {}

                @Override
                public String getServletInfo() {
                    return "";
                }

                @Override
                public void destroy() {}
            };

    @AfterEach
    void clearTenant() {
        ReviewTenant.clear();
    }

    @Test
    void setsTenantFromHeaderAndClearsAfter() throws Exception {
        ReviewpilotProperties properties = new ReviewpilotProperties();
        ReviewpilotTenantFilter filter = new ReviewpilotTenantFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Tenant-Id", "acme");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        assertThat(ReviewTenant.current()).isEqualTo("default");
    }

    @Test
    void tenantVisibleDuringChain() throws Exception {
        ReviewpilotProperties properties = new ReviewpilotProperties();
        ReviewpilotTenantFilter filter = new ReviewpilotTenantFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Tenant-Id", "acme");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Filter assertFilter = (req, res, c) -> assertThat(ReviewTenant.current()).isEqualTo("acme");
        filter.doFilter(request, response, new MockFilterChain(NOOP_SERVLET, assertFilter));
        assertThat(ReviewTenant.current()).isEqualTo("default");
    }

    @Test
    void missingHeaderFallsBackToDefault() throws Exception {
        ReviewpilotProperties properties = new ReviewpilotProperties();
        ReviewpilotTenantFilter filter = new ReviewpilotTenantFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Filter assertFilter =
                (req, res, c) -> assertThat(ReviewTenant.current()).isEqualTo("default");
        filter.doFilter(request, response, new MockFilterChain(NOOP_SERVLET, assertFilter));
    }

    @Test
    void customHeaderNameUsed() throws Exception {
        ReviewpilotProperties properties = new ReviewpilotProperties();
        properties.setTenantHeader("X-Org-Id");
        ReviewpilotTenantFilter filter = new ReviewpilotTenantFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Org-Id", "team-b");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Filter assertFilter =
                (req, res, c) -> assertThat(ReviewTenant.current()).isEqualTo("team-b");
        filter.doFilter(request, response, new MockFilterChain(NOOP_SERVLET, assertFilter));
    }

    @Test
    void clearsTenantEvenWhenChainThrows() throws Exception {
        ReviewpilotProperties properties = new ReviewpilotProperties();
        ReviewpilotTenantFilter filter = new ReviewpilotTenantFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");
        request.addHeader("X-Tenant-Id", "acme");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Filter boom =
                (req, res, c) -> {
                    throw new RuntimeException("boom");
                };
        try {
            filter.doFilter(request, response, new MockFilterChain(NOOP_SERVLET, boom));
        } catch (RuntimeException expected) {
            assertThat(expected.getMessage()).isEqualTo("boom");
        }
        assertThat(ReviewTenant.current()).isEqualTo("default");
    }
}
