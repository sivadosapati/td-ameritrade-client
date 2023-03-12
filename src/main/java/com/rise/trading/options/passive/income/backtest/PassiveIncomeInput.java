package com.rise.trading.options.passive.income.backtest;

public class PassiveIncomeInput {
	public double optionPercentagePremium = 0.002;
	public double strikePriceIncrementOrDecrement = 1;
	public double initialStockEntryDistanceFromStrikePrice = 1;//-10
	public double stockExitGain = 1;//1
	public double stockExitLoss = 0.25;//0.25

	public double distanceForEntryStockPurchaseAfterSelling = 0.25;//0.25
	public double distanceForExitOnGainAfterStockPurchase = 1.25;//1.25
	public double distanceForExitOnLossAfterStockPurchase = 0;//0
	
	public double spreadDistance = 5;
	
}
