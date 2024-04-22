package com.rise.trading.options;

public interface PassiveIncomeInput {
	String getAccountId();
	String getStockTicker();
	
	Integer getOverrideLongQuantity();
	Integer getOverrideShortQuantity();
	
}
