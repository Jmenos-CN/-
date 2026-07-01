package com.jmens.advisor.common.cache;

public final class CacheKey {

  private CacheKey() {
  }

  public static String quote(String code) {
    return "stock:quote:" + code;
  }

  public static String kline(String code, int days) {
    return "stock:kline:" + code + ":" + days;
  }

  public static String report(String code, String analysisType) {
    return "advisor:report:" + code + ":" + analysisType;
  }
}
