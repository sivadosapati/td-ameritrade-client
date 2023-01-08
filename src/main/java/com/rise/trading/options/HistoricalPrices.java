package com.rise.trading.options;

import java.util.Date;

import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.model.history.Candle;
import com.studerw.tda.model.history.FrequencyType;
import com.studerw.tda.model.history.PriceHistReq;
import com.studerw.tda.model.history.PriceHistReq.Builder;
import com.studerw.tda.model.history.PriceHistory;

public class HistoricalPrices {

	private HttpTdaClient httpTdaClient = Util.getHttpTDAClient();

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		HistoricalPrices hp = new HistoricalPrices();
		hp.fetchHistoricalPrices("QQQ");

	}

	public void fetchHistoricalPrices(String symbol) {
		long now = System.currentTimeMillis();
		PriceHistReq request = Builder.priceHistReq().withSymbol(symbol)
				//.withStartDate(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 1))
				//.withEndDate(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 6))
				.withFrequencyType(FrequencyType.minute).withFrequency(1).build();

		PriceHistory priceHistory = httpTdaClient.priceHistory(request);
		System.out.println(priceHistory.getCandles().size());
		for (Candle c : priceHistory.getCandles()) {

			System.out.println(new Date(c.getDatetime()) + " : " + c.getOpen() + " : " + c.getLow() + " : "
					+ c.getHigh() + " : " + c.getClose());
		}
		/*
		 * assertThat(priceHistory).isNotNull();
		 * assertThat(priceHistory.getCandles().size()).isGreaterThan(1);
		 * assertThat(priceHistory.getSymbol()).isEqualTo("VGIAX");
		 * assertThat(priceHistory.isEmpty()).isFalse();
		 * LOGGER.debug(priceHistory.toString());
		 * 
		 * Candle candle = priceHistory.getCandles().get(0);
		 * LOGGER.debug(candle.toString());
		 * assertThat(candle.getOpen()).isGreaterThan(new BigDecimal("1.00"));
		 * assertThat(candle.getHigh()).isGreaterThan(new BigDecimal("1.00"));
		 * assertThat(candle.getLow()).isGreaterThan(new BigDecimal("1.00"));
		 * assertThat(candle.getClose()).isGreaterThan(new BigDecimal("1.00")); //should
		 * not have taken more than 20 seconds assertThat(candle.getDatetime() -
		 * now).isLessThan(20000);
		 */

	}

}
