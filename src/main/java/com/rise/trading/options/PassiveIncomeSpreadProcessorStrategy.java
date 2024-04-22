package com.rise.trading.options;

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
				System.out.println(s.toSpreadString());
				closeAndOpenNewSpread(s, spreads, input);
				processSpreadsStatus = true;
			}
		}
		return processSpreadsStatus;

	}

	private void closeAndOpenNewSpread(Spread s, Spreads ss, ProcessSpreadsInput input) {

		Position sp = s.getShortPosition();
		Position lp = s.getLongPosition();
		System.out.println(sp.getShortQuantity() + " -> " + sp.getLongQuantity() + " -> " + s.getQuantity());
		System.out.println(lp.getShortQuantity() + " -> " + lp.getLongQuantity() + " -> " + s.getQuantity());
		adjustInputWithOverrides(input, sp, s.getQuantity());
		// closeOptionPositionAtMarketPrice(input.getAccountId(), s.getShortPosition(),
		// input);
		adjustInputWithOverrides(input, lp, s.getQuantity());
		// closeOptionPositionAtMarketPrice(input.getAccountId(), s.getLongPosition(),
		// input);
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
