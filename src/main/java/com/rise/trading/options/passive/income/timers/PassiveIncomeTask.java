package com.rise.trading.options.passive.income.timers;

public interface PassiveIncomeTask {
	void placeOrderAtMarketOpen(PassiveIncomeInput input);
	void placeOrderAtMarketClose(PassiveIncomeInput input);

}
