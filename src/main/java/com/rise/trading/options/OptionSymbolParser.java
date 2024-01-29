package com.rise.trading.options;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OptionSymbolParser {
    public static void main(String[] args) {
        String optionSymbol = "IWM_122923C205.67";
        OptionData optionData = parse(optionSymbol);
        System.out.println("Stock Ticker: " + optionData.stockTicker);
        System.out.println("Date: " + optionData.date);
        System.out.println("Put or Call: " + optionData.putOrCall);
        System.out.println("Price: " + optionData.price);
    }

    public static OptionData parse(String symbol) {
        // Define the regex pattern for parsing the option symbol
        //String regex = "([A-Z]+)_(\\d{6})([PC])([0-9]+\\.[0-9]+)";
        String regex = "([A-Z]+)_(\\d{6})([PC])(\\d+(?:\\.\\d+)?)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(symbol);

        // Extract components from the symbol using regex groups
        if (matcher.matches()) {
            String stockTicker = matcher.group(1);
            String date = matcher.group(2);
            String putOrCall = matcher.group(3);
            BigDecimal price = new BigDecimal(matcher.group(4));

            return new OptionData(stockTicker, date, putOrCall, price);
        } else {
            throw new IllegalArgumentException("Invalid option symbol format: " + symbol);
        }
    }

}
