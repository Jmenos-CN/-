package com.jmens.advisor.modules.stock.infrastructure;

import com.jmens.advisor.modules.stock.domain.StockNewsItem;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.service.StockNewsPort;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SinaStockNewsClient implements StockNewsPort {

  private static final String DEFAULT_NEWS_URL =
      "https://vip.stock.finance.sina.com.cn/corp/go.php/vCB_AllNewsStock/symbol/";
  private static final Charset SINA_CHARSET = Charset.forName("GB18030");
  private static final String SOURCE = "Sina Finance";
  private static final int ARTICLE_SUMMARY_LIMIT = 240;
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
  private static final Pattern NEWS_ITEM = Pattern.compile(
      "(\\d{4}-\\d{2}-\\d{2})&nbsp;(\\d{2}:\\d{2}).*?<a\\s+target=['\"]_blank['\"]\\s+href=['\"]([^'\"]+)['\"]>(.*?)</a>",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL
  );
  private static final Pattern ARTICLE_BODY = Pattern.compile(
      "<div[^>]+id=['\"]artibody['\"][^>]*>(.*?)</div>",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL
  );
  private static final Pattern META_DESCRIPTION = Pattern.compile(
      "<meta[^>]+name=['\"]description['\"][^>]+content=['\"]([^'\"]+)['\"][^>]*>",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL
  );
  private static final Pattern CHARSET = Pattern.compile(
      "charset=([^;\\s]+)",
      Pattern.CASE_INSENSITIVE
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
    while (matcher.find()) {
      items.add(new StockNewsItem(
          cleanText(matcher.group(4)),
          matcher.group(3),
          LocalDateTime.of(LocalDate.parse(matcher.group(1)), LocalTime.parse(matcher.group(2))),
          SOURCE
      ));
    }
    return deduplicateAndRank(items, limit);
  }

  public static String parseArticleSummary(String raw) {
    String body = extractFirstGroup(ARTICLE_BODY, raw);
    if (body.isBlank()) {
      body = extractFirstGroup(META_DESCRIPTION, raw);
    }
    return limitSummary(cleanText(body));
  }

  @Override
  public List<StockNewsItem> getRecentNews(StockSymbol symbol, int limit) {
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(newsBaseUrl + symbol.sinaCode() + ".phtml"))
          .header("User-Agent", "Mozilla/5.0")
          .timeout(REQUEST_TIMEOUT)
          .GET()
          .build();
      HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() >= 400) {
        throw new IllegalStateException("Sina stock news request failed: status=" + response.statusCode());
      }
      return parseNews(new String(response.body(), SINA_CHARSET), limit).stream()
          .map(this::enrichWithSummary)
          .toList();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Sina stock news request interrupted", e);
    } catch (Exception e) {
      throw new IllegalStateException("Sina stock news request failed: " + symbol.code(), e);
    }
  }

  private StockNewsItem enrichWithSummary(StockNewsItem item) {
    if (item.url() == null || item.url().isBlank()) {
      return item;
    }
    try {
      HttpRequest request = HttpRequest.newBuilder(URI.create(item.url()))
          .header("User-Agent", "Mozilla/5.0")
          .timeout(REQUEST_TIMEOUT)
          .GET()
          .build();
      HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() >= 400) {
        return item;
      }
      Charset charset = charsetFrom(response.headers(), StandardCharsets.UTF_8);
      String summary = parseArticleSummary(new String(response.body(), charset));
      if (summary.isBlank()) {
        return item;
      }
      return new StockNewsItem(item.title(), item.url(), item.publishedAt(), item.source(), summary);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return item;
    } catch (Exception ignored) {
      return item;
    }
  }

  private static List<StockNewsItem> deduplicateAndRank(List<StockNewsItem> items, int limit) {
    int max = Math.max(limit, 0);
    Set<String> seenTitles = new HashSet<>();
    Set<String> seenUrls = new HashSet<>();
    return items.stream()
        .sorted(Comparator.comparing(StockNewsItem::publishedAt).reversed())
        .filter(item -> {
          String normalizedTitle = normalizeTitle(item.title());
          String normalizedUrl = normalizeUrl(item.url());
          boolean duplicate = seenTitles.contains(normalizedTitle) || seenUrls.contains(normalizedUrl);
          seenTitles.add(normalizedTitle);
          seenUrls.add(normalizedUrl);
          return !duplicate;
        })
        .limit(max)
        .toList();
  }

  private static String extractFirstGroup(Pattern pattern, String raw) {
    Matcher matcher = pattern.matcher(raw);
    return matcher.find() ? matcher.group(1) : "";
  }

  private static String cleanText(String value) {
    return value
        .replaceAll("(?i)<br\\s*/?>", "\n")
        .replaceAll("(?i)</p>", "\n")
        .replaceAll("<[^>]+>", "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replaceAll("\\s+", " ")
        .trim();
  }

  private static String limitSummary(String value) {
    if (value.length() <= ARTICLE_SUMMARY_LIMIT) {
      return value;
    }
    return value.substring(0, ARTICLE_SUMMARY_LIMIT);
  }

  private static String normalizeTitle(String title) {
    return cleanText(title == null ? "" : title)
        .replaceAll("\\s+", "")
        .toLowerCase(Locale.ROOT);
  }

  private static String normalizeUrl(String url) {
    return url == null ? "" : url.trim().toLowerCase(Locale.ROOT);
  }

  private static Charset charsetFrom(HttpHeaders headers, Charset fallback) {
    return headers.firstValue("Content-Type")
        .map(CHARSET::matcher)
        .filter(Matcher::find)
        .map(matcher -> matcher.group(1).replace("\"", ""))
        .map(name -> {
          try {
            return Charset.forName(name);
          } catch (Exception ignored) {
            return fallback;
          }
        })
        .orElse(fallback);
  }
}
