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
	public Double callStrikePrice;
	public Double putStrikePrice;
	public Double callGainOrLossFromStocks = 0d;
	public Double putGainOrLossFromStocks = 0d;

	public Double potentialCallOptionGainOrLoss = 0d;
	public Double potentialPutOptionGainOrLoss = 0d;
	public Double totalGainOrLoss;



	public Integer callTransactions = 0;
	public Integer putTransactions = 0;

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