package io.github.qifan777.knowledge.ai.message.util;

import cn.hutool.extra.spring.SpringUtil;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackResolver;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import org.springframework.core.io.ResourceLoader;

public class ChatModelFactory {

  private static FunctionCallbackResolver functionCallbackResolver;
  private static ObservationRegistry observationRegistry;

  static {
    functionCallbackResolver = SpringUtil.getBean(FunctionCallbackResolver.class);
    observationRegistry = SpringUtil.getBean(ObservationRegistry.class);
  }

  public static ChatModel create(String baseUrl, String model) {

    ChatModel chatModel =
        new OllamaChatModel(
            new OllamaApi(baseUrl),
            OllamaOptions.builder().model(model).build(),

            functionCallbackResolver,
            new ArrayList<FunctionCallback>(),
            observationRegistry,
            ModelManagementOptions.defaults());

    return chatModel;
  }
}
