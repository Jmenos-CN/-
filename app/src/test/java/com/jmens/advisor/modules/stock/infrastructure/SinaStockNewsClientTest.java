package com.jmens.advisor.modules.stock.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.stock.domain.StockNewsItem;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SinaStockNewsClientTest {

  private HttpServer server;

  @BeforeEach
  void setUp() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
  }

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void parsesSinaStockNewsHtml() {
    String raw = """
        <div class="datelist"><ul>
        &nbsp;&nbsp;&nbsp;&nbsp;2026-07-02&nbsp;17:20&nbsp;&nbsp;<a target='_blank' href='https://finance.sina.com.cn/news1.shtml'>Market Update One</a> <br>
        &nbsp;&nbsp;&nbsp;&nbsp;2026-07-02&nbsp;15:05&nbsp;&nbsp;<a target='_blank' href='https://finance.sina.com.cn/news2.shtml'>Market Update Two</a> <br>
        </ul></div>
        """;

    List<StockNewsItem> news = SinaStockNewsClient.parseNews(raw, 1);

    assertThat(news).hasSize(1);
    assertThat(news.getFirst().title()).isEqualTo("Market Update One");
    assertThat(news.getFirst().url()).isEqualTo("https://finance.sina.com.cn/news1.shtml");
    assertThat(news.getFirst().publishedAt()).hasToString("2026-07-02T17:20");
    assertThat(news.getFirst().source()).isEqualTo("Sina Finance");
  }

  @Test
  void deduplicatesAndSortsNewsBeforeApplyingLimit() {
    String raw = """
        <div class="datelist"><ul>
        &nbsp;&nbsp;&nbsp;&nbsp;2026-07-01&nbsp;09:30&nbsp;&nbsp;<a target='_blank' href='https://finance.sina.com.cn/older.shtml'>Older Market Update</a> <br>
        &nbsp;&nbsp;&nbsp;&nbsp;2026-07-02&nbsp;17:20&nbsp;&nbsp;<a target='_blank' href='https://finance.sina.com.cn/newer.shtml'>Newer Market Update</a> <br>
        &nbsp;&nbsp;&nbsp;&nbsp;2026-07-02&nbsp;17:21&nbsp;&nbsp;<a target='_blank' href='https://finance.sina.com.cn/newer-copy.shtml'>  Newer   Market Update </a> <br>
        </ul></div>
        """;

    List<StockNewsItem> news = SinaStockNewsClient.parseNews(raw, 2);

    assertThat(news)
        .extracting(StockNewsItem::title)
        .containsExactly("Newer Market Update", "Older Market Update");
  }

  @Test
  void fetchesRecentNewsFromHttpEndpoint() {
    String raw = """
        <div class="datelist"><ul>
        &nbsp;&nbsp;&nbsp;&nbsp;2026-07-02&nbsp;17:20&nbsp;&nbsp;<a target='_blank' href='https://finance.sina.com.cn/news1.shtml'>Market Article</a> <br>
        </ul></div>
        """;
    server.createContext("/", exchange -> {
      byte[] body = raw.getBytes(Charset.forName("GB18030"));
      exchange.getResponseHeaders().add("Content-Type", "text/html; charset=gb2312");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/news/";
    SinaStockNewsClient client = new SinaStockNewsClient(HttpClient.newHttpClient(), baseUrl);

    List<StockNewsItem> news = client.getRecentNews(new StockSymbol("600519", "SH"), 5);

    assertThat(news).singleElement()
        .satisfies(item -> assertThat(item.title()).isEqualTo("Market Article"));
  }

  @Test
  void enrichesNewsWithArticleSummary() {
    String port = String.valueOf(server.getAddress().getPort());
    String articleUrl = "http://127.0.0.1:" + port + "/article/news1.shtml";
    String raw = """
        <div class="datelist"><ul>
        &nbsp;&nbsp;&nbsp;&nbsp;2026-07-02&nbsp;17:20&nbsp;&nbsp;<a target='_blank' href='%s'>Market Article</a> <br>
        </ul></div>
        """.formatted(articleUrl);
    String article = """
        <html><body>
        <div id="artibody">
          <p>Company revenue growth stayed resilient while sector demand recovered.</p>
          <p>Management said it will keep channel inventory stable.</p>
        </div>
        </body></html>
        """;
    server.createContext("/news/sh600519.phtml", exchange -> {
      byte[] body = raw.getBytes(Charset.forName("GB18030"));
      exchange.getResponseHeaders().add("Content-Type", "text/html; charset=gb2312");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.createContext("/article/news1.shtml", exchange -> {
      byte[] body = article.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    String baseUrl = "http://127.0.0.1:" + port + "/news/";
    SinaStockNewsClient client = new SinaStockNewsClient(HttpClient.newHttpClient(), baseUrl);

    List<StockNewsItem> news = client.getRecentNews(new StockSymbol("600519", "SH"), 5);

    assertThat(news).singleElement()
        .satisfies(item -> assertThat(item.summary())
            .contains("Company revenue growth stayed resilient")
            .contains("channel inventory stable"));
  }

  @Test
  void keepsTitleOnlyNewsWhenArticleFetchFails() {
    String port = String.valueOf(server.getAddress().getPort());
    String articleUrl = "http://127.0.0.1:" + port + "/article/missing.shtml";
    String raw = """
        <div class="datelist"><ul>
        &nbsp;&nbsp;&nbsp;&nbsp;2026-07-02&nbsp;17:20&nbsp;&nbsp;<a target='_blank' href='%s'>Market Article</a> <br>
        </ul></div>
        """.formatted(articleUrl);
    server.createContext("/news/sh600519.phtml", exchange -> {
      byte[] body = raw.getBytes(Charset.forName("GB18030"));
      exchange.getResponseHeaders().add("Content-Type", "text/html; charset=gb2312");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    String baseUrl = "http://127.0.0.1:" + port + "/news/";
    SinaStockNewsClient client = new SinaStockNewsClient(HttpClient.newHttpClient(), baseUrl);

    List<StockNewsItem> news = client.getRecentNews(new StockSymbol("600519", "SH"), 5);

    assertThat(news).singleElement()
        .satisfies(item -> {
          assertThat(item.title()).isEqualTo("Market Article");
          assertThat(item.summary()).isEmpty();
        });
  }
}
