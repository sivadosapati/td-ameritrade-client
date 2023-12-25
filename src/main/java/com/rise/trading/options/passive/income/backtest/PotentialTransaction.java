package com.rise.trading.options.passive.income.backtest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;

import com.rise.trading.options.BaseObject;
import com.rise.trading.options.ToJSONString;

public class PotentialTransaction extends BaseObject implements ToJSONString {
	public HistoricalPriceForMinute minute;
	public Transaction t;
	//private Date d;

}

enum OptionTransactionType {
	SELL_CALL, SELL_PUT, BUY_CALL, BUY_PUT;
}

enum StockTransactionType {
	BUY_STOCK, SELL_TO_CLOSE_STOCK, SELL_SHORT_STOCK, BUY_TO_COVER_STOCK;
}

interface Transaction {
}

class StockTransaction extends BaseObject implements ToJSONString, Transaction {
	public StockTransactionType type;
	public Double stockPrice;
	public HistoricalPriceForMinute minute;

}

class OptionTransaction extends BaseObject implements Transaction {
	OptionTransactionType type;
	double strikePrice;
	double potentialOptionValue;

	Stack<StockEntry> stockEntriesForCall = new Stack<StockEntry>();
	Collection<StockTransaction> potentialStockTransactionsForCall = new ArrayList<StockTransaction>();

	Stack<StockEntry> stockEntriesForPut = new Stack<StockEntry>();
	Collection<StockTransaction> potentialStockTransactionsForPut = new ArrayList<StockTransaction>();

	public HistoricalPriceForMinute minute;

	public StockEntry getPotentialStockEntryForCall() {
		return stockEntriesForCall.peek();
	}

	public void addPotentialStockEntryForCall(double entry, double gain, double loss) {
		stockEntriesForCall.push(new StockEntry(entry, gain, loss));
	}

	public void addStockTransactionForCall(StockTransaction st) {
		potentialStockTransactionsForCall.add(st);
	}

	public StockEntry getPotentialStockEntryForPut() {
		return stockEntriesForPut.peek();
	}

	public void addPotentialStockEntryForPut(double entry, double gain, double loss) {
		stockEntriesForPut.push(new StockEntry(entry, gain, loss));
	}

	public void addStockTransactionForPut(StockTransaction st) {
		potentialStockTransactionsForPut.add(st);
	}
}

class StockEntry extends BaseObject implements ToJSONString {
	public double entry;
	public double gain;
	public double loss;

	public StockEntry(double entry, double gain, double loss) {
		super();
		this.entry = entry;
		this.gain = gain;
		this.loss = loss;
	}

}