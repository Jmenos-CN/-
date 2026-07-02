package com.jmens.advisor.modules.stock.infrastructure;

import com.jmens.advisor.modules.stock.domain.KLinePoint;
import com.jmens.advisor.modules.stock.domain.StockQuote;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.service.StockDataPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SinaStockDataClient implements StockDataPort {

  private static final String DEFAULT_QUOTE_URL = "https://hq.sinajs.cn/list=";
  private static final String DEFAULT_KLINE_URL = "https://quotes.sina.cn/cn/api/jsonp_v2.php/var%20_";
  private static final Charset SINA_CHARSET = Charset.forName("GB18030");
  private static final Pattern QUOTE_BODY = Pattern.compile("\"([^\"]*)\"");
  private static final Pattern KLINE_BODY = Pattern.compile("=\\s*\\((\\[.*])\\)\\s*;?", Pattern.DOTALL);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final HttpClient httpClient;
  private final String quoteBaseUrl;
  private final String klineBaseUrl;

  public SinaStockDataClient() {
    this(HttpClient.newHttpClient(), DEFAULT_QUOTE_URL, DEFAULT_KLINE_URL);
  }

  public SinaStockDataClient(HttpClient httpClient, String quoteBaseUrl) {
    this(httpClient, quoteBaseUrl, DEFAULT_KLINE_URL);
  }

  public SinaStockDataClient(HttpClient httpClient, String quoteBaseUrl, String klineBaseUrl) {
    this.httpClient = httpClient;
    this.quoteBaseUrl = quoteBaseUrl;
    this.klineBaseUrl = klineBaseUrl;
  }

  public static StockQuote parseQuote(String code, String raw) {
    Matcher matcher = QUOTE_BODY.matcher(raw);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Sina quote payload is invalid");
    }
    String[] values = matcher.group(1).split(",");
    BigDecimal previousClose = decimal(values[2]);
    BigDecimal latestPrice = decimal(values[3]);
    BigDecimal changePercent = previousClose.compareTo(BigDecimal.ZERO) == 0
        ? BigDecimal.ZERO
        : latestPrice.subtract(previousClose)
            .multiply(new BigDecimal("100"))
            .divide(previousClose, 2, RoundingMode.HALF_UP);
    LocalDate date = LocalDate.parse(values[30]);
    LocalTime time = LocalTime.parse(values[31]);
    return new StockQuote(
        code,
        values[0],
        latestPrice,
        previousClose,
        changePercent,
        Long.parseLong(values[8]),
        decimal(values[9]),
        LocalDateTime.of(date, time)
    );
  }

  private static BigDecimal decimal(String value) {
    return new BigDecimal(value);
  }

  /**
   * Parses Sina JSONP K-line payloads into daily candles.
   *
   * @param raw raw JSONP response from Sina K-line endpoint
   * @param limit max number of most recent candles to return
   * @return ordered K-line points from oldest to newest
   */
  public static List<KLinePoint> parseKLine(String raw, int limit) {
    Matcher matcher = KLINE_BODY.matcher(raw);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Sina kline payload is invalid");
    }
    try {
      List<Map<String, String>> rows = OBJECT_MAPPER.readValue(
          matcher.group(1),
          new TypeReference<>() {}
      );
      int start = Math.max(0, rows.size() - Math.max(limit, 0));
      List<KLinePoint> points = new ArrayList<>();
      for (Map<String, String> row : rows.subList(start, rows.size())) {
        points.add(new KLinePoint(
            LocalDate.parse(row.get("day")),
            decimal(row.get("open")),
            decimal(row.get("close")),
            decimal(row.get("high")),
            decimal(row.get("low")),
            Long.parseLong(row.get("volume"))
        ));
      }
      return points;
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Sina kline payload JSON is invalid", e);
    }
  }

  @Override
  public StockQuote getRealtimeQuote(StockSymbol symbol) {
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(quoteBaseUrl + symbol.sinaCode()))
          .header("Referer", "https://finance.sina.com.cn")
          .GET()
          .build();
      HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() >= 400) {
        throw new IllegalStateException("Sina quote request failed: status=" + response.statusCode());
      }
      String raw = new String(response.body(), SINA_CHARSET);
      return parseQuote(symbol.code(), raw);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Sina quote request interrupted", e);
    } catch (Exception e) {
      throw new IllegalStateException("Sina quote request failed: " + symbol.code(), e);
    }
  }

  @Override
  public List<KLinePoint> getRecentKLine(StockSymbol symbol, int days) {
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(buildKlineUrl(symbol, days)))
          .header("Referer", "https://finance.sina.com.cn")
          .GET()
          .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 400) {
        throw new IllegalStateException("Sina kline request failed: status=" + response.statusCode());
      }
      return parseKLine(response.body(), days);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Sina kline request interrupted", e);
    } catch (Exception e) {
      throw new IllegalStateException("Sina kline request failed: " + symbol.code(), e);
    }
  }

  private String buildKlineUrl(StockSymbol symbol, int days) {
    return klineBaseUrl
        + symbol.sinaCode()
        + "_240_"
        + System.currentTimeMillis()
        + "=/CN_MarketDataService.getKLineData?symbol="
        + symbol.sinaCode()
        + "&scale=240&ma=no&datalen="
        + days;
  }
}
