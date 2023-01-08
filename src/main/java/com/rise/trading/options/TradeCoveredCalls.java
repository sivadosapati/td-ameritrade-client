package com.rise.trading.options;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.model.account.Duration;
import com.studerw.tda.model.account.EquityInstrument;
import com.studerw.tda.model.account.Instrument;
import com.studerw.tda.model.account.Instrument.AssetType;
import com.studerw.tda.model.account.OptionInstrument;
import com.studerw.tda.model.account.Order;
import com.studerw.tda.model.account.OrderLegCollection;
import com.studerw.tda.model.account.OrderLegCollection.Instruction;
import com.studerw.tda.model.account.OrderStrategy;
import com.studerw.tda.model.account.OrderStrategyType;
import com.studerw.tda.model.account.OrderType;
import com.studerw.tda.model.account.Position;
import com.studerw.tda.model.account.SecuritiesAccount;
import com.studerw.tda.model.account.Session;
import com.studerw.tda.model.option.Option;
import com.studerw.tda.model.option.OptionChain;
import com.studerw.tda.model.option.OptionChainReq;
import com.studerw.tda.model.option.OptionChainReq.Range;

public class TradeCoveredCalls extends BaseHandler {

	private OrderHandler orderHandler = new OrderHandler();
	private PositionsHandler positionsManager = new PositionsHandler();

	public static void main(String[] args) {
		String accountId = Util.getAccountId1();
		TradeCoveredCalls tcc = new TradeCoveredCalls();
		HttpTdaClient client = Util.getHttpTDAClient();
		// tcc.tradeCoveredCallsOnAnAccount(client, accountId);
		// List<Order> orders = tcc.getOrders(client, accountId);
		// String json = Util.toJSON(orders);
		// System.out.println(json);
		// tcc.findPotentialCoveredCallTrades(Util.getAccountId1());
		tcc.displayInstruments(Util.getAccountId1());

	}

	private void displayInstruments(String accountId) {
		GroupedPositions gp = positionsManager.getGroupedPositions(accountId);
		for (GroupedPosition x : gp.getGroupedPositions()) {
			System.out.println(toJSON(x));
			if( x.getEquity()!=null) {
				System.out.println("Equity component -> "+x.getSymbol());
				EquityInstrument ei = (EquityInstrument)x.getEquity().getInstrument();
				System.out.println(toJSON(ei));
				System.out.println(toJSON(x));
			}
		}
	}

	private void findPotentialCoveredCallTrades(String accountId) {
		GroupedPositions gp = positionsManager.getGroupedPositions(accountId);
		for (GroupedPosition x : gp.getGroupedPositions()) {
			Order order = findOrderForCoveredCall(x);
			System.out.println(toJSON(order));
		}
	}

	public List<Order> getOrders(HttpTdaClient client, String accountId) {
		List<Order> orders = client.fetchOrders();
		return orders;
	}

	public void tradeCoveredCallsOnAnAccount(HttpTdaClient client, String accountId) {
		GroupedPositions gp = positionsManager.getGroupedPositions(accountId);
		// System.out.println(gp.toString());
		Set<String> symbols = new HashSet<String>(gp.getSymbols());
		symbols.removeAll(Util.getSymbolsToBeSkipped(accountId));
		// symbols.forEach(System.out::println);
		for (String symbol : symbols) {
			boolean b = gp.isEligibleForCoveredCall(symbol);
			// System.out.println(symbol + " -> " + b);
			if (b == true) {
				// System.out.println("Eligible for covered call trade -> " + symbol + " -> " +
				// accountId);
				placeAnOrderForCoveredCall(accountId, gp.getGroupedPosition(symbol));
			} else {
				// System.out.println("Not Eligible for covered call trade -> " + symbol + " ->
				// " + accountId);

			}
		}
		// displayAccount(account);
	}

	private void placeAnOrderForCoveredCall(String accountId, GroupedPosition gp) {
		Order order = findOrderForCoveredCall(gp);
		// System.out.println(Util.toJSON(order));
		getClient().placeOrder(accountId, order);

	}

	private Order findOrderForCoveredCall(GroupedPosition gp) {
		// OptionChainReq ocr =
		// OptionChainReq.Builder.optionChainReq().withSymbol(gp.getSymbol())
		// .withContractType(OptionChainReq.ContractType.CALL).withRange(Range.NTM)
		// .withToDate(findPotentialWeeklyExpirationDate()).withIncludeQuotes(true)
		// .withUnderlyingPrice(underlyingPrice)
		// .build();
		// OptionChain oc = getClient().getOptionChain(ocr);
		OptionChain oc = orderHandler.getOptionChain(gp.getSymbol(), OptionChainReq.ContractType.CALL,
				findPotentialWeeklyExpirationDate(), Range.NTM);
		// String json = Util.toJSON(oc);
		// System.out.println(json);
		Option option = identifyOptionThatNeedsToBePlacedForCoveredCallInTheCurrentWeekOrNextWeek(oc);
		// System.out.println(Util.toJSON(option));
		Order order = orderHandler.createOrderForCoveredCall(option, gp);
		return order;
	}

	private Option identifyOptionThatNeedsToBePlacedForCoveredCallInTheCurrentWeekOrNextWeek(OptionChain oc) {
		BigDecimal price = oc.getUnderlyingPrice();
		Map<String, Map<BigDecimal, List<Option>>> optionMap = oc.getCallExpDateMap();

		System.out.println(oc.getSymbol());
		System.out.println(toJSON(optionMap));
		if (optionMap.size() == 0) {
			return null;
		}
		// TODO - This needs to be fixed, I'm assuming that only the weekly calls are
		// being returned
		Iterator<Map<BigDecimal, List<Option>>> iterator = optionMap.values().iterator();
		Map<BigDecimal, List<Option>> firstEntry = iterator.next();
		if (shouldPickTheNextWeekExpiringOptions()) {
			firstEntry = iterator.next();
		}
		BigDecimal nearestStrikePrice = orderHandler.getNearestStrikePrice(price, firstEntry.keySet());
		List<Option> options = firstEntry.get(nearestStrikePrice);
		Option o = options.get(0);
		return o;

	}

	private boolean shouldPickTheNextWeekExpiringOptions() {
		LocalDateTime ldt = LocalDateTime.now();
		// System.out.println(ldt.getHour());
		if (ldt.getDayOfWeek() == DayOfWeek.THURSDAY) {
			// Pick the options for next week after 10AM on Thursday
			if (ldt.getHour() > 10) {
				return true;
			}
			return false;
		}
		if (ldt.getDayOfWeek() == DayOfWeek.FRIDAY) {
			return true;
		}

		return false;
	}

	private LocalDateTime findPotentialWeeklyExpirationDate() {
		LocalDateTime ldt = LocalDateTime.now();
		return ldt.plusDays(14);
	}

	private Order simpleOrder() {
		Order order = new Order();
		order.setOrderType(OrderType.MARKET);
		order.setSession(Session.NORMAL);
		order.setDuration(Duration.DAY);
		order.setOrderStrategyType(OrderStrategyType.SINGLE);

		OrderLegCollection olc = new OrderLegCollection();
		olc.setInstruction(Instruction.SELL);
		olc.setQuantity(new BigDecimal("360.888"));
		order.getOrderLegCollection().add(olc);

		Instrument instrument = new EquityInstrument();
		instrument.setSymbol("F");
		olc.setInstrument(instrument);
		// LOGGER.debug(order.toString());
		return order;
	}

	private void displayAccount(SecuritiesAccount account) {
		System.out.println("Print Positions");
		for (Position p : account.getPositions()) {
			System.out.println(p);
		}
		List<OrderStrategy> orders = account.getOrderStrategies();
		System.out.println("Order Positions");
		for (OrderStrategy os : orders) {
			System.out.println(os);
		}
	}

}
