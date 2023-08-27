package com.rise.trading.options.passive.income.backtest;

public class LongAndShortEveryDayTransactionSummary extends TransactionSummary {

	public Double longStockStartOfTheDay;
	public Double longStockEndOfTheDay = 0d;
	public Double shortStockStartOfTheDay;
	public Double shortStockEndOfTheDay = 0d;
	public Double longStockQuantity = 0d;
	public Double longStockQuantityEndOfTheDay = 0d;
	public Double shortStockQuantity = 0d;
	public Double shortStockQuantityEndOfTheDay = 0d;
	public Double cumulativeGainOrLoss = 0d;
	public Double potentialGainOrLossOnStocksWhenLiquidated = 0d;


	public void calculatePotentialGainOrLossOnStocksIfLiquidatedEndOfDay() {
		double x = 0;
		if (longStockEndOfTheDay > 0) {
			x = close * longStockQuantityEndOfTheDay - longStockEndOfTheDay;
		}
		if (shortStockEndOfTheDay < 0) {
			x += -1 * (close * shortStockQuantityEndOfTheDay + shortStockEndOfTheDay);
		}
		potentialGainOrLossOnStocksWhenLiquidated = x;
	}

}
