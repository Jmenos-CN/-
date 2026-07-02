package com.jmens.advisor.modules.stock.service;

import com.jmens.advisor.modules.stock.domain.StockNewsItem;
import com.jmens.advisor.modules.stock.domain.StockSymbol;
import java.util.List;

public interface StockNewsPort {

  List<StockNewsItem> getRecentNews(StockSymbol symbol, int limit);
}
