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

import io.github.nephets.reviewpilot.contract.CodeChunk;
import io.github.nephets.reviewpilot.contract.DiffFile;
import io.github.nephets.reviewpilot.contract.Finding;
import io.github.nephets.reviewpilot.contract.FindingSource;
import io.github.nephets.reviewpilot.contract.LlmReviewComment;
import io.github.nephets.reviewpilot.contract.ReviewSeverity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MockReviewLlmClientTest {

    private static DiffFile file(String path, String content) {
        int end = content.split("\\R", -1).length;
        return DiffFile.builder()
                .path(path)
                .status(DiffFile.FileStatus.ADDED)
                .language("java")
                .addChunk(
                        CodeChunk.builder()
                                .filePath(path)
                                .startLine(1)
                                .endLine(end)
                                .content(content)
                                .build())
                .build();
    }

    private static LlmReviewRequest req(DiffFile... files) {
        return LlmReviewRequest.builder().diffFiles(List.of(files)).build();
    }

    @Test
    void providerIdIsMock() {
        assertThat(new MockReviewLlmClient().providerId()).isEqualTo("mock");
    }

    @Test
    void healthyByDefault() {
        assertThat(new MockReviewLlmClient().healthy()).isTrue();
    }

    @Test
    void detectsHardcodedSecretAsBlocker() {
        LlmReviewResponse response =
                new MockReviewLlmClient().review(req(file("A.java", "String password = \"x\";")));
        assertThat(response.getFindings())
                .anyMatch(
                        f ->
                                f.getRuleId().equals("hardcoded-secret")
                                        && f.getSeverity() == ReviewSeverity.BLOCKER
                                        && f.getSource() == FindingSource.AI);
        assertThat(response.getComments()).isNotEmpty();
    }

    @Test
    void detectsSelectStarAsMajor() {
        LlmReviewResponse response =
                new MockReviewLlmClient()
                        .review(req(file("A.java", "select * from users where id = 1")));
        assertThat(response.getFindings())
                .anyMatch(
                        f ->
                                f.getRuleId().equals("select-star")
                                        && f.getSeverity() == ReviewSeverity.MAJOR);
    }

    @Test
    void detectsPrintStackTraceAsMinor() {
        LlmReviewResponse response =
                new MockReviewLlmClient().review(req(file("A.java", "e.printStackTrace();")));
        assertThat(response.getFindings())
                .anyMatch(
                        f ->
                                f.getRuleId().equals("print-stack-trace")
                                        && f.getSeverity() == ReviewSeverity.MINOR);
    }

    @Test
    void detectsSystemOutAsMinor() {
        LlmReviewResponse response =
                new MockReviewLlmClient()
                        .review(req(file("A.java", "System.out.println(\"hi\");")));
        assertThat(response.getFindings())
                .anyMatch(
                        f ->
                                f.getRuleId().equals("sysout")
                                        && f.getSeverity() == ReviewSeverity.MINOR);
    }

    @Test
    void detectsTodoAsMinor() {
        LlmReviewResponse response =
                new MockReviewLlmClient().review(req(file("A.java", "// TODO: clean this up")));
        assertThat(response.getFindings())
                .anyMatch(
                        f ->
                                f.getRuleId().equals("todo-comment")
                                        && f.getSeverity() == ReviewSeverity.MINOR);
    }

    @Test
    void detectsThreadSleepAsMinor() {
        LlmReviewResponse response =
                new MockReviewLlmClient().review(req(file("A.java", "Thread.sleep(1000);")));
        assertThat(response.getFindings())
                .anyMatch(
                        f ->
                                f.getRuleId().equals("thread-sleep")
                                        && f.getSeverity() == ReviewSeverity.MINOR);
    }

    @Test
    void cleanDiffProducesNoFindings() {
        LlmReviewResponse response =
                new MockReviewLlmClient().review(req(file("A.java", "int a = 1;")));
        assertThat(response.getFindings()).isEmpty();
        assertThat(response.getComments()).isEmpty();
    }

    @Test
    void emptyDiffProducesEmptyResponse() {
        LlmReviewResponse response = new MockReviewLlmClient().review(req());
        assertThat(response.getFindings()).isEmpty();
        assertThat(response.getComments()).isEmpty();
    }

    @Test
    void nullRequestReturnsRawFallback() {
        LlmReviewResponse response = new MockReviewLlmClient().review(null);
        assertThat(response.getRawText()).contains("空请求");
        assertThat(response.getFindings()).isEmpty();
    }

    @Test
    void findingsAndCommentsShareStableIds() {
        LlmReviewResponse response =
                new MockReviewLlmClient().review(req(file("A.java", "String password = \"x\";")));
        assertThat(response.getFindings()).hasSameSizeAs(response.getComments());
        assertThat(response.getFindings().get(0).getId())
                .isEqualTo(response.getComments().get(0).getId());
        assertThat(response.getFindings().get(0).getId()).startsWith("mock-");
    }

    @Test
    void stableIdIsDeterministic() {
        MockReviewLlmClient c1 = new MockReviewLlmClient();
        MockReviewLlmClient c2 = new MockReviewLlmClient();
        LlmReviewResponse r1 = c1.review(req(file("A.java", "String password = \"x\";")));
        LlmReviewResponse r2 = c2.review(req(file("A.java", "String password = \"x\";")));
        assertThat(r1.getFindings().get(0).getId()).isEqualTo(r2.getFindings().get(0).getId());
    }

    @Test
    void rawTextContainsFilePathAndSeverity() {
        LlmReviewResponse response =
                new MockReviewLlmClient().review(req(file("A.java", "String password = \"x\";")));
        assertThat(response.getRawText()).contains("A.java");
        assertThat(response.getRawText()).contains("BLOCKER");
        assertThat(response.getModel()).isEqualTo("mock-deterministic");
    }

    @Test
    void onlyFirstMatchingRulePerLineApplies() {
        // 同时含 password 与 todo 的单行只命中 password(BLOCKER)，避免重复计数
        LlmReviewResponse response =
                new MockReviewLlmClient()
                        .review(req(file("A.java", "String password = \"x\"; // TODO")));
        assertThat(response.getFindings()).hasSize(1);
        assertThat(response.getFindings().get(0).getRuleId()).isEqualTo("hardcoded-secret");
    }

    @Test
    void customRulesAreUsed() {
        MockReviewLlmClient.Rule rule =
                new MockReviewLlmClient.Rule(
                        "danger",
                        ReviewSeverity.CRITICAL,
                        io.github.nephets.reviewpilot.contract.FindingCategory.SECURITY,
                        "custom-danger",
                        "检测到危险调用",
                        "移除");
        MockReviewLlmClient client = new MockReviewLlmClient(List.of(rule));
        LlmReviewResponse response = client.review(req(file("A.java", "doDanger();")));
        assertThat(response.getFindings())
                .anyMatch(
                        f ->
                                f.getRuleId().equals("custom-danger")
                                        && f.getSeverity() == ReviewSeverity.CRITICAL);
    }

    @Test
    void nullRulesTreatedAsEmpty() {
        MockReviewLlmClient client = new MockReviewLlmClient(null);
        LlmReviewResponse response = client.review(req(file("A.java", "String password = \"x\";")));
        assertThat(response.getFindings()).isEmpty();
    }

    @Test
    void commentKindIsIssue() {
        LlmReviewResponse response =
                new MockReviewLlmClient().review(req(file("A.java", "String password = \"x\";")));
        LlmReviewComment comment = response.getComments().get(0);
        assertThat(comment.getKind()).isEqualTo(LlmReviewComment.CommentKind.ISSUE);
        assertThat(comment.getSuggestion()).isNotBlank();
    }

    @Test
    void multipleFilesAndLinesAggregated() {
        LlmReviewResponse response =
                new MockReviewLlmClient()
                        .review(
                                req(
                                        file("A.java", "String password = \"x\";"),
                                        file("B.java", "System.out.println(\"y\");")));
        assertThat(response.getFindings()).hasSize(2);
    }

    @Test
    void findingHasLineNumber() {
        LlmReviewResponse response =
                new MockReviewLlmClient()
                        .review(req(file("A.java", "line1\nString password = \"x\";")));
        Finding f =
                response.getFindings().stream()
                        .filter(x -> x.getRuleId().equals("hardcoded-secret"))
                        .findFirst()
                        .orElseThrow();
        assertThat(f.getLine()).isEqualTo(2);
    }
}
