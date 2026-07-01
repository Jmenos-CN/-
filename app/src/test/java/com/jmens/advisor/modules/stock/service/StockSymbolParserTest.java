package com.jmens.advisor.modules.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jmens.advisor.modules.stock.domain.StockSymbol;
import org.junit.jupiter.api.Test;

class StockSymbolParserTest {

  private final StockSymbolParser parser = new StockSymbolParser();

  @Test
  void parsesShanghaiCode() {
    StockSymbol symbol = parser.parse("帮我分析 600519");

    assertThat(symbol.code()).isEqualTo("600519");
    assertThat(symbol.exchange()).isEqualTo("SH");
    assertThat(symbol.sinaCode()).isEqualTo("sh600519");
  }

  @Test
  void parsesShenzhenCode() {
    StockSymbol symbol = parser.parse("看看000001怎么样");

    assertThat(symbol.code()).isEqualTo("000001");
    assertThat(symbol.exchange()).isEqualTo("SZ");
    assertThat(symbol.sinaCode()).isEqualTo("sz000001");
  }

  @Test
  void rejectsTextWithoutStockCode() {
    assertThatThrownBy(() -> parser.parse("这只股票怎么样"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("未识别到股票代码");
  }
}
