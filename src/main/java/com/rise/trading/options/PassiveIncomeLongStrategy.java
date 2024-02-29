package com.rise.trading.options;

import com.studerw.tda.model.account.OptionInstrument;
import com.studerw.tda.model.account.Position;

public class PassiveIncomeLongStrategy extends PassiveIncomeStrategy {

	@Override
	public int getAdjacentWidthForHigherOption() {
		return 5;
	}

	@Override
	public int getAdjacentWidthForLowerOption() {
		return 5;
	}

	@Override
	protected double getPotentialProfitMarketValueForLongPositions(int longQuantity, Position p) {
		return longQuantity * p.getAveragePrice().doubleValue() * 1.5 * 100;
	}

	@Override
	protected double getPotentialProfitValueForShortPosition(int shortQuantity, Position p) {
		return -1 * shortQuantity * p.getAveragePrice().doubleValue() * 0.8 * 100;
	}

	@Override
	public void placeNextLongOptionOrder(String accountId, OptionInstrument oi, int shortQuantity, int longQuantity,
			PossibleOptionAndProfit pop) {
		pop.od.swapPutOrCall();
		super.placeNextLongOptionOrder(accountId, oi, shortQuantity, longQuantity, pop);

	}

	@Override
	public void closeOptionIfInProfitAndPotentiallyOpenNewOne(PassiveIncomeOptionProcessorInput input) {
		System.out.println(this.getClass().getName() + " -> " + input.ticker + " -> " + input.accountId);
		super.closeOptionIfInProfitAndPotentiallyOpenNewOne(input);
	}

	@Override
	public boolean shouldOptionsBeRolled(String ticker) {
		return Util.isLastFewMinutesOfMarketHours(ticker);
	}

	@Override
	protected boolean shouldPlaceTradeForNextDayOrWeekOptions(Ticker ticker) {
		return Util.isLastHourOfTrading();
	}
}
