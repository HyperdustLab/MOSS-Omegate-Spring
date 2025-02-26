package io.github.qifan777.knowledge.ai.message.dto;

import lombok.Data;

@Data
public class AiMessageParams {
  Boolean enableVectorStore = true;
  Boolean enableAgent;
  String userId;
}
