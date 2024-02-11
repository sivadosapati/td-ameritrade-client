package com.rise.trading.options;

import java.math.BigDecimal;

public class OptionData {
	String stockTicker;
	String date;
	String putOrCall;
	BigDecimal price;

	public OptionData(String stockTicker, String date, String putOrCall, BigDecimal price) {
		this.stockTicker = stockTicker;
		this.date = date;
		this.putOrCall = putOrCall;
		this.price = price;
	}
	
	public void setPrice(double price) {
		this.price = new BigDecimal(price);
	}

	public String getAdjacentHigherOption(double increment) {
		return convert(price.doubleValue() + increment);
	}

	public String getAdjacentLowerOption(double decrement) {
		return convert(price.doubleValue() - decrement);
	}

	private String convert(double x) {
		return stockTicker + "_" + date + putOrCall + convertDecimalToString(x);
	}

	public static String convertDecimalToString(double number) {
		// Check if the number has no decimal part
		if (number == (int) number) {
			return String.valueOf((int) number);
		} else {
			return String.valueOf(number);
		}
	}
}