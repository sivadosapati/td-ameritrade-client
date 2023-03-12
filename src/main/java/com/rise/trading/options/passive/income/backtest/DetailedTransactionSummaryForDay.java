package com.rise.trading.options.passive.income.backtest;

import java.util.List;

public class DetailedTransactionSummaryForDay {
	public TransactionSummary summary;
	public OptionTransaction callTransaction = null;
	public OptionTransaction putTransaction = null;
	public List<PotentialTransaction> stockTransactionsForCall;
	public List<PotentialTransaction> stockTransactionsForPut;
	public HistoricalPricesForDay day;

	public int getMinuteTransactions() {
		return day.prices.size();
	}

	public StockTransaction getStockTransactionForCall(HistoricalPriceForMinute minute) {
		return fetchPotentialTransactionIfExisting(stockTransactionsForCall, minute);
	}

	public StockTransaction getStockTransactionForPut(HistoricalPriceForMinute minute) {
		return fetchPotentialTransactionIfExisting(stockTransactionsForPut, minute);
	}

	private StockTransaction fetchPotentialTransactionIfExisting(List<PotentialTransaction> transactions,
			HistoricalPriceForMinute minute) {
		for (PotentialTransaction pt : transactions) {
			if (pt.minute == minute) {
				Transaction t = pt.t;
				if (t instanceof StockTransaction) {
					return (StockTransaction) t;
				}
			}
		}
		return null;

	}

}
