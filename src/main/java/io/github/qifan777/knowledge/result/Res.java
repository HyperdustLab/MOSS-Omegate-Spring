package io.github.qifan777.knowledge.result;

import lombok.Data;

@Data
public class Res {

  private Integer code;

  private Object result;

  public Res(Integer code, Object result) {
    this.code = code;
    this.result = result;
  }
}
