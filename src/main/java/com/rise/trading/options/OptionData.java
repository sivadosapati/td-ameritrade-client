package com.rise.trading.options;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.TreeSet;

import org.joda.time.Days;

import com.studerw.tda.model.account.OptionInstrument;
import com.studerw.tda.model.account.OptionInstrument.PutCall;

public class OptionData {
	public String stockTicker;
	public String date;
	public String putOrCall;
	public BigDecimal price;

	public String adjacentHigherSymbol;
	public String adjacentLowerSymbol;

	int quantity;
	public String symbol;
	
	public static void main(String args[]) {
		OptionData od = OptionSymbolParser.parse("SPY_031924P210");
		System.out.println(od.getDaysToExpiry());
	}

	public String getStockTicker() {
		return stockTicker;
	}

	public String getDate() {
		return date;
	}

	public String getKeyWithoutStrikePrice() {
		OptionData x = this;
		return x.stockTicker + "_" + x.date + x.putOrCall;
	}

	public String getOppositeKeyWithoutStrikePrice() {
		OptionData x = this;
		return x.stockTicker + "_" + x.date + getReversePutOrCall();
	}

	// If Quantity is negative, then it is short position else, it's a long position

	public String getAdjacentHigherSymbol() {
		return adjacentHigherSymbol;
	}

	public void setAdjacentHigherSymbol(String adjacentHigherSymbol) {
		this.adjacentHigherSymbol = adjacentHigherSymbol;
	}

	public String getAdjacentLowerSymbol() {
		return adjacentLowerSymbol;
	}

	public void setAdjacentLowerSymbol(String adjacentLowerSymbol) {
		this.adjacentLowerSymbol = adjacentLowerSymbol;
	}

	public void setQuantity(int q) {
		this.quantity = q;
	}

	public LocalDateTime getDateTime() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMddyy");
		LocalDate dateTime = LocalDate.parse(date, formatter);
		return dateTime.atStartOfDay();
	}

	public boolean isPut() {
		if (putOrCall.equals("P")) {
			return true;
		}
		return false;
	}

	public boolean isCall() {
		if (putOrCall.equals("C")) {
			return true;
		}
		return false;
	}

	public void swapPutOrCall() {
		if (isCall()) {
			putOrCall = "P";
		} else {
			putOrCall = "C";
		}
	}

	public String getPutOrCall() {
		return this.putOrCall;
	}

	public String getReversePutOrCall() {
		if (this.putOrCall.equals("P")) {
			return "C";
		}
		return "P";
	}

	public String getPutOrCall(boolean isCall) {
		if (isCall) {
			return "C";
		}
		return "P";
	}

	public int getQuantity() {
		return quantity;
	}

	public OptionData(String stockTicker, String date, String putOrCall, BigDecimal price) {
		this.stockTicker = stockTicker;
		this.date = date;
		this.putOrCall = putOrCall;
		this.price = price;
	}

	public void setPrice(double price) {
		this.price = new BigDecimal(price);
	}

	public void adjustPriceForNextOption(double stockPrice) {
		if (isCall()) {
			if (stockPrice > price.doubleValue()) {
				this.price = new BigDecimal(stockPrice);
			}
		} else {
			if (stockPrice < price.doubleValue()) {
				this.price = new BigDecimal(stockPrice);
			}
		}
	}

	public String getAdjacentHigherOption(double increment) {
		return convert(price.doubleValue() + increment);
	}

	public String getAdjacentLowerOption(double decrement) {
		return convert(price.doubleValue() - decrement);
	}

	public String createOptionSymbolForPrice(double x) {
		if (isCall()) {
			x = Math.ceil(x);
		} else {
			x = Math.floor(x);
		}
		return convert(x);
	}

	private String convert(double x) {
		return stockTicker + "_" + date + putOrCall + Util.convertDecimalToString(x);
	}

	public String toString() {
		return convert(price.doubleValue()) + " : " + quantity;
	}

	public String getOptionSymbol() {
		return convert(this.getPrice().doubleValue());
	}

	public PutCall getPutCall() {
		if (isCall()) {
			return OptionInstrument.PutCall.CALL;
		}
		return OptionInstrument.PutCall.PUT;
	}

	public String makePossibleProtectionLongOption(Double currentStockPrice, double percentageDeviation) {
		Double dp =  findPickableStockPrice(currentStockPrice, percentageDeviation);
		double p = dp.doubleValue();
		String s = convert(p);
		return s;

	}

	private Double findPickableStockPrice(double currentStockPrice, double percentageDeviation) {
		return Util.findPickableStockPrice(currentStockPrice, this.price.doubleValue(), isCall(), percentageDeviation);
	}

	public void adjustPriceFromPricesMap(TreeSet<Double> prices) {
		Double p = null;
		if (isCall()) {
			p = prices.last();
		} else {
			p = prices.first();
		}
		this.price = new BigDecimal(p);

	}

	public BigDecimal getPrice() {
		return price;
	}

	public boolean isExpiringToday() {
		LocalDate ld = LocalDate.now();
		boolean b = getDateTime().toLocalDate().equals(ld);
		return b;
	}

	public int getDaysToExpiry() {
		LocalDate ld = LocalDate.now();
		LocalDate exp = getDateTime().toLocalDate();
		int daysBetween = (int)ChronoUnit.DAYS.between(ld, exp);
		return daysBetween;
	}
}