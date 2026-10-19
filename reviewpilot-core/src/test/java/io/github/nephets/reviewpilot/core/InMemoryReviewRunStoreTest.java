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

import io.github.nephets.reviewpilot.contract.ReviewRunId;
import io.github.nephets.reviewpilot.contract.ReviewRunStatus;
import io.github.nephets.reviewpilot.contract.ReviewStage;
import io.github.nephets.reviewpilot.contract.RuleConfig;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryReviewRunStoreTest {

    private static ReviewRunInstance instance(String tenant, String runId) {
        return ReviewRunInstance.create(
                ReviewRunId.of(runId),
                tenant,
                ReviewPipeline.builder()
                        .pipelineId("p1")
                        .addStage(
                                ReviewStageDef.builder()
                                        .stageId(ReviewStage.FETCH_DIFF.id())
                                        .executor(ctx -> StageResult.success())
                                        .build())
                        .ruleConfig(RuleConfig.defaultConfig())
                        .build(),
                ReviewRequest.scripted("demo"),
                Instant.EPOCH);
    }

    @Test
    void saveAndFindById() {
        InMemoryReviewRunStore store = new InMemoryReviewRunStore();
        ReviewRunInstance inst = instance("acme", "r1");
        store.save(inst);
        assertThat(store.findById("acme", "r1")).contains(inst);
    }

    @Test
    void findByIdNullReturnsEmpty() {
        InMemoryReviewRunStore store = new InMemoryReviewRunStore();
        assertThat(store.findById(null, "r1")).isEmpty();
        assertThat(store.findById("acme", null)).isEmpty();
    }

    @Test
    void tenantIsolation() {
        InMemoryReviewRunStore store = new InMemoryReviewRunStore();
        store.save(instance("acme", "r1"));
        store.save(instance("other", "r1"));
        assertThat(store.findById("acme", "r1")).isPresent();
        assertThat(store.findById("other", "r1")).isPresent();
        assertThat(store.findAll("acme")).hasSize(1);
        assertThat(store.findAll("other")).hasSize(1);
    }

    @Test
    void findByStatus() {
        InMemoryReviewRunStore store = new InMemoryReviewRunStore();
        ReviewRunInstance inst = instance("acme", "r1");
        inst.setStatus(ReviewRunStatus.SUCCEEDED);
        store.save(inst);
        assertThat(store.findByStatus("acme", ReviewRunStatus.SUCCEEDED)).hasSize(1);
        assertThat(store.findByStatus("acme", ReviewRunStatus.FAILED)).isEmpty();
    }

    @Test
    void findByStatusNullReturnsEmpty() {
        assertThat(new InMemoryReviewRunStore().findByStatus("acme", null)).isEmpty();
    }

    @Test
    void findAllNullTenantReturnsEmpty() {
        assertThat(new InMemoryReviewRunStore().findAll(null)).isEmpty();
    }

    @Test
    void saveNullRejected() {
        assertThatThrownBy(() -> new InMemoryReviewRunStore().save(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void invalidCapacityRejected() {
        assertThatThrownBy(() -> new InMemoryReviewRunStore(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveMutatesStoredReferenceInPlace() {
        InMemoryReviewRunStore store = new InMemoryReviewRunStore();
        ReviewRunInstance inst = instance("acme", "r1");
        store.save(inst);
        inst.setStatus(ReviewRunStatus.FAILED);
        assertThat(store.findById("acme", "r1").orElseThrow().getStatus())
                .isEqualTo(ReviewRunStatus.FAILED);
    }
}
