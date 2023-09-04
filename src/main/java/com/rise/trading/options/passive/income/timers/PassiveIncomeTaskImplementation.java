package com.rise.trading.options.passive.income.timers;

import com.rise.trading.options.PassiveIncomeStrategy;

public class PassiveIncomeTaskImplementation implements PassiveIncomeTask {
	
	private PassiveIncomeStrategy strategy = new PassiveIncomeStrategy();

	@Override
	public void placeOrderAtMarketOpen(PassiveIncomeInput input) {
		strategy.placeDailyTradeForPassiveIncome(input.getAccountId(), input.getTicker(), input.getStrikeDistanceForCall(), input.getStrikeDistanceForPut(), input.getContracts());
		strategy.placeClosingTradesForOptionsOnDailyExpiringOptions(input.getAccountId(), input.getTicker());
	}

	@Override
	public void placeOrderAtMarketClose(PassiveIncomeInput input) {
		strategy.placeNextDayTradeForPassiveIncome(input.getAccountId(), input.getTicker(), input.getStrikeDistanceForCall(), input.getStrikeDistanceForPut(), input.getContracts());

	}

}
