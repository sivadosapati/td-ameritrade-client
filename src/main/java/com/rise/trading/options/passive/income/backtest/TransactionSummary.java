package com.rise.trading.options.passive.income.backtest;

public class TransactionSummary {
	public String ticker;
	public String date;
	public double open;
	public double close;
	public int callTransactions;
	public double callGainOrLossFromStocks;
	public int putTransactions;
	public double putGainOrLossFromStocks;
	public double callStrikePrice;
	public double putStrikePrice;
	public double potentialCallOptionGainOrLoss;
	public double potentialPutOptionGainOrLoss;
	public double totalGainOrLoss;

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