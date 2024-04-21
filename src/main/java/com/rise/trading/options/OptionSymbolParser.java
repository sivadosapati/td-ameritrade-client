package com.rise.trading.options;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.studerw.tda.model.account.OptionInstrument;
import com.studerw.tda.model.account.Position;

public class OptionSymbolParser {
	public static void main(String[] args) {
		String optionSymbol = "UNG1_021624C7";
		OptionData optionData = parse(optionSymbol);
		System.out.println("Stock Ticker: " + optionData.stockTicker);
		System.out.println("Date: " + optionData.date);
		System.out.println("Put or Call: " + optionData.putOrCall);
		System.out.println("Price: " + optionData.price);
	}

	public static OptionData parse(Position p) {
		String symbol = ((OptionInstrument) p.getInstrument()).getSymbol();
		OptionData od = parse(symbol);
		od.setPosition(p);
		int x = p.getLongQuantity().intValue();
		if (x != 0) {
			od.setQuantity(x);

		} else {
			od.setQuantity(-1 * p.getShortQuantity().intValue());
		}
		return od;
	}

	public static OptionData parse(String symbol) {
		// Define the regex pattern for parsing the option symbol
		// String regex = "([A-Z]+)_(\\d{6})([PC])([0-9]+\\.[0-9]+)";
		// String regex = "([A-Z]+)_(\\d{6})([PC])(\\d+(?:\\.\\d+)?)";
		String regex = ("([A-Za-z0-9]+)_(\\d{6})([CP])(\\d+\\.?\\d*)");

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(symbol);

		// Extract components from the symbol using regex groups
		if (matcher.matches()) {
			String stockTicker = matcher.group(1);
			String date = matcher.group(2);
			String putOrCall = matcher.group(3);
			BigDecimal price = new BigDecimal(matcher.group(4));

			OptionData od = new OptionData(stockTicker, date, putOrCall, price);

			od.symbol = symbol;
			return od;
		} else {
			throw new IllegalArgumentException("Invalid option symbol format: " + symbol);
		}
	}

}
