package com.jmens.advisor.modules.advisor.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration for the advisor LLM provider.
 *
 * <p>Values are environment-backed so credentials stay outside the repository while tests can keep
 * LLM access disabled.
 */
@ConfigurationProperties(prefix = "advisor.llm")
public class AdvisorLlmProperties {

  private boolean enabled;
  private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
  private String apiKey = "";
  private String modelName = "qwen-plus";
  private double temperature = 0.2;
  private Duration timeout = Duration.ofSeconds(60);

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public double getTemperature() {
    return temperature;
  }

  public void setTemperature(double temperature) {
    this.temperature = temperature;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public void setTimeout(Duration timeout) {
    this.timeout = timeout;
  }
}
