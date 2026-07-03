package com.jmens.advisor.modules.advisor.config;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.advisor.peer")
public record PeerGroupProperties(Map<String, List<String>> groups) {

  public PeerGroupProperties {
    groups = groups == null ? defaultGroups() : groups;
  }

  private static Map<String, List<String>> defaultGroups() {
    return Map.of(
        "liquor", List.of("600519", "000858", "000568", "600809"),
        "bank", List.of("600036", "601166", "601398", "601328"),
        "securities", List.of("600030", "600837", "601688")
    );
  }
}
