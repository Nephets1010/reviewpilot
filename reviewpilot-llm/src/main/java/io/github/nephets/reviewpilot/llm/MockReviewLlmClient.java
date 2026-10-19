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
import io.github.nephets.reviewpilot.contract.FindingCategory;
import io.github.nephets.reviewpilot.contract.FindingSource;
import io.github.nephets.reviewpilot.contract.LlmReviewComment;
import io.github.nephets.reviewpilot.contract.ReviewSeverity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 确定性、离线的 {@link ReviewLlmClient} 实现。
 *
 * <p><b>为什么默认用它：</b>评审链路里 LLM 是最不可控、最贵的外部依赖。这个 Mock 不联网、不调任何模型 SDK， 而是按一组<b>固定的规则</b>扫描 diff
 * 内容产出发现——同样的输入永远得到同样的输出， 测试全程可断言、零成本、零网络。真实适配器见 {@link StubHttpReviewLlmClient}（仅 STUB，默认不接线）。
 *
 * <p>发现 id 由「文件路径 + 行号 + 规则 id」稳定哈希得出，因此多次运行结果可复现。
 */
public final class MockReviewLlmClient implements ReviewLlmClient {

    private static final Logger log = LoggerFactory.getLogger(MockReviewLlmClient.class);

    private static final String MODEL = "mock-deterministic";

    private final List<Rule> rules;

    /** 使用内置规则集构造。 */
    public MockReviewLlmClient() {
        this(defaultRules());
    }

    /**
     * 使用指定规则集构造（便于测试注入自定义规则）。
     *
     * @param rules 规则集，为 {@code null} 时按空规则处理
     */
    public MockReviewLlmClient(List<Rule> rules) {
        this.rules = rules == null ? List.of() : List.copyOf(rules);
    }

    @Override
    public String providerId() {
        return "mock";
    }

    @Override
    public boolean healthy() {
        return true;
    }

    @Override
    public LlmReviewResponse review(LlmReviewRequest request) {
        log.debug(
                "review 使用 Mock 客户端评审: diffFiles={}, existingFindings={}",
                request == null ? 0 : request.getDiffFiles().size(),
                request == null ? 0 : request.getExistingFindings().size());
        LlmReviewResponse.Builder response = LlmReviewResponse.builder().model(MODEL);
        if (request == null) {
            return response.rawText("(空请求)").build();
        }
        StringBuilder raw = new StringBuilder();
        for (DiffFile file : request.getDiffFiles()) {
            for (CodeChunk chunk : file.getChunks()) {
                scan(chunk, response, raw);
            }
        }
        response.rawText(raw.toString());
        return response.build();
    }

    private void scan(CodeChunk chunk, LlmReviewResponse.Builder response, StringBuilder raw) {
        String[] lines = chunk.getContent().split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineNo = chunk.getStartLine() + i;
            String lower = line.toLowerCase();
            for (Rule rule : rules) {
                if (lower.contains(rule.keyword())) {
                    String id = stableId(chunk.getFilePath(), lineNo, rule.ruleId());
                    Finding finding =
                            Finding.builder()
                                    .id(id)
                                    .severity(rule.severity())
                                    .category(rule.category())
                                    .source(FindingSource.AI)
                                    .filePath(chunk.getFilePath())
                                    .line(lineNo)
                                    .message(rule.message())
                                    .ruleId(rule.ruleId())
                                    .suggestion(rule.suggestion())
                                    .code(line.strip())
                                    .build();
                    LlmReviewComment comment =
                            LlmReviewComment.builder()
                                    .id(id)
                                    .filePath(chunk.getFilePath())
                                    .line(lineNo)
                                    .severity(rule.severity())
                                    .message(rule.message())
                                    .suggestion(rule.suggestion())
                                    .model(MODEL)
                                    .kind(LlmReviewComment.CommentKind.ISSUE)
                                    .raw(line.strip())
                                    .build();
                    response.addFinding(finding).addComment(comment);
                    raw.append(chunk.getFilePath())
                            .append(':')
                            .append(lineNo)
                            .append(' ')
                            .append(rule.severity())
                            .append(": ")
                            .append(rule.message())
                            .append('\n');
                    break;
                }
            }
        }
    }

    private static String stableId(String filePath, int line, String ruleId) {
        int h = Objects.hash(filePath, line, ruleId);
        return "mock-" + Integer.toHexString(h);
    }

    private static List<Rule> defaultRules() {
        List<Rule> list = new ArrayList<>();
        list.add(
                new Rule(
                        "password",
                        ReviewSeverity.BLOCKER,
                        FindingCategory.SECURITY,
                        "hardcoded-secret",
                        "疑似硬编码凭据/密钥，存在泄露风险",
                        "使用密钥管理服务或环境变量注入，避免明文写入源码"));
        list.add(
                new Rule(
                        "select *",
                        ReviewSeverity.MAJOR,
                        FindingCategory.PERFORMANCE,
                        "select-star",
                        "避免使用 SELECT *，应显式列出所需列",
                        "改为 SELECT col1, col2 ... 明确字段"));
        list.add(
                new Rule(
                        "printstacktrace",
                        ReviewSeverity.MINOR,
                        FindingCategory.STYLE,
                        "print-stack-trace",
                        "直接打印堆栈不利于日志采集与监控",
                        "使用结构化日志框架记录异常"));
        list.add(
                new Rule(
                        "system.out.println",
                        ReviewSeverity.MINOR,
                        FindingCategory.STYLE,
                        "sysout",
                        "生产代码应避免 System.out.println",
                        "使用日志框架替换"));
        list.add(
                new Rule(
                        "todo",
                        ReviewSeverity.MINOR,
                        FindingCategory.MAINTAINABILITY,
                        "todo-comment",
                        "存在 TODO/FIXME 待办，建议跟踪处理",
                        "登记到任务系统并补充负责人"));
        list.add(
                new Rule(
                        "thread.sleep",
                        ReviewSeverity.MINOR,
                        FindingCategory.PERFORMANCE,
                        "thread-sleep",
                        "忙等/硬睡眠可能影响吞吐",
                        "评估是否可用等待/通知或调度替代"));
        return list;
    }

    /** 扫描规则：关键字（小写匹配）+ 严重度 + 归类 + 文案 + 建议。 */
    public record Rule(
            String keyword,
            ReviewSeverity severity,
            FindingCategory category,
            String ruleId,
            String message,
            String suggestion) {}
}
