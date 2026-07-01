package com.jmens.advisor.modules.stock.infrastructure;

import com.jmens.advisor.modules.stock.domain.KLinePoint;
import com.jmens.advisor.modules.stock.domain.StockQuote;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.service.StockDataPort;
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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SinaStockDataClient implements StockDataPort {

  private static final String DEFAULT_QUOTE_URL = "https://hq.sinajs.cn/list=";
  private static final Charset SINA_CHARSET = Charset.forName("GB18030");
  private static final Pattern QUOTE_BODY = Pattern.compile("\"([^\"]*)\"");

  private final HttpClient httpClient;
  private final String quoteBaseUrl;

  public SinaStockDataClient() {
    this(HttpClient.newHttpClient(), DEFAULT_QUOTE_URL);
  }

  public SinaStockDataClient(HttpClient httpClient, String quoteBaseUrl) {
    this.httpClient = httpClient;
    this.quoteBaseUrl = quoteBaseUrl;
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
    return List.of();
  }
}
