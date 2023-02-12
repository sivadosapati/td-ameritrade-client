package com.rise.trading.options.passive.income.backtest;

public class PassiveIncomeInput {
	public double optionPercentagePremium = 0.002;
	public double strikePriceIncrementOrDecrement = 1;
	public double initialStockEntryDistanceFromStrikePrice = -10;//1
	public double stockExitGain = 10;//1
	public double stockExitLoss = 5;//0.25

	public double distanceForEntryStockPurchaseAfterSelling = 10;//0.25
	public double distanceForExitOnGainAfterStockPurchase = 10;//1.25
	public double distanceForExitOnLossAfterStockPurchase = 5;//0
	
	public double spreadDistance = 5;
	
}
