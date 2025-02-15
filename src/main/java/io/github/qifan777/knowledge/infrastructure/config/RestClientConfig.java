package io.github.qifan777.knowledge.infrastructure.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestClientConfig {

  @Bean
  public RestTemplate createRestTemplate() {
    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(100); // 设置最大连接数
    connectionManager.setDefaultMaxPerRoute(20); // 设置每个路由的最大连接数

    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
    httpClientBuilder.setConnectionManager(connectionManager);
    httpClientBuilder.setDefaultRequestConfig(
        RequestConfig.custom()
            .setConnectTimeout(120000) // 设置连接超时为60秒
            .setSocketTimeout(30000) // 设置读取超时为30秒
            .build());

    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    return new RestTemplate(factory);
  }

  @Bean
  public RestClient restClient() {

    return RestClient.create(createRestTemplate());
  }
}
