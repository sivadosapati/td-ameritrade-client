package com.rise.trading.options;

import com.studerw.tda.model.account.Position;

public class Spread {
	public String putOrCall;
	public String ticker;
	public String date;
	public OptionData longPosition;
	public OptionData shortPosition;
	public int quantity;

	public String toSpreadString() {
		return shortPosition.getOptionSymbol() + "[" + shortPosition.getAbsoluteQuantity() + "]:"
				+ longPosition.getOptionSymbol() + "[" + longPosition.getAbsoluteQuantity() + "]:" + quantity + " : "
				+ toPriceInfo();
	}

	public double getSpreadDistance() {
		return Math.abs(longPosition.getPrice().doubleValue() - shortPosition.getPrice().doubleValue());
	}

	public boolean isSpreadReturnGreaterThan(int percentage) {
		Position lp = longPosition.getPosition();
		Position sp = shortPosition.getPosition();
		double lpAvg = lp.getAveragePrice().doubleValue();
		double spAvg = sp.getAveragePrice().doubleValue();
		double lpMarket = lp.getMarketValue().doubleValue() / (100 * longPosition.getAbsoluteQuantity());
		double spMarket = Math.abs(sp.getMarketValue().doubleValue() / (100 * shortPosition.getAbsoluteQuantity()));
		lpAvg = Util.rnd(lpAvg);
		spAvg = Util.rnd(spAvg);
		lpMarket = Util.rnd(lpMarket);
		spMarket = Util.rnd(spMarket);
		double purchaseCredit = spAvg - lpAvg;
		double marketCredit = spMarket - lpMarket;
		double totalReturn = (purchaseCredit - marketCredit) * quantity * 100;
		// Return should include commission price for each contract, normally it is 65
		// cents per contract and closing a spread means 4 contracts for buying and
		// selling a spread
		double expectedReturn = quantity * (0.65 * 4 + (percentage * getSpreadDistance()));
		if (totalReturn > expectedReturn) {
			return true;
		}
		return false;
	}

	public String toPriceInfo() {
		Position lp = longPosition.getPosition();
		Position sp = shortPosition.getPosition();

		double lpAvg = lp.getAveragePrice().doubleValue();
		double spAvg = sp.getAveragePrice().doubleValue();
		double lpMarket = lp.getMarketValue().doubleValue() / (100 * longPosition.getAbsoluteQuantity());
		double spMarket = Math.abs(sp.getMarketValue().doubleValue() / (100 * shortPosition.getAbsoluteQuantity()));
		lpAvg = Util.rnd(lpAvg);
		spAvg = Util.rnd(spAvg);
		lpMarket = Util.rnd(lpMarket);
		spMarket = Util.rnd(spMarket);

		double purchaseCredit = spAvg - lpAvg;
		double marketCredit = spMarket - lpMarket;
		double percentageReturn = (purchaseCredit - marketCredit) * 100 / purchaseCredit;
		percentageReturn = Util.rnd(percentageReturn);
		purchaseCredit = Util.rnd(purchaseCredit);
		marketCredit = Util.rnd(marketCredit);

		double totalReturn = (purchaseCredit - marketCredit) * quantity * 100;
		double expectedReturn = quantity * (0.5 * 4 + 100 * 0.05 * getSpreadDistance());
		return "Long: [" + lpAvg + ":" + lpMarket + "] -> Short[" + spAvg + ":" + spMarket + "] -> [PC,MC = PR : "
				+ purchaseCredit + "," + marketCredit + " = " + percentageReturn + "] : [ER,TR]=[" + expectedReturn
				+ "," + totalReturn + "]";

	}

	public Spread(OptionData od) {
		this.putOrCall = od.getPutOrCall();
		this.ticker = od.getStockTicker();
		this.date = od.getDate();
		this.quantity = od.getAbsoluteQuantity();
		setOptionData(od);
	}

	public boolean isMatched() {
		if (longPosition == null) {
			return false;
		}
		if (shortPosition == null) {
			return false;
		}
		int x = longPosition.getAbsoluteQuantity();
		int y = shortPosition.getAbsoluteQuantity();
		if (x == y) {
			return true;
		}
		if (quantity == Math.min(x, y)) {
			return true;
		}
		return false;
	}

	public void setLongPosition(OptionData od) {
		this.longPosition = od;
		findQuantity();

	}

	public void setShortPosition(OptionData od) {
		this.shortPosition = od;
		findQuantity();
	}

	private void findQuantity() {
		if (longPosition != null && shortPosition != null) {
			quantity = Math.min(longPosition.getAbsoluteQuantity(), shortPosition.getAbsoluteQuantity());
		}
	}

	public void setOptionData(OptionData od) {
		Position p = od.getPosition();
		// System.out.println("Position -> "+Util.toJSON(p));
		if (p == null)
			return;

		if (od.isLong()) {
			setLongPosition(od);
		} else {
			setShortPosition(od);
		}

	}
}
