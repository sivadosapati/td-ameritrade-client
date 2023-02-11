package com.rise.trading.options.passive.income.backtest;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PassiveIncomeBackwardTestStrategy {

	private PassiveIncomeInput input = new PassiveIncomeInput();

	public void setPassiveIncomeInput(PassiveIncomeInput input) {
		this.input = input;
	}

	public static void mainOld(String args[]) throws Exception {
		HistoricalPricesFetcher fetcher = new HistoricalPricesFetcher();
		String ticker = "QQQ";
		HistoricalPricesForMultipleDays md = fetcher.getHistoricalPrices(ticker);
		PassiveIncomeBackwardTestStrategy pis = new PassiveIncomeBackwardTestStrategy();// .processTransactions(day);
		// HistoricalPricesForDay day = md.historicalPricesMap.get("18-01-2023");
		// System.out.println(day.getPricesForOnlyHoursWhereOptionsAreTraded());

		System.setOut(new PrintStream(new FileOutputStream(ticker + ".csv")));
		System.out.println(TransactionSummary.getHeader());
		for (String key : md.historicalPricesMap.keySet()) {
			TransactionSummary summary = pis.processTransactions(md.historicalPricesMap.get(key));
			if (summary == null) {
				continue;
			}
			System.out.println(summary.toString());
			// break;
		}

	}

	public static void main(String x[]) {
		PassiveIncomeBackwardTestStrategy pibs = new PassiveIncomeBackwardTestStrategy();
		List<TransactionSummary> ts = pibs.getTransactionSummariesGroupedByBusinessWeek("AAPL");
		for (TransactionSummary xx : ts) {
			System.out.println(xx.date);
		}
	}

	public List<TransactionSummary> getTransactionSummariesGroupedByBusinessWeek(String ticker) {
		HistoricalPricesFetcher fetcher = new HistoricalPricesFetcher();
		HistoricalPricesForMultipleDays md = fetcher.getHistoricalPrices(ticker);
		Map<String, HistoricalPricesForDay> results = md.getPricesGroupedByBusinessWeeks();
		List<TransactionSummary> summaries = new ArrayList<TransactionSummary>();
		PrintStream out = null;
		try {
			out = new PrintStream(new FileOutputStream("/Users/sivad/aapl.csv"), true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (String key : results.keySet()) {
			// out.println("Week -> " + key);
			HistoricalPricesForDay x = results.get(key);
			// x.printToStream(out);
			// System.out.println(key + " -> " + x.prices.size());
			TransactionSummary summary = processTransactions(results.get(key));
			if (summary == null) {
				continue;
			}
			summary.totalGainOrLoss = summary.calculateTotalGainOrLoss();
			summaries.add(summary);
		}
		return summaries;

	}

	public List<TransactionSummary> getTransactionSummaries(String ticker) {
		HistoricalPricesFetcher fetcher = new HistoricalPricesFetcher();
		HistoricalPricesForMultipleDays md = fetcher.getHistoricalPrices(ticker);
		List<TransactionSummary> summaries = new ArrayList<TransactionSummary>();
		for (String key : md.historicalPricesMap.keySet()) {
			TransactionSummary summary = processTransactions(md.historicalPricesMap.get(key));
			if (summary == null) {
				continue;
			}
			summary.totalGainOrLoss = summary.calculateTotalGainOrLoss();
			summaries.add(summary);
		}
		return summaries;

	}

	public TransactionSummary processTransactions(HistoricalPricesForDay day) {

		if (day == null) {
			return null;
		}

		Collection<HistoricalPriceForMinute> prices = day.getPricesForOnlyHoursWhereOptionsAreTraded();
		if (prices.size() == 0)
			return null;
		TransactionSummary summary = new TransactionSummary();
		summary.date = day.day;
		summary.ticker = day.ticker;
		OptionTransaction callTransaction = null;
		OptionTransaction putTransaction = null;
		StockTransaction stockTransactionForCall = null;
		StockTransaction stockTransactionForPut = null;
		List<PotentialTransaction> stockTransactionsForCall = new ArrayList<PotentialTransaction>();
		List<PotentialTransaction> stockTransactionsForPut = new ArrayList<PotentialTransaction>();

		HistoricalPriceForMinute begin = null;
		HistoricalPriceForMinute end = null;
		for (HistoricalPriceForMinute minute : prices) {
			if (begin == null) {
				begin = minute;
			}
			end = minute;
			if (callTransaction == null) {
				callTransaction = makeSellCallTransaction(minute);
			}
			if (putTransaction == null) {
				putTransaction = makeSellPutTransaction(minute);
			}
			StockTransaction callResponse = identifyStockTransactionForCall(minute, stockTransactionForCall,
					callTransaction);
			StockTransaction putResponse = identifyStockTransactionForPut(minute, stockTransactionForPut,
					putTransaction);

			// Work for Call
			if (callResponse == stockTransactionForCall) {
				// continue;
			}
			if (callResponse != stockTransactionForCall) {
				PotentialTransaction pt = new PotentialTransaction();
				pt.minute = minute;
				pt.t = callResponse;
				stockTransactionsForCall.add(pt);
			}
			if (callResponse != null) {
				if (callResponse.type == StockTransactionType.SELL_TO_CLOSE_STOCK) {
					stockTransactionForCall = null;
				} else {
					stockTransactionForCall = callResponse;
				}
			}

			// Work for Put
			if (putResponse != stockTransactionForPut) {
				PotentialTransaction pt = new PotentialTransaction();
				pt.minute = minute;
				pt.t = putResponse;
				stockTransactionsForPut.add(pt);
			}
			if (putResponse != null) {
				if (putResponse.type == StockTransactionType.BUY_TO_COVER_STOCK) {
					stockTransactionForPut = null;
				} else {
					stockTransactionForPut = putResponse;
				}
			}

		}
		summary.open = begin.open;
		summary.close = end.close;

		summary.callStrikePrice = callTransaction.strikePrice;
		summary.callTransactions = stockTransactionsForCall.size();
		summary.potentialCallOptionGainOrLoss = callTransaction.potentialOptionValue
				- findCallGainOrLoss(summary.close, summary.callStrikePrice);
		summary.callGainOrLossFromStocks = getGainOrLossFromStockTransactions(stockTransactionsForCall);
		summary.callGainOrLossFromStocks += matchGainOrLossFromStocksForCall(summary, stockTransactionsForCall);
		// System.out.println("----Option Transactions");
		// displayOptionTransactions(putTransaction);
		// System.out.println("----Stock Transaction at each minute");
		// displayStockTransactions(stockTransactionsForPut);
		// displayStockTransactions(stockTransactionsForPut);

		summary.putStrikePrice = putTransaction.strikePrice;
		summary.putTransactions = stockTransactionsForPut.size();
		summary.potentialPutOptionGainOrLoss = putTransaction.potentialOptionValue
				- findPutGainOrLoss(summary.close, summary.putStrikePrice);
		summary.putGainOrLossFromStocks = -1 * getGainOrLossFromStockTransactions(stockTransactionsForPut);
		summary.putGainOrLossFromStocks += -1 * matchGainOrLossFromStocksForCall(summary, stockTransactionsForPut);

		return summary;
	}

	private double matchGainOrLossFromStocksForCall(TransactionSummary summary,
			List<PotentialTransaction> stockTransactions) {

		if (stockTransactions.size() % 2 == 0) {
			return 0;
		}
		PotentialTransaction pt = stockTransactions.get(stockTransactions.size() - 1);
		StockTransaction st = (StockTransaction) pt.t;
		return summary.close - st.stockPrice;

	}

	private double findCallGainOrLoss(double close, double callStrikePrice) {
		if (close < callStrikePrice) {
			return 0;
		}
		return close - callStrikePrice;
	}

	private double findPutGainOrLoss(double close, double putStrikePrice) {
		if (close > putStrikePrice) {
			return 0;
		}
		return putStrikePrice - close;
	}

	private double getGainOrLossFromStockTransactions(Collection<PotentialTransaction> stockTransactions) {
		double doubleGainOrLoss = 0;
		double start = 0;

		// PotentialTransaction last;

		for (PotentialTransaction pt : stockTransactions) {
			// last = pt;
			StockTransaction st = (StockTransaction) pt.t;
			if (start == 0) {
				start = st.stockPrice;
				continue;
			} else {
				// System.out.println(st.stockPrice - start);
				doubleGainOrLoss += st.stockPrice - start;
				start = 0;
			}
		}
		// System.out.println(day.day);
		// System.out.println(day.ticker + "," + day.day + "," + begin.open + "," +
		// end.close + ","
		// + stockTransactions.size() + "," + doubleGainOrLoss + "," + "CALL");
		return doubleGainOrLoss;

	}

	private void displayStockTransactions(Collection<PotentialTransaction> stockTransactions) {
		for (PotentialTransaction pt : stockTransactions) {
			System.out.println(pt);
		}

	}

	private void displayOptionTransactions(OptionTransaction ot) {
		System.out.println("...Stock Entry/Gains");

		for (StockEntry entry : ot.stockEntriesForCall) {
			System.out.println(entry);
		}

		System.out.println("...Stock Transactions....");
		for (StockTransaction st : ot.potentialStockTransactionsForCall) {
			System.out.println(st);
		}

	}

	private StockTransaction identifyStockTransactionForCall(HistoricalPriceForMinute minute,
			StockTransaction stockTransactionForCall, OptionTransaction ot) {
		StockEntry entry = ot.getPotentialStockEntryForCall();
		double price = entry.entry;
		if (stockTransactionForCall == null) {

			if (minute.open > price) {
				StockTransaction call = buyStockForCallOption(minute, ot);
				return call;
			}
			return null;
		}
		price = entry.gain;
		if (minute.open > price) {
			StockTransaction sell = sellStockForCallOption(minute, ot);
			return sell;
		}
		price = entry.loss;
		if (minute.open < price) {
			StockTransaction sell = sellStockForCallOption(minute, ot);
			return sell;
		}

		return stockTransactionForCall;
	}

	private StockTransaction sellStockForCallOption(HistoricalPriceForMinute minute, OptionTransaction ot) {
		PassiveIncomeInput input = getPassiveIncomeInput();
		StockTransaction sell = new StockTransaction();
		sell.type = StockTransactionType.SELL_TO_CLOSE_STOCK;
		sell.stockPrice = minute.open;
		sell.minute = minute;
		ot.addStockTransactionForCall(sell);
		// ot.addPotentialStockEntryForCall(sell.stockPrice + 0.25, sell.stockPrice +
		// 1.5, sell.stockPrice - 0.25);
		ot.addPotentialStockEntryForCall(sell.stockPrice + input.distanceForEntryStockPurchaseAfterSelling,
				sell.stockPrice + input.distanceForExitOnGainAfterStockPurchase,
				sell.stockPrice - input.distanceForExitOnLossAfterStockPurchase);
		return sell;
	}

	private StockTransaction buyStockForCallOption(HistoricalPriceForMinute minute, OptionTransaction ot) {
		StockTransaction call = new StockTransaction();
		call.type = StockTransactionType.BUY_STOCK;
		call.stockPrice = minute.open;
		call.minute = minute;
		ot.addStockTransactionForCall(call);
		ot.addPotentialStockEntryForCall(call.stockPrice + 0.25, call.stockPrice + 1.5, call.stockPrice - 0.25);
		return call;
	}

	private StockTransaction identifyStockTransactionForPut(HistoricalPriceForMinute minute,
			StockTransaction stockTransactionForPut, OptionTransaction ot) {
		StockEntry entry = ot.getPotentialStockEntryForPut();
		double price = entry.entry;
		if (stockTransactionForPut == null) {

			if (minute.open < price) {
				StockTransaction put = sellShortStockForPutOption(minute, ot);
				return put;
			}
			return null;
		}
		price = entry.gain;
		if (minute.open < price) {
			StockTransaction sell = buyToCoverStockForPutOption(minute, ot);
			return sell;
		}
		price = entry.loss;
		if (minute.open > price) {
			StockTransaction sell = buyToCoverStockForPutOption(minute, ot);
			return sell;
		}

		return stockTransactionForPut;
	}

	private StockTransaction buyToCoverStockForPutOption(HistoricalPriceForMinute minute, OptionTransaction ot) {
		StockTransaction sell = new StockTransaction();
		sell.type = StockTransactionType.BUY_TO_COVER_STOCK;
		sell.stockPrice = minute.open;
		sell.minute = minute;
		ot.addStockTransactionForPut(sell);
		ot.addPotentialStockEntryForPut(sell.stockPrice - 0.25, sell.stockPrice - 1.5, sell.stockPrice + 0.25);
		return sell;
	}

	private StockTransaction sellShortStockForPutOption(HistoricalPriceForMinute minute, OptionTransaction ot) {
		StockTransaction put = new StockTransaction();
		put.type = StockTransactionType.SELL_SHORT_STOCK;
		put.stockPrice = minute.open;
		put.minute = minute;
		ot.addStockTransactionForPut(put);
		ot.addPotentialStockEntryForPut(put.stockPrice - 0.25, put.stockPrice - 1.5, put.stockPrice + 0.25);
		return put;
	}

	class PassiveIncomeInput {
		double optionPercentagePremium = 0.002;
		double strikePriceIncrementOrDecrement = 1;
		double initialStockEntryDistanceFromStrikePrice = 1;
		double stockExitGain = 1;
		double stockExitLoss = 0.25;

		double distanceForEntryStockPurchaseAfterSelling = 0.25;
		double distanceForExitOnGainAfterStockPurchase = 1.25;
		double distanceForExitOnLossAfterStockPurchase = 0;
		
		double spreadDistance = 5;
		

	}

	public PassiveIncomeInput getPassiveIncomeInput() {
		return input;
	}

	private OptionTransaction makeSellCallTransaction(HistoricalPriceForMinute minute) {
		PassiveIncomeInput input = getPassiveIncomeInput();
		double o = minute.open;
		// double sp = Math.ceil(o) + 1;
		double sp = Math.ceil(o) + input.strikePriceIncrementOrDecrement;
		OptionTransaction ot = new OptionTransaction();
		ot.strikePrice = sp;
		ot.type = OptionTransactionType.SELL_CALL;
		// ot.potentialOptionValue = sp * 0.002;
		ot.potentialOptionValue = sp * input.optionPercentagePremium;
		// ot.addPotentialStockEntryForCall(sp - 1, sp + 1, sp - 1.25);
		ot.addPotentialStockEntryForCall(sp - input.initialStockEntryDistanceFromStrikePrice, sp + input.stockExitGain,
				sp - input.initialStockEntryDistanceFromStrikePrice - input.stockExitLoss);

		return ot;
	}

	private OptionTransaction makeSellPutTransaction(HistoricalPriceForMinute minute) {
		double o = minute.open;
		// double sp = Math.floor(o) - 1;
		double sp = Math.floor(o) - input.strikePriceIncrementOrDecrement;
		OptionTransaction ot = new OptionTransaction();
		ot.strikePrice = sp;
		ot.type = OptionTransactionType.SELL_PUT;
		// ot.potentialOptionValue = sp * 0.002;
		ot.potentialOptionValue = sp * input.optionPercentagePremium;
		// ot.addPotentialStockEntryForPut(sp + 1, sp - 1, sp + 1.25);
		ot.addPotentialStockEntryForPut(sp + input.initialStockEntryDistanceFromStrikePrice, sp - input.stockExitGain,
				sp + input.initialStockEntryDistanceFromStrikePrice + input.stockExitLoss);
		;

		return ot;
	}
}

class HistoricalPriceForMinute {
	public long timestamp;
	public Date d;
	public double open, low, high, close, volume;

	public String toString() {
		return timestamp + "," + d + "," + open + "," + low + "," + high + "," + close + "," + volume;
	}

}

class HistoricalPricesForDay {
	public String ticker;
	public Collection<HistoricalPriceForMinute> prices = new ArrayList<HistoricalPriceForMinute>();
	public String day;

	public void addHistoricalPriceForMinute(HistoricalPriceForMinute minute) {
		prices.add(minute);
	}

	Collection<HistoricalPriceForMinute> getPricesForOnlyHoursWhereOptionsAreTraded() {
		Collection<HistoricalPriceForMinute> prices = new ArrayList<HistoricalPriceForMinute>();
		for (HistoricalPriceForMinute x : this.prices) {
			Date d = x.d;
			if (d.getHours() >= 6 && d.getHours() < 13) {
				if (d.getHours() == 6) {
					if (d.getMinutes() >= 30) {
						prices.add(x);
						continue;
					} else {
						continue;
					}
				}
				prices.add(x);
				continue;
			}
		}

		return prices;
	}

	public static Comparator<HistoricalPricesForDay> dayComparator = new Comparator<HistoricalPricesForDay>() {
		@Override
		public int compare(HistoricalPricesForDay jc1, HistoricalPricesForDay jc2) {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d-M-yyyy");
			LocalDate x = LocalDate.parse(jc1.day, dtf);
			LocalDate y = LocalDate.parse(jc2.day, dtf);
			return x.compareTo(y);
		}
	};

	public void printToStream(PrintStream out) {
		for (HistoricalPriceForMinute m : prices) {
			out.println(m.toString());
		}
	}

}

class HistoricalPricesForMultipleDays {
	String ticker;
	Map<String, HistoricalPricesForDay> historicalPricesMap = new LinkedHashMap<String, HistoricalPricesForDay>();

	public void addHistoricalPricesForDay(String date, HistoricalPricesForDay hpd) {
		historicalPricesMap.put(date, hpd);

	}

	public HistoricalPricesForDay mergeAllHistoricalPricesBySortingTheDates() {
		ArrayList<HistoricalPricesForDay> values = new ArrayList<HistoricalPricesForDay>(historicalPricesMap.values());
		Collections.sort(values, HistoricalPricesForDay.dayComparator);
		HistoricalPricesForDay result = new HistoricalPricesForDay();
		StringBuffer dates = new StringBuffer("[");
		for (HistoricalPricesForDay x : values) {
			if (result.ticker == null) {
				result.ticker = x.ticker;
			}
			if (result.day == null) {
				result.day = x.day;
				dates.append(x.day + "={");
			}
			dates.append(x.day + ",");
			Collection<HistoricalPriceForMinute> hpm = result.prices;
			hpm.addAll(x.getPricesForOnlyHoursWhereOptionsAreTraded());
			dates.append("(" + hpm.size() + ")");

		}
		dates.append("}]");
		// System.out.println(dates);
		return result;
	}

	public Map<String, HistoricalPricesForDay> getPricesGroupedByBusinessWeeks() {

		Map<String, HistoricalPricesForMultipleDays> results = new LinkedHashMap<String, HistoricalPricesForMultipleDays>();

		for (String key : historicalPricesMap.keySet()) {
			String weeklyKey = getWeeklyKey(key);
			HistoricalPricesForMultipleDays days = results.get(weeklyKey);
			if (days == null) {
				days = new HistoricalPricesForMultipleDays();
				results.put(weeklyKey, days);
			}
			days.addHistoricalPricesForDay(key, historicalPricesMap.get(key));
		}

		Map<String, HistoricalPricesForDay> output = new LinkedHashMap<String, HistoricalPricesForDay>();
		for (String key : results.keySet()) {
			HistoricalPricesForMultipleDays x = (HistoricalPricesForMultipleDays) results.get(key);
			output.put(key, x.mergeAllHistoricalPricesBySortingTheDates());
		}
		return output;

	}

	private String getWeeklyKey(String key) {
		try {
			DateTimeFormatter df = DateTimeFormatter.ofPattern("d-M-yyyy");
			LocalDate ld = LocalDate.parse(key, df);

			if (ld.getDayOfWeek() == DayOfWeek.MONDAY) {
				return ld.minusDays(0).format(df);
			}
			if (ld.getDayOfWeek() == DayOfWeek.TUESDAY) {
				return ld.minusDays(1).format(df);
			}
			if (ld.getDayOfWeek() == DayOfWeek.WEDNESDAY) {
				return ld.minusDays(2).format(df);
			}
			if (ld.getDayOfWeek() == DayOfWeek.THURSDAY) {
				return ld.minusDays(3).format(df);
			}
			if (ld.getDayOfWeek() == DayOfWeek.FRIDAY) {
				return ld.minusDays(4).format(df);
			}
			if (ld.getDayOfWeek() == DayOfWeek.SATURDAY) {
				return ld.minusDays(5).format(df);
			}
			if (ld.getDayOfWeek() == DayOfWeek.SUNDAY) {
				return ld.minusDays(6).format(df);
			}
			throw new RuntimeException("Should not reach this line");
			// return key;
		} catch (Exception e) {
			System.out.println("Key[" + key + "]");
			// TODO Auto-generated catch block
			e.printStackTrace();
			return key;
		}
	}

}
