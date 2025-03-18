package io.github.qifan777.knowledge.ai.agent.web3;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.qifan777.knowledge.ai.agent.AbstractAgent;
import io.github.qifan777.knowledge.ai.agent.Agent;
import io.github.qifan777.knowledge.ai.message.util.ChatModelFactory;
import java.util.List;
import java.util.function.Function;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

@Agent
@Description(value = "Web3交易机器人，可以查询代币列表和当前代币价格")
public class Web3Agent extends AbstractAgent implements Function<Web3Agent.Request, String> {
  private final String SYSTEM =
      """
            你是一个Web3交易机器人，能够查询代币列表以及每个代币的当前价格。
            你可以回答关于Web3和代币相关的所有问题。
            """;

  @Value("${hyperAGI.api}")
  private  String api;

  @Override
  public String apply(Request request) {

    String resBody =
        HttpRequest.get(api + "/sys/dict/getDictText/sys_config/qwen2_5:32b").execute().body();

    String baseURL = new JSONObject(resBody).getStr("result");

    ChatModel chatModel = ChatModelFactory.create(baseURL, "qwen2.5:32b");

    return ChatClient.create(chatModel)
        .prompt()
        .system(SYSTEM)
        .user(request.query)
        .functions(getAgentFunctions(this.getClass()))
        .call()
        .content();
  }

  public record Request(
      @JsonProperty(required = true) @JsonPropertyDescription(value = "用户原始的提问") String query) {}

  @Component
  @Description("查询当前已发行的代币列表")
  public static class TokenList implements Function<TokenList.Request, List<String>> {
    @Override
    public List<String> apply(Request request) {

      String body =
          HttpRequest.get("https://api.coingecko.com/api/v3/coins/markets")
              .form("vs_currency", "USD")
              .form("per_page", "10")
              .header("x-cg-demo-api-key", "CG-iqeKUxdxuaCzgeXqFDEXATFN")
              .execute()
              .body();

      JSONArray results = new JSONArray(body);

      return results.stream().map(i -> ((JSONObject) i).getStr("id")).toList();
    }

    public record Request() {}
  }

  @Component
  @Description("查询指定代币的当前价格、市场总值、24小时交易量和24小时变化")
  public static class TokenPrice implements Function<TokenPrice.Request, String> {

    @Override
    public String apply(Request request) {
      // 这里假设通过API查询某个代币的当前价格
      String token = request.query; // 获取用户请求的代币名
      TokenData tokenData = getPriceForToken(token); // 获取代币相关数据
      if (tokenData == null) {
        return "未找到该代币";
      }
      // 格式化返回代币信息
      return String.format(
          "Price in USD: %s\nMarket Cap in USD: %s\n24h Volume in USD: %s\n24h Change in USD: %s",
          tokenData.price, tokenData.marketCap, tokenData.volume24h, tokenData.change24h);
    }

    private TokenData getPriceForToken(String token) {
      String body =
          HttpRequest.get("https://api.coingecko.com/api/v3/coins/markets")
              .form("vs_currency", "USD")
              .form("ids", token)
              .header("x-cg-demo-api-key", "CG-iqeKUxdxuaCzgeXqFDEXATFN")
              .execute()
              .body();

      JSONArray results = new JSONArray(body);

      if (results.isEmpty()) {
        return null; // 未找到代币时返回null
      }

      JSONObject result = (JSONObject) results.get(0);

      // 提取代币的相关信息
      String price = result.getStr("current_price");
      String marketCap = result.getStr("market_cap");
      String volume24h = result.getStr("total_volume");
      String change24h = result.getStr("price_change_percentage_24h");

      // 返回一个TokenData对象
      return new TokenData(price, marketCap, volume24h, change24h);
    }

    public record Request(
        @JsonProperty(required = true) @JsonPropertyDescription("代币名称") String query) {}

    // 用来存储代币相关信息的内部类
    private static class TokenData {
      String price;
      String marketCap;
      String volume24h;
      String change24h;

      TokenData(String price, String marketCap, String volume24h, String change24h) {
        this.price = price;
        this.marketCap = marketCap;
        this.volume24h = volume24h;
        this.change24h = change24h;
      }
    }
  }
}
