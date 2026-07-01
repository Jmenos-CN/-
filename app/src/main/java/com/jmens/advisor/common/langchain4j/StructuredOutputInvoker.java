package com.jmens.advisor.common.langchain4j;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;

public class StructuredOutputInvoker {

  private final ObjectMapper objectMapper;
  private final int maxAttempts;

  public StructuredOutputInvoker(ObjectMapper objectMapper, int maxAttempts) {
    this.objectMapper = objectMapper;
    this.maxAttempts = Math.max(1, maxAttempts);
  }

  public <T> T invoke(ChatModel chatModel, String systemPrompt, String userPrompt, Class<T> responseType) {
    Exception lastError = null;
    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
      String prompt = buildPrompt(systemPrompt, userPrompt, responseType, lastError);
      try {
        String content = chatModel.chat(prompt);
        return objectMapper.readValue(extractJson(content), responseType);
      } catch (Exception e) {
        lastError = e;
      }
    }
    throw new IllegalStateException("结构化输出解析失败: " + lastError.getMessage(), lastError);
  }

  private String buildPrompt(String systemPrompt, String userPrompt, Class<?> responseType, Exception lastError) {
    String retryContext = lastError == null ? "" : "\n上一次解析失败: " + lastError.getMessage();
    return """
        # System
        %s

        请只返回合法 JSON，不要返回 Markdown。目标类型: %s
        %s

        # User
        %s
        """.formatted(systemPrompt, responseType.getSimpleName(), retryContext, userPrompt);
  }

  private String extractJson(String content) {
    int start = content.indexOf('{');
    int end = content.lastIndexOf('}');
    if (start >= 0 && end > start) {
      return content.substring(start, end + 1);
    }
    return content;
  }
}
