package com.jmens.advisor.modules.stock.service;

import com.jmens.advisor.modules.stock.domain.StockSymbol;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StockSymbolParser {

  private static final Pattern STOCK_CODE = Pattern.compile("(?<!\\d)([0369]\\d{5})(?!\\d)");

  public StockSymbol parse(String input) {
    if (input == null || input.isBlank()) {
      throw new IllegalArgumentException("未识别到股票代码");
    }
    Matcher matcher = STOCK_CODE.matcher(input);
    if (!matcher.find()) {
      throw new IllegalArgumentException("未识别到股票代码");
    }
    String code = matcher.group(1);
    String exchange = code.startsWith("6") || code.startsWith("9") ? "SH" : "SZ";
    return new StockSymbol(code, exchange);
  }
}
