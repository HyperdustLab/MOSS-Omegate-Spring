package io.github.qifan777.knowledge.ai.message.util;

import java.util.HashMap;
import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;

public class ChatModelMap {
  // Create a ThreadLocal for storing ChatModel
  private static final Map<String, ChatModel> chatModelHashMap = new HashMap<>();

  /**
   * Set the ChatModel for the current thread.
   *
   * @param chatModel the ChatModel to set
   */
  public static void set(String key, ChatModel chatModel) {
    chatModelHashMap.put(key, chatModel);
  }

  /**
   * Get the ChatModel for the current thread.
   *
   * @return the ChatModel for the current thread, or null if not set
   */
  public static ChatModel get(String key) {
    return chatModelHashMap.get(key);
  }

  /** Remove the ChatModel from the current thread. */
  public static void remove(String key) {
    chatModelHashMap.remove(key);
  }
}
