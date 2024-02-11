package com.rise.trading.options.passive.income.timers;

import com.rise.trading.options.PassiveIncomeStrategy;

public class PassiveIncomeTaskImplementation implements PassiveIncomeTask {

	private PassiveIncomeStrategy strategy = new PassiveIncomeStrategy();

	@Override
	public void placeOrderAtMarketOpen(PassiveIncomeInput input) {
		String code = input.getTicker();
		PassiveIncomeStrategy.Ticker ticker = PassiveIncomeStrategy.Ticker.make(code);
		strategy.placeDailyTradeForPassiveIncome(input.getAccountId(), ticker, input.getStrikeDistanceForCall(),
				input.getStrikeDistanceForPut(), input.getContracts());
		strategy.placeClosingTradesForOptionsOnDailyExpiringOptions(input.getAccountId(), input.getTicker());
	}

	@Override
	public void placeOrderAtMarketClose(PassiveIncomeInput input) {
		String code = input.getTicker();
		PassiveIncomeStrategy.Ticker ticker = PassiveIncomeStrategy.Ticker.make(code);
		strategy.placeNextDayTradeForPassiveIncome(input.getAccountId(), ticker,
				input.getStrikeDistanceForCall(), input.getStrikeDistanceForPut(), input.getContracts());

	}

}
