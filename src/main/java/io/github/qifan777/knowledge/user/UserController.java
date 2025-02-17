package io.github.qifan777.knowledge.user;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import io.github.qifan777.knowledge.user.dto.UserRegisterInput;
import io.qifan.infrastructure.common.exception.BusinessException;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.babyfish.jimmer.client.FetchBy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RequestMapping("user")
@RestController
public class UserController {
  private static Map<String, String> tokenMap = new HashMap<>();
  @Resource private UserRepository userRepository;

  @Value("${hyperAGI.api}")
  private String api;

  @GetMapping
  public @FetchBy(value = "FETCHER", ownerType = UserRepository.class) User userInfo() {
    return userRepository
        .findById(StpUtil.getLoginIdAsString(), UserRepository.FETCHER)
        .orElseThrow(() -> new BusinessException("用户信息不存在"));
  }

  @PostMapping("login")
  public SaTokenInfo login(@RequestParam String token) {

    String body =
        HttpRequest.get(api + "/mgn/agent/getAgentByToken")
            .header("X-Access-Token", token)
            .execute()
            .body();

    JSONObject json = new JSONObject(body);

    String userId = json.getStr("result");

    tokenMap.put(userId, token);

    StpUtil.login(userId);
    return StpUtil.getTokenInfo();
  }

  @PostMapping("register")
  public SaTokenInfo register(@RequestBody UserRegisterInput input) {
    Optional<User> byPhone = userRepository.findByPhone(input.getPhone());
    if (byPhone.isPresent()) {
      throw new BusinessException("手机号已存在, 请登录");
    }
    User save =
        userRepository.save(
            UserDraft.$.produce(
                draft -> {
                  draft.setPhone(input.getPhone()).setPassword(BCrypt.hashpw(input.getPassword()));
                }));
    StpUtil.login(save.id());
    return StpUtil.getTokenInfo();
  }

  @GetMapping("/getSystemPrompt")
  public String getSystemPrompt() {

    String userId = (String) StpUtil.getLoginId();

    String token = tokenMap.get(userId);
    String body =
        HttpRequest.get(api + "/mgn/agent/getSystemPrompt")
            .header("X-Access-Token", token)
            .execute()
            .body();

    JSONObject json = new JSONObject(body);

    return json.getStr("result");
  }

  @GetMapping("/getAgent")
  public JSONObject getAgent() {

    String userId = (String) StpUtil.getLoginId();

    String token = tokenMap.get(userId);
    String body =
        HttpRequest.get(api + "/sys/getCurrUser").header("X-Access-Token", token).execute().body();

    JSONObject agent = new JSONObject();

    JSONObject user = new JSONObject(body).getJSONObject("result");

    agent.set("realname", user.getStr("realName"));
    agent.set("avatar", user.getStr("avatar"));

    body =
        HttpRequest.get(api + "/mgn/agent/list")
            .form("walletAddress", user.getStr("walletAddress"))
            .header("X-Access-Token", token)
            .execute()
            .body();

    JSONArray agents = new JSONObject(body).getJSONObject("result").getJSONArray("records");

    if (!agents.isEmpty()) {

      JSONObject _agent = agents.getJSONObject(0);

      agent.set("agentId", _agent.getStr("sid"));
      agent.set("agentName", _agent.getStr("nickName"));
      agent.set("agentAvatar", _agent.getStr("avatar"));

    }

    return agent;
  }
}
