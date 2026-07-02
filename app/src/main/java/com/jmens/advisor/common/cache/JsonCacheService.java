package com.jmens.advisor.common.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class JsonCacheService {

  private final CacheClient cacheClient;
  private final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  public JsonCacheService(CacheClient cacheClient) {
    this.cacheClient = cacheClient;
  }

  public <T> Optional<T> get(String key, Class<T> valueType) {
    return cacheClient.get(key).map(value -> read(key, value, valueType));
  }

  public <T> Optional<List<T>> getList(String key, Class<T> elementType) {
    return cacheClient.get(key).map(value -> readList(key, value, elementType));
  }

  public void put(String key, Object value, Duration ttl) {
    try {
      cacheClient.put(key, objectMapper.writeValueAsString(value), ttl);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to serialize cache value: " + key, exception);
    }
  }

  private <T> T read(String key, String value, Class<T> valueType) {
    try {
      return objectMapper.readValue(value, valueType);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize cache value: " + key, exception);
    }
  }

  private <T> List<T> readList(String key, String value, Class<T> elementType) {
    try {
      return objectMapper.readValue(
          value,
          objectMapper.getTypeFactory().constructCollectionType(List.class, elementType)
      );
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Failed to deserialize cache list value: " + key, exception);
    }
  }
}
