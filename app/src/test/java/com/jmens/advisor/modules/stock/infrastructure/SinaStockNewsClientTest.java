package com.jmens.advisor.modules.stock.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.stock.domain.StockNewsItem;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.Charset;
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
        &nbsp;&nbsp;&nbsp;&nbsp;2026-07-02&nbsp;17:20&nbsp;&nbsp;<a target='_blank' href='https://finance.sina.com.cn/news1.shtml'>14只白酒股下跌 贵州茅台重回1200元/股</a> <br>
        &nbsp;&nbsp;&nbsp;&nbsp;2026-07-02&nbsp;15:05&nbsp;&nbsp;<a target='_blank' href='https://finance.sina.com.cn/news2.shtml'>贵州茅台涨0.84%，成交额61.22亿元</a> <br>
        </ul></div>
        """;

    List<StockNewsItem> news = SinaStockNewsClient.parseNews(raw, 1);

    assertThat(news).hasSize(1);
    assertThat(news.get(0).title()).isEqualTo("14只白酒股下跌 贵州茅台重回1200元/股");
    assertThat(news.get(0).url()).isEqualTo("https://finance.sina.com.cn/news1.shtml");
    assertThat(news.get(0).publishedAt()).hasToString("2026-07-02T17:20");
    assertThat(news.get(0).source()).isEqualTo("Sina Finance");
  }

  @Test
  void fetchesRecentNewsFromHttpEndpoint() {
    String raw = """
        <div class="datelist"><ul>
        &nbsp;&nbsp;&nbsp;&nbsp;2026-07-02&nbsp;17:20&nbsp;&nbsp;<a target='_blank' href='https://finance.sina.com.cn/news1.shtml'>贵州茅台新闻</a> <br>
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
        .satisfies(item -> assertThat(item.title()).isEqualTo("贵州茅台新闻"));
  }
}
