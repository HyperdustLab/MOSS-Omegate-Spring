package io.github.qifan777.knowledge.user;

import java.io.Serializable;
import lombok.Data;

@Data
public class UserToken implements Serializable {

  private String token;

  public UserToken(String token) {
    this.token = token;
  }
}
