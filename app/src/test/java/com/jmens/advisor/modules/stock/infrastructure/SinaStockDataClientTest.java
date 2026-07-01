package com.jmens.advisor.modules.stock.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.domain.StockQuote;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.Charset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
}
