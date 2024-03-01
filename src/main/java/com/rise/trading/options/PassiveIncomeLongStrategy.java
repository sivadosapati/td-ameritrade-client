package com.rise.trading.options;

import com.rise.trading.options.PassiveIncomeStrategy.Ticker;
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
	protected void openNewOptionPositionForSellCallOrPut(String accountId, Ticker ticker, int shortQuantity,
			OptionInstrument.PutCall opc) {
		if( opc == OptionInstrument.PutCall.CALL) {
			opc = OptionInstrument.PutCall.PUT;
		}
		else {
			opc = OptionInstrument.PutCall.CALL;
		}
		super.openNewOptionPositionForSellCallOrPut(accountId, ticker, shortQuantity, opc);
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
		if( Util.isTodayFriday()) {
			return true;
		}
	
		return super.shouldPlaceTradeForNextDayOrWeekOptions(ticker);
	}
}
