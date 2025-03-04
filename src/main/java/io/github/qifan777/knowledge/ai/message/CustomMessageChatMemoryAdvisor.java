package io.github.qifan777.knowledge.ai.message;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.MessageAggregator;
import reactor.core.publisher.Flux;

public class CustomMessageChatMemoryAdvisor extends AbstractChatMemoryAdvisor<ChatMemory> {

  private final String additionalDescription;

  public CustomMessageChatMemoryAdvisor(
      ChatMemory chatMemory,
      String defaultConversationId,
      int chatHistoryWindowSize,
      String additionalDescription) {
    super(
        chatMemory,
        defaultConversationId,
        chatHistoryWindowSize,
        true,
        Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER);
    this.additionalDescription = additionalDescription;
  }

  public static Builder builder(ChatMemory chatMemory) {
    return new Builder(chatMemory);
  }

  @Override
  public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
    advisedRequest = this.before(advisedRequest);
    AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);
    this.observeAfter(advisedResponse);
    return advisedResponse;
  }

  @Override
  public Flux<AdvisedResponse> aroundStream(
      AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
    Flux<AdvisedResponse> advisedResponses =
        this.doNextWithProtectFromBlockingBefore(advisedRequest, chain, this::before);
    return new MessageAggregator().aggregateAdvisedResponse(advisedResponses, this::observeAfter);
  }

  private AdvisedRequest before(AdvisedRequest request) {
    String conversationId = this.doGetConversationId(request.adviseContext());
    int chatMemoryRetrieveSize = this.doGetChatMemoryRetrieveSize(request.adviseContext());

    // 1. Retrieve the chat memory for the current conversation.
    List<Message> memoryMessages =
        this.getChatMemoryStore().get(conversationId, chatMemoryRetrieveSize);

    // 2. Create a new list of messages with the additional description as a system message.
    List<Message> advisedMessages = new ArrayList<>();
    advisedMessages.add(new SystemMessage(this.additionalDescription));
    advisedMessages.add(new SystemMessage("---------------------"));
    advisedMessages.addAll(memoryMessages);
    advisedMessages.addAll(request.messages());
    advisedMessages.add(new SystemMessage("---------------------"));

    // 3. Create a new request with the advised messages.
    AdvisedRequest advisedRequest = AdvisedRequest.from(request).messages(advisedMessages).build();

    // 4. Add the new user input to the conversation memory.
    UserMessage userMessage = new UserMessage(request.userText(), request.media());
    this.getChatMemoryStore().add(conversationId, userMessage);

    return advisedRequest;
  }

  private void observeAfter(AdvisedResponse advisedResponse) {
    List<Message> assistantMessages =
        advisedResponse.response().getResults().stream().map(g -> (Message) g.getOutput()).toList();
    this.getChatMemoryStore()
        .add(this.doGetConversationId(advisedResponse.adviseContext()), assistantMessages);
  }

  public static class Builder extends AbstractChatMemoryAdvisor.AbstractBuilder<ChatMemory> {

    private String additionalDescription = "";

    protected Builder(ChatMemory chatMemory) {
      super(chatMemory);
    }

    public Builder additionalDescription(String additionalDescription) {
      this.additionalDescription = additionalDescription;
      return this;
    }

    @Override
    public CustomMessageChatMemoryAdvisor build() {
      return new CustomMessageChatMemoryAdvisor(
          this.chatMemory,
          this.conversationId,
          this.chatMemoryRetrieveSize,
          this.additionalDescription);
    }
  }
}
