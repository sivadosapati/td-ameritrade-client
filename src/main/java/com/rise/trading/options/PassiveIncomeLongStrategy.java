package com.rise.trading.options;

import com.studerw.tda.model.account.OptionInstrument;
import com.studerw.tda.model.account.OptionInstrument.PutCall;
import com.studerw.tda.model.account.Position;

public class PassiveIncomeLongStrategy extends PassiveIncomeStrategy {

	public static int MAX_LONG_POSITIONS = 4;

	public static int MAX_SHORT_POSITIONS = 6;

	@Override
	public int getAdjacentWidthForHigherOption(PassiveIncomeOptionProcessorInput input) {
		return 5;
	}

	@Override
	public int getAdjacentWidthForLowerOption(PassiveIncomeOptionProcessorInput input) {
		return 5;
	}

	@Override
	protected double getPotentialProfitMarketValueForLongPositions(int longQuantity, PassiveIncomeOptionProcessorInput input) {
		return super.getPotentialProfitMarketValueForLongPositions(longQuantity, input);
	}

	@Override
	protected double getPotentialProfitValueForShortPosition(int shortQuantity, PassiveIncomeOptionProcessorInput input) {
		Position p = input.position;
		return -1 * shortQuantity * p.getAveragePrice().doubleValue() * 0.8 * 100;
	}

	@Override
	public void placeNextLongOptionOrder(String accountId, OptionInstrument oi, int shortQuantity, int longQuantity,
			PossibleOptionAndProfit pop, PassiveIncomeOptionProcessorInput input) {
		pop.od.swapPutOrCall();
		int maxLongOptions = input.optionPositions.getOptionsWithNetShortAndLong(pop.od);
		if (maxLongOptions <= MAX_LONG_POSITIONS) {
			System.out.println("placeNextLongOptionOrder -> " + maxLongOptions);
			super.placeNextLongOptionOrder(accountId, oi, shortQuantity, longQuantity, pop, input);
		} else {
			System.out.println("placeNextLongOptionOrder -> Already reached MAX_LONG_POSITIONS -> " + maxLongOptions);
		}

	}

	@Override
	protected double longGain() {
		return 1.5d;
	}

	@Override
	protected void openNewOptionPositionForSellCallOrPut(String accountId, Ticker ticker, int shortQuantity,
			OptionInstrument.PutCall opc, PassiveIncomeOptionProcessorInput input) {
		if(ticker.isRollOptionsForNextDayOrWeek()) {
			super.openNewOptionPositionForSellCallOrPut(accountId, ticker, shortQuantity, opc, input);
			return;
		}
		super.openNewOptionPositionForSellCallOrPut(accountId, ticker, shortQuantity, opc, input);
		if (opc == OptionInstrument.PutCall.CALL) {
			opc = OptionInstrument.PutCall.PUT;
		} else {
			opc = OptionInstrument.PutCall.CALL;
		}
		OptionData od = input.getOptionData();
		od.swapPutOrCall();
		int maxShortOptions = input.optionPositions.getOptionsWithNetShortAndLong(od);
		if (maxShortOptions <= MAX_SHORT_POSITIONS) {
			System.out.println("openNewOptionPositionForSellCallOrPut -> " + maxShortOptions);
			placeNewOptionPositionForSellCallOrput(accountId, ticker, shortQuantity, opc, input);
			
		} else {
			System.out.println("openNewOptionPositionForSellCallOrPut -> Already reached MAX_SHORT_POSITIONS -> "
					+ maxShortOptions);
		}

	}

	private void placeNewOptionPositionForSellCallOrput(String accountId, Ticker ticker, int shortQuantity, PutCall opc,
			PassiveIncomeOptionProcessorInput input) {
		super.openNewOptionPositionForSellCallOrPut(accountId, ticker, shortQuantity, opc, input);
		
	}

	@Override
	public void closeOptionIfInProfitAndPotentiallyOpenNewOne(PassiveIncomeOptionProcessorInput input) {
		// System.out.println(this.getClass().getName() + " -> " + input.ticker + " -> "
		// + input.accountId);
		super.closeOptionIfInProfitAndPotentiallyOpenNewOne(input);
	}

	@Override
	public boolean shouldOptionsBeRolled(String ticker) {
		// return Util.isLastFewMinutesOfMarketHours(ticker);
		return super.shouldOptionsBeRolled(ticker);
	}

	@Override
	protected boolean shouldPlaceTradeForNextDayOrWeekOptions(Ticker ticker) {
		if (Util.isTodayFriday()) {
			return true;
		}

		return super.shouldPlaceTradeForNextDayOrWeekOptions(ticker);
	}

	@Override
	protected int getQuantityIncrement() {
		return 0;
	}
}
