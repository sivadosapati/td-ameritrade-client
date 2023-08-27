package com.rise.trading.options.algos;

import java.math.BigDecimal;

import com.rise.trading.options.OrderHandler;
import com.rise.trading.options.Util;
import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.model.quote.EquityQuote;
import com.studerw.tda.model.quote.EtfQuote;

public class EveryDaySellCallAndPutTradingStrategy extends OrderHandler {

	public static void main(String[] args) {
		EveryDaySellCallAndPutTradingStrategy x = new EveryDaySellCallAndPutTradingStrategy();
		x.placeOrder(Util.getAccountId1(), "QQQ");

	}

	public void placeOrder(String accountId, String ticker) {
		HttpTdaClient client = getClient();
		EtfQuote quote = (EtfQuote) client.fetchQuote(ticker);
		// System.out.println(Util.toJSON(quote));
		// System.out.println(quote.getOtherFields().get("lastPrice"));
		// System.out.println(quote.getOtherFields());
		BigDecimal bd = quote.getLastPrice();
		double sellCallStrikePrice = Math.ceil(bd.doubleValue() + 1);
		double sellPutStrikePrice = Math.floor(bd.doubleValue() - 1);
		System.out.println(bd + " -> "+sellCallStrikePrice + " -> "+sellPutStrikePrice);
	}

}
