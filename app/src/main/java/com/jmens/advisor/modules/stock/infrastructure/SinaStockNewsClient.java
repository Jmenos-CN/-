package com.jmens.advisor.modules.stock.infrastructure;

import com.jmens.advisor.modules.stock.domain.StockNewsItem;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.service.StockNewsPort;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SinaStockNewsClient implements StockNewsPort {

  private static final String DEFAULT_NEWS_URL =
      "https://vip.stock.finance.sina.com.cn/corp/go.php/vCB_AllNewsStock/symbol/";
  private static final Charset SINA_CHARSET = Charset.forName("GB18030");
  private static final String SOURCE = "Sina Finance";
  private static final Pattern NEWS_ITEM = Pattern.compile(
      "(\\d{4}-\\d{2}-\\d{2})&nbsp;(\\d{2}:\\d{2}).*?<a\\s+target=['\"]_blank['\"]\\s+href=['\"]([^'\"]+)['\"]>(.*?)</a>",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL
  );

  private final HttpClient httpClient;
  private final String newsBaseUrl;

  public SinaStockNewsClient() {
    this(HttpClient.newHttpClient(), DEFAULT_NEWS_URL);
  }

  public SinaStockNewsClient(HttpClient httpClient, String newsBaseUrl) {
    this.httpClient = httpClient;
    this.newsBaseUrl = newsBaseUrl;
  }

  public static List<StockNewsItem> parseNews(String raw, int limit) {
    Matcher matcher = NEWS_ITEM.matcher(raw);
    List<StockNewsItem> items = new ArrayList<>();
    int max = Math.max(limit, 0);
    while (matcher.find() && items.size() < max) {
      items.add(new StockNewsItem(
          cleanText(matcher.group(4)),
          matcher.group(3),
          LocalDateTime.of(LocalDate.parse(matcher.group(1)), LocalTime.parse(matcher.group(2))),
          SOURCE
      ));
    }
    return items;
  }

  @Override
  public List<StockNewsItem> getRecentNews(StockSymbol symbol, int limit) {
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(newsBaseUrl + symbol.sinaCode() + ".phtml"))
          .header("User-Agent", "Mozilla/5.0")
          .GET()
          .build();
      HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() >= 400) {
        throw new IllegalStateException("Sina stock news request failed: status=" + response.statusCode());
      }
      return parseNews(new String(response.body(), SINA_CHARSET), limit);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Sina stock news request interrupted", e);
    } catch (Exception e) {
      throw new IllegalStateException("Sina stock news request failed: " + symbol.code(), e);
    }
  }

  private static String cleanText(String value) {
    return value
        .replaceAll("<[^>]+>", "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .trim();
  }
}
