package com.jmens.advisor.modules.stock.domain;

public record StockSymbol(String code, String exchange) {

  public String sinaCode() {
    return exchange.equals("SH") ? "sh" + code : "sz" + code;
  }
}
