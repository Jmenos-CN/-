package com.jmens.advisor.modules.stock.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.domain.KLinePoint;
import com.jmens.advisor.modules.stock.domain.StockQuote;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.Charset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

class SinaStockDataClientTest {

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
  void parsesSinaQuotePayload() {
    String raw = "var hq_str_sh600519=\"贵州茅台,1500.00,1490.00,1510.00,1520.00,1488.00,0,0,123456,185000000.00,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2026-07-01,10:30:00\";";

    StockQuote quote = SinaStockDataClient.parseQuote("600519", raw);

    assertThat(quote.code()).isEqualTo("600519");
    assertThat(quote.name()).isEqualTo("贵州茅台");
    assertThat(quote.latestPrice()).isEqualByComparingTo("1510.00");
    assertThat(quote.changePercent()).isEqualByComparingTo("1.34");
    assertThat(quote.volume()).isEqualTo(123456L);
  }

  @Test
  void fetchesRealtimeQuoteFromHttpEndpoint() {
    String raw = "var hq_str_sh600519=\"贵州茅台,1500.00,1490.00,1510.00,1520.00,1488.00,0,0,123456,185000000.00,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2026-07-01,10:30:00\";";
    server.createContext("/list=sh600519", exchange -> {
      byte[] body = raw.getBytes(Charset.forName("GB18030"));
      exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=GB18030");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/list=";
    SinaStockDataClient client = new SinaStockDataClient(HttpClient.newHttpClient(), baseUrl);

    StockQuote quote = client.getRealtimeQuote(new StockSymbol("600519", "SH"));

    assertThat(quote.name()).isEqualTo("贵州茅台");
    assertThat(quote.latestPrice()).isEqualByComparingTo("1510.00");
  }

  @Test
  void parsesSinaKLinePayload() {
    String raw = "var _sh600519_240_1751448000000=(["
        + "{\"day\":\"2026-06-30\",\"open\":\"1187.000\",\"high\":\"1195.670\",\"low\":\"1176.000\",\"close\":\"1185.490\",\"volume\":\"3960779\"},"
        + "{\"day\":\"2026-07-01\",\"open\":\"1180.100\",\"high\":\"1196.800\",\"low\":\"1166.330\",\"close\":\"1193.010\",\"volume\":\"4247381\"}"
        + "]);";

    List<KLinePoint> points = SinaStockDataClient.parseKLine(raw, 20);

    assertThat(points).hasSize(2);
    assertThat(points.get(0).tradeDate()).hasToString("2026-06-30");
    assertThat(points.get(0).open()).isEqualByComparingTo("1187.000");
    assertThat(points.get(1).close()).isEqualByComparingTo("1193.010");
    assertThat(points.get(1).volume()).isEqualTo(4247381L);
  }

  @Test
  void fetchesRecentKLineFromHttpEndpoint() {
    String raw = "var _sh600519_240_1751448000000=(["
        + "{\"day\":\"2026-07-01\",\"open\":\"1180.100\",\"high\":\"1196.800\",\"low\":\"1166.330\",\"close\":\"1193.010\",\"volume\":\"4247381\"}"
        + "]);";
    server.createContext("/", exchange -> {
      byte[] body = raw.getBytes(Charset.forName("UTF-8"));
      exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    String quoteBaseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/list=";
    String klineBaseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/kline/";
    SinaStockDataClient client = new SinaStockDataClient(
        HttpClient.newHttpClient(),
        quoteBaseUrl,
        klineBaseUrl
    );

    List<KLinePoint> points = client.getRecentKLine(new StockSymbol("600519", "SH"), 5);

    assertThat(points).hasSize(1);
    assertThat(points.get(0).close()).isEqualByComparingTo("1193.010");
  }
}
