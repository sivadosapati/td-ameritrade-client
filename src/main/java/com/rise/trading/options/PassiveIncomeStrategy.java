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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.joda.time.Days;

import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.model.account.Duration;
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
import com.studerw.tda.model.option.OptionChainReq.Range;
import com.studerw.tda.model.quote.Quote;

public class PassiveIncomeStrategy extends BaseHandler {

	public static int MAX_NUMBER_OF_SELL_POSITIONS = 10;

	public static void main(String args[]) {
		final PassiveIncomeStrategy pis = new PassiveIncomeStrategy();
		// pis.placeClosingTradesForOptionsOnDailyExpiringOptions(Util.getAccountId4(),"QQQ");
		// pis.placeClosingTradesForEquityOnDailyExpiringOptions(Util.getAccountId4(),"QQQ");
		// pis.closeShortEquitiesIfTheyAreInProfitAndPlaceAnotherOrderIfThereAreSellOptions(Util.getAccountId4(),
		// "QQQ", 1,
//				0.5);

		Runnable r = new Runnable() {
			public void run() {
				System.out.println("Time -> " + new java.util.Date());
				pis.closeOptionsThatAreInProfitAndPotentiallyOpenNewOnes(Util.getAccountId1());
			}
		};
		// ScheduledExecutorService executor =
		// Executors.newSingleThreadScheduledExecutor();
		// executor.scheduleAtFixedRate(r, 0, 5, TimeUnit.MINUTES);
		// new Scanner().start();

		pis.closeOptionsThatAreInProfitAndPotentiallyOpenNewOnes(Util.getAccountId1());
		// pis.closeOptionsThatAreInProfitAndPotentiallyOpenNewOnes(Util.getAccountId6());
		// pis.closeOptionsThatAreInProfitAndPotentiallyOpenNewOnes(Util.getAccountId7());

		System.out.println(pis.getNextWeekFriday());
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

	class OptionPositions {
		List<OptionData> options = new ArrayList<OptionData>();

		public void addOptionPosition(Position p) {
			OptionData od = OptionSymbolParser.parse(p);
			// System.out.println(od + " -> "+od.getQuantity());
			options.add(od);
		}

		public List<OptionData> compute() {
			List<OptionData> data = new ArrayList<OptionData>();
			Map<String, Integer> optionData = new HashMap<String, Integer>();
			for (OptionData x : options) {
				String key = x.stockTicker + "_" + x.date + x.putOrCall;
				Integer i = optionData.get(key);
				if (i == null) {
					i = 0;
				}
				i += x.quantity;
				optionData.put(key, i);

			}
			// System.out.println(optionData);
			for (String k : optionData.keySet()) {
				Integer y = optionData.get(k);
				if (y < 0) {
					OptionData od = OptionSymbolParser.parse(k + "01");
					od.setQuantity(y);
					data.add(od);
					System.out.println(od);
				}
			}
			return data;
		}
	}

	public void closeOptionsThatAreInProfitAndPotentiallyOpenNewOnes(String accountId) {
		GroupedPositions gps = getGroupedPositions(accountId);
		//LocalDate ld = getTheImmediateBusinessDay().toLocalDate();
		Set<String> symbols = new HashSet<String>();
		OptionPositions op = new OptionPositions();
		for (GroupedPosition gp : gps.getGroupedPositions()) {
			List<Position> o = gp.getOptions();
			for (Position p : o) {
				symbols.add(Util.getTickerSymbol(p));
				op.addOptionPosition(p);
			}
		}
		// System.out.println(op.compute());
		op.compute();
		Map<String, Double> prices = Util.getLatestTickerPrices(symbols);
		System.out.println("Account Id : -> " + accountId + " : Prices -> " + prices);
		for (GroupedPosition gp : gps.getGroupedPositions()) {
			List<Position> o = gp.getOptions();
			// List<Position> o = getOptionPositionsThatExpireOnThisDay(gp, ld);

			for (Position p : o) {
				try {
					closeOptionIfInProfitAndPotentiallyOpenNewOne(p, accountId, prices.get(Util.getTickerSymbol(p)));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private double potentialProfitForOptionPosition(Position p) {
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
			potentialProfitMarketValue = -1 * shortQuantity * 0.05d * 100;
			// potentialProfitMarketValue = -1 * shortQuantity *
			// p.getAveragePrice().doubleValue() * 0.1 * 100;
			// pop = isOptionInProfit(marketValue, potentialProfitMarketValue, oi,
			// currentStockPrice);
		}
		if (longQuantity > 0) {
			potentialProfitMarketValue = longQuantity * p.getAveragePrice().doubleValue() * 2.5 * 100;
			// pop = isOptionInProfit(marketValue, potentialProfitMarketValue, oi,
			// currentStockPrice);
		}
		return potentialProfitMarketValue;
	}

	private void closeOptionIfInProfitAndPotentiallyOpenNewOne(Position p, String accountId, double currentStockPrice) {
		OptionInstrument oi = (OptionInstrument) p.getInstrument();

		OptionInstrument.PutCall pc = oi.getPutCall();
		// System.out.println(oi.getSymbol() + " -> ");
		String ticker = oi.getUnderlyingSymbol();
		Ticker tick = Ticker.make(ticker);
		// System.out.println(Util.toJSON(p));
		int shortQuantity = p.getShortQuantity().intValue();
		int longQuantity = p.getLongQuantity().intValue();
		double marketValue = p.getMarketValue().doubleValue();
		// SELL PUT or SELL CALL
		// boolean profitable = false;

		double potentialProfitMarketValue = potentialProfitForOptionPosition(p);
		PossibleOptionAndProfit pop = isOptionInProfit(marketValue, potentialProfitMarketValue, oi, currentStockPrice);

		if (pop.profitable) {
			System.out.println("Average Price -> " + p.getAveragePrice().doubleValue());
			// String option = pop.nextOption;
			// Order order = Util.makeOption(pop.nextOption, 1, pop.od., price, pc, type, i)
			if (longQuantity > 0) {
				placeNextLongOptionOrder(accountId, oi, shortQuantity, longQuantity, pop);
			}
			closeSellOptionAndOpenNewOne(accountId, p, tick);
			return;

		}
		if (Util.isLastFewMinutesOfMarketHours(ticker)) {
			if (isOptionExpiringToday(oi.getSymbol())) {
				System.out.println("Last minutes of trading and acting on -> " + oi.getSymbol());
				Ticker x = createTicker(ticker, currentStockPrice, pop.getOptionPrice());
				closeSellOptionAndOpenNewOne(accountId, p, x);

			}
		}

	}

	private void placeNextLongOptionOrder(String accountId, OptionInstrument oi, int shortQuantity, int longQuantity,
			PossibleOptionAndProfit pop) {
		Order order = null;
		try {
			order = Util.makeOption(pop.nextOption, getQuantity(shortQuantity, longQuantity), Duration.DAY, null,
					oi.getPutCall(), OrderType.MARKET, pop.instruction);
			System.out.println(Util.toJSON(order));
			getClient().placeOrder(accountId, order);
		} catch (RuntimeException e) {
			e.printStackTrace();
			order = Util.makeSmartOptionWhenTheSymbolDoesntExist(pop.nextOption,
					getQuantity(shortQuantity, longQuantity), Duration.DAY, null, oi.getPutCall(), OrderType.MARKET,
					pop.instruction);
			System.out.println(Util.toJSON(order));
			getClient().placeOrder(accountId, order);
		}
	}

	private Ticker createTicker(String stock, double currentStockPrice, BigDecimal optionPrice) {
		Ticker t = Ticker.make(stock, optionPrice);
		double x = optionPrice.doubleValue() - currentStockPrice;
		if (x > 0) {
			t.setOptionSpreadDistance((int) (x + 1));
		} else {
			t.setOptionSpreadDistance((int) (-x + 1));
		}

		return t;
	}

	private void closeSellOptionAndOpenNewOne(String accountId, Position p, Ticker ticker) {
		System.out.println("Closing option and opening a new one -> " + Util.toJSON(p));
		closeOptionPositionAtMarketPrice(accountId, p);
		openNewOptionPositionForSellCallOrPut(accountId, p, ticker);

	}

	private void openNewOptionPositionForSellCallOrPut(String accountId, Position p, Ticker ticker) {
		OptionInstrument oi = (OptionInstrument) p.getInstrument();
		int shortQuantity = p.getShortQuantity().intValue();
		if (shortQuantity > 0) {
			openNewOptionPositionForSellCallOrPut(accountId, ticker, shortQuantity, oi.getPutCall());
		}
	}

	private void openNewOptionPositionForSellCallOrPut(String accountId, Ticker ticker, int shortQuantity,
			OptionInstrument.PutCall opc) {

		if (opc == OptionInstrument.PutCall.CALL) {
			if (Util.isLastHourOfTrading()) {
				// System.out.println("Opening a new sell call for Next Day -> " +
				// Util.toJSON(p));
				placeNextDayTradeForPassiveIncome(accountId, ticker, 0f, null, shortQuantity);

			} else {
				// System.out.println("Opening a new sell call for Current Day -> " +
				// Util.toJSON(p));
				placeDailyTradeForPassiveIncome(accountId, ticker, 1f, null, shortQuantity);
			}
		}
		if (opc == OptionInstrument.PutCall.PUT) {
			if (Util.isLastHourOfTrading()) {
				// System.out.println("Opening a new sell put for Next Day -> " +
				// Util.toJSON(p));
				placeNextDayTradeForPassiveIncome(accountId, ticker, null, 0f, shortQuantity);
			} else {
				// System.out.println("Opening a new sell put for Current Day -> " +
				// Util.toJSON(p));
				placeDailyTradeForPassiveIncome(accountId, ticker, null, 1f, shortQuantity);
			}
		}

	}

	private int getQuantity(int s, int l) {
		if (s > 0) {
			return s;
		}
		return l + 1;
	}

	private PossibleOptionAndProfit isOptionInProfit(double marketValue, double potentialProfitMarketValue,
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
			od.setPrice(Math.floor(currentStockPrice));
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
		String d = oi.getDescription() + "[" + marketValue + " , " + potentialProfitMarketValue + "] : "
				+ pop.nextOption + " :: " + x + " -> " + y;

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
			System.out.println(Util.toJSON(quote));
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

	public void placeDailyTradeForPassiveIncomeOld(String accountId, String stockTicker, float callDistance,
			float putDistance, int numberOfContracts) {
		HttpTdaClient client = getClient();
		LocalDateTime to = getTheImmediateBusinessDay();
		LocalDateTime from = to;
		OptionChainReq request = Util.makeOptionChainRequest(stockTicker, from, to);
		// OptionChain chain = client.getOptionChain(stockTicker);
		OptionChain chain = client.getOptionChain(request);
		// System.out.println(Util.toJSON(chain));
		BigDecimal price = chain.getUnderlyingPrice();
		BigDecimal callPrice = findCallPrice(price, callDistance);
		Option callOption = Util.getCallOption(chain, callPrice);
		BigDecimal putPrice = findPutPrice(price, putDistance);
		Option putOption = Util.getPutOption(chain, putPrice);
		System.out.println(callPrice + " : " + putPrice);
		System.out.println(Util.toJSON(callOption));
		System.out.println(Util.toJSON(putOption));
		Order callOrder = createSellOrder(callOption, numberOfContracts);
		Order putOrder = createSellOrder(putOption, numberOfContracts);
		System.out.println(Util.toJSON(callOrder));
		System.out.println(Util.toJSON(putOrder));
		client.placeOrder(accountId, callOrder);

		int numberOfStocks = numberOfContracts * 100;
		placeLongStockOrderForPassiveIncome(accountId, stockTicker, callPrice, numberOfStocks);
		client.placeOrder(accountId, putOrder);
		placeShortStockOrderForPassiveIncome(accountId, stockTicker, putPrice, numberOfStocks);

	}

	public void placeWeeklyTradeForPassiveIncome(String accountId, Ticker stockTicker, float callDistance,
			float putDistance, int numberOfContracts) {
		LocalDateTime friday = getImmediateFriday();
		placeOptionTradesForPassiveIncome(accountId, stockTicker, callDistance, putDistance, numberOfContracts, friday,
				friday);
	}

	public void placeNextWeeklyTradeForPassiveIncome(String accountId, Ticker stockTicker, float callDistance,
			float putDistance, int numberOfContracts) {
		LocalDateTime friday = getNextWeekFriday();
		placeOptionTradesForPassiveIncome(accountId, stockTicker, callDistance, putDistance, numberOfContracts, friday,
				friday);
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
		placeOptionTradesForPassiveIncome(accountId, stockTicker, callDistance, putDistance, numberOfContracts, from,
				to);
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

	public void placeNextDayTradeForPassiveIncome(String accountId, Ticker stockTicker, Float callDistance,
			Float putDistance, int numberOfContracts) {
		LocalDateTime from = getTheNextBusinessDay();
		LocalDateTime to = getTheNextBusinessDay();

		if (!isTickerHasZeroDTE(stockTicker.ticker)) {
			LocalDateTime x = LocalDateTime.now();
			if (x.getDayOfWeek() == DayOfWeek.FRIDAY) {
				from = getNextWeekFriday();
				to = getNextWeekFriday();
			} else {
				from = getImmediateFriday();
				to = getImmediateFriday();
			}
			System.out.println(stockTicker + " -> " + from + " -> " + to);
		}
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

		int optionSpreadDistance;

		public void setOptionSpreadDistance(int x) {
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
	}

	private int computeOptionSpreadDistance(int os, int tickerOS, Long expiryDate) {
		try {
			if (tickerOS != 0) {
				return tickerOS;
			}
			Instant instant = Instant.ofEpochMilli(expiryDate);
			LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));

			LocalDateTime now = LocalDateTime.now();

			long diff = ChronoUnit.DAYS.between(localDateTime, now);

			System.out.println("Difference for days -> " + expiryDate + " -> " + localDateTime + " -> [" + diff + "]");
			if (diff > 5) {
				diff -= 2;
			}
			if (diff > 10) {
				diff = 10;
			}
			if (diff <= 0) {
				diff = 1;
			}
			System.out.println("New calculated spread distance -> " + diff);
			return (int) diff;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}
		return os;
	}

	public void placeOptionTradesForPassiveIncome(String accountId, Ticker ticker, Float callDistance,
			Float putDistance, int numberOfContracts, LocalDateTime from, LocalDateTime to) {
		HttpTdaClient client = getClient();
		OptionChainReq request = Util.makeOptionChainRequest(ticker.ticker, from, to);
		// OptionChain chain = client.getOptionChain(stockTicker);
		OptionChain chain = client.getOptionChain(request);
		// System.out.println(Util.toJSON(chain));
		BigDecimal price = chain.getUnderlyingPrice();

		if (ticker.price != null) {
			price = ticker.price;
		}
		int optionSpreadDistance = 1;
		if (ticker.optionSpreadDistance != 0) {
			optionSpreadDistance = ticker.optionSpreadDistance;
		}

		if (putDistance != null) {
			BigDecimal putPrice = findPutPrice(price, putDistance);
			Option sellPutOption = Util.getPutOption(chain, putPrice);
			// Option buyPutOption = getPutOption(chain, new BigDecimal(putPrice.intValue()
			// - optionSpreadDistance));
			optionSpreadDistance = computeOptionSpreadDistance(optionSpreadDistance, ticker.optionSpreadDistance,
					sellPutOption.getExpirationDate());
			Option buyPutOption = Util.getPutOption(chain,
					new BigDecimal(sellPutOption.getStrikePrice().doubleValue() - optionSpreadDistance));
			System.out.println(Util.toJSON(sellPutOption) + " -> " + Util.toJSON(buyPutOption));
			Order putOrder = createSpreadSellOrder(sellPutOption, buyPutOption, numberOfContracts);
			System.out.println(Util.toJSON(putOrder));
			client.placeOrder(accountId, putOrder);

			Option sellPutProtectionOption = Util.getPutOption(chain,
					new BigDecimal(putPrice.intValue() + optionSpreadDistance));
			Order sellPutProtectionOrder = Util.makeOption(sellPutProtectionOption.getSymbol(), numberOfContracts,
					Duration.GOOD_TILL_CANCEL, 0.04d, OptionInstrument.PutCall.PUT, OrderType.LIMIT,
					Instruction.BUY_TO_OPEN);
		}
		if (callDistance != null) {
			BigDecimal callPrice = findCallPrice(price, callDistance);
			Option sellCallOption = Util.getCallOption(chain, callPrice);
			// Option buyCallOption = getCallOption(chain, new
			// BigDecimal(callPrice.intValue() + optionSpreadDistance));
			optionSpreadDistance = computeOptionSpreadDistance(optionSpreadDistance, ticker.optionSpreadDistance,
					sellCallOption.getExpirationDate());
			Option buyCallOption = Util.getCallOption(chain,
					new BigDecimal(sellCallOption.getStrikePrice().doubleValue() + optionSpreadDistance));
			System.out.println(Util.toJSON(sellCallOption) + " -> " + Util.toJSON(buyCallOption));
			Order callOrder = createSpreadSellOrder(sellCallOption, buyCallOption, numberOfContracts);
			System.out.println(Util.toJSON(callOrder));
			client.placeOrder(accountId, callOrder);

			Option sellCallProtectionOption = Util.getCallOption(chain,
					new BigDecimal(callPrice.intValue() - optionSpreadDistance));
			Order sellCallProtectionOrder = Util.makeOption(sellCallProtectionOption.getSymbol(), numberOfContracts,
					Duration.GOOD_TILL_CANCEL, 0.04d, OptionInstrument.PutCall.CALL, OrderType.LIMIT,
					Instruction.BUY_TO_OPEN);

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

	}

	private LocalDate getReleaseDate(LocalDateTime x) {
		LocalDateTime ldt = LocalDateTime.now().plusDays(1);
		return ldt.toLocalDate();
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

	private Order createSpreadSellOrderxxx(Option sellOption, Option buyOption, int numberOfContracts) {
		// --------

		if (sellOption == null)
			return null;
		BigDecimal quantity = new BigDecimal(numberOfContracts);
		Order order = new Order();
		order.setOrderType(OrderType.LIMIT);
		order.setSession(Session.NORMAL);
		order.setDuration(Duration.DAY);
		// order.setQuantity(callQuantity);
		order.setPrice(sellOption.getBidPrice());
		order.setOrderStrategyType(OrderStrategyType.SINGLE);
		// order.setComplexOrderStrategyType(ComplexOrderStrategyType.NONE);

		OrderLegCollection olc = new OrderLegCollection();

		olc.setInstruction(Instruction.SELL_TO_OPEN);
		olc.setQuantity(quantity);
		order.getOrderLegCollection().add(olc);

		OptionInstrument instrument = new OptionInstrument();

		instrument.setSymbol(sellOption.getSymbol());
		instrument.setAssetType(AssetType.OPTION);
		// instrument.setPutCall(OptionInstrument.));
		if (sellOption.getPutCall() == Option.PutCall.PUT) {
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

	private void openNewOptionPositionForSellCallOrPutBackup(String accountId, Position p, Ticker ticker) {
		OptionInstrument oi = (OptionInstrument) p.getInstrument();
		int shortQuantity = p.getShortQuantity().intValue();
		int tradeShortQuantity = shortQuantity;
		if (shortQuantity > 0) {
			if (oi.getPutCall() == OptionInstrument.PutCall.CALL) {
				if (Util.isLastHourOfTrading()) {
					// System.out.println("Opening a new sell call for Next Day -> " +
					// Util.toJSON(p));
					placeNextDayTradeForPassiveIncome(accountId, ticker, 0f, null, tradeShortQuantity);

				} else {
					// System.out.println("Opening a new sell call for Current Day -> " +
					// Util.toJSON(p));
					placeDailyTradeForPassiveIncome(accountId, ticker, 1f, null, tradeShortQuantity);
				}
			}
			if (oi.getPutCall() == OptionInstrument.PutCall.PUT) {
				if (Util.isLastHourOfTrading()) {
					// System.out.println("Opening a new sell put for Next Day -> " +
					// Util.toJSON(p));
					placeNextDayTradeForPassiveIncome(accountId, ticker, null, 0f, tradeShortQuantity);
				} else {
					// System.out.println("Opening a new sell put for Current Day -> " +
					// Util.toJSON(p));
					placeDailyTradeForPassiveIncome(accountId, ticker, null, 1f, tradeShortQuantity);
				}
			}
		}
	}

}
