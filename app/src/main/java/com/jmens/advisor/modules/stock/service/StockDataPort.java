package com.jmens.advisor.modules.stock.service;

import com.jmens.advisor.modules.stock.domain.KLinePoint;
import com.jmens.advisor.modules.stock.domain.StockQuote;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import java.util.List;

public interface StockDataPort {

  StockQuote getRealtimeQuote(StockSymbol symbol);

  List<KLinePoint> getRecentKLine(StockSymbol symbol, int days);
}
