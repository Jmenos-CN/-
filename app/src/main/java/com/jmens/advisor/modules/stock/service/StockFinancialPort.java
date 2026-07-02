package com.jmens.advisor.modules.stock.service;

import com.jmens.advisor.modules.stock.domain.StockFinancialSnapshot;
import com.jmens.advisor.modules.stock.domain.StockSymbol;

public interface StockFinancialPort {

  StockFinancialSnapshot getLatestSnapshot(StockSymbol symbol);
}
