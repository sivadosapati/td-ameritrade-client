package com.rise.trading.options.passive.income.backtest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
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

	public static void main(String args[]) {
		PassiveIncomeBackwardTestStrategy pibs = new PassiveIncomeBackwardTestStrategy();
		List<String> strings = Arrays.asList("9-02-2023");
		pibs.getTransactionSummaries("SPY", strings);

	}

	public static void mainAnother(String x[]) {
		PassiveIncomeBackwardTestStrategy pibs = new PassiveIncomeBackwardTestStrategy();
		List<TransactionSummary> ts = pibs.getTransactionSummariesGroupedByBusinessWeek("AAPL");
		for (TransactionSummary xx : ts) {
			System.out.println(xx.date);
		}
	}

	public List<TransactionSummary> getTransactionSummariesGroupedByBusinessWeek(String ticker) {

		HistoricalPricesForMultipleDays md = fetcher.getHistoricalPrices(ticker);
		Map<String, HistoricalPricesForDay> results = md.getPricesGroupedByBusinessWeeks();
		return getTransactionSummaries(results);

	}

	public List<TransactionSummary> getTransactionSummariesGroupedByBusinessWeek(String ticker, List<String> dates) {

		HistoricalPricesForMultipleDays md = fetcher.getHistoricalPrices(ticker, dates);
		Map<String, HistoricalPricesForDay> results = md.getPricesGroupedByBusinessWeeks();
		return getTransactionSummaries(results);

	}

	HistoricalPricesFetcher fetcher = new HistoricalPricesFetcher();

	public List<TransactionSummary> getTransactionSummaries(String ticker) {

		HistoricalPricesForMultipleDays md = fetcher.getHistoricalPrices(ticker);
		return getTransactionSummaries(md);

	}

	public List<TransactionSummary> getTransactionSummaries(String ticker, List<String> days) {
		HistoricalPricesForMultipleDays md = fetcher.getHistoricalPrices(ticker, days);
		return getTransactionSummaries(md);
	}

	private Double findPotentialOptionPremium(Double strikePrice, Double open) {
		double delta = strikePrice - open;
		if (delta > 6) {
			return 5d;
		}
		if (delta > 5) {
			return 10d;
		}
		if (delta > 4) {
			return 0.01 * strikePrice;
		}
		if (delta > 3) {
			return 0.015 * strikePrice;
		}
		if (delta > 2) {
			return 0.02 * strikePrice;
		}
		if (delta > 1) {
			return 0.025 * strikePrice;
		}
		return 0.03 * strikePrice;
	}

	public List<TransactionSummary> processBuyingLongAndShortEveryDayWhenApplicable(String ticker, List<String> days) {
		HistoricalPricesForMultipleDays md = fetcher.getHistoricalPrices(ticker, days);
		List<HistoricalPricesForDay> sortedPrices = md.sortHistoricalPricesByDay();
		double longStock = 0;
		double shortStock = 0;
		double longStockQuantity = 0;
		double shortStockQuantity = 0;
		double callPremium = 0;
		double putPremium = 0;
		double callStrikePrice = 0;
		double putStrikePrice = 0;
		boolean boughtLongStocksInDay = false;
		boolean boughtShortStocksInDay = false;
		List<TransactionSummary> summaries = new ArrayList<TransactionSummary>();
		double cumulativeGainOrLoss = 0d;
		for (HistoricalPricesForDay day : sortedPrices) {
			boughtLongStocksInDay = false;
			boughtShortStocksInDay = false;
			PassiveIncomeInput input = getPassiveIncomeInput();

			if (day == null) {
				continue;
			}

			Collection<HistoricalPriceForMinute> prices = day.getPricesForOnlyHoursWhereOptionsAreTraded();
			if (prices.size() == 0)
				continue;
			LongAndShortEveryDayTransactionSummary summary = new LongAndShortEveryDayTransactionSummary();
			summary.date = day.day;
			summary.ticker = day.ticker;
			summary.longStockStartOfTheDay = longStock;
			summary.shortStockStartOfTheDay = shortStock;
			summary.longStockQuantity = longStockQuantity;
			summary.shortStockQuantity = shortStockQuantity;
			summary.cumulativeGainOrLoss = cumulativeGainOrLoss;
			HistoricalPriceForMinute begin = null;
			HistoricalPriceForMinute end = null;
			double high = 0.0;
			double low = Double.MAX_VALUE;
			for (HistoricalPriceForMinute minute : prices) {
				double hm = minute.high;
				double lm = minute.low;
				if (hm > high)
					high = hm;
				if (lm < low)
					low = lm;
				if (begin == null) {
					begin = minute;
				}
				end = minute;
				if (end == begin) {
					double o = minute.open;
					// One possible condition

					callStrikePrice = Math.ceil(o) + input.strikePriceIncrementOrDecrement;
					callPremium = callStrikePrice * input.optionPercentagePremium * 100;
					putStrikePrice = Math.floor(o) - input.strikePriceIncrementOrDecrement;
					putPremium = putStrikePrice * input.optionPercentagePremium * 100;

					// Other possible condition

					/*
					 * if (longStock > 0) { double potentialCallStrike = Math.ceil(longStock / 100);
					 * if (potentialCallStrike > o) { callStrikePrice = potentialCallStrike;
					 * callPremium = findPotentialOptionPremium(callStrikePrice, o); } } if
					 * (shortStock < 0) { double pcs = Math.floor(-1 * shortStock / 100); if (pcs <
					 * o) { putStrikePrice = pcs; putPremium = findPotentialOptionPremium(o,
					 * putStrikePrice); } }
					 */

					continue;
				}
				double open = minute.open;
				if (open > callStrikePrice - input.initialStockEntryDistanceFromStrikePrice) {
					if (boughtLongStocksInDay == false) {
						boughtLongStocksInDay = true;
						longStock += 100 * open;
						longStockQuantity += 100;
					}

				}
				if (open < putStrikePrice + input.initialStockEntryDistanceFromStrikePrice) {
					if (boughtShortStocksInDay == false) {
						boughtShortStocksInDay = true;
						shortStock += -100 * open;
						shortStockQuantity += 100;
					}
				}

			}
			summary.open = begin.open;
			summary.close = end.close;
			summary.high = high;
			summary.low = low;

			summary.potentialCallOptionGainOrLoss = callPremium;
			summary.potentialPutOptionGainOrLoss = putPremium;
			summary.callStrikePrice = callStrikePrice;
			summary.putStrikePrice = putStrikePrice;
			summary.totalGainOrLoss = callPremium + putPremium;
			if (summary.close > summary.callStrikePrice) {
				// summary.callGainOrLossFromStocks = summary.callStrikePrice * 100 - longStock;
				// summary.totalGainOrLoss += summary.callGainOrLossFromStocks;
				// longStock = 0;
				summary.callGainOrLossFromStocks = 100 * (summary.callStrikePrice - summary.close);
				summary.totalGainOrLoss += summary.callGainOrLossFromStocks;
				longStock -= 100 * summary.callStrikePrice;
				longStockQuantity -= 100;

			}
			if (summary.close < summary.putStrikePrice) {
				// summary.putGainOrLossFromStocks = -1 * (summary.putStrikePrice * 100 +
				// shortStock);
				// summary.totalGainOrLoss += summary.putGainOrLossFromStocks;
				// shortStock = 0;

				summary.putGainOrLossFromStocks = 100 * (summary.close - summary.putStrikePrice);
				summary.totalGainOrLoss += summary.putGainOrLossFromStocks;
				shortStock = shortStock + 100 * summary.putStrikePrice;
				shortStockQuantity -= 100;

			}
			summary.longStockEndOfTheDay = longStock;
			summary.shortStockEndOfTheDay = shortStock;
			summary.shortStockQuantityEndOfTheDay = shortStockQuantity;
			summary.longStockQuantityEndOfTheDay = longStockQuantity;
			summary.calculatePotentialGainOrLossOnStocksIfLiquidatedEndOfDay();
			cumulativeGainOrLoss += summary.totalGainOrLoss;
			summary.cumulativeGainOrLoss = cumulativeGainOrLoss;

			summaries.add(summary);

		}
		return summaries;
	}

	protected List<TransactionSummary> getTransactionSummaries(Map<String, HistoricalPricesForDay> map) {
		List<HistoricalPricesForDay> prices = new ArrayList<HistoricalPricesForDay>(map.values());
		Collections.sort(prices, HistoricalPricesForDay.dayComparator);
		List<TransactionSummary> summaries = new ArrayList<TransactionSummary>();
		int sno = 1;
		for (HistoricalPricesForDay price : prices) {
			TransactionSummary summary = processTransactions(price);
			if (summary == null) {
				continue;
			}
			summary.id = sno++;
			summary.totalGainOrLoss = summary.calculateTotalGainOrLoss();
			summaries.add(summary);
		}
		return summaries;
	}

	protected List<TransactionSummary> getTransactionSummaries(HistoricalPricesForMultipleDays md) {
		List<HistoricalPricesForDay> prices = md.sortHistoricalPricesByDay();
		List<TransactionSummary> summaries = new ArrayList<TransactionSummary>();
		int sno = 1;
		for (HistoricalPricesForDay day : prices) {
			TransactionSummary summary = processTransactions(day);
			if (summary == null) {
				continue;
			}
			summary.id = sno++;
			summary.totalGainOrLoss = summary.calculateTotalGainOrLoss();
			summaries.add(summary);
		}
		return summaries;
	}

	DateTimeFormatter df = DateTimeFormatter.ofPattern("d-MM-yyyy");

	private LocalDate parseDate(String date) {

		return LocalDate.parse(date, df);
	}

	public DetailedTransactionSummaryForDay processDetailedTransactions(String ticker, LocalDate day) {

		HistoricalPricesForDay his = fetcher.getHistoricalPricesForDay(ticker, day);
		return processDetailedTransactions(his);
	}

	public DetailedTransactionSummaryForDay processDetailedTransactions(HistoricalPricesForDay day) {
		if (day == null) {
			return null;
		}

		Collection<HistoricalPriceForMinute> prices = day.getPricesForOnlyHoursWhereOptionsAreTraded();
		if (prices.size() == 0)
			return null;
		DetailedTransactionSummaryForDay txn = new DetailedTransactionSummaryForDay();
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
		double high = 0.0;
		double low = Double.MAX_VALUE;
		for (HistoricalPriceForMinute minute : prices) {
			double hm = minute.high;
			double lm = minute.low;
			if (hm > high)
				high = hm;
			if (lm < low)
				low = lm;
			if (begin == null) {
				begin = minute;
			}
			end = minute;
			if (callTransaction == null) {
				callTransaction = makeSellCallTransaction(minute);
				// continue;
			}
			if (putTransaction == null) {
				putTransaction = makeSellPutTransaction(minute);
				// continue;
			}
			if (begin == minute) {
				continue;
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
		summary.high = high;
		summary.low = low;

		summary.callStrikePrice = callTransaction.strikePrice;
		summary.callTransactions = stockTransactionsForCall.size();
		summary.potentialCallOptionGainOrLoss = callTransaction.potentialOptionValue
				- findCallGainOrLoss(summary.close, summary.callStrikePrice);
		summary.callGainOrLossFromStocks = getGainOrLossFromStockTransactions(stockTransactionsForCall);
		summary.callGainOrLossFromStocks += matchGainOrLossFromStocksForCall(summary, stockTransactionsForCall);

		// displayResults(putTransaction, stockTransactionsForCall,
		// stockTransactionsForPut);

		summary.putStrikePrice = putTransaction.strikePrice;
		summary.putTransactions = stockTransactionsForPut.size();
		summary.potentialPutOptionGainOrLoss = putTransaction.potentialOptionValue
				- findPutGainOrLoss(summary.close, summary.putStrikePrice);
		summary.putGainOrLossFromStocks = -1 * getGainOrLossFromStockTransactions(stockTransactionsForPut);
		summary.putGainOrLossFromStocks += -1 * matchGainOrLossFromStocksForCall(summary, stockTransactionsForPut);

		txn.summary = summary;
		txn.callTransaction = callTransaction;
		txn.putTransaction = putTransaction;
		txn.stockTransactionsForCall = stockTransactionsForCall;
		txn.stockTransactionsForPut = stockTransactionsForPut;
		txn.day = day;
		return txn;

	}

	protected void displayResults(OptionTransaction putTransaction, List<PotentialTransaction> stockTransactionsForCall,
			List<PotentialTransaction> stockTransactionsForPut) {
		// Display results
		System.out.println("----Option Transactions");
		displayOptionTransactions(putTransaction);
		System.out.println("----Stock Transaction at each minute");
		displayStockTransactions(stockTransactionsForCall);
		displayStockTransactions(stockTransactionsForPut);
		// Display results
	}

	public TransactionSummary processTransactions(HistoricalPricesForDay day) {
		DetailedTransactionSummaryForDay txn = processDetailedTransactions(day);
		if (txn == null)
			return null;
		return txn.summary;
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

	private double findCallGainOrLossOld(double close, double callStrikePrice) {
		if (close < callStrikePrice) {
			return 0;
		}
		return close - callStrikePrice;
	}

	private double findPutGainOrLossOld(double close, double putStrikePrice) {
		if (close > putStrikePrice) {
			return 0;
		}
		return putStrikePrice - close;
	}

	private double findCallGainOrLoss(double close, double callStrikePrice) {
		if (close < callStrikePrice) {
			return 0;
		}
		if (close < callStrikePrice + input.spreadDistance) {
			return close - callStrikePrice;
		}
		return input.spreadDistance;
	}

	private double findPutGainOrLoss(double close, double putStrikePrice) {
		if (close > putStrikePrice) {
			return 0;
		}
		if (close > putStrikePrice - input.spreadDistance) {
			return putStrikePrice - close;
		}
		return input.spreadDistance;
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
		// ot.addPotentialStockEntryForCall(call.stockPrice + 0.25, call.stockPrice +
		// 1.5, call.stockPrice - 0.25);
		ot.addPotentialStockEntryForCall(call.stockPrice + input.distanceForEntryStockPurchaseAfterSelling,
				call.stockPrice + input.distanceForExitOnGainAfterStockPurchase,
				call.stockPrice - input.distanceForExitOnLossAfterStockPurchase);
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
		// ot.addPotentialStockEntryForPut(sell.stockPrice - 0.25, sell.stockPrice -
		// 1.5, sell.stockPrice + 0.25);
		ot.addPotentialStockEntryForPut(sell.stockPrice - input.distanceForEntryStockPurchaseAfterSelling,
				sell.stockPrice - input.distanceForExitOnGainAfterStockPurchase,
				sell.stockPrice + input.distanceForExitOnLossAfterStockPurchase);
		return sell;
	}

	private StockTransaction sellShortStockForPutOption(HistoricalPriceForMinute minute, OptionTransaction ot) {
		StockTransaction sell = new StockTransaction();
		sell.type = StockTransactionType.SELL_SHORT_STOCK;
		sell.stockPrice = minute.open;
		sell.minute = minute;
		ot.addStockTransactionForPut(sell);
		// ot.addPotentialStockEntryForPut(put.stockPrice - 0.25, put.stockPrice - 1.5,
		// put.stockPrice + 0.25);
		ot.addPotentialStockEntryForPut(sell.stockPrice - input.distanceForEntryStockPurchaseAfterSelling,
				sell.stockPrice - input.distanceForExitOnGainAfterStockPurchase,
				sell.stockPrice + input.distanceForExitOnLossAfterStockPurchase);

		return sell;
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
