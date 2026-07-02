package com.jmens.advisor.modules.stock.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jmens.advisor.modules.stock.domain.StockFinancialSnapshot;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.service.StockFinancialPort;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

@Component
public class EastmoneyFinancialClient implements StockFinancialPort {

  private static final String DEFAULT_FINANCIAL_URL =
      "https://datacenter-web.eastmoney.com/api/data/v1/get";
  private static final String COLUMNS = String.join(",",
      "SECUCODE",
      "SECURITY_CODE",
      "SECURITY_NAME_ABBR",
      "REPORT_DATE",
      "REPORT_TYPE",
      "EPSJB",
      "BPS",
      "TOTALOPERATEREVE",
      "PARENTNETPROFIT",
      "ROEJQ",
      "ZCFZL"
  );
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final HttpClient httpClient;
  private final String financialUrl;

  public EastmoneyFinancialClient() {
    this(HttpClient.newHttpClient(), DEFAULT_FINANCIAL_URL);
  }

  public EastmoneyFinancialClient(HttpClient httpClient, String financialUrl) {
    this.httpClient = httpClient;
    this.financialUrl = financialUrl;
  }

  public static StockFinancialSnapshot parseLatestSnapshot(String raw) {
    try {
      JsonNode root = OBJECT_MAPPER.readTree(raw);
      if (!root.path("success").asBoolean(false)) {
        throw new IllegalArgumentException("Eastmoney financial payload is unsuccessful");
      }
      JsonNode data = root.path("result").path("data");
      if (!data.isArray() || data.isEmpty()) {
        throw new IllegalArgumentException("Eastmoney financial payload has no data");
      }
      JsonNode row = data.get(0);
      return new StockFinancialSnapshot(
          text(row, "SECURITY_CODE"),
          text(row, "SECURITY_NAME_ABBR"),
          parseDate(text(row, "REPORT_DATE")),
          text(row, "REPORT_TYPE"),
          decimal(row, "EPSJB"),
          decimal(row, "BPS"),
          decimal(row, "TOTALOPERATEREVE"),
          decimal(row, "PARENTNETPROFIT"),
          decimal(row, "ROEJQ"),
          decimal(row, "ZCFZL")
      );
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Eastmoney financial payload JSON is invalid", e);
    }
  }

  @Override
  public StockFinancialSnapshot getLatestSnapshot(StockSymbol symbol) {
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(buildUrl(symbol)))
          .header("User-Agent", "Mozilla/5.0")
          .header("Referer", "https://data.eastmoney.com/")
          .timeout(REQUEST_TIMEOUT)
          .GET()
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      if (response.statusCode() >= 400) {
        throw new IllegalStateException("Eastmoney financial request failed: status=" + response.statusCode());
      }
      return parseLatestSnapshot(response.body());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Eastmoney financial request interrupted", e);
    } catch (Exception e) {
      throw new IllegalStateException("Eastmoney financial request failed: " + symbol.code(), e);
    }
  }

  private String buildUrl(StockSymbol symbol) {
    String secuCode = symbol.code() + "." + symbol.exchange();
    String filter = URLEncoder.encode("(SECUCODE=\"" + secuCode + "\")", StandardCharsets.UTF_8);
    return financialUrl
        + "?reportName=RPT_F10_FINANCE_MAINFINADATA"
        + "&columns=" + COLUMNS
        + "&filter=" + filter
        + "&pageNumber=1"
        + "&pageSize=1"
        + "&sortColumns=REPORT_DATE"
        + "&sortTypes=-1"
        + "&source=WEB"
        + "&client=WEB";
  }

  private static String text(JsonNode row, String field) {
    JsonNode node = row.path(field);
    return node.isMissingNode() || node.isNull() ? "" : node.asText();
  }

  private static BigDecimal decimal(JsonNode row, String field) {
    JsonNode node = row.path(field);
    if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
      return null;
    }
    return node.decimalValue();
  }

  private static LocalDate parseDate(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return LocalDate.parse(value.substring(0, 10));
  }
}
