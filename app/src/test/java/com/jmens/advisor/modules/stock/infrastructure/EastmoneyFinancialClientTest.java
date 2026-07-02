package com.jmens.advisor.modules.stock.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.jmens.advisor.modules.stock.domain.StockFinancialSnapshot;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.sun.net.httpserver.HttpServer;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EastmoneyFinancialClientTest {

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
  void parsesLatestFinancialSnapshot() {
    String raw = financialPayload();

    StockFinancialSnapshot snapshot = EastmoneyFinancialClient.parseLatestSnapshot(raw);

    assertThat(snapshot.stockCode()).isEqualTo("600519");
    assertThat(snapshot.stockName()).isEqualTo("Kweichow Moutai");
    assertThat(snapshot.reportDate()).hasToString("2026-03-31");
    assertThat(snapshot.reportType()).isEqualTo("Q1");
    assertThat(snapshot.eps()).isEqualByComparingTo("21.76");
    assertThat(snapshot.bps()).isEqualByComparingTo("216.32234994607");
    assertThat(snapshot.totalOperatingRevenue()).isEqualByComparingTo("54702912385.23");
    assertThat(snapshot.parentNetProfit()).isEqualByComparingTo("27242512886.45");
    assertThat(snapshot.roe()).isEqualByComparingTo("10.57");
    assertThat(snapshot.debtRatio()).isEqualByComparingTo("12.1227489682");
  }

  @Test
  void fetchesFinancialSnapshotFromHttpEndpoint() {
    server.createContext("/api/data/v1/get", exchange -> {
      byte[] body = financialPayload().getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
      exchange.sendResponseHeaders(200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    server.start();
    String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/data/v1/get";
    EastmoneyFinancialClient client = new EastmoneyFinancialClient(HttpClient.newHttpClient(), baseUrl);

    StockFinancialSnapshot snapshot = client.getLatestSnapshot(new StockSymbol("600519", "SH"));

    assertThat(snapshot.stockCode()).isEqualTo("600519");
    assertThat(snapshot.roe()).isEqualByComparingTo(new BigDecimal("10.57"));
  }

  private static String financialPayload() {
    return """
        {
          "success": true,
          "message": "ok",
          "code": 0,
          "result": {
            "data": [
              {
                "SECUCODE": "600519.SH",
                "SECURITY_CODE": "600519",
                "SECURITY_NAME_ABBR": "Kweichow Moutai",
                "REPORT_DATE": "2026-03-31 00:00:00",
                "REPORT_TYPE": "Q1",
                "EPSJB": 21.76,
                "BPS": 216.32234994607,
                "TOTALOPERATEREVE": 54702912385.23,
                "PARENTNETPROFIT": 27242512886.45,
                "ROEJQ": 10.57,
                "ZCFZL": 12.1227489682
              }
            ]
          }
        }
        """;
  }
}
