package com.rise.trading.options;

import java.time.LocalDateTime;

import com.studerw.tda.model.account.Position;

public class Spread {
	public String getPutOrCall() {
		return putOrCall;
	}

	public void setPutOrCall(String putOrCall) {
		this.putOrCall = putOrCall;
	}

	public String getTicker() {
		return ticker;
	}

	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	public String getDate() {
		return date;
	}

	public LocalDateTime getLocalDateTime() {
		return shortPosition.getDateTime();
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	private String putOrCall;
	private String ticker;
	private String date;
	private OptionData longPosition;
	private OptionData shortPosition;
	private int quantity;

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

	public int getQuantity() {
		return quantity;
	}

	public Position getShortPosition() {
		return shortPosition.getPosition();
	}

	public Position getLongPosition() {
		return longPosition.getPosition();
	}

	public boolean matchesAlternateSpread(Spread spread) {
		if (spread == null) {
			return false;
		}
		if (!this.getTicker().equals(spread.getTicker())) {
			return false;
		}
		if (!this.getDate().equals(spread.getDate())) {
			return false;
		}
		if (this.getPutOrCall().equals(spread.getPutOrCall())) {
			return false;
		}

		return true;
	}

	public double getStrikePriceForShortOption() {
		return shortPosition.getPrice().doubleValue();
	}

	public double getStrikePriceForLongOption() {
		return longPosition.getPrice().doubleValue();
	}

	public boolean isCall() {
		return shortPosition.isCall();
	}

	public boolean isPut() {
		return shortPosition.isPut();
	}

	public double findPriceForAlternateSpread(double currentStockPrice) {
		double shortPrice = this.getStrikePriceForShortOption();
		if (isCall()) {
			if (currentStockPrice < shortPrice) {
				return currentStockPrice;
			}
			else {
				return shortPrice;
			}
		}
		if(isPut()) {
			if(currentStockPrice > shortPrice) {
				return currentStockPrice;
			}
			return shortPrice;
		}
		throw new RuntimeException("Unreachable code");
	}
}
