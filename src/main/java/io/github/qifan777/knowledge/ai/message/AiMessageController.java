package io.github.qifan777.knowledge.ai.message;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.qifan777.knowledge.ai.agent.Agent;
import io.github.qifan777.knowledge.ai.message.dto.AiMessageInput;
import io.github.qifan777.knowledge.ai.message.dto.AiMessageWrapper;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.model.Media;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RequestMapping("message")
@RestController
@AllArgsConstructor
@Slf4j
public class AiMessageController {
  private final AiMessageChatMemory chatMemory;
  private final ChatModel chatModel;
  //  private final ImageModel imageModel;
  private final VectorStore vectorStore;
  private final ObjectMapper objectMapper;
  private final AiMessageRepository messageRepository;
  private final ApplicationContext applicationContext;

  @DeleteMapping("history/{sessionId}")
  public void deleteHistory(@PathVariable String sessionId) {
    chatMemory.clear(sessionId);
  }

  /**
   * 消息保存
   *
   * @param input 用户发送的消息/AI回复的消息
   */
  @PostMapping
  public void save(@RequestBody AiMessageInput input) {
    messageRepository.save(input.toEntity());
  }

  //  @PostMapping("chat/image")
  //  public String textToImageChat(@RequestBody AiMessageInput input) {
  //    return imageModel
  //        .call(new ImagePrompt(input.getTextContent()))
  //        .getResult()
  //        .getOutput()
  //        .getUrl();
  //  }

  /**
   * 为了支持文件问答，需要同时接收json（AiMessageWrapper json体）和 MultipartFile（文件） Content-Type 从 application/json
   * 修改为 multipart/form-data 之前接收请求参数是用@RequestBody, 现在使用@RequestPart
   * 接收json字符串再手动转成AiMessageWrapper.
   * SpringMVC的@RequestPart是支持自动将Json字符串转换为Java对象，也就是说可以等效`@RequestBody`，
   * 但是由于前端FormData无法设置Part的Content-Type，所以只能手动转json字符串再转成Java对象。
   *
   * @param input 消息包含文本信息，会话id，多媒体信息（图片语言）。参考src/main/dto/AiMessage.dto
   * @param file 文件问答
   * @return SSE流
   */
  @SneakyThrows
  @PostMapping(value = "chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  @SaIgnore
  public Flux<ServerSentEvent<String>> chat(
      @RequestParam String input, @RequestParam(required = false) String content) {
    AiMessageWrapper aiMessageWrapper = objectMapper.readValue(input, AiMessageWrapper.class);

    String userId = null;

    try {
      userId = (String) StpUtil.getLoginId();
    } catch (Exception ignored) {

      userId = aiMessageWrapper.getParams().getUserId();
    }

    String[] functionBeanNames = new String[0];
    // 如果启用Agent则获取Agent的bean
    if (aiMessageWrapper.getParams().getEnableAgent()) {
      // 获取带有Agent注解的bean
      Map<String, Object> beansWithAnnotation =
          applicationContext.getBeansWithAnnotation(Agent.class);
      functionBeanNames = new String[beansWithAnnotation.size()];
      functionBeanNames = beansWithAnnotation.keySet().toArray(functionBeanNames);
    }
    String finalUserId = userId;

    // 启用文件问答
    // agent列表`
    // 使用历史消息
    // 使用向量数据库
    // 和前端监听的事件相对应

    return ChatClient.create(chatModel)
        .prompt()

        // agent列表`
        .functions(functionBeanNames)
        // 启用文件问答
        .system(promptSystemSpec -> useFile(promptSystemSpec, content))
        .advisors(
            advisorSpec -> {
              // 使用历史消息
              useChatHistory(advisorSpec, aiMessageWrapper.getMessage().getSessionId());
              // 使用向量数据库
              useVectorStore(
                  advisorSpec, aiMessageWrapper.getParams().getEnableVectorStore(), finalUserId);
            })
        .user(promptUserSpec -> toPrompt(promptUserSpec, aiMessageWrapper.getMessage()))
        .stream()
        .chatResponse()
        .map(
            chatResponse ->
                ServerSentEvent.builder(toJson(chatResponse))
                    // 和前端监听的事件相对应
                    .event("message")
                    .build());
  }

  //  /**
  //   * 不通过流的形式完成推理
  //   *
  //   * @param input 消息内容（文本，图片，等）
  //   * @param content 文件内容（如果需要的话）
  //   * @return ChatResponse 推理结果
  //   */
  //  @SneakyThrows
  //  @PostMapping(value = "chat-sync", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  //  public ChatResponse chatSync(
  //      @RequestParam String input, @RequestParam(required = false) String content) {
  //
  //    // 将传入的 input JSON 字符串解析为 AiMessageWrapper 对象
  //    AiMessageWrapper aiMessageWrapper = JSONUtil.toBean(input, AiMessageWrapper.class);
  //    // 获取 functionBeanNames（Agent 支持功能）
  //    String[] functionBeanNames = new String[0];
  //    if (aiMessageWrapper.getParams().getEnableAgent()) {
  //      Map<String, Object> beansWithAnnotation =
  //          applicationContext.getBeansWithAnnotation(Agent.class);
  //      functionBeanNames = new String[beansWithAnnotation.size()];
  //      functionBeanNames = beansWithAnnotation.keySet().toArray(functionBeanNames);
  //    }
  //
  //    // 使用 ChatClient 创建一个同步推理请求
  //    ChatResponse chatResponse =
  //        ChatClient.create(chatModel)
  //            .prompt()
  //            .system(promptSystemSpec -> useFile(promptSystemSpec, content)) // 启用文件问答
  //            .user(promptUserSpec -> toPrompt(promptUserSpec, aiMessageWrapper.getMessage()))
  //            //
  //            .functions(functionBeanNames)
  //            .advisors(
  //                advisorSpec -> {
  //                  useChatHistory(
  //                      advisorSpec, aiMessageWrapper.getMessage().getSessionId()); // 使用历史消息
  //                  useVectorStore(
  //                      advisorSpec,
  //                      aiMessageWrapper.getParams().getEnableVectorStore(),
  //                      aiMessageWrapper.getParams().getUserId()); // 使用向量数据库
  //                })
  //            .call()
  //            .chatResponse();
  //
  //    // 返回最终的推理结果
  //    return chatResponse;
  //  }

  @SneakyThrows
  public String toJson(ChatResponse response) {
    return objectMapper.writeValueAsString(response);
  }

  public void toPrompt(ChatClient.PromptUserSpec promptUserSpec, AiMessageInput input) {
    // AiMessageInput转成Message
    Message message = AiMessageChatMemory.toSpringAiMessage(input.toEntity());
    if (message instanceof UserMessage userMessage
        && !CollectionUtils.isEmpty(userMessage.getMedia())) {
      // 用户发送的图片/语言
      Media[] medias = new Media[userMessage.getMedia().size()];
      promptUserSpec.media(userMessage.getMedia().toArray(medias));
    }
    // 用户发送的文本
    promptUserSpec.text(message.getContent());
  }

  public void useChatHistory(ChatClient.AdvisorSpec advisorSpec, String sessionId) {
    // 1. 如果需要存储会话和消息到数据库，自己可以实现ChatMemory接口，这里使用自己实现的AiMessageChatMemory，数据库存储。
    // 2. 传入会话id，MessageChatMemoryAdvisor会根据会话id去查找消息。
    // 3. 只需要携带最近10条消息2
    // MessageChatMemoryAdvisor会在消息发送给大模型之前，从ChatMemory中获取会话的历史消息，然后一起发送给大模型。
    advisorSpec.advisors(new MessageChatMemoryAdvisor(chatMemory, sessionId, 10));
  }

  public void useVectorStore(
      ChatClient.AdvisorSpec advisorSpec, Boolean enableVectorStore, String userId) {
    if (!enableVectorStore) return;

    FilterExpressionBuilder b = new FilterExpressionBuilder();

    Expression exp = b.eq("userId", userId).build();

    log.info("filterExpression: {}", exp);

    SearchRequest searchRequest = SearchRequest.defaults().withFilterExpression(exp);

    // question_answer_context是一个占位符，会替换成向量数据库中查询到的文档。QuestionAnswerAdvisor会替换。
    String promptWithContext =
        """
                ---------------------
                {question_answer_context}
                ---------------------
                """;

    advisorSpec.advisors(new QuestionAnswerAdvisor(vectorStore, searchRequest, promptWithContext));
  }

  @SneakyThrows
  public void useFile(ChatClient.PromptSystemSpec spec, String content) {

    if (StrUtil.isBlank(content)) {
      return;
    }
    Message message =
        new PromptTemplate(
                """
                The following content is additional knowledge. You can refer to it when answering questions.
                ---------------------
                {context}
                ---------------------
                """)
            .createMessage(Map.of("context", content));
    spec.text(message.getContent());
  }
}
