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
package io.github.nephets.reviewpilot.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StubHttpReviewLlmClientTest {

    @Test
    void providerIdIsStubHttp() {
        assertThat(new StubHttpReviewLlmClient().providerId()).isEqualTo("stub-http");
    }

    @Test
    void notHealthy() {
        assertThat(new StubHttpReviewLlmClient().healthy()).isFalse();
    }

    @Test
    void reviewThrowsLlmClientException() {
        StubHttpReviewLlmClient client = new StubHttpReviewLlmClient();
        assertThatThrownBy(() -> client.review(LlmReviewRequest.builder().build()))
                .isInstanceOf(LlmClientException.class)
                .hasMessageContaining("STUB");
    }

    @Test
    void defaultBaseUrlIsLocalhost() {
        StubHttpReviewLlmClient client = new StubHttpReviewLlmClient();
        assertThatThrownBy(() -> client.review(LlmReviewRequest.builder().build()))
                .hasMessageContaining("http://localhost:11434");
    }

    @Test
    void customBaseUrlRecorded() {
        StubHttpReviewLlmClient client = new StubHttpReviewLlmClient("https://llm.example.com");
        assertThatThrownBy(() -> client.review(LlmReviewRequest.builder().build()))
                .hasMessageContaining("https://llm.example.com");
    }

    @Test
    void nullBaseUrlFallsBackToDefault() {
        StubHttpReviewLlmClient client = new StubHttpReviewLlmClient(null);
        assertThatThrownBy(() -> client.review(LlmReviewRequest.builder().build()))
                .hasMessageContaining("http://localhost:11434");
    }
}
