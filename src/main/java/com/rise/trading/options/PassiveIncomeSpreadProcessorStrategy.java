package com.rise.trading.options;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.studerw.tda.model.account.OptionInstrument;
import com.studerw.tda.model.account.Order;
import com.studerw.tda.model.account.OrderLegCollection;
import com.studerw.tda.model.account.OrderType;
import com.studerw.tda.model.account.Position;

public class PassiveIncomeSpreadProcessorStrategy extends PassiveIncomeStrategy {

	public boolean processSpreads(ProcessSpreadsInput input) {

		SpreadsProcessor sp = new SpreadsProcessor();
		Spreads spreads = sp.makeSpreads(input.getGroupedPostion());
		if (spreads.isEmpty()) {
			return false;
		}
		boolean processSpreadsStatus = false;
		for (Spread s : spreads.getSpreads()) {
			if (s.isSpreadReturnGreaterThan(5)) {
				// System.out.println(s.toSpreadString());
				closeAndOpenNewSpread(s, spreads, input);
				processSpreadsStatus = true;
			}
			Spread x = spreads.getAlternateSpreadIfExisting(s);
			if (x == null) {
				System.out.println("Create Alternate Spread -> " + s.toSpreadString());
				createAlternateSpread(s, spreads, input);
				processSpreadsStatus = true;
			} else {
				// System.out.println("Alternate Spread exists for -> "+s.toSpreadString()+" ->
				// "+x.toSpreadString());
			}
		}
		return processSpreadsStatus;

	}

	private void createAlternateSpread(Spread s, Spreads ss, ProcessSpreadsInput input) {
		double currentStockPrice = input.getGroupedPostion().getCurrentStockPrice();
		int quantity = s.getQuantity();
		LocalDateTime date = s.getLocalDateTime();
		Ticker ticker = Ticker.make(input.getStockTicker());
		ticker.setOptionSpreadDistance(0.5f);
		ticker.setPrice(new BigDecimal(s.findPriceForAlternateSpread(currentStockPrice)));
		Float callDistance = s.isCall() ? null : 2f;
		Float putDistance = s.isPut() ? null : 2f;
		Order o = getWorkingOrderForSpreadIfPresent(input.getCurrentWorkingOrders(), ticker, quantity,
				s.isPut() ? OptionInstrument.PutCall.CALL : OptionInstrument.PutCall.PUT, OrderType.NET_CREDIT,
				OrderLegCollection.Instruction.SELL_TO_OPEN);
		if (o != null) {
			System.out.println("Cancelling an existing working order -> " + Util.toJSON(o));
			getClient().cancelOrder(input.getAccountId(), o.getOrderId() + "");
		}
		int counter = 0;
		while (true) {
			OptionTrade ot = placeOptionTradesForPassiveIncome(input.getAccountId(), ticker, callDistance, putDistance,
					quantity, date, date);
			if (ot.isTradeSuccessful()) {
				break;
			}
			if(counter >= 2) {
				System.out.println("Tried placing spread 3 times with different long options but didn't work for -> "+s.toSpreadString());
				break;
			}
			ticker.setOptionSpreadDistance(ticker.getOptionSpreadDistance() + 0.5f);
			if( callDistance!=null) {
				callDistance = callDistance--;
			}
			if( putDistance!=null) {
				putDistance--;
			}
			counter++;
		}
	}

	private void closeAndOpenNewSpread(Spread s, Spreads ss, ProcessSpreadsInput input) {
		Position sp = s.getShortPosition();
		Position lp = s.getLongPosition();
		// System.out.println(sp.getShortQuantity() + " -> " + sp.getLongQuantity() + "
		// -> " + s.getQuantity());
		// System.out.println(lp.getShortQuantity() + " -> " + lp.getLongQuantity() + "
		// -> " + s.getQuantity());
		adjustInputWithOverrides(input, sp, s.getQuantity());
		closeOptionPositionAtMarketPrice(input.getAccountId(), s.getShortPosition(), input);
		adjustInputWithOverrides(input, lp, s.getQuantity());
		closeOptionPositionAtMarketPrice(input.getAccountId(), s.getLongPosition(), input);
		input.setOverrideLongQuantity(null);
		input.setOverrideShortQuantity(null);
	}

	private void adjustInputWithOverrides(ProcessSpreadsInput input, Position sp, int quantity) {
		input.setOverrideLongQuantity(null);
		input.setOverrideShortQuantity(null);
		int lq = sp.getLongQuantity().intValue();
		if (lq != 0) {
			if (lq != quantity) {
				input.setOverrideLongQuantity(quantity);
			}
		}
		int sq = sp.getShortQuantity().intValue();
		if (sq != 0) {
			if (sq != quantity) {
				input.setOverrideShortQuantity(quantity);
			}
		}

	}

}
