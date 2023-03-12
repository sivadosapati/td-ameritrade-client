package com.rise.trading.options.passive.income.backtest;

import java.time.LocalDate;

public class TransactionSummary {
	public Integer id;
	public String ticker;
	public LocalDate date;
	public Double open;
	public Double low;
	public Double high;
	public Double close;
	public Integer callTransactions;
	public Double callGainOrLossFromStocks;
	public Integer putTransactions;
	public Double putGainOrLossFromStocks;
	public Double callStrikePrice;
	public Double putStrikePrice;
	public Double potentialCallOptionGainOrLoss;
	public Double potentialPutOptionGainOrLoss;
	public Double totalGainOrLoss;

	public static String getHeader() {
		return "ticker,date,open,close,callTransactions,callGainOrLossFromStocks,callStrikePrice,potentialCallOptionGainOrLoss,putTransactions,putGainOrLossFromStocks,putStrikePrice,potentialPutOptionGainOrLoss,totalGainOrLoss";
	}

	public String toString() {
		return ticker + "," + date + "," + open + "," + close + "," + callTransactions + "," + callGainOrLossFromStocks
				+ "," + callStrikePrice + "," + potentialCallOptionGainOrLoss + "," + putTransactions + ","
				+ putGainOrLossFromStocks + "," + putStrikePrice + "," + potentialPutOptionGainOrLoss + ","
				+ totalGainOrLoss;

	}

	public double calculateTotalGainOrLoss() {
		return callGainOrLossFromStocks + putGainOrLossFromStocks + potentialCallOptionGainOrLoss
				+ potentialPutOptionGainOrLoss;
	}
}