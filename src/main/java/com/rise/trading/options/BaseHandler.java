package com.rise.trading.options;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.rise.trading.options.PassiveIncomeStrategy.Ticker;
import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.model.account.Duration;
import com.studerw.tda.model.account.Instrument;
import com.studerw.tda.model.account.Instrument.AssetType;
import com.studerw.tda.model.account.OptionInstrument;
import com.studerw.tda.model.account.Order;
import com.studerw.tda.model.account.OrderLegCollection;
import com.studerw.tda.model.account.OrderLegCollection.Instruction;
import com.studerw.tda.model.account.OrderRequest;
import com.studerw.tda.model.account.OrderStrategyType;
import com.studerw.tda.model.account.OrderType;
import com.studerw.tda.model.account.Position;
import com.studerw.tda.model.account.SecuritiesAccount;
import com.studerw.tda.model.account.Session;
import com.studerw.tda.model.account.Status;
import com.studerw.tda.model.option.Option;
import com.studerw.tda.model.option.OptionChain;

public class BaseHandler {

	public static void main(String args[]) throws Exception {
		displayPositions();
	}

	public Order getWorkingOrderForSpreadIfPresent(List<Order> orders, Ticker ticker, int quantity,
			OptionInstrument.PutCall putCall, OrderType orderType, OrderLegCollection.Instruction instruction) {
		if (orders == null)
			return null;
		if (orders.size() == 0)
			return null;
		for (Order o : orders) {
			// System.out.println("Working Order -> " + Util.toJSON(o));
			if (o.getQuantity().intValue() == quantity && o.getOrderType() == orderType) {
				List<OrderLegCollection> olc = o.getOrderLegCollection();
				if (olc.size() == 2) {
					OrderLegCollection one = olc.get(0);
					OptionInstrument a = (OptionInstrument) one.getInstrument();
					if (a.getPutCall() == putCall && one.getInstruction() == instruction
							&& a.getUnderlyingSymbol().equals(ticker.ticker)) {
						return o;
					}
				}
			}
		}
		return null;

	}

	private static void displayPositions() throws Exception {
		PositionsHandler h = new PositionsHandler();
		GroupedPositions gps = h.getGroupedPositionsWithCurrentStockPrice(Util.getAccountId4());
		for (GroupedPosition gp : gps.getGroupedPositions()) {
			Position p = gp.getEquity();
			if (p == null)
				continue;
			// System.out.println(gp.getSymbol()+ " -> "+Util.toJSON(p));
			// System.out.println("--------------");
			int x = p.getLongQuantity().intValue();
			if (x < 100)
				continue;
			double avg = p.getAveragePrice().doubleValue();
			System.out.println("--------------");
			System.out.println(gp.getSymbol() + " -> " + x + " -> " + avg + " -> " + gp.getCurrentStockPrice());
			LocalDateTime now = LocalDateTime.now();
			OptionChain oc = Util.getOptionChain(gp.getSymbol(), now, now.plusDays(60));
			getFirstOptionThatCanReturnPercentReturn(oc, Math.max(avg, gp.getCurrentStockPrice().doubleValue()), 1);
		}

	}

	public Option getPossibleOptionThatCanReturnPercentReturn(String ticker, Double priceBegin, Double percentageReturn,
			LocalDateTime start, LocalDateTime end) {
		OptionChain oc = Util.getOptionChain(ticker, start, end);
		return getFirstOptionThatCanReturnPercentReturn(oc, priceBegin, percentageReturn);
	}

	private static Option getFirstOptionThatCanReturnPercentReturn(OptionChain oc, double avg, double percentage) {
		Map<String, Map<BigDecimal, List<Option>>> optionMap = oc.getCallExpDateMap();
		for (Map.Entry<String, Map<BigDecimal, List<Option>>> e : optionMap.entrySet()) {
			String key = e.getKey();
			Map<BigDecimal, List<Option>> value = e.getValue();
			// System.out.println(key);
			for (Map.Entry<BigDecimal, List<Option>> ee : value.entrySet()) {
				BigDecimal k = ee.getKey();
				if (k.doubleValue() < avg) {
					continue;
				}
				if (k.doubleValue() > avg + 5) {
					continue;
				}
				List<Option> v = ee.getValue();

				for (Option o : v) {
					double ask = o.getBidPrice().doubleValue();
					if (ask < avg * 0.01 * percentage)
						continue;
					// System.out.println("\t\t"+Util.toJSON(o));
					System.out.println(key);
					System.out.println("\t" + k);
					System.out.println("\t\t " + o.getSymbol() + " -> " + o.getBidPrice() + " -> " + o.getAskPrice());
					return o;
				}
			}
		}
		return null;

	}

	public HttpTdaClient getClient() {
		return Util.getHttpTDAClient();
	}

	public String toJSON(Object o) {
		return Util.toJSON(o);
	}

	protected GroupedPosition getGroupedPosition(String accountId, String ticker) {
		return getGroupedPositions(accountId).getGroupedPosition(ticker);
	}

	public LocalDate parseDateForOptionSymbol(String symbol) {
		String x[] = symbol.split("_");
		String a = x[1];
		if (a.contains("P")) {
			return parseDateForOption(a.substring(0, a.indexOf("P")));
		}
		return parseDateForOption(a.substring(0, a.indexOf("C")));
	}

	public List<Position> getOptionPositionsThatExpireOnThisDay(GroupedPositions gps, LocalDate day) {
		Collection<GroupedPosition> col = gps.getGroupedPositions();

		List<Position> list = new ArrayList<Position>();
		for (GroupedPosition gp : col) {
			list.addAll(getOptionPositionsThatExpireOnThisDay(gp, day));
		}

		return list;
	}

	public boolean isOptionExpiringOnThisDay(String symbol, LocalDate day) {
		LocalDate positionDayOfExpiry = parseDateForOptionSymbol(symbol);
		if (positionDayOfExpiry.isEqual(day)) {
			return true;
		}
		return false;
	}

	public boolean isOptionExpiringToday(String symbol) {
		return isOptionExpiringOnThisDay(symbol, LocalDate.now());
	}

	protected List<Position> getOptionPositionsThatExpireOnThisDay(GroupedPosition gp, LocalDate day) {

		List<Position> matched = new ArrayList<Position>();
		if (gp == null)
			return matched;
		List<Position> position = gp.getOptions();
		for (Position p : position) {
			String symbol = p.getInstrument().getSymbol();
			boolean b = isOptionExpiringOnThisDay(symbol, day);
			if (b) {
				matched.add(p);
			}
		}
		return matched;
	}

	private LocalDate parseDateForOption(String substring) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMddyy");
		// TemporalAccessor accessor = formatter.parse(substring);

		LocalDate day = LocalDate.parse(substring, formatter);
		return day;
	}

	public GroupedPositions getGroupedPositions(String accountId) {
		final SecuritiesAccount account = getClient().getAccount(accountId, true, true);
		// System.out.println(account.);
		List<Position> positions = account.getPositions();

		// System.out.println(positions);
		GroupedPositions gp = new GroupedPositions(positions, accountId);

		return gp;

	}

	public GroupedPositions getGroupedPositionsWithCurrentStockPrice(String accountId) {
		GroupedPositions gps = getGroupedPositions(accountId);
		Set<String> tickers = gps.findTradeableSymbols();
		Map<String, Double> prices = Util.getLatestTickerPrices(tickers);
		for (Map.Entry<String, Double> e : prices.entrySet()) {
			GroupedPosition gp = gps.getGroupedPosition(e.getKey());
			gp.setCurrentStockPrice(e.getValue());

		}
		return gps;
	}

	public List<Order> getCurrentWorkingOrders(String accountId) {
		OrderRequest request = new OrderRequest();
		List<Order> orders = getClient().fetchOrders(accountId, request);
		List<Order> returnableOrders = new ArrayList<Order>();
		for (Order order : orders) {
			if (order.getStatus() == Status.REJECTED) {
				continue;
			}
			if (order.getStatus() == Status.CANCELED) {
				continue;
			}
			if (order.getStatus() == Status.FILLED) {
				continue;
			}
			if (order.getStatus() == Status.EXPIRED) {
				continue;
			}

			returnableOrders.add(order);
		}
		return returnableOrders;
	}

	public List<Order> getFilledOrders(String accountId) {
		OrderRequest request = new OrderRequest();
		List<Order> orders = getClient().fetchOrders(accountId, request);
		List<Order> returnableOrders = new ArrayList<Order>();
		for (Order order : orders) {

			if (order.getStatus() == Status.FILLED) {
				returnableOrders.add(order);
			}

		}
		return returnableOrders;
	}

	public List<Order> getCurrentWorkingOrders(String accountId, LocalDate day) {
		List<Order> orders = getCurrentWorkingOrders(accountId);
		List<Order> returnableOrders = new ArrayList<Order>();

		for (Order o : orders) {

		}

		return returnableOrders;
	}

	public List<Order> getWorkingOrderIfPresentForPosition(String accountId, Position p) {
		List<Order> orders = getCurrentWorkingOrders(accountId);
		return getWorkingOrderIfPresentForPosition(orders, p);

	}

	public List<Order> getWorkingOrderIfPresentForPosition(List<Order> workingOrders, Position p) {
		// System.out.println(Util.toJSON(workingOrders));
		List<Order> ro = new ArrayList<Order>();
		// Instrument ip = p.getInstrument();
		for (Order o : workingOrders) {
			OrderLegCollection olc = o.getOrderLegCollection().iterator().next();
			if (olc.getInstrument().getSymbol().equals(p.getInstrument().getSymbol())) {
				ro.add(o);
			}
		}
		return ro;
	}

	protected double rnd(double a) {
		DecimalFormat format = new DecimalFormat("0.00");
		return Double.parseDouble(format.format(a));
	}

	protected BigDecimal findStrikePrice(String os, char putOrCallChar) {
		String x = os.substring(os.lastIndexOf(putOrCallChar) + 1);
		return new BigDecimal(x);
	}

	protected void displayProfitOrLossForEachPosition(GroupedPositions gp) {
		for (GroupedPosition x : gp.getGroupedPositions()) {
			for (Position p : x.getOptions()) {
				System.out.println(Util.toJSON(p));
				System.out.println(p.getInstrument().getSymbol() + " -> " + p.getAveragePrice() + " -> "
						+ p.getMarketValue().doubleValue());
			}
		}
	}

	protected void placeProtectionOrdersForShortPositions(String accountId) {
		PositionsHandler ph = new PositionsHandler();
		OrderHandler oh = new OrderHandler();

		GroupedPositions gp = ph.getGroupedPositions(accountId);
		List<Order> orders = oh.getCurrentWorkingOrders(accountId);
		TreeSet<String> positions = new TreeSet<String>();
		for (GroupedPosition x : gp.getGroupedPositions()) {
			for (Position p : x.getOptions()) {
				if (p.getShortQuantity().intValue() > 0) {
					positions.add(p.getInstrument().getSymbol());
				}
				// System.out.println(Util.toJSON(p));
				// System.out.println(p.getInstrument().getSymbol() + " -> " +
				// p.getAveragePrice() + " -> "
				// + p.getMarketValue().doubleValue());
			}
		}
		System.out.println("----");
		TreeSet<String> orderStrings = new TreeSet<String>();
		for (Order order : orders) {
			orderStrings.add(order.getOrderLegCollection().get(0).getInstrument().getSymbol());
		}
		List<Order> placeableOrders = new ArrayList<Order>();
		for (GroupedPosition x : gp.getGroupedPositions()) {
			for (Position p : x.getOptions()) {
				if (p.getShortQuantity().intValue() > 0) {
					// positions.add(p.getInstrument().getSymbol());
					String symbol = p.getInstrument().getSymbol();
					OptionData od = OptionSymbolParser.parse(symbol);
					String h = od.getAdjacentHigherOption(1);
					String l = od.getAdjacentLowerOption(1);
					boolean he = orderStrings.contains(h);
					boolean le = orderStrings.contains(l);
					System.out.println(symbol + "[" + h + " = " + he + " , " + l + " = " + le + "]");
					if (he == false) {
						placeableOrders.add(makeProtectionOrder(h, p));
					}
					if (le == false) {
						placeableOrders.add(makeProtectionOrder(l, p));
					}
					// System.out.println(x + "["+h+" = "+he+" , "+l+" = "+le+"]");
				}
				// System.out.println(Util.toJSON(p));
				// System.out.println(p.getInstrument().getSymbol() + " -> " +
				// p.getAveragePrice() + " -> "
				// + p.getMarketValue().doubleValue());
			}
		}
		for (Order o : placeableOrders) {
			// System.out.println(Util.toJSON(o));
			try {
				getClient().placeOrder(accountId, o);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private Order makeProtectionOrder(String symbol, Position currentPosition) {
		OptionInstrument oi = (OptionInstrument) currentPosition.getInstrument();
		Order order = Util.makeOption(symbol, currentPosition.getShortQuantity().intValue(), Duration.GOOD_TILL_CANCEL,
				0.04d, oi.getPutCall(), OrderType.LIMIT, OrderLegCollection.Instruction.BUY_TO_OPEN);
		return order;
	}

	List<String> tickersThatTradeAfterHoursForOptions = Arrays.asList("QQQ", "SPY", "IWM");

	public void closeOptionPositionAtMarketPrice(String accountId, Position position, PassiveIncomeInput input) {
		List<Order> currentWorkingOrders = input.getCurrentWorkingOrders();
		Order oo = getWorkingOrderHavingPositionIfExists(currentWorkingOrders, position);
		if (oo != null) {
			System.out.println("BaseHandler->closeOptionPositionAtMarketPrice -> Can't close the option position -> "+Util.toJSON(position)+", as we have a working order -> " + Util.toJSON(oo));
			return;
		}

		// BigDecimal callQuantity = new
		// BigDecimal(gp.getNumberOfPotentialCoveredCallContracts());

		OptionInstrument oi = (OptionInstrument) position.getInstrument();
		double marketValue = position.getMarketValue().doubleValue();
		Order order = new Order();
		order.setOrderType(OrderType.MARKET);
		order.setSession(Session.NORMAL);
		order.setDuration(Duration.GOOD_TILL_CANCEL);
		// order.setQuantity(callQuantity);
		int longQuantity = input.getOverrideLongQuantity() == null ? position.getLongQuantity().intValue()
				: input.getOverrideLongQuantity();
		int shortQuantity = input.getOverrideShortQuantity() == null ? position.getShortQuantity().intValue()
				: input.getOverrideShortQuantity();
		order.setOrderStrategyType(OrderStrategyType.SINGLE);
		// order.setComplexOrderStrategyType(ComplexOrderStrategyType.NONE);

		OrderLegCollection olc = new OrderLegCollection();
		order.getOrderLegCollection().add(olc);
		if (longQuantity > 0) {
			// order.setPrice(new BigDecimal(closingPriceForLongPosition));
			// System.out.println("LONG " + longQuantity);
			olc.setQuantity(new BigDecimal(longQuantity));
			olc.setInstruction(Instruction.SELL_TO_CLOSE);
			if (marketValue / longQuantity < 1) {
				// System.out.println("Don't close the position " + oi.getSymbol() + " as the
				// value is less than $0.01");
				return;
			}

		} else {
			// order.setPrice(new BigDecimal(closingPriceForShortPosition));
			// System.out.println("SHORT " + shortQuantity);
			olc.setQuantity(new BigDecimal(shortQuantity));
			olc.setInstruction(Instruction.BUY_TO_CLOSE);
		}
		OptionInstrument instrument = new OptionInstrument();

		instrument.setSymbol(oi.getSymbol());
		instrument.setAssetType(AssetType.OPTION);
		// instrument.setPutCall(PutCall.CALL);
		// instrument.setUnderlyingSymbol(gp.symbol);
		instrument.setOptionDeliverables(null);
		olc.setInstrument(instrument);
		// LOGGER.debug(order.toString());
		System.out.println("closeOptionPositionAtMarketPrice -> " + Util.toJSON(order));
		getClient().placeOrder(accountId, order);

	}

	private Order getWorkingOrderHavingPositionIfExists(List<Order> workingOrders, Position position) {
		if (workingOrders == null) {
			return null;
		}
		for (Order o : workingOrders) {
			if (isOrderAlignedToPosition(o, position)) {
				return o;
			}
		}
		return null;
	}

	private boolean isOrderAlignedToPosition(Order o, Position position) {
		String ps = position.getInstrument().getSymbol();
		List<OrderLegCollection> olc = o.getOrderLegCollection();
		if (olc.size() == 1) {
			OrderLegCollection x = olc.get(0);
			Instrument i = x.getInstrument();
			if (i instanceof OptionInstrument) {
				OptionInstrument oi = (OptionInstrument) i;
				if (oi.getSymbol().equals(ps)) {
					return true;
				}

			}
		}
		return false;
	}

	protected Order createClosingOrder(Position position, double closingPriceForLongPosition,
			double closingPriceForShortPosition) {
		// BigDecimal callQuantity = new
		// BigDecimal(gp.getNumberOfPotentialCoveredCallContracts());
		OptionInstrument oi = (OptionInstrument) position.getInstrument();
		Order order = new Order();
		order.setOrderType(OrderType.LIMIT);
		order.setSession(Session.NORMAL);
		order.setDuration(Duration.GOOD_TILL_CANCEL);
		// order.setQuantity(callQuantity);
		int longQuantity = position.getLongQuantity().intValue();
		int shortQuantity = position.getShortQuantity().intValue();
		order.setOrderStrategyType(OrderStrategyType.SINGLE);
		// order.setComplexOrderStrategyType(ComplexOrderStrategyType.NONE);

		OrderLegCollection olc = new OrderLegCollection();
		order.getOrderLegCollection().add(olc);
		if (longQuantity > 0) {
			order.setPrice(new BigDecimal(closingPriceForLongPosition));
			// System.out.println("LONG " + longQuantity);
			olc.setQuantity(new BigDecimal(longQuantity));
			olc.setInstruction(Instruction.SELL_TO_CLOSE);

		} else {
			order.setPrice(new BigDecimal(closingPriceForShortPosition));
			// System.out.println("SHORT " + shortQuantity);
			olc.setQuantity(new BigDecimal(shortQuantity));
			olc.setInstruction(Instruction.BUY_TO_CLOSE);
		}
		OptionInstrument instrument = new OptionInstrument();

		instrument.setSymbol(oi.getSymbol());
		instrument.setAssetType(AssetType.OPTION);
		// instrument.setPutCall(PutCall.CALL);
		// instrument.setUnderlyingSymbol(gp.symbol);
		instrument.setOptionDeliverables(null);
		olc.setInstrument(instrument);
		// LOGGER.debug(order.toString());
		return order;
	}

}
