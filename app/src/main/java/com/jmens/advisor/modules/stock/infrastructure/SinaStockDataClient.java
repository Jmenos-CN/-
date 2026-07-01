package com.jmens.advisor.modules.stock.infrastructure;

import com.jmens.advisor.modules.stock.domain.KLinePoint;
import com.jmens.advisor.modules.stock.domain.StockQuote;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import com.jmens.advisor.modules.stock.service.StockDataPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SinaStockDataClient implements StockDataPort {

  private static final Pattern QUOTE_BODY = Pattern.compile("\"([^\"]*)\"");

  public static StockQuote parseQuote(String code, String raw) {
    Matcher matcher = QUOTE_BODY.matcher(raw);
    if (!matcher.find()) {
      throw new IllegalArgumentException("Sina quote payload is invalid");
    }
    String[] values = matcher.group(1).split(",");
    BigDecimal previousClose = decimal(values[2]);
    BigDecimal latestPrice = decimal(values[3]);
    BigDecimal changePercent = previousClose.compareTo(BigDecimal.ZERO) == 0
        ? BigDecimal.ZERO
        : latestPrice.subtract(previousClose)
            .multiply(new BigDecimal("100"))
            .divide(previousClose, 2, RoundingMode.HALF_UP);
    LocalDate date = LocalDate.parse(values[30]);
    LocalTime time = LocalTime.parse(values[31]);
    return new StockQuote(
        code,
        values[0],
        latestPrice,
        previousClose,
        changePercent,
        Long.parseLong(values[8]),
        decimal(values[9]),
        LocalDateTime.of(date, time)
    );
  }

  private static BigDecimal decimal(String value) {
    return new BigDecimal(value);
  }

  @Override
  public StockQuote getRealtimeQuote(StockSymbol symbol) {
    throw new UnsupportedOperationException("HTTP fetch will be added after parser tests pass");
  }

  @Override
  public List<KLinePoint> getRecentKLine(StockSymbol symbol, int days) {
    return List.of();
  }
}
