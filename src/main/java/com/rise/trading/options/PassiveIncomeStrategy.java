package com.rise.trading.options;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import com.rise.trading.options.symbols.OptionSymbolManager;
import com.rise.trading.options.symbols.OptionSymbolWithAdjacents;
import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.model.account.Duration;
import com.studerw.tda.model.account.EquityInstrument;
import com.studerw.tda.model.account.Instrument.AssetType;
import com.studerw.tda.model.account.OptionInstrument;
import com.studerw.tda.model.account.OptionInstrument.PutCall;
import com.studerw.tda.model.account.Order;
import com.studerw.tda.model.account.OrderLegCollection;
import com.studerw.tda.model.account.OrderLegCollection.Instruction;
import com.studerw.tda.model.account.OrderLegCollection.OrderLegType;
import com.studerw.tda.model.account.OrderStrategyType;
import com.studerw.tda.model.account.OrderType;
import com.studerw.tda.model.account.Position;
import com.studerw.tda.model.account.Session;
import com.studerw.tda.model.option.Option;
import com.studerw.tda.model.option.OptionChain;
import com.studerw.tda.model.option.OptionChainReq;
import com.studerw.tda.model.quote.Quote;

import net.jodah.expiringmap.ExpiringMap;

public class PassiveIncomeStrategy extends BaseHandler implements PassiveIncomeOptionProcessor {

	private static final int MAX_SELL_OPTION_COUNT = 50;

	public static int MAX_NUMBER_OF_SELL_POSITIONS = 25;

	private static PassiveIncomeOptionProcessor chain = makePassiveIncomeProcessorChain();

	// private List<String> stockTickerSymbolsThatCanBeShortedWhenLongExists =
	// Arrays.asList();
	// private List<String> stockTickerSymbolsThatCanBeShortedWhenLongExists =
	// Arrays.asList("TQQQ", "IWM", "SPY", "AMD");

	private List<String> skippedSymbols = Arrays.asList("SOLO", "WKHS", "XOS", "CRBP", "QQQ");
	private static String[] accounts = { Util.getAccountId1(), Util.getAccountId6(), Util.getAccountId7(),
			Util.getAccountId4(), Util.getAccountId2(), Util.getAccountId3() };

	public static void main(String args[]) {
		final PassiveIncomeStrategy pis = new PassiveIncomeStrategy();
		for (String a : accounts) {
			pis.closeOptionsThatAreInProfitAndPotentiallyOpenNewOnes(a);
			// pis.closeOptionsThatAreInProfitAndPotentiallyOpenNewOnes(a);
		}
		// placeSomeMoreTrades(pis);

	}

	private static PassiveIncomeOptionProcessorChain makePassiveIncomeProcessorChain() {
		PassiveIncomeOptionProcessorChain chain = new PassiveIncomeOptionProcessorChain();
		return chain;
	}

	private static void placeSomeTrades(final PassiveIncomeStrategy pis) {
		String a = Util.getAccountId1();
		Ticker ticker = Ticker.make("ARM");

		pis.placeNextWeeklyTradeForPassiveIncome(a, ticker, 1f, 1f, 1);
		// ticker.optionSpreadDistance = 10;
		pis.placeNextDayTradeForPassiveIncome(a, ticker, 20f, 20f, 1);
		ticker.optionSpreadDistance = 20;
		ticker.price = new BigDecimal(750);
		pis.placeNextDayTradeForPassiveIncome(a, ticker, 30f, 30f, 1);
	}

	private static void placeSomeMoreTrades(final PassiveIncomeStrategy pis) {
		String a = Util.getAccountId6();
		Ticker ticker = Ticker.make("MSFT");
		LocalDateTime from = LocalDateTime.now().plusDays(1);
		LocalDateTime to = from.plusDays(31);

		pis.placeOptionTradesForPassiveIncome(a, ticker, 2f, 2f, 1, from, to);
	}

	public int getMaximumSellPositions(String ticker) {
		return MAX_NUMBER_OF_SELL_POSITIONS;
	}

	public void closeSellOptionsThatAreInProfit(String accountId, String ticker, double gainPercentage) {
		GroupedPosition gp = getGroupedPosition(accountId, ticker);

		if (gp == null) {
			return;
		}
		LocalDate day = getTheImmediateBusinessDay().toLocalDate();
		List<Position> positions = getOptionPositionsThatExpireOnThisDay(gp, day);
		if (positions.size() == 0) {
			return;
		}
		Position sellCallOptionPosition = getSellOptionPosition(positions, PutCall.CALL);
		if (sellCallOptionPosition == null) {
			return;
		}
	}

	public void closeShortEquitiesIfTheyAreInProfitAndPlaceAnotherOrderIfThereAreSellOptions(String accountId,
			String ticker, double gainDollars, double decrementFromStrikePriceLowerThreshold) {
		GroupedPosition gp = getGroupedPosition(accountId, ticker);

		if (gp == null) {
			return;
		}
		Position equity = gp.getShortEquity();
		if (equity == null) {
			return;
		}
		LocalDate day = getTheImmediateBusinessDay().toLocalDate();
		List<Position> positions = getOptionPositionsThatExpireOnThisDay(gp, day);
		if (positions.size() == 0) {
			return;
		}
		double currentStockPrice = equity.getMarketValue().doubleValue() / equity.getShortQuantity().doubleValue();
		PutCall pc = PutCall.PUT;
		Position sellPutOptionPosition = getSellOptionPosition(positions, pc);
		if (sellPutOptionPosition == null) {
			return;
		}
		List<Order> filledOrders = getFilledOrders(accountId);
		List<Order> filledOrderForEquity = getWorkingOrderIfPresentForPosition(filledOrders, equity);
		if (filledOrderForEquity == null) {
			return;
		}

		BigDecimal optionStrikePrice = findStrikePrice(sellPutOptionPosition.getInstrument().getSymbol(),
				getPutCallChar(pc));
		final int numberOfStocks = sellPutOptionPosition.getShortQuantity().intValue() * 100;
		if (currentStockPrice < optionStrikePrice.doubleValue() - gainDollars) {
			System.out.println("Matched condition for closing the equity when it is trading higher");
			cancelEquityWorkingOrderIfPresent(accountId, equity);

			Order order = makeStockOrder(ticker, new BigDecimal(currentStockPrice), numberOfStocks,
					Instruction.BUY_TO_COVER, OrderType.MARKET);
			getClient().placeOrder(accountId, order);
			Order newOrder = makeStockOrder(ticker,
					new BigDecimal(currentStockPrice - decrementFromStrikePriceLowerThreshold), numberOfStocks,
					Instruction.SELL_SHORT, OrderType.STOP_LIMIT);
			getClient().placeOrder(accountId, newOrder);
			return;
		}
		if (currentStockPrice >= optionStrikePrice.doubleValue() + decrementFromStrikePriceLowerThreshold) {
			System.out.println("Triggered stop loss range for closing the position at loss");

			cancelEquityWorkingOrderIfPresent(accountId, equity);

			final Order order = makeStockOrder(ticker, new BigDecimal(currentStockPrice), numberOfStocks,
					Instruction.BUY_TO_COVER, OrderType.MARKET);
			getClient().placeOrder(accountId, order);
			Order newOrder = makeStockOrder(ticker, new BigDecimal(optionStrikePrice.doubleValue()), numberOfStocks,
					Instruction.SELL_SHORT, OrderType.STOP_LIMIT);
			getClient().placeOrder(accountId, newOrder);
			return;

		}
	}

	public void closeLongEquitiesIfTheyAreInProfitAndPlaceAnotherOrderIfThereAreSellOptions(String accountId,
			String ticker, double gainDollars, double decrementFromStrikePriceLowerThreshold) {
		GroupedPosition gp = getGroupedPosition(accountId, ticker);

		if (gp == null) {
			return;
		}
		Position equity = gp.getEquity();
		if (equity == null) {
			return;
		}
		LocalDate day = getTheImmediateBusinessDay().toLocalDate();
		List<Position> positions = getOptionPositionsThatExpireOnThisDay(gp, day);
		if (positions.size() == 0) {
			return;
		}
		double currentStockPrice = equity.getMarketValue().doubleValue() / equity.getLongQuantity().doubleValue();
		PutCall pc = PutCall.CALL;
		Position sellCallOption = getSellOptionPosition(positions, pc);
		if (sellCallOption == null) {
			return;
		}
		List<Order> filledOrders = getFilledOrders(accountId);
		List<Order> filledOrderForEquity = getWorkingOrderIfPresentForPosition(filledOrders, equity);
		if (filledOrderForEquity == null) {
			return;
		}

		BigDecimal optionStrikePrice = findStrikePrice(sellCallOption.getInstrument().getSymbol(), getPutCallChar(pc));
		final int numberOfStocks = sellCallOption.getShortQuantity().intValue() * 100;
		if (currentStockPrice >= optionStrikePrice.doubleValue() - gainDollars) {
			System.out.println("Matched condition for closing the equity when it is trading higher");
			cancelEquityWorkingOrderIfPresent(accountId, equity);

			Order order = makeStockOrder(ticker, new BigDecimal(currentStockPrice), numberOfStocks,
					Instruction.SELL_TO_CLOSE, OrderType.MARKET);
			getClient().placeOrder(accountId, order);
			Order newOrder = makeStockOrder(ticker,
					new BigDecimal(currentStockPrice - decrementFromStrikePriceLowerThreshold), numberOfStocks,
					Instruction.BUY_TO_OPEN, OrderType.STOP_LIMIT);
			getClient().placeOrder(accountId, newOrder);
			return;
		}
		if (currentStockPrice < optionStrikePrice.doubleValue() + decrementFromStrikePriceLowerThreshold) {
			System.out.println("Triggered stop loss range for closing the position at loss");

			cancelEquityWorkingOrderIfPresent(accountId, equity);

			Order order = makeStockOrder(ticker, new BigDecimal(currentStockPrice), numberOfStocks,
					Instruction.SELL_TO_CLOSE, OrderType.MARKET);
			getClient().placeOrder(accountId, order);
			Order newOrder = makeStockOrder(ticker, new BigDecimal(optionStrikePrice.doubleValue()), numberOfStocks,
					Instruction.BUY_TO_OPEN, OrderType.STOP_LIMIT);
			getClient().placeOrder(accountId, newOrder);
			return;

		}

	}

	private void cancelEquityWorkingOrderIfPresent(String accountId, Position equity) {
		List<Order> orders = getCurrentWorkingOrders(accountId);
		List<Order> leo = getWorkingOrderIfPresentForPosition(orders, equity);
		if (leo != null && leo.size() != 0) {
			getClient().cancelOrder(accountId, leo.iterator().next().getOrderId() + "");
		}
	}

	private Position getSellOptionPosition(List<Position> positions, PutCall pc) {

		for (Position p : positions) {

			int contracts = p.getShortQuantity().intValue();
			OptionInstrument oi = (OptionInstrument) p.getInstrument();
			if (contracts > 0 && oi.getPutCall() == pc) {
				return p;
			}
		}
		return null;
	}

	public void placeClosingTradesForOptionsOnDailyExpiringOptions(String accountId, String stock) {

		LocalDate day = getTheImmediateBusinessDay().toLocalDate();
		GroupedPositions gps = getGroupedPositions(accountId);
		List<Position> position = getOptionPositionsThatExpireOnThisDay(gps.getGroupedPosition(stock), day);
		placeClosingTradesForOptionsOnDailyExpiringOptons(accountId, position);
	}

	private OptionSymbolManager optionSymbolManager = new OptionSymbolManager();

	class LongAndShortQuantity {
		int longQuantity;
		int shortQuantity;

		public int getNetQuantity() {
			return longQuantity + shortQuantity;
		}

		public int getLongQuantity() {
			return longQuantity;
		}

		public int getShortQuantity() {
			return shortQuantity;
		}

		public void incrementLongQuantity(int x) {
			longQuantity += x;

		}

	}

	class OptionPositions {
		// List<OptionData> options = new ArrayList<OptionData>();
		public Map<String, OptionData> optionDataMap = new HashMap<String, OptionData>();

		public Map<String, LongAndShortQuantity> optionsWithNetShortAndLong = null;

		public int getOptionsWithNetShortAndLong(OptionData od) {
			String symbol = od.getKeyWithoutStrikePrice();
			LongAndShortQuantity las = optionsWithNetShortAndLong.get(symbol);
			if (las != null) {
				return las.getNetQuantity();
			} else {
				// System.out.println(
				// "getOptionsWithNetShortAndLong " + symbol + " has no LongAndShortQuantity,
				// hence returning 0 ");
				return 0;
			}

		}

		public OptionData findPurchasableOptionDataForOppositeSellCallsOrPuts(OptionData od, Double stockPrice) {
			String key = od.getOppositeKeyWithoutStrikePrice();
			TreeMap<Double, String> keyMap = new TreeMap<Double, String>();

			for (Map.Entry<String, OptionData> entry : this.optionDataMap.entrySet()) {
				String s = entry.getKey();
				OptionData value = entry.getValue();
				if (s.contains(key)) {
					int x = value.getQuantity();
					if (x > 0) {
						continue;
					}
					keyMap.put(value.price.doubleValue(), s);
				}
			}
			Double priceForPurchase = null;
			Double firstKey = null;
			Double lastKey = null;
			if (keyMap.size() == 0) {

				if (od.isPut()) {
					priceForPurchase = stockPrice;

				} else {
					priceForPurchase = stockPrice;

				}
			} else {
				if (od.isPut()) {
					priceForPurchase = keyMap.firstKey();
					firstKey = priceForPurchase;
					if (stockPrice < priceForPurchase) {
						priceForPurchase = stockPrice;
					}

				} else {
					priceForPurchase = keyMap.lastKey();
					lastKey = priceForPurchase;
					if (stockPrice > priceForPurchase) {
						priceForPurchase = stockPrice;
					}
				}

			}
			// System.out.println("findPurchasableOptionDataForOppositeSellCallsOrPuts -> "
			// + priceForPurchase + " -> "
			// + stockPrice + " -> " + firstKey + " -> " + lastKey);

			return new OptionData(od.getStockTicker(), od.getDate(), od.getReversePutOrCall(),
					new BigDecimal(priceForPurchase));

		}

		public Map<String, LongAndShortQuantity> getOptionsWithNetShortAndLong() {
			return optionsWithNetShortAndLong;
		}

		public String getAdjacentHigherSymbol(String optionSymbol) {
			OptionData od = getOptionData(optionSymbol);
			if (od == null) {
				return null;
			}
			return od.adjacentHigherSymbol;
		}

		public String getAdjacentLowerSymbol(String optionSymbol) {
			OptionData od = getOptionData(optionSymbol);
			if (od == null) {
				return null;
			}
			return od.adjacentLowerSymbol;
		}

		public void addOptionPosition(Position p) {
			OptionData od = OptionSymbolParser.parse(p);
			// System.out.println(od + " -> "+od.getQuantity());
			// options.add(od);
			optionDataMap.put(od.symbol, od);
		}

		public void adjustLowerAndHigherAdjacentOptionSymbols(OptionSymbolManager manager) {
			for (OptionData x : optionDataMap.values()) {
				String key = x.symbol;
				OptionSymbolWithAdjacents oswa = manager.getAdjacentOptionSymbols(key);
				adjustLowerAndHigherAdjacentOptionSymbolsIfPresent(x, oswa);
			}
		}

		public void adjustOptionsWithNetShortAndLongBasedOnEquity(GroupedPositions gps) {
			for (GroupedPosition gp : gps.getGroupedPositions()) {
				adjustOptionsWithNetShortAndLongBasedOnEquity(gp);
			}
		}

		private void adjustOptionsWithNetShortAndLongBasedOnEquity(GroupedPosition gp) {
			String symbol = gp.getSymbol();
			int l = gp.getNumberOfPotentialCoveredCallContracts();
			int s = gp.getNumberOfPotentialCoveredPutContracts();
			adjustCovered(l, symbol, "C");
			adjustCovered(s, symbol, "P");

		}

		private void adjustCovered(int x, String symbol, String putOrCall) {
			if (x == 0)
				return;

			for (Map.Entry<String, LongAndShortQuantity> entry : this.optionsWithNetShortAndLong.entrySet()) {
				String key = entry.getKey();
				if (key.endsWith(putOrCall)) {
					if (key.startsWith(symbol)) {
						LongAndShortQuantity lasq = entry.getValue();
						lasq.incrementLongQuantity(x);
						return;
					}
				}
			}
		}

		public List<OptionData> computeOptionsThatNeedProtection() {
			List<OptionData> data = new ArrayList<OptionData>();
			Map<String, LongAndShortQuantity> optionData = new HashMap<String, LongAndShortQuantity>();
			Map<String, TreeSet<Double>> maxPriceMap = new HashMap<String, TreeSet<Double>>();
			for (OptionData x : optionDataMap.values()) {
				String keyWithoutStrikePrice = x.getKeyWithoutStrikePrice();
				LongAndShortQuantity i = optionData.get(keyWithoutStrikePrice);
				if (i == null) {
					i = new LongAndShortQuantity();
					optionData.put(keyWithoutStrikePrice, i);
				}
				int q = x.quantity;
				if (q > 0) {
					i.longQuantity += q;
				}
				if (q < 0) {
					i.shortQuantity += q;
				}
				// optionData.put(keyWithoutStrikePrice, i);

				TreeSet<Double> prices = maxPriceMap.get(keyWithoutStrikePrice);
				if (prices == null) {
					prices = new TreeSet<Double>();
					maxPriceMap.put(keyWithoutStrikePrice, prices);
				}
				if (x.quantity < 0) {
					prices.add(x.price.doubleValue());
				}

			}
			this.optionsWithNetShortAndLong = optionData;
			// System.out.println(optionData);
			List<OptionData> optionsWithNoProtection = new ArrayList<OptionData>();
			for (String k : optionData.keySet()) {
				LongAndShortQuantity las = optionData.get(k);
				int y = las.getNetQuantity();
				if (y < 0) {
					OptionData od = OptionSymbolParser.parse(k + "1");
					od.adjustPriceFromPricesMap(maxPriceMap.get(k));

					od.setQuantity(y);
					data.add(od);
					// System.out.println(od);
					optionsWithNoProtection.add(od);
				}
			}
			return optionsWithNoProtection;
		}

		private void adjustLowerAndHigherAdjacentOptionSymbolsIfPresent(OptionData x, OptionSymbolWithAdjacents oswa) {
			String high = oswa.adjacentHigherSymbol;
			String low = oswa.adjacentLowerSymbol;
			OptionData xx = getOptionData(high);
			if (xx != null) {
				x.adjacentHigherSymbol = xx.symbol;
			}
			xx = getOptionData(low);
			if (xx != null) {
				x.adjacentLowerSymbol = xx.symbol;
			}

		}

		public OptionData getOptionData(String symbol) {
			return optionDataMap.get(symbol);
		}

		public int getShortPositions(String key) {
			KeyMatcher km = (String s) -> {
				return s.equals(key);
			};
			return getShortPositions(km);
		}

		public int getShortPositions(KeyMatcher keyMatcher) {
			int total = 0;
			for (Map.Entry<String, LongAndShortQuantity> entry : optionsWithNetShortAndLong.entrySet()) {
				String s = entry.getKey();
				LongAndShortQuantity value = entry.getValue();
				if (keyMatcher.matches(s)) {
					total += value.getShortQuantity();
				}
			}
			if (total == 0) {
				return 0;
			}
			return -1 * total;
		}

		public int getTotalNumberOfSellCalls(String stockTicker) {
			KeyMatcher km = (String s) -> {
				return s.startsWith(stockTicker) && s.endsWith("C");
			};
			return getShortPositions(km);
		}

	}

	interface KeyMatcher {
		boolean matches(String key);
	}

	private void stripSkippedSymbols(GroupedPositions gps) {
		for (String s : skippedSymbols) {
			gps.removePosition(s);
		}
	}

	public void closeOptionsThatAreInProfitAndPotentiallyOpenNewOnes(String accountId) {
		GroupedPositions gps = getGroupedPositionsWithCurrentStockPrice(accountId);
		gps.setCurrentWorkingOrders(getCurrentWorkingOrders(accountId));
		Map<String, Double> prices = gps.getCurrentEquityPrices();
		System.out.println("Account Id : -> " + accountId + " : Prices -> " + prices);
		stripSkippedSymbols(gps);
		// LocalDate ld = getTheImmediateBusinessDay().toLocalDate();

		OptionPositions op = identifyOptionPositions(accountId, gps, prices);

		// System.out.println("Option positions -> " + accountId + "\n" +
		// Util.toJSON(op));
		closeOptionPositionsIfInProfit(accountId, gps, op, prices);

		closeEquitiesWhenInProfit(accountId, gps);

		placeCoveredCallsForEquities(accountId, gps, op);
	}

	protected OptionPositions identifyOptionPositions(String accountId, GroupedPositions gps,
			Map<String, Double> prices) {
		OptionPositions op = new OptionPositions();
		for (GroupedPosition gp : gps.getGroupedPositions()) {
			List<Position> o = gp.getOptions();
			for (Position p : o) {
				op.addOptionPosition(p);
			}
		}

		// System.out.println(op.compute());
		try {
			op.adjustLowerAndHigherAdjacentOptionSymbols(optionSymbolManager);
			List<OptionData> optionsWithoutProtection = op.computeOptionsThatNeedProtection();
			// System.out.println("Before \n" +Util.toJSON(op));
			// op.adjustOptionsWithNetShortAndLongBasedOnEquity(gps);
			// System.out.println("After \n "+Util.toJSON(op));
			adjustOptionsWithoutProtectionWithCoveredCall(optionsWithoutProtection, gps);
			purchaseLongOptionsIfNeeded(optionsWithoutProtection, prices, accountId, gps);
			// purchaseSellCallOrPutIfLongOptionsExist(op, prices, accountId, gps);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return op;
	}

	protected void closeOptionPositionsIfInProfit(String accountId, GroupedPositions gps, OptionPositions op,
			Map<String, Double> prices) {
		for (GroupedPosition gp : gps.getGroupedPositions()) {
			ProcessSpreadsInput input = makeProcessSpreadsInput(gp);
			boolean b = chain.processSpreads(input);
			if (b == true) {
				System.out.println("Processed Spreads for -> " + gp.getSymbol() + " -> " + gp.getAccountId());
				continue;
			}
			// System.out.println("Processing Option Positions -> " + gp.getSymbol() + " ->
			// " + gp.getAccountId());
			processOptionPositions(accountId, op, prices, gp);
		}
	}

	protected ProcessSpreadsInput makeProcessSpreadsInput(GroupedPosition gp) {
		ProcessSpreadsInput input = new ProcessSpreadsInput();
		input.setAccountId(gp.getAccountId());
		input.setStockTicker(gp.getSymbol());
		input.setGroupedPostion(gp);
		return input;
	}

	private void processOptionPositions(String accountId, OptionPositions op, Map<String, Double> prices,
			GroupedPosition gp) {
		List<Position> o = gp.getOptions();
		// List<Position> o = getOptionPositionsThatExpireOnThisDay(gp, ld);

		for (Position p : o) {
			try {
				String s = p.getInstrument().getSymbol();
				if (s.equals("UNG1_011725C10")) {
					continue;
				}
				PassiveIncomeOptionProcessorInput input = PassiveIncomeOptionProcessorInput.make(p, accountId,
						gp.getSymbol(), prices.get(gp.getSymbol()), op);
				// closeOptionIfInProfitAndPotentiallyOpenNewOne(p, accountId,
				// prices.get(Util.getTickerSymbol(p)),
				// op);
				// chain.closeOptionIfInProfitAndPotentiallyOpenNewOne(x);
				chain.closeOptionIfInProfitAndPotentiallyOpenNewOne(input);
			} catch (Exception e) {
				System.out.println(
						"Exception in processing closeOptionInProfit -> " + Util.toJSON(p) + " -> " + accountId);
				e.printStackTrace();
			}
		}
	}

	private void placeCoveredCallsForEquities(String accountId, GroupedPositions gps, OptionPositions op) {
		for (GroupedPosition gp : gps.getGroupedPositions()) {
			String stockTicker = gp.getSymbol();
			if (stockTicker.equals("QS"))
				continue;
			int x = gp.getNumberOfPotentialCoveredCallContracts();
			if (x != 0) {
				int y = op.getTotalNumberOfSellCalls(stockTicker);
				if (y < x) {
					Ticker ticker = Ticker.make(stockTicker);

					// openNewOptionPositionForSellCallOrPut(accountId, ticker, x-y,
					// OptionInstrument.PutCall.CALL, input);
					try {
						System.out.println("placeCoveredCallsForEquities -> " + y + " -> " + x + " -> " + accountId
								+ " -> " + Util.toJSON(gp) + " -> " + Util.toJSON(op));
						ticker.setOptionSpreadDistance(0.5f);
						ticker.setPrice(gp.getEquity().getAveragePrice());
						List<Order> orders = getCurrentWorkingOrders(accountId);
						Order o = getWorkingOrderForSpreadIfPresent(orders, ticker, x - y,
								OptionInstrument.PutCall.CALL, OrderType.NET_CREDIT,
								OrderLegCollection.Instruction.SELL_TO_OPEN);
						if (o != null) {
							System.out.println("Cancelling an existing working order -> " + Util.toJSON(o));
							getClient().cancelOrder(accountId, o.getOrderId() + "");
						}
						OptionTrade ot = placeNextDayTradeForPassiveIncome(accountId, ticker, ticker.getRollDistance(),
								null, x - y);
						if (!ot.isTradeSuccessful()) {
							LocalDateTime start = LocalDateTime.now();
							LocalDateTime end = start.plusDays(30);
							double avg = ticker.price.doubleValue();
							double priceBegin = Math.max(avg, gp.getCurrentStockPrice());
							Option option = getPossibleOptionThatCanReturnPercentReturn(ticker.ticker, priceBegin, 1d,
									start, end);
							System.out.println(
									"Finding a new possible option for covered calls -> " + Util.toJSON(option));
							if (option != null) {
								Order order = Util.makeSellOptionOrder(option.getSymbol(), x - y);
								getClient().placeOrder(accountId, order);
							}
							// placeOptionTradesForPassiveIncome(accountId, ticker, callDistance, null,
							// numberOfContracts, from, to)

						}
					} catch (Exception e) {
						System.out.println("Exception in placeNextDayTradeForPassiveIncome -> " + accountId + " -> "
								+ Util.toJSON(ticker) + " -> " + stockTicker);
						e.printStackTrace();
					}
					continue;
				}

			}

		}
	}

	private void closeEquitiesWhenInProfit(String accountId, GroupedPositions gps) {
		for (GroupedPosition gp : gps.getGroupedPositions()) {

			Position p = gp.getEquity();
			if (p != null) {
				workOnEquity(accountId, gp, p);
				continue;
			}
			p = gp.getShortEquity();
			if (p != null) {
				workOnEquity(accountId, gp, p);
				continue;
			}
		}
	}

	private Map<String, String> kidsEquityClosures = ExpiringMap.builder().expiration(1, TimeUnit.DAYS).build();

	protected void workOnEquity(String accountId, GroupedPosition gp, Position p) {
		// System.out.println("Equity Position -> " + Util.toJSON(gp));
		EquityInstrument ei = (EquityInstrument) p.getInstrument();
		double purchaseAvgPrice = p.getAveragePrice().doubleValue();
		double marketValue = p.getMarketValue().doubleValue();
		double longQuantity = p.getLongQuantity().doubleValue();

		if (longQuantity > 0) {
			double purchaseValue = longQuantity * purchaseAvgPrice;
			if (marketValue > purchaseValue * 1.01d) {

				if (Util.isAccountForKids(accountId)) {
					closeEquityForKids(accountId, gp, ei, marketValue, longQuantity, purchaseValue);
				} else {
					Order o = Util.makeStockOrder(ei.getSymbol(), new BigDecimal(marketValue / longQuantity),
							(int) longQuantity, Instruction.SELL, OrderType.MARKET);
					System.out.println("Selling Equity -> " + Util.toJSON(o));
					getClient().placeOrder(accountId, o);
				}

				return;
			}
		}
		double shortQuantity = p.getShortQuantity().doubleValue();
		if (shortQuantity > 0) {
			marketValue = marketValue * -1;
			double purchaseValue = shortQuantity * purchaseAvgPrice;
			if (marketValue < purchaseValue * 0.99d) {
				Order o = Util.makeStockOrder(ei.getSymbol(), new BigDecimal(marketValue / shortQuantity),
						(int) shortQuantity, Instruction.BUY_TO_COVER, OrderType.MARKET);
				System.out.println("Selling Equity -> " + Util.toJSON(o));
				// getClient().placeOrder(accountId, o);
				return;
			}
		}

	}

	private void closeEquityForKids(String accountId, GroupedPosition gp, EquityInstrument ei, double marketValue,
			double longQuantity, double purchaseValue) {
		if (marketValue > purchaseValue * 1.25d) {
			String key = accountId + ":" + gp.getSymbol();
			if (kidsEquityClosures.containsKey(key)) {
				System.out.println("Not closing -> " + key + " because it was already sold today");
			} else {
				kidsEquityClosures.put(key, key);
				Order o = Util.makeStockOrder(ei.getSymbol(), new BigDecimal(marketValue / longQuantity), 1,
						Instruction.SELL, OrderType.MARKET);
				System.out.println("Selling Kids Equity -> " + Util.toJSON(o));
				getClient().placeOrder(accountId, o);
				o = Util.makeStockOrder(ei.getSymbol(),
						new BigDecimal(Util.rnd(gp.getCurrentStockPrice() * 0.90)), 1, Instruction.BUY,
						OrderType.LIMIT);

				o.setDuration(Duration.GOOD_TILL_CANCEL);
				o = Util.makeOrderForBuyingStockAtLimitPrice(ei.getSymbol(),
						Util.rnd(gp.getCurrentStockPrice() * 0.90), 1);
				;
				System.out.println("Place a buy order -> " + Util.toJSON(o));
				getClient().placeOrder(accountId, o);
			}
		}
	}

	protected void findPossibleCoveredCallsAndSellCallsOnThem(String accountId, GroupedPosition gp, Position p) {
		int x = gp.getNumberOfPotentialCoveredCallContracts();

	}

	private void adjustOptionsWithoutProtectionWithCoveredCall(List<OptionData> optionsWithoutProtection,
			GroupedPositions gps) {
		List<OptionData> potentialRemovals = new ArrayList<OptionData>();
		for (GroupedPosition gp : gps.getGroupedPositions()) {
			int x = gp.getNumberOfPotentialCoveredCallContracts();
			if (x == 0)
				continue;
			for (OptionData od : optionsWithoutProtection) {
				if (od.getQuantity() >= 0) {
					continue;
				}
				String ticker = od.getStockTicker();
				if (ticker.equals(gp.getSymbol())) {
					int q = -1 * od.getQuantity();
					// System.out.println("adjustOptionsWithoutProtectionWithCoveredCall : before "
					// + q + " : " + x
					// + " -> " + od.getKeyWithoutStrikePrice());
					if (q >= x) {
						q = q - x;
						x = 0;
					} else {
						q = 0;
						x = x - q;
					}
					// System.out.println("adjustOptionsWithoutProtectionWithCoveredCall : after " +
					// q + " : " + x + " -> "
					// + od.getKeyWithoutStrikePrice());
					if (q == 0) {
						potentialRemovals.add(od);
						continue;
					}
					od.setQuantity(q * -1);
					if (x == 0) {
						break;
					}

				}
			}
		}
		optionsWithoutProtection.removeAll(potentialRemovals);

	}

	protected void purchaseSellCallOrPutIfLongOptionsExist(OptionPositions op, Map<String, Double> prices,
			String accountId, GroupedPositions gps) {
		for (Map.Entry<String, LongAndShortQuantity> entry : op.getOptionsWithNetShortAndLong().entrySet()) {
			String keyWithOutPrice = entry.getKey();
			LongAndShortQuantity las = entry.getValue();
			int value = las.getNetQuantity();
			if (value == 0) {
				continue;
			}
			int sc = Math.abs(las.getShortQuantity());
			if (sc >= MAX_SELL_OPTION_COUNT) {
				// System.out.println(
				// "purchaseSellCallOrPutIfLongOptionExist -> Too many sell options and there is
				// no point purchasing another sell option -> "
				// + sc + " -> " + keyWithOutPrice);
				continue;
			}
			OptionData od = OptionSymbolParser.parse(keyWithOutPrice + "1");
			purcahseSellCallOrPutIfLongOptionExists(op, od, prices.get(od.getStockTicker()), accountId, value, 1, gps);
		}

	}

	private void purcahseSellCallOrPutIfLongOptionExists(OptionPositions op, OptionData od, Double stockPrice,
			String accountId, Integer value, int quantity, GroupedPositions gps) {
		// if
		// (stockTickerSymbolsThatCanBeShortedWhenLongExists.contains(od.getStockTicker())
		// == false) {
		// return;
		// }
		if (Util.isLastFewHoursOfTrading(2) && od.isExpiringToday()) {
			// System.out.println("Last 2 hours of trading -> Not Executing
			// purchaseSellCallOrPutIfLongOptionExists -> "
			// + od.getKeyWithoutStrikePrice() + " -> " + accountId + " -> " + value);
			return;
		}
		if (value > 0) {
			// placeNextSmartShortOptionOrder(op, od, stockPrice, accountId, quantity);
			optimizedPlaceNextSmartShortOptionOrder(op, od, accountId, quantity, gps, stockPrice);
		}
	}

	private void optimizedPlaceNextSmartShortOptionOrder(OptionPositions op, OptionData od, String accountId,
			int quantity, GroupedPositions gps, Double stockPrice) {

		OptionData x = op.findPurchasableOptionDataForOppositeSellCallsOrPuts(od, stockPrice);
		x.swapPutOrCall();
		String os = x.getOptionSymbol();
		os = findSellableOption(os, od);
		// Position p = gps.getOptionPositionIfExisting(x);
		Position p = gps.getOptionPositionIfExisting(os);
		boolean placeOrder = false;
		if (p != null) {
			double d = (-0.01) * p.getMarketValue().doubleValue() / p.getShortQuantity().doubleValue();
			double t = p.getAveragePrice().doubleValue();
			double tt = 1.2 * d;
			if (t > tt) {
				System.out.println("-------Placing purchaseSellCallOrPutIfLongOptionExists -> " + os + " -> "
						+ accountId + " -> " + d + " -> " + t + " -> " + tt);
				placeOrder = true;

			} else {
				System.out.println("-------Not Placing purchaseSellCallOrPutIfLongOptionExists -> " + os + " -> "
						+ accountId + " -> " + d + " -> " + t + " -> " + tt);
				placeOrder = false;
			}
		} else {
			System.out.println("---------No Position -> Placing purchaseSellCallOrPutIfLongOptionExists -> " + os
					+ " -> " + accountId + " -> " + Util.toJSON(od) + " -> " + Util.toJSON(x));
			placeOrder = true;
		}

		if (placeOrder) {
			try {
				// os = findSellableOption(os, od);
				placeNextSmartShortOptionOrder(accountId, os, quantity, od.getPutCall());
			} catch (DontBuyOptionException de) {
				System.out
						.println("-----Write Code for DontBuyOptionException -> " + os + " -> " + new java.util.Date());
			}
		}
	}

	private String findSellableOption(String os, OptionData od) {
		OptionSymbolWithAdjacents adj = optionSymbolManager.getAdjacentOptionSymbols(os);
		if (od.isCall()) {
			return adj.adjacentHigherSymbol;
		} else {
			return adj.adjacentLowerSymbol;
		}
	}

	private void purchaseLongOptionsIfNeeded(List<OptionData> optionsWithoutProtection, Map<String, Double> prices,
			String accountId, GroupedPositions gps) {
		for (OptionData od : optionsWithoutProtection) {
			try {
				if (od.getStockTicker().equals("UNG1")) {
					continue;
				}
				int quantity = -1 * od.getQuantity();
				Double currentStockPrice = prices.get(od.getStockTicker());
				String symbol = od.makePossibleProtectionLongOption(currentStockPrice, 10);
				placeNextSmartLongOptionOrder(accountId, symbol, quantity, od.getPutCall());
			} catch (Exception e) {
				System.out.println("Exception in purchaseLongOptionsIfNeeded -> " + accountId + "\n" + Util.toJSON(od));
				e.printStackTrace();
			}
		}

	}

	protected double potentialProfitForOptionPosition(PassiveIncomeOptionProcessorInput input) {
		Position p = input.position;
		double potentialProfitMarketValue = 0;
		OptionInstrument oi = (OptionInstrument) p.getInstrument();
		// String symbol = oi.getSymbol();
		// OptionInstrument.PutCall pc = oi.getPutCall();
		String ticker = oi.getUnderlyingSymbol();
		// System.out.println(Util.toJSON(p));
		int shortQuantity = p.getShortQuantity().intValue();
		int longQuantity = p.getLongQuantity().intValue();
		// double marketValue = p.getMarketValue().doubleValue();
		// SELL PUT or SELL CALL
		// boolean profitable = false;
		// PossibleOptionAndProfit pop = null;
		if (shortQuantity > 0 /* && oi.getPutCall() == OptionInstrument.PutCall.PUT */) {
			potentialProfitMarketValue = getPotentialProfitValueForShortPosition(shortQuantity, input);
			// potentialProfitMarketValue = -1 * shortQuantity *
			// p.getAveragePrice().doubleValue() * 0.1 * 100;
			// pop = isOptionInProfit(marketValue, potentialProfitMarketValue, oi,
			// currentStockPrice);
		}
		if (longQuantity > 0) {
			potentialProfitMarketValue = getPotentialProfitMarketValueForLongPositions(longQuantity, input);
			// pop = isOptionInProfit(marketValue, potentialProfitMarketValue, oi,
			// currentStockPrice);
		}
		return potentialProfitMarketValue;
	}

	protected double getPotentialProfitMarketValueForLongPositions(int longQuantity,
			PassiveIncomeOptionProcessorInput input) {
		Position p = input.position;
		double v = p.getAveragePrice().doubleValue();
		double lg = 0;
		if (v < 0.10d) {
			lg = longGainForContractsLessThan10Cents();

		} else {
			lg = longGain();
		}

		return longQuantity * v * lg * 100;
	}

	protected double longGainForContractsLessThan10Cents() {
		return 10d;
	}

	protected double longGain() {
		return 2.5d;
	}

	protected double getPotentialProfitValueForShortPosition(int shortQuantity,
			PassiveIncomeOptionProcessorInput input) {
		// OptionData od = OptionSymbolParser.parse(p);
		Position p = input.position;
		double multiply = -1 * shortQuantity * 100;
		double price = p.getAveragePrice().doubleValue();
		double marketValue = p.getMarketValue().doubleValue();
		return 0.05d * multiply;
	}

	protected double getPotentialProfiteValueForShortPositionExperimental(int shortQuantity, Position p) {
		double multiply = -1 * shortQuantity * 100;
		double price = p.getAveragePrice().doubleValue();
		if (price > 1 && price <= 5) {
			return 0.10d * multiply;
		}
		if (price > 5 && price <= 10) {
			return 0.25d * multiply;
		}
		if (price > 10) {
			return 0.5d * multiply;
		}

		return 0.05d * multiply;
	}

	private Map<String, String> purchaseSellCallOrPutMapFor15Mins = ExpiringMap.builder()
			.expiration(15, TimeUnit.MINUTES) // Set expiration time
			.build();

	@Override
	public void closeOptionIfInProfitAndPotentiallyOpenNewOne(PassiveIncomeOptionProcessorInput input) {
		OptionPositions op = input.optionPositions;
		Position p = input.position;
		String ticker = input.getStockTicker();
		double currentStockPrice = input.currentStockPrice;
		String accountId = input.getAccountId();
		OptionInstrument oi = (OptionInstrument) p.getInstrument();
		String symbol = oi.getSymbol();

		Ticker tick = Ticker.make(ticker);

		int shortQuantity = p.getShortQuantity().intValue();
		int longQuantity = p.getLongQuantity().intValue();
		double marketValue = p.getMarketValue().doubleValue();

		double potentialProfitMarketValue = potentialProfitForOptionPosition(input);
		PossibleOptionAndProfit pop = isOptionInProfit(marketValue, potentialProfitMarketValue, oi, currentStockPrice);

		if (pop.profitable) {
			System.out.println(
					"Average Price -> " + p.getAveragePrice().doubleValue() + " -> " + Util.toJSON(input.position));
			if (longQuantity > 0) {
				placeNextLongOptionOrder(accountId, oi, shortQuantity, longQuantity, pop, input);
				closeSellOptionAndOpenNewOne(accountId, p, tick, input);
			}
			if (shortQuantity > 0) {
				closeSellOptionAndOpenNewOne(accountId, p, tick, input);
			}
			return;
		}
		if (shortQuantity > 0) {
			double purchasePrice = shortQuantity * p.getAveragePrice().doubleValue() * 100;
			double market = -marketValue;
			double percentage = market / purchasePrice;
			String low = op.getAdjacentLowerSymbol(symbol);

			if (market < 0.20 * purchasePrice) {
				purchaseProtectionLongOrder(accountId, symbol, low, shortQuantity, purchasePrice, market, percentage);
			}
			double limitToPurchaseAgain = 2 * purchasePrice;
			if (market > limitToPurchaseAgain) {
				// purchaseAggressively(input, purchasePrice, market, limitToPurchaseAgain);
			}
		}
		if (shouldOptionsBeRolled(ticker)) {
			Ticker tck = createTickerForRollingOption(input.getStockTicker(), currentStockPrice, pop.getOptionPrice());
			tck.setRollOptionsForNextDayOrWeek(true);
			if (isOptionExpiringToday(oi.getSymbol())) {
				if (isPositionInBorderDuringExpiryTime(input)) {
					System.out
							.println("Not closing the position as it is in boundary -> " + Util.toJSON(input.position));
					return;
				}
				System.out.println("Last minutes of trading and acting on -> " + oi.getSymbol() + " -> " + marketValue
						+ " -> Quantity -> " + (shortQuantity > 0 ? shortQuantity : longQuantity));

				// System.out.println("Last minutes of trading and acting on -> " +
				// oi.getSymbol());

				closeSellOptionAndOpenNewOne(accountId, p, tck, input);
				return;
			}
			OptionData od = input.getOptionData();
			if (od.getDaysToExpiry() <= 2) {
				Double strikePrice = od.getPrice().doubleValue();
				Double stockPrice = input.currentStockPrice;
				Double delta = strikePrice - stockPrice;
				if (Math.abs(delta) <= 5) {
					return;
				}
				Double deviation = delta * 100 / strikePrice;
				if (od.isCall()) {
					if (deviation < -2.5d) {
						System.out.println(
								"shouldOptionsBeRolled() -> Rolling options to next day or week as the stock price has deviated from strike price call -> "
										+ strikePrice + " -> " + stockPrice + " -> " + od.symbol);
						closeSellOptionAndOpenNewOne(accountId, p, tck, input);
						return;
					}
				}
				if (od.isPut()) {
					if (deviation > 2.5d) {
						System.out.println(
								"shouldOptionsBeRolled() -> Rolling options to next day or week as the stock price has deviated from strike price put -> "
										+ strikePrice + " -> " + stockPrice + " -> " + od.symbol);
						closeSellOptionAndOpenNewOne(accountId, p, tck, input);
						return;
					}
				}

			}
		}

	}

	private boolean isPositionInBorderDuringExpiryTime(PassiveIncomeOptionProcessorInput input) {
		if (input.isShort()) {
			System.out.println("Code in isPositionInBorderDuringExpiryTime");
		}
		return false;
	}

	private void purchaseAggressively(PassiveIncomeOptionProcessorInput input, double purchasePrice, double market,
			double limitToPurchaseAgain) {
		String s = input.position.getInstrument().getSymbol();
		String printableString = input.getAccountId() + "," + input.getStockTicker() + "," + s + ","
				+ input.currentStockPrice + "," + market + "," + purchasePrice + "," + limitToPurchaseAgain + ","
				+ new java.util.Date() + "," + Util.toJSON(input.position);
		if (canCreatePurchaseOptions(s)) {
			// System.out.println("Potential to Purchase Again -> " + market + " -> " +
			// purchasePrice + " -> "
			// + limitToPurchaseAgain + " -> " + Util.toJSON(input.position));
			if (!repurchasableTickers.contains(input.getStockTicker())) {
				return;
			}
			System.out.println("Creating a purchase entry for," + printableString);
			purchaseAnotherSellCallOrPutWhenIncreasedBy50PercentIfApplicable(input);
		} else {
			System.out.println("Not Creating a purchase entry for," + printableString);
		}
	}

	private boolean canCreatePurchaseOptions(String symbol) {
		String x = purchaseSellCallOrPutMapFor15Mins.get(symbol);

		if (x == null) {
			purchaseSellCallOrPutMapFor15Mins.put(symbol, symbol);
			return true;
		}
		return false;
	}

	List<String> repurchasableTickers = Arrays.asList("IWM", "SPY", "QQQ", "TQQQ");

	protected void purchaseAnotherSellCallOrPutWhenIncreasedBy50PercentIfApplicable(
			PassiveIncomeOptionProcessorInput input) {

		try {
			String ticker = input.getStockTicker();
			Ticker tick = Ticker.make(ticker, input.getPositionStrikePrice());
			tick.setOptionSpreadDistance(10);
			tick.setRollDistance(0f);
			tick.setOptionData(input.getOptionData());
			// System.out.println("purchaseAnotherSellCallOrPutWhenIncreasedBy50PercentIfApplicable
			// -> "
			// + Util.toJSON(input.position));
			System.out.println(
					"purchaseAnotherSellCallOrPutWhenIncreasedBy50PercentIfApplicable," + input.getPrintableString());
			openNewOptionPositionForSellCallOrPut(input.getAccountId(), tick, 1, input.getPutCall(), input);
			sellShortCallOrPutByFlippingOptionData(input, 1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void sellShortCallOrPutByFlippingOptionData(PassiveIncomeOptionProcessorInput input, int quantity) {
		PassiveIncomeOptionProcessorInput inputClone = new PassiveIncomeOptionProcessorInput();
		String symbol = input.getOptionData().getOptionSymbol();
		String lowerSymbol = optionSymbolManager.getAdjacentOptionSymbols(symbol).adjacentLowerSymbol;
		OptionData od = OptionSymbolParser.parse(lowerSymbol);
		od.swapPutOrCall();
		inputClone.setOptionData(od);
		inputClone.optionPositions = input.optionPositions;
		inputClone.setAccountId(input.getAccountId());
		Ticker tick = Ticker.make(input.getStockTicker(), od.getPrice());
		tick.setOptionData(od);
		tick.setOptionSpreadDistance(10);
		openNewOptionPositionForSellCallOrPut(inputClone, tick, 1);
	}

	protected void openNewOptionPositionForSellCallOrPut(PassiveIncomeOptionProcessorInput input, Ticker ticker,
			int quantity) {
		if (input.getPrintableMessage() != null) {
			System.out.println(input.getPrintableMessage());
		}
		this.openNewOptionPositionForSellCallOrPut(input.getAccountId(), ticker, quantity,
				input.getOptionData().getPutCall(), input);
	}

	private void purchaseProtectionLongOrder(String accountId, String symbol, String low, int shortQuantity,
			double purchasePrice, double market, double percentage) {
		if (low == null) {
			String newLow = optionSymbolManager.getAdjacentOptionSymbols(symbol).adjacentLowerSymbol;
			System.out.println("New Low < 0.20 -> " + newLow + " -> for -> " + symbol + " -> " + purchasePrice + " -> "
					+ market + " -> " + percentage);
			placeProtectionLongOrder(accountId, newLow, OptionSymbolParser.parse(newLow).getPutCall(), shortQuantity);
		}
	}

	public boolean shouldOptionsBeRolled(String ticker) {
		return Util.isLastFewMinutesOfMarketHours(ticker);
	}

	public void placeNextLongOptionOrder(String accountId, OptionInstrument oi, int shortQuantity, int longQuantity,
			PossibleOptionAndProfit pop, PassiveIncomeOptionProcessorInput input) {

		String o = null;

		if (pop.od.isCall()) {
			o = pop.od.getAdjacentHigherOption(getAdjacentWidthForHigherOption(input));
		} else {
			o = pop.od.getAdjacentLowerOption(getAdjacentWidthForLowerOption(input));
		}
		placeNextSmartLongOptionOrder(accountId, o, getQuantity(shortQuantity, longQuantity), oi.getPutCall());

	}

	protected void placeNextSmartLongOptionOrder(String accountId, String symbol, int quantity,
			OptionInstrument.PutCall pc) {

		placeNextSmartMarketOptionOrder(accountId, symbol, quantity, pc, Instruction.BUY_TO_OPEN);
	}

	protected void placeNextSmartShortOptionOrder(String accountId, String symbol, int quantity,
			OptionInstrument.PutCall pc) {
		placeNextSmartMarketOptionOrder(accountId, symbol, quantity, pc, Instruction.SELL_TO_OPEN);
	}

	protected void placeNextSmartMarketOptionOrder(String accountId, String symbol, int quantity,
			OptionInstrument.PutCall pc, Instruction i) throws DontBuyOptionException {
		if (!Util.canOptionsBeTradedNow()) {
			// System.out.println("This is not market hours for trading options and I can't
			// place market orders -> "
			// + accountId + " -> " + symbol + " -> " + quantity);
			return;
		}
		Order order = null;
		try {
			order = Util.makeOption(symbol, quantity, Duration.DAY, null, pc, OrderType.MARKET, i);
			// System.out.println("placeNextSmartLongOptionOrder -> Happy Path - > " +
			// Util.toJSON(order));
			getClient().placeOrder(accountId, order);
		} catch (DontBuyOptionException de) {
			// System.out.println("placeNextSmartOptionOrder -> DontBuyOptionException " +
			// de.getMessage());
			throw de;
		} catch (RuntimeException e) {
			order = Util.makeSmartOptionWhenTheSymbolDoesntExist(symbol, quantity, Duration.DAY, null, pc,
					OrderType.MARKET, i);
			// System.out.println("placeNextSmartLongOptionOrder -> No Option exists path -
			// > " + Util.toJSON(order));
			getClient().placeOrder(accountId, order);
		}
	}

	public int getAdjacentWidthForHigherOption(PassiveIncomeOptionProcessorInput input) {
		OptionData od = input.getOptionData();
		double pick = Util.findPickableStockPrice(input.currentStockPrice, od.getPrice().doubleValue(), od.isCall(),
				10);
		return (int) (pick - input.currentStockPrice);
	}

	public int getAdjacentWidthForLowerOption(PassiveIncomeOptionProcessorInput input) {
		OptionData od = input.getOptionData();
		double pick = Util.findPickableStockPrice(input.currentStockPrice, od.getPrice().doubleValue(), od.isPut(), 5);
		return Math.abs((int) (input.currentStockPrice - pick));

	}

	private void placeProtectionLongOrder(String accountId, String symbol, OptionInstrument.PutCall pc, int quantity) {
		Order order = null;
		try {
			order = Util.makeOption(symbol, quantity, Duration.DAY, null, pc, OrderType.MARKET,
					Instruction.BUY_TO_OPEN);
			// System.out.println(Util.toJSON(order));
			getClient().placeOrder(accountId, order);
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	private Ticker createTickerForRollingOption(String stock, double currentStockPrice, BigDecimal optionStrikePrice) {
		Ticker t = Ticker.make(stock, optionStrikePrice);
		double x = optionStrikePrice.doubleValue() - currentStockPrice;
		if (x > 0) {
			t.setOptionSpreadDistance((int) (x + 5));
		} else {
			t.setOptionSpreadDistance((int) (-x + 5));
		}
		t.setRollDistance(0f);
		t.setRollOptionsForNextDayOrWeek(true);

		return t;
	}

	protected void closeSellOptionAndOpenNewOne(String accountId, Position p, Ticker ticker,
			PassiveIncomeOptionProcessorInput input) {
		// System.out.println("Closing option and opening a new one -> " +
		// Util.toJSON(p));
		closeOptionPositionAtMarketPrice(accountId, p, input);
		openNewOptionPositionForSellCallOrPut(accountId, p, ticker, input);

	}

	protected void openNewOptionPositionForSellCallOrPut(String accountId, Position p, Ticker ticker,
			PassiveIncomeOptionProcessorInput input) {
		OptionInstrument oi = (OptionInstrument) p.getInstrument();
		int shortQuantity = p.getShortQuantity().intValue();
		if (shortQuantity > 0) {
			openNewOptionPositionForSellCallOrPut(accountId, ticker, shortQuantity, oi.getPutCall(), input);
		}
	}

	protected boolean shouldPlaceTradeForNextDayOrWeekOptions(Ticker ticker) {
		return ticker.isRollOptionsForNextDayOrWeek() || Util.isLastHourOfTrading();
	}

	protected void openNewOptionPositionForSellCallOrPut(String accountId, Ticker ticker, int shortQuantity,
			OptionInstrument.PutCall opc, PassiveIncomeOptionProcessorInput input) {

		// RuntimeException e = new RuntimeException();
		// e.printStackTrace();

		if (shortQuantity > MAX_NUMBER_OF_SELL_POSITIONS && !ticker.getRollOptionsForNextDayOrWeek()) {
			System.out.println(
					"Limiting to only MAX options -> " + MAX_NUMBER_OF_SELL_POSITIONS + " from -> " + shortQuantity);
			shortQuantity = MAX_NUMBER_OF_SELL_POSITIONS;

		}

		String key = input.getOptionData().getKeyWithoutStrikePrice();
		int shortPositions = input.optionPositions.getShortPositions(key);
		System.out.println("openNewOptionPositionForSellCallOrPut -> " + shortPositions + " -> " + key);
		if (shortPositions >= MAX_SELL_OPTION_COUNT && !ticker.isRollOptionsForNextDayOrWeek()) {
			System.out.println("Number of Sell Positions is greater than -> " + MAX_SELL_OPTION_COUNT + " from -> "
					+ key + " so not creating more sell positions");
			return;
		}
		if (ticker.isRollOptionsForNextDayOrWeek()) {
			if (opc == OptionInstrument.PutCall.CALL) {
				placeNextWeeklyTradeForPassiveIncome(accountId, ticker, ticker.getRollDistance(), null, shortQuantity);
			}
			if (opc == OptionInstrument.PutCall.PUT) {
				placeNextWeeklyTradeForPassiveIncome(accountId, ticker, null, ticker.getRollDistance(), shortQuantity);
			}
			return;
		}

		if (opc == OptionInstrument.PutCall.CALL) {
			if (shouldPlaceTradeForNextDayOrWeekOptions(ticker)) {
				// System.out.println("Opening a new sell call for Next Day -> " +
				// Util.toJSON(p));
				placeNextDayTradeForPassiveIncome(accountId, ticker, ticker.getRollDistance(), null, shortQuantity);

			} else {
				// System.out.println("Opening a new sell call for Current Day -> " +
				// Util.toJSON(p));
				placeDailyTradeForPassiveIncome(accountId, ticker, ticker.getRollDistance(), null, shortQuantity);
			}
		}
		if (opc == OptionInstrument.PutCall.PUT) {
			if (shouldPlaceTradeForNextDayOrWeekOptions(ticker)) {
				// System.out.println("Opening a new sell put for Next Day -> " +
				// Util.toJSON(p));
				placeNextDayTradeForPassiveIncome(accountId, ticker, null, ticker.getRollDistance(), shortQuantity);
			} else {
				// System.out.println("Opening a new sell put for Current Day -> " +
				// Util.toJSON(p));
				placeDailyTradeForPassiveIncome(accountId, ticker, null, ticker.getRollDistance(), shortQuantity);
			}
		}

	}

	protected int getQuantity(int s, int l) {
		if (s > 0) {
			return s;
		}
		return l + getQuantityIncrement();
	}

	protected int getQuantityIncrement() {
		// return 1;
		return 0;
	}

	protected PossibleOptionAndProfit isOptionInProfit(double marketValue, double potentialProfitMarketValue,
			OptionInstrument oi, double currentStockPrice) {

		PossibleOptionAndProfit pop = new PossibleOptionAndProfit();
		if (marketValue >= potentialProfitMarketValue) {
			// System.out.println("In Profit -> " + d);
			pop.profitable = true;
		} else {
			// System.out.println("No Profit -> " + d);
			pop.profitable = false;
		}
		String symbol = oi.getSymbol();
		OptionData od = OptionSymbolParser.parse(symbol);
		if (pop.profitable == true) {
			// od.setPrice(Util.getLatestTickerPrice(symbol));
			// od.setPrice(currentStockPrice);
			// od.setPrice(Math.floor(currentStockPrice));
			od.adjustPriceForNextOption(currentStockPrice);
		}
		String x = od.getAdjacentHigherOption(1);
		String y = od.getAdjacentLowerOption(1);
		pop.od = od;
		if (marketValue > 0) {
			pop.instruction = Instruction.BUY_TO_OPEN;
			setNextOption(oi, pop, x, y);
		} else {
			pop.instruction = Instruction.SELL_TO_OPEN;
			setNextOption(oi, pop, y, x);
		}
		// String d = oi.getDescription() + "[" + marketValue + " , " +
		// potentialProfitMarketValue + "] : "
		// + pop.nextOption + " :: " + x + " -> " + y;

		return pop;
	}

	private void setNextOption(OptionInstrument oi, PossibleOptionAndProfit pop, String x, String y) {
		if (oi.getPutCall() == OptionInstrument.PutCall.CALL) {
			pop.nextOption = x;
		} else {
			pop.nextOption = y;
		}
	}

	class PossibleOptionAndProfit {
		boolean profitable;
		String nextOption;
		OptionData od;
		Instruction instruction;

		public BigDecimal getOptionPrice() {
			return od.price;
		}

		public OptionData getLatestOptionData(HttpTdaClient client) {
			Quote quote = client.fetchQuote(od.stockTicker);
			// System.out.println(Util.toJSON(quote));
			double price = Util.getPrice(quote);
			return new OptionData(od.stockTicker, od.date, od.putOrCall, new BigDecimal(price));
		}
	}

	private void placeClosingTradesForOptionsOnDailyExpiringOptons(String accountId, List<Position> optionPositions) {
		List<Position> position = optionPositions;
		System.out.println(Util.toJSON(position));
		List<Order> orders = getCurrentWorkingOrders(accountId);
		// System.out.println(Util.toJSON(orders));
		for (Position p : position) {

			List<Order> o = getWorkingOrderIfPresentForPosition(orders, p);
			// System.out.println(Util.toJSON(o));
			if (o.size() == 0) {
				Order order = createPotentialClosingOrder(p);
				if (order != null) {
					getClient().placeOrder(accountId, order);
				}
			}
		}
	}

	public void placeClosingTradesForEquityOnDailyExpiringOptions(String accountId, String stock) {
		LocalDate day = getTheImmediateBusinessDay().toLocalDate();
		GroupedPositions gps = getGroupedPositions(accountId);
		List<Position> position = getOptionPositionsThatExpireOnThisDay(gps.getGroupedPosition(stock), day);
		placeClosingTradesForEquityOnDailyExpiringOptions(accountId, position, gps);
	}

	public void adjustWorkingEquityOrdersByMovingTheClosingPrice(String accountId, String stock,
			double priceAdjustment) {
		List<Order> orders = getCurrentWorkingOrders(accountId);
		for (Order o : orders) {
			OrderLegCollection olc = o.getOrderLegCollection().iterator().next();
			if (olc.getOrderLegType() == OrderLegType.EQUITY) {
				if (olc.getInstruction() == Instruction.SELL_SHORT) {
					if (o.getOrderType() == OrderType.STOP_LIMIT) {
						o.setStopPrice(new BigDecimal(o.getStopPrice().doubleValue() + priceAdjustment));
						o.setPrice(o.getStopPrice());
						// getClient().cancelOrder(accountId, o.getOrderId()+"");
						// o.setOrderId(null);
						getClient().replaceOrder(accountId, o, o.getOrderId() + "");
						continue;
					}
				}
				if (olc.getInstruction() == Instruction.SELL) {
					if (o.getOrderType() == OrderType.LIMIT) {
						// o.setStopPrice(new BigDecimal(o.getStopPrice().doubleValue() +
						// priceAdjustment));
						o.setPrice(new BigDecimal(o.getPrice().doubleValue() + priceAdjustment));
						// getClient().cancelOrder(accountId, o.getOrderId()+"");
						// o.setOrderId(null);
						getClient().replaceOrder(accountId, o, o.getOrderId() + "");
						// getClient().
						continue;
					}
				}
				if (olc.getInstruction() == Instruction.BUY) {
					if (o.getOrderType() == OrderType.STOP_LIMIT) {
						o.setStopPrice(new BigDecimal(o.getStopPrice().doubleValue() - priceAdjustment));
						o.setPrice(o.getStopPrice());
						getClient().replaceOrder(accountId, o, o.getOrderId() + "");
						continue;
					}
				}
			}

		}
	}

	private void placeClosingTradesForEquityOnDailyExpiringOptions(String accountId, List<Position> position,
			GroupedPositions gps) {
		List<Order> orders = getCurrentWorkingOrders(accountId);
		for (Position p : position) {
			OptionInstrument oi = (OptionInstrument) p.getInstrument();
			String us = oi.getUnderlyingSymbol();
			GroupedPosition gp = gps.getGroupedPosition(us);
			int contracts = p.getShortQuantity().intValue();
			int stockCount = contracts * 100;
			BigDecimal strikePrice = findStrikePrice(p.getInstrument().getSymbol(), getPutCallChar(oi.getPutCall()));
			if (contracts > 0) {
				if (oi.getPutCall() == PutCall.CALL) {
					Position le = gp.getEquity();
					if (le == null) {
						placeLongStockOrderForPassiveIncome(accountId, us, strikePrice, stockCount);
						continue;
					}
					List<Order> leo = getWorkingOrderIfPresentForPosition(orders, le);
					if (!isOneOfTheOrderOfParticularInstruction(leo, Instruction.SELL)) {
						BigDecimal d = new BigDecimal(
								rnd(strikePrice.doubleValue() - p.getAveragePrice().doubleValue() / 2));
						Order o = makeStockOrder(us, d, stockCount, Instruction.SELL, OrderType.STOP_LIMIT);
						getClient().placeOrder(accountId, o);
						continue;
					}
				}
				if (oi.getPutCall() == PutCall.PUT) {
					Position se = gp.getShortEquity();
					if (se == null) {
						placeShortStockOrderForPassiveIncome(accountId, us, strikePrice, stockCount);
						continue;
					}
					List<Order> seo = getWorkingOrderIfPresentForPosition(orders, se);
					if (!isOneOfTheOrderOfParticularInstruction(seo, Instruction.BUY_TO_COVER)) {
						BigDecimal d = new BigDecimal(
								rnd(strikePrice.doubleValue() + p.getAveragePrice().doubleValue() / 2));
						Order o = makeStockOrder(us, d, stockCount, Instruction.BUY_TO_COVER, OrderType.STOP_LIMIT);
						getClient().placeOrder(accountId, o);
						continue;
					}
				}
			}

		}
	}

	private boolean isOneOfTheOrderOfParticularInstruction(List<Order> orders, Instruction in) {
		for (Order o : orders) {
			Instruction i = o.getOrderLegCollection().iterator().next().getInstruction();
			if (i == in) {
				return true;
			}
		}
		return false;
	}

	private char getPutCallChar(PutCall pc) {
		if (pc == PutCall.PUT) {
			return 'P';
		}
		return 'C';
	}

	private Order createPotentialClosingOrder(Position p) {
		double d = p.getAveragePrice().doubleValue();
		return createClosingOrder(p, d * 5.0d, 0.01d);
	}

	private LocalDateTime getTheImmediateBusinessDay() {
		return LocalDateTime.now().plusDays(Util.findDaysToExpiration());

	}

	private LocalDateTime getTheNextBusinessDay() {
		LocalDateTime time = getTheImmediateBusinessDay().plusDays(1);
		if (time.getDayOfWeek() == DayOfWeek.SATURDAY) {
			return time.plusDays(2);

		}
		if (time.getDayOfWeek() == DayOfWeek.SUNDAY) {
			return time.plusDays(1);
		}

		return time;
	}

	public void placeWeeklyTradeForPassiveIncome(String accountId, Ticker stockTicker, Float callDistance,
			Float putDistance, int numberOfContracts) {
		LocalDateTime friday = getImmediateFriday();

		LocalDateTime newFrom = friday;
		LocalDateTime newTo = newFrom.plusDays(31);
		placeOptionTradesForPassiveIncome(accountId, stockTicker, callDistance, putDistance, numberOfContracts, newFrom,
				newTo);
	}

	public void placeNextWeeklyTradeForPassiveIncome(String accountId, Ticker stockTicker, Float callDistance,
			Float putDistance, int numberOfContracts) {
		LocalDateTime friday = getNextWeekFriday();
		LocalDateTime newFrom = friday;
		LocalDateTime newTo = newFrom.plusDays(31);
		placeOptionTradesForPassiveIncome(accountId, stockTicker, callDistance, putDistance, numberOfContracts, newFrom,
				newTo);
	}

	private LocalDateTime getNextWeekFriday() {
		// LocalDateTime x = getImmediateFriday();
		return getImmediateFriday().plusDays(7);
	}

	private LocalDateTime getImmediateFriday() {
		LocalDateTime time = LocalDateTime.now();
		DayOfWeek week = time.getDayOfWeek();
		if (week == DayOfWeek.FRIDAY) {
			return time;
		}
		if (week == DayOfWeek.THURSDAY) {
			return time.plusDays(1);
		}
		if (week == DayOfWeek.WEDNESDAY) {
			return time.plusDays(2);
		}
		if (week == DayOfWeek.TUESDAY) {
			return time.plusDays(3);
		}
		if (week == DayOfWeek.MONDAY) {
			return time.plusDays(4);
		}
		if (week == DayOfWeek.SUNDAY) {
			return time.plusDays(5);
		}
		if (week == DayOfWeek.SATURDAY) {
			return time.plusDays(6);
		}

		return time;
	}

	public void placeDailyTradeForPassiveIncome(String accountId, Ticker stockTicker, Float callDistance,
			Float putDistance, int numberOfContracts) {
		LocalDateTime to = getTheImmediateBusinessDay();
		LocalDateTime from = to;
		if (!isTickerHavingOptionsForToday(stockTicker.ticker)) {
			to = getImmediateFriday();
			from = getImmediateFriday();
		}

		LocalDateTime newFrom = getTheImmediateBusinessDay();
		LocalDateTime newTo = newFrom.plusDays(31);
		placeOptionTradesForPassiveIncome(accountId, stockTicker, callDistance, putDistance, numberOfContracts, newFrom,
				newTo);
	}

	private boolean isOptionMarketOpenToday() {
		LocalDateTime time = LocalDateTime.now();
		if (time.getDayOfWeek() == DayOfWeek.SATURDAY || time.getDayOfWeek() == DayOfWeek.SUNDAY) {
			return false;
		}
		int d = time.getDayOfMonth();
		Month m = time.getMonth();
		if ((d == 1 && m == Month.JANUARY)) {
			return false;
		}
		if (m == Month.DECEMBER) {
			if (d == 25 || d == 26) {
				return false;
			}
		}
		if (m == Month.JULY) {
			if (d == 4) {
				return false;
			}
		}
		return true;

	}

	private boolean isTickerHavingOptionsForToday(String stockTicker) {
		boolean b = isOptionMarketOpenToday();
		if (b == false) {
			return false;
		}
		LocalDateTime time = LocalDateTime.now();
		if (time.getDayOfWeek() == DayOfWeek.FRIDAY) {
			return true;
		}

		return isTickerHasZeroDTE(stockTicker);
	}

	private boolean isTickerHasZeroDTE(String stockTicker) {
		boolean b = stockTicker.equals("QQQ") || stockTicker.equals("SPY");
		if (b) {
			return true;
		}
		return false;
	}

	public OptionTrade placeNextDayTradeForPassiveIncome(String accountId, Ticker stockTicker, Float callDistance,
			Float putDistance, int numberOfContracts) {

		LocalDateTime to = getTheNextBusinessDay();
		LocalDateTime today = LocalDateTime.now();

		LocalDateTime from = today;
		// from = getTheNextBusinessDay();

		if (!isTickerHasZeroDTE(stockTicker.ticker)) {
			LocalDateTime x = LocalDateTime.now();
			if (x.getDayOfWeek() == DayOfWeek.FRIDAY) {
				// from = getNextWeekFriday();
				from = today;
				to = getNextWeekFriday();
			} else {
				// from = getImmediateFriday();
				from = today;
				to = getImmediateFriday();
			}
			// System.out.println("placeNextDayTradeForPassiveIncome -> " + stockTicker + "
			// -> " + from + " -> " + to);
		}
		LocalDateTime newFrom = getTheNextBusinessDay();
		LocalDateTime newTo = newFrom.plusDays(31);
		return placeOptionTradesForPassiveIncome(accountId, stockTicker, callDistance, putDistance, numberOfContracts,
				newFrom, newTo);
	}

	public void placeNextWeekTradeForPassiveIncome(String accountId, Ticker stockTicker, Float callDistance,
			Float putDistance, int numberOfContracts) {
		LocalDateTime from = getNextWeekFriday();
		LocalDateTime to = getNextWeekFriday();

		LocalDateTime newFrom = getNextWeekFriday();
		LocalDateTime newTo = newFrom.plusDays(31);

		placeOptionTradesForPassiveIncome(accountId, stockTicker, callDistance, putDistance, numberOfContracts, from,
				to);
	}

	public void placeNextDayTradeForPassiveIncomeOnSellCalls(String accountId, Ticker stockTicker, float callDistance,
			int numberOfContracts) {
		placeNextDayTradeForPassiveIncome(accountId, stockTicker, callDistance, null, numberOfContracts);
	}

	public void placeNextDayTradeForPassiveIncomeOnSellPuts(String accountId, Ticker stockTicker, float putDistance,
			int numberOfContracts) {
		placeNextDayTradeForPassiveIncome(accountId, stockTicker, null, putDistance, numberOfContracts);
	}

	public static class Ticker {
		String ticker;
		BigDecimal price;

		float optionSpreadDistance;

		float rollDistance = 2;

		boolean rollOptionsForNextDayOrWeek = false;

		OptionData optionData;

		public void setOptionData(OptionData od) {
			this.optionData = od;
		}

		public OptionData getOptionData() {
			return optionData;
		}

		public LocalDateTime getOptionExpiryDateIfPresent() {
			if (optionData == null) {
				return null;
			}
			return optionData.getDateTime();
		}

		public void setRollOptionsForNextDayOrWeek(boolean b) {
			rollOptionsForNextDayOrWeek = b;
		}

		public boolean isRollOptionsForNextDayOrWeek() {
			return rollOptionsForNextDayOrWeek;
		}

		public boolean getRollOptionsForNextDayOrWeek() {
			return rollOptionsForNextDayOrWeek;
		}

		public void setRollDistance(float x) {
			this.rollDistance = x;
		}

		public float getRollDistance() {
			return rollDistance;
		}

		public void setOptionSpreadDistance(float x) {
			this.optionSpreadDistance = x;
		}

		public Ticker(String ticker) {
			this.ticker = ticker;
		}

		public Ticker(String ticker, BigDecimal price) {
			this(ticker);
			this.price = price;
		}

		public static Ticker make(String stock) {
			return new Ticker(stock);
		}

		public static Ticker make(String stock, BigDecimal price) {
			return new Ticker(stock, price);
		}

		public String toString() {
			return ticker + " -> " + price + " -> " + optionSpreadDistance;
		}

		public void setPrice(BigDecimal price) {
			this.price = price;
		}
	}

	private float computeOptionSpreadDistance(float os, float tickerOS, Long expiryDate) {
		try {
			if (tickerOS != 0) {
				return tickerOS;
			}
			Instant instant = Instant.ofEpochMilli(expiryDate);
			LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));

			LocalDateTime now = LocalDateTime.now();

			long diff = ChronoUnit.DAYS.between(now, localDateTime);

			// System.out.println("Difference for days -> " + expiryDate + " -> " +
			// localDateTime + " -> [" + diff + "]");
			if (diff > 5) {
				diff -= 2;
			}
			if (diff > 10) {
				diff = 10;
			}
			if (diff <= 0) {
				diff = 1;
			}
			// System.out.println("New calculated spread distance -> " + diff);
			return (int) diff;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		return os;
	}

	public static final double SPREAD_NET_CREDIT = 0.13d;

	public OptionTrade placeOptionTradesForPassiveIncome(String accountId, Ticker ticker, Float callDistance,
			Float putDistance, int numberOfContracts, LocalDateTime from, LocalDateTime to) {

		OptionTrade ot = new OptionTrade();
		HttpTdaClient client = getClient();
		LocalDateTime x = ticker.getOptionExpiryDateIfPresent();
		if (x != null) {
			from = x;
			to = x;
		}
		OptionChainReq request = Util.makeOptionChainRequest(ticker.ticker, from, to);
		// OptionChain chain = client.getOptionChain(stockTicker);
		OptionChain chain = client.getOptionChain(request);
		// System.out.println(Util.toJSON(chain));
		BigDecimal price = chain.getUnderlyingPrice();

		if (ticker.price != null) {
			price = ticker.price;
		}

		float optionSpreadDistance = 1;
		if (putDistance != null) {
			BigDecimal putPrice = findPutPrice(price, putDistance);
			Option sellPutOption = Util.getPutOption(chain, putPrice);
			if (sellPutOption == null) {
				System.out.println("placeOptionTradesForPassiveIncome - Null Sell Option -> " + putPrice + " -> "
						+ price + " -> " + putDistance + " -> " + Util.toJSON(ticker) + " -> " + from + " -> " + to);
				ot.tradeSuccessful = false;
				return ot;
			}
			// Option buyPutOption = getPutOption(chain, new BigDecimal(putPrice.intValue()
			// - optionSpreadDistance));
			optionSpreadDistance = computeOptionSpreadDistance(optionSpreadDistance, ticker.optionSpreadDistance,
					sellPutOption.getExpirationDate());
			Option buyPutOption = Util.getPutOption(chain,
					new BigDecimal(sellPutOption.getStrikePrice().doubleValue() - optionSpreadDistance));
			System.out.println(Util.toJSON(sellPutOption) + " -> " + Util.toJSON(buyPutOption));
			Order putOrder = createSpreadSellOrder(sellPutOption, buyPutOption, numberOfContracts);
			double netCredit = putOrder.getPrice().doubleValue();
			if (netCredit >= SPREAD_NET_CREDIT) {
				// System.out.println(Util.toJSON(putOrder));
				client.placeOrder(accountId, putOrder);
				return ot;
			} else {
				// System.out.println(netCredit + " is less than 0.10 and it's not worth to open
				// a spread for -> "
				// + Util.toJSON(putOrder));
				ot.tradeSuccessful = false;
				return ot;
			}

			// Option sellPutProtectionOption = Util.getPutOption(chain,
			// new BigDecimal(putPrice.intValue() + optionSpreadDistance));
			// Order sellPutProtectionOrder =
			// Util.makeOption(sellPutProtectionOption.getSymbol(), numberOfContracts,
			// Duration.GOOD_TILL_CANCEL, 0.04d, OptionInstrument.PutCall.PUT,
			// OrderType.LIMIT,
			// Instruction.BUY_TO_OPEN);
		}
		if (callDistance != null) {
			BigDecimal callPrice = findCallPrice(price, callDistance);
			Option sellCallOption = Util.getCallOption(chain, callPrice);
			if (sellCallOption == null) {
				System.out.println("Sell Call Option is Null -> " + price + " -> " + callDistance + " -> " + callPrice
						+ " -> " + ticker.ticker + " -> " + from + " -> " + to);
				ot.tradeSuccessful = false;
				return ot;

			}
			// Option buyCallOption = getCallOption(chain, new
			// BigDecimal(callPrice.intValue() + optionSpreadDistance));
			optionSpreadDistance = computeOptionSpreadDistance(optionSpreadDistance, ticker.optionSpreadDistance,
					sellCallOption.getExpirationDate());
			Option buyCallOption = Util.getCallOption(chain,
					new BigDecimal(sellCallOption.getStrikePrice().doubleValue() + optionSpreadDistance));
			System.out.println(Util.toJSON(sellCallOption) + " -> " + Util.toJSON(buyCallOption));
			Order callOrder = createSpreadSellOrder(sellCallOption, buyCallOption, numberOfContracts);
			double netCredit = callOrder.getPrice().doubleValue();
			if (netCredit >= SPREAD_NET_CREDIT) {
				// System.out.println(Util.toJSON(callOrder));
				client.placeOrder(accountId, callOrder);
				return ot;
			} else {
				System.out.println(netCredit + " is less than 0.10 and it's not worth to open a spread for -> "
						+ Util.toJSON(callOrder));
				ot.tradeSuccessful = false;
				return ot;

			}

			// Option sellCallProtectionOption = Util.getCallOption(chain,
			// new BigDecimal(callPrice.intValue() - optionSpreadDistance));
			// Order sellCallProtectionOrder =
			// Util.makeOption(sellCallProtectionOption.getSymbol(), numberOfContracts,
			// Duration.GOOD_TILL_CANCEL, 0.04d, OptionInstrument.PutCall.CALL,
			// OrderType.LIMIT,
			// Instruction.BUY_TO_OPEN);

		}

		// Util.makeOption(String optionSymbol, int quantity, Duration d, double
		// price,OptionInstrument.PutCall pc, OrderType type, Instruction i) {

		// sellPutProtectionOrder.setReleaseTime(new Date(System.currentTimeMillis() +
		// 10000000));
		// sellPutProtectionOrder.setCancelTime(getReleaseDate(from));
		// client.placeOrder(accountId, sellCallProtectionOrder);
		// client.placeOrder(accountId, sellPutProtectionOrder);
		// int numberOfStocks = numberOfContracts * 100;
		// placeLongStockOrderForPassiveIncome(accountId, stockTicker, callPrice,
		// numberOfStocks);
		// client.placeOrder(accountId, putOrder);
		// placeShortStockOrderForPassiveIncome(accountId, stockTicker, putPrice,
		// numberOfStocks);
		return ot;

	}

	private Order createSpreadSellOrder(Option sellOption, Option buyOption, int numberOfContracts) {
		BigDecimal contracts = new BigDecimal(numberOfContracts);
		Order o = new Order();
		o.setOrderType(OrderType.NET_CREDIT);
		o.setSession(Session.NORMAL);
		// o.setDuration(sellOption.get);
		o.setDuration(Duration.GOOD_TILL_CANCEL);
		// o.setOrderType(OrderType.LIMIT);
		// o.setOrderType(OrderType.MARKET);
		BigDecimal x = sellOption.getLastPrice().subtract(buyOption.getLastPrice());
		o.setPrice(x);
		o.setOrderStrategyType(OrderStrategyType.SINGLE);

		List<OrderLegCollection> col = o.getOrderLegCollection();
		col.add(makeOrderLegCollection(sellOption, Instruction.SELL_TO_OPEN, contracts));
		col.add(makeOrderLegCollection(buyOption, Instruction.BUY_TO_OPEN, contracts));
		return o;

	}

	private OrderLegCollection makeOrderLegCollection(Option option, Instruction i, BigDecimal contracts) {
		OrderLegCollection olc = new OrderLegCollection();
		olc.setInstruction(i);
		olc.setQuantity(contracts);
		OptionInstrument oi = new OptionInstrument();
		oi.setSymbol(option.getSymbol());
		oi.setAssetType(AssetType.OPTION);
		oi.setOptionDeliverables(null);
		olc.setInstrument(oi);
		return olc;
	}

	private void placeShortStockOrderForPassiveIncome(String accountId, String stockTicker, BigDecimal stockPrice,
			int numberOfStocks) {
		HttpTdaClient client = getClient();
		// int numberOfStocksForShort = shortStockOrderCanBePlaced(accountId,
		// numberOfStocks, stockTicker);
		int numberOfStocksForShort = numberOfStocks;
		if (numberOfStocksForShort > 0) {

			Order putStockOrder = makePutStockOrder(stockTicker, new BigDecimal(stockPrice.doubleValue() + 0.5f),
					numberOfStocksForShort);
			client.placeOrder(accountId, putStockOrder);
		}
	}

	private void placeLongStockOrderForPassiveIncome(String accountId, String stockTicker, BigDecimal stockPrice,
			int numberOfStocks) {
		HttpTdaClient client = getClient();
		// int numberOfStocksForLong = longStockOrderCanBePlaced(accountId,
		// numberOfStocks, stockTicker);
		int numberOfStocksForLong = numberOfStocks;
		if (numberOfStocksForLong > 0) {
			Order longStockOrder = makeLongStockOrder(stockTicker, new BigDecimal(stockPrice.doubleValue() - 0.5f),
					numberOfStocksForLong);
			client.placeOrder(accountId, longStockOrder);
		}
	}

	private int shortStockOrderCanBePlaced(String accountId, int numberOfStocks, String stockTicker) {
		PositionsHandler handler = new PositionsHandler();
		GroupedPositions gps = handler.getGroupedPositions(accountId);
		GroupedPosition gp = gps.getGroupedPosition(stockTicker);
		if (gp == null) {
			return numberOfStocks;
		}
		Position p = gp.getShortEquity();

		if (p == null) {
			return numberOfStocks;
		}
		double quantity = p.getShortQuantity().doubleValue();
		if (quantity > numberOfStocks) {
			return 0;
		}
		return (int) (numberOfStocks - quantity);

	}

	private int longStockOrderCanBePlaced(String accountId, int numberOfStocks, String stockTicker) {
		PositionsHandler handler = new PositionsHandler();
		GroupedPositions gps = handler.getGroupedPositions(accountId);
		GroupedPosition gp = gps.getGroupedPosition(stockTicker);
		if (gp == null) {
			return numberOfStocks;
		}
		Position p = gp.getEquity();
		if (p == null) {
			return numberOfStocks;
		}
		double quantity = p.getLongQuantity().doubleValue();
		if (quantity > numberOfStocks) {
			return 0;
		}
		return (int) (numberOfStocks - quantity);

	}

	private Order makePutStockOrder(String stockTicker, BigDecimal bigDecimal, int numberOfStocks) {
		return makeStockOrder(stockTicker, bigDecimal, numberOfStocks, Instruction.SELL_SHORT, OrderType.STOP_LIMIT);
	}

	private Order makeStockOrder(String stockTicker, BigDecimal price, int numberOfStocks, Instruction instruction,
			OrderType orderType) {

		return Util.makeStockOrder(stockTicker, price, numberOfStocks, instruction, orderType);
	}

	private Order makeLongStockOrder(String stockTicker, BigDecimal stockPrice, int numberOfStocks) {

		return makeStockOrder(stockTicker, stockPrice, numberOfStocks, Instruction.BUY, OrderType.STOP_LIMIT);
	}

	private Order createSellOrder(Option option, int numberOfContracts) {
		if (option == null)
			return null;
		BigDecimal quantity = new BigDecimal(numberOfContracts);
		Order order = new Order();
		order.setOrderType(OrderType.LIMIT);
		order.setSession(Session.NORMAL);
		order.setDuration(Duration.DAY);
		// order.setQuantity(callQuantity);
		order.setPrice(option.getBidPrice());
		order.setOrderStrategyType(OrderStrategyType.SINGLE);
		// order.setComplexOrderStrategyType(ComplexOrderStrategyType.NONE);

		OrderLegCollection olc = new OrderLegCollection();

		olc.setInstruction(Instruction.SELL_TO_OPEN);
		olc.setQuantity(quantity);
		order.getOrderLegCollection().add(olc);

		OptionInstrument instrument = new OptionInstrument();

		instrument.setSymbol(option.getSymbol());
		instrument.setAssetType(AssetType.OPTION);
		// instrument.setPutCall(OptionInstrument.));
		if (option.getPutCall() == Option.PutCall.PUT) {
			instrument.setPutCall(OptionInstrument.PutCall.PUT);
		} else {
			instrument.setPutCall(OptionInstrument.PutCall.CALL);
		}
		// instrument.setUnderlyingSymbol(gp.symbol);
		instrument.setOptionDeliverables(null);
		olc.setInstrument(instrument);
		// LOGGER.debug(order.toString());
		return order;
	}

	private BigDecimal findPutPrice(BigDecimal price, float putDistance) {
		double p = Math.ceil(price.floatValue() - putDistance);
		return new BigDecimal(p);
	}

	private BigDecimal findCallPrice(BigDecimal price, float callDistance) {
		double p = Math.floor(price.floatValue() + callDistance);
		return new BigDecimal(p);
	}

	@Override
	public boolean processSpreads(ProcessSpreadsInput input) {
		return false;

	}

}
