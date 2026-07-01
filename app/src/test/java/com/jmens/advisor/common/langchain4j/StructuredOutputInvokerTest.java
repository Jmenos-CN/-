package com.jmens.advisor.common.langchain4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class StructuredOutputInvokerTest {

  @Test
  void retriesAfterInvalidJsonAndInjectsLastError() {
    ChatModel chatModel = Mockito.mock(ChatModel.class);
    when(chatModel.chat(contains("JSON")))
        .thenReturn("not json")
        .thenReturn("{\"summary\":\"ok\",\"score\":88}");
    StructuredOutputInvoker invoker = new StructuredOutputInvoker(new ObjectMapper(), 2);

    DemoResult result = invoker.invoke(chatModel, "system", "user", DemoResult.class);

    assertThat(result.summary()).isEqualTo("ok");
    assertThat(result.score()).isEqualTo(88);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(chatModel, times(2)).chat(captor.capture());
    assertThat(captor.getAllValues().get(1)).contains("上一次解析失败");
  }

  record DemoResult(String summary, int score) {}
}
