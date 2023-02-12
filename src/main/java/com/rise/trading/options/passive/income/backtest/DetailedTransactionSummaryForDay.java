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

}
