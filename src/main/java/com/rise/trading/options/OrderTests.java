package com.rise.trading.options;

import static com.rise.trading.options.Util.getAccountId1;
import static com.rise.trading.options.Util.getHttpTDAClient;
import static com.rise.trading.options.Util.makeOptionWithClosingOrderForSellCallOrPutWithVerticalProtection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.client.TdaClient;
import com.studerw.tda.model.account.Duration;
import com.studerw.tda.model.account.Instrument;
import com.studerw.tda.model.account.OptionInstrument;
import com.studerw.tda.model.account.OptionInstrument.PutCall;
import com.studerw.tda.model.account.Order;
import com.studerw.tda.model.account.OrderLegCollection;
import com.studerw.tda.model.account.OrderLegCollection.Instruction;
import com.studerw.tda.model.account.OrderStrategyType;
import com.studerw.tda.model.account.OrderType;
import com.studerw.tda.model.account.Position;
import com.studerw.tda.model.quote.Quote;

public class OrderTests extends BaseHandler {

	private static void dailySellCalls() {
		OrderTests.dailySellCalls(Util.getAccountId1(), "QQQ", 1, 3, 0.25);
		// OrderTests.dailySellCalls(Util.getAccountId1(), "SPY", 1, 3, 0.25);

	}

	private static void dailySellPuts() {
		OrderTests.dailySellPuts(Util.getAccountId1(), "QQQ", 1, 3, 0.25);
		// OrderTests.dailySellPuts(Util.getAccountId1(), "SPY", 1, 3, 0.25);
	}

	public static void mainyyy(String args[]) throws Exception {

		placeOrdersForOptionsExpiringToday(getAccountId1());
	}

	public static void main(String args[]) throws Exception {
		String account = Util.getAccountId6();
		Order x = Util.makeOption("SPY_031924P490", 1, Duration.GOOD_TILL_CANCEL, 0.05d, OptionInstrument.PutCall.PUT,
				OrderType.LIMIT, Instruction.BUY_TO_OPEN);
		// Date d = getReleaseTime();
		LocalDate ld = getCancelTime();
		x.setCancelTime(ld);
		//x.setCancelable(true);
		//x.setEditable(true);
		//x.setReleaseTime(getReleaseTime());
		getHttpTDAClient().placeOrder(account, x);

		fetchOrders(account);
	}

	public static void mainxxxx(String args[]) {
		String stock = "IWM";
		Quote quote = Util.getHttpTDAClient().fetchQuote(stock);
		double price = Util.getPrice(quote);
		System.out.println(price);
	}

	public static void main1234(String args[]) {
		String stock = "IWM";
		Quote quote = Util.getHttpTDAClient().fetchQuote(stock);
		double price = Util.getPrice(quote);
		System.out.println(price);
		Order order = Util.makeOrderForBuyingStockAtLimitPrice(stock, price + 0.01d, 1);
		// getHttpTDAClient().placeOrder(Util.getAccountId1(), order);

		// getHttpTDAClient().pla

		List<Order> orders = new OrderTests().getCurrentWorkingOrders(Util.getAccountId1());
		for (Order o : orders) {
			System.out.println(Util.toJSON(o));
		}
	}

	public static void main123(String x[]) throws Exception {
		String account = Util.getAccountId1();

		// oh.displayProfitOrLossForEachPosition(gp);
		OrderTests ot = new OrderTests();
		ot.placeProtectionOrdersForShortPositions(account);
	}

	private static Date getReleaseTime() {
		Date d = new Date(123, 12, 26, 06, 30, 30);
		return d;
	}

	private static LocalDate getCancelTime() {
		LocalDateTime time = java.time.LocalDateTime.now();
		return time.plusDays(1).toLocalDate();

	}

	public static void mainaaaa(String args[]) throws Exception {
		String acct = Util.getAccountId6();
		OptionSellCallPut oscp = makeOptionSellCallPutForMarket("SPY", "092723", 427.15, 1, OrderType.LIMIT);
		testAdvancedSellOrders(acct, oscp.getSellOrderForCall(), 1, 1);
		testAdvancedSellOrders(acct, oscp.getSellOrderForPut(), 1, 1);
	}

	public static void mainxxx(String args[]) throws Exception {
		fetchOrders(Util.getAccountId1());
		BigDecimal originalValue = new BigDecimal("0.010001232132675");

		// Round to 2 decimal places using RoundingMode.HALF_UP
		BigDecimal roundedValue = originalValue.setScale(2, RoundingMode.HALF_UP);

		System.out.println("Original Value: " + originalValue);
		System.out.println("Rounded Value: " + roundedValue);

		// OrderTests.dailySellCalls(Util.getAccountId1(), "QQQ", 1, 3, 0.20);
		// OrderTests.dailySellPuts(Util.getAccountId1(), "QQQ", 1, 3, 0.20);
		OptionSellCallPut oscp = makeOptionSellCallPutForMarket("QQQ", "091223", 376.87, 0.2, OrderType.LIMIT);
		// testAdvancedSellOrders(getAccountId1(), oscp.getSellOrderForCall(), 5, 3);
		// testAdvancedSellOrders(getAccountId1(), oscp.getSellOrderForPut(), 5, 3);

		dailySellCalls();
		dailySellPuts();
	}

	static class OrderPlacements {
		List<Order> newOrders = new ArrayList<Order>();
		List<Order> replacableOrders = new ArrayList<Order>();
	}

	public static void placeOrdersForOptionsExpiringToday(String accountId) {
		LocalDate ld = LocalDate.now().plusDays(1);
		OrderPlacements op = findPotentialOrderPlacements(accountId, ld);
	}

	private static OrderPlacements findPotentialOrderPlacements(String accountId, LocalDate ld) {
		OrderPlacements op = new OrderPlacements();
		List<Order> orders = new OrderHandler().getCurrentWorkingOrders(accountId);
		PositionsHandler ph = new PositionsHandler();
		GroupedPositions gp = ph.getGroupedPositions(accountId);
		List<Position> ps = ph.getOptionPositionsThatExpireOnThisDay(gp, ld);
		for (Position p : ps) {
			Instrument i = p.getInstrument();
			if (i instanceof OptionInstrument) {
				System.out.println(Util.toJSON(p));
				int x = p.getShortQuantity().intValue();
				if (x != 0) {
					findPotentialProtectingWorkingOrderForShortPositionAndPlaceInOrderPlacements(p, orders);
				}
			}
		}

		return op;
	}

	private static void findPotentialProtectingWorkingOrderForShortPositionAndPlaceInOrderPlacements(Position p,
			List<Order> orders) {
		//

	}

	public static void mainxx(String[] args) throws Exception {
		// replaceOrder();
		// testOrderChain();
		// testSellCallOrderChain();
		// fetchOrders(Util.getAccountId6());
		// testSellCallOrderForQQQ();
		// testSellPutOrderForQQQ();
		OptionSellCallPut oscp = new OptionSellCallPut("QQQ", "091223", 1, 1, 378, 376, 370, 384, 0.20,
				OrderType.LIMIT);
		String symbol = "QQQ";

		double price = Util.getLatestTickerPrice(symbol);
		String date = getDate();
		System.out.println(price + "->" + date);

		// OptionSellCallPut oscp = makeOptionSellCallPutForMarket(symbol, date, price,
		// 0.25, OrderType.MARKET);
		// testSellOrderForQQQ(Util.getAccountId6(),oscp, 1, 5);
		// testAdvancedSellOrders(Util.getAccountId1(), oscp, 2, 3);
		String accountId = Util.getAccountId1();
		int numberOfContracts = 3;
		int numberOfCycles = 1;
		// testAdvancedSellOrders(accountId, oscp.getSellOrderForCall(),
		// numberOfContracts, numberOfCycles);
		// testAdvancedSellOrders(accountId, oscp.getSellOrderForPut(),
		// numberOfContracts, numberOfCycles);

		// Util.getHttpTDAClient().placeOrder(Util.getAccountId1(),
		// Util.makeOptionWithClosingOrderForSellCallsOrPuts("QQQ_091223C378", 5,
		// Duration.GOOD_TILL_CANCEL, 1, OptionInstrument.PutCall.CALL, OrderType.LIMIT,
		// Instruction.SELL_TO_OPEN));
		// Util.getHttpTDAClient().placeOrder(Util.getAccountId1(),Util.makeOptionWithClosingOrderForSellCallsOrPuts("QQQ_091223P376",
		// 5, Duration.GOOD_TILL_CANCEL, 1, OptionInstrument.PutCall.PUT,
		// OrderType.LIMIT, Instruction.SELL_TO_OPEN));

		getHttpTDAClient().placeOrder(getAccountId1(),
				makeOptionWithClosingOrderForSellCallOrPutWithVerticalProtection("QQQ_091223C378", "QQQ_091223C383", 5,
						Duration.GOOD_TILL_CANCEL, 1, OptionInstrument.PutCall.CALL, OrderType.LIMIT,
						Instruction.SELL_TO_OPEN));
		getHttpTDAClient().placeOrder(getAccountId1(),
				makeOptionWithClosingOrderForSellCallOrPutWithVerticalProtection("QQQ_091223P376", "QQQ_091223P371", 5,
						Duration.GOOD_TILL_CANCEL, 1, OptionInstrument.PutCall.PUT, OrderType.LIMIT,
						Instruction.SELL_TO_OPEN));
	}

	public static void dailySellCalls(String account, String stock, int numberOfContracts, int numberOfCycles,
			double stopLoss) {
		Quote quote = Util.getHttpTDAClient().fetchQuote(stock);
		double price = Util.getPrice(quote);
		String date = getDate();
		OptionSellCallPut oscp = makeOptionSellCallPutForMarket(stock, date, price, stopLoss, OrderType.MARKET);
		testAdvancedSellOrders(account, oscp.getSellOrderForCall(), numberOfContracts, numberOfCycles);
	}

	public static void dailySellPuts(String account, String stock, int numberOfContracts, int numberOfCycles,
			double stopLoss) {
		Quote quote = Util.getHttpTDAClient().fetchQuote(stock);
		double price = Util.getPrice(quote);
		String date = getDate();
		OptionSellCallPut oscp = makeOptionSellCallPutForMarket(stock, date, price, stopLoss, OrderType.MARKET);
		testAdvancedSellOrders(account, oscp.getSellOrderForPut(), numberOfContracts, numberOfCycles);

	}

	private static String getDate() {

		Date d = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("MMddYY");
		// return "090723";
		return sdf.format(d);
	}

	private static OptionSellCallPut makeOptionSellCallPutForMarket(String stock, String date, double stockPrice,
			double stopLossForLong, OrderType orderType) {
		int callStrike = (int) Math.ceil(stockPrice) + 1;
		int putStrike = (int) Math.floor(stockPrice) - 1;
		int callLong = callStrike - 9;
		int putLong = putStrike + 9;
		int callPremium = 1;
		int putPremium = 1;
		return new OptionSellCallPut(stock, date, callPremium, putPremium, callStrike, putStrike, callLong, putLong,
				stopLossForLong, orderType);

	}

	private static void fetchOrders(String acct) throws Exception {

		HttpTdaClient client = Util.getHttpTDAClient();
		List<Order> orders = new OrderHandler().getCurrentWorkingOrders(acct);
		System.out.println(Util.toJSON(orders));
	}

	static class OptionSellCallPut {
		double callStrike;
		double putStrike;
		double callLong;
		double putLong;
		double callPremium;
		double putPremium;
		double stopLossForLongs;
		String date;
		String stock;
		OrderType orderType;

		public OptionSellCallPut(String stock, String date, double callPremium, double putPremium, double callStrike,
				double putStrike, double callLong, double putLong, double stopLossForLongs, OrderType orderType) {
			this.stock = stock;
			this.date = date;
			this.callPremium = callPremium;
			this.putPremium = putPremium;
			this.callStrike = callStrike;
			this.putStrike = putStrike;
			this.callLong = callLong;
			this.putLong = putLong;
			this.stopLossForLongs = stopLossForLongs;
			this.orderType = orderType;
		}

		private String round(double price) {
			return "" + (int) price;
		}

		public String getShortCallOption() {
			return stock + "_" + date + "C" + round(callStrike);
		}

		public String getShortVerticalCallOption() {
			return stock + "_" + date + "C" + round(callStrike + 5);
		}

		public String getLongCallOption() {
			return stock + "_" + date + "C" + round(callLong);
		}

		public String getShortPutOption() {
			return stock + "_" + date + "P" + round(putStrike);
		}

		public String getShortVerticalPutOption() {
			return stock + "_" + date + "P" + round(putStrike - 5);
		}

		public String getLongPutOption() {
			return stock + "_" + date + "P" + round(putLong);
		}

		public double getLongCallOptionPrice() {
			return callStrike - callLong - stopLossForLongs;
		}

		public double getLongPutOptionPrice() {
			return putLong - putStrike - stopLossForLongs;
		}

		public double getLongCallStopLossPrice() {
			return getLongCallOptionPrice() - stopLossForLongs;
		}

		public double getLongPutStopLossPrice() {
			return getLongPutOptionPrice() - stopLossForLongs;
		}

		public SellOrder getSellOrderForPut() {
			return new SellOrder() {

				@Override
				public String getShortOption() {
					return getShortPutOption();
				}

				@Override
				public String getLongOption() {
					return getLongPutOption();
				}

				@Override
				public PutCall getPutOrCall() {
					return OptionInstrument.PutCall.PUT;
				}

				@Override
				public double getShortPremium() {
					return putPremium;
				}

				@Override
				public double getLongPremium() {
					return getLongPutOptionPrice();
				}

				@Override
				public double getLongStopLoss() {
					return getLongPutStopLossPrice();
				}

				@Override
				public SellOrder advanceSellOrderForUpwardDirection() {
					return getBackwardOptionSellCallPut().getSellOrderForPut();
				}

				@Override
				public OrderType getOpeningOrderType() {
					return OptionSellCallPut.this.orderType;
				}

				@Override
				public SellOrder oppositeSellOrder() {
					return getSellOrderForCall();
				}

				@Override
				public String getShortVertical() {
					return getShortVerticalPutOption();
				}

			};
		}

		public OptionSellCallPut getAdvanceOptionSellCallPut() {
			OptionSellCallPut oscp = new OptionSellCallPut(stock, date, callPremium, putPremium, callStrike + 1,
					putStrike + 1, callLong + 1, putLong + 1, stopLossForLongs, OrderType.MARKET);
			return oscp;
		}

		public OptionSellCallPut getBackwardOptionSellCallPut() {
			OptionSellCallPut oscp = new OptionSellCallPut(stock, date, callPremium, putPremium, callStrike - 1,
					putStrike - 1, callLong - 1, putLong - 1, stopLossForLongs, OrderType.MARKET);
			return oscp;
		}

		public SellOrder getSellOrderForCall() {
			return new SellOrder() {

				@Override
				public String getShortOption() {
					return getShortCallOption();
				}

				@Override
				public String getLongOption() {
					return getLongCallOption();
				}

				@Override
				public PutCall getPutOrCall() {
					return OptionInstrument.PutCall.CALL;
				}

				@Override
				public double getShortPremium() {
					return callPremium;
				}

				@Override
				public double getLongPremium() {
					return getLongCallOptionPrice();
				}

				@Override
				public double getLongStopLoss() {
					return getLongCallStopLossPrice();
				}

				@Override
				public SellOrder advanceSellOrderForUpwardDirection() {
					return getAdvanceOptionSellCallPut().getSellOrderForCall();
				}

				@Override
				public OrderType getOpeningOrderType() {
					return OptionSellCallPut.this.orderType;
				}

				@Override
				public SellOrder oppositeSellOrder() {
					return getSellOrderForPut();
				}

				@Override
				public String getShortVertical() {
					return getShortVerticalCallOption();
				}

			};
		}
	}

	private static void testAdvancedSellOrders(String accountId, OptionSellCallPut oscp, int numberOfContracts,
			int numberOfCycles) {
		testAdvancedSellOrders(accountId, oscp.getSellOrderForPut(), numberOfContracts, numberOfCycles);
		testAdvancedSellOrders(accountId, oscp.getSellOrderForCall(), numberOfContracts, numberOfCycles);
	}

	static interface SellOrder {
		String getShortOption();

		String getShortVertical();

		String getLongOption();

		OptionInstrument.PutCall getPutOrCall();

		double getShortPremium();

		double getLongPremium();

		double getLongStopLoss();

		SellOrder advanceSellOrderForUpwardDirection();

		OrderType getOpeningOrderType();

		SellOrder oppositeSellOrder();
	}

	private static void testAdvancedSellOrders(String accountId, SellOrder sellOrder, int numberOfContracts,
			int numberOfCycles) {
		Order order = makeAdvancedSellOrder(sellOrder, numberOfContracts, numberOfCycles);
		String acct = accountId;
		HttpTdaClient client = Util.getHttpTDAClient();
		client.placeOrder(acct, order);

	}

	private static Order makeAdvancedSellOrder(SellOrder sellOrder, int numberOfContracts, int numberOfCycles) {
		Duration d = Duration.DAY;
		Order order = Util.makeOptionWithClosingOrderForSellCallOrPutWithVerticalProtection(sellOrder.getShortOption(),
				sellOrder.getShortVertical(), numberOfContracts, d, sellOrder.getShortPremium(),
				sellOrder.getPutOrCall(), sellOrder.getOpeningOrderType(), Instruction.SELL_TO_OPEN);
		Order first = order;
		// first.setOrderStrategyType(OrderStrategyType.TRIGGER);
		for (int i = 0; i < numberOfCycles; i++) {
			OrderType stop = OrderType.STOP_LIMIT;
			int longContracts = (i + 1) * numberOfContracts;
			//// Calls
			Order x = Util.makeOption(sellOrder.getLongOption(), longContracts, d, sellOrder.getLongPremium(),
					sellOrder.getPutOrCall(), stop, Instruction.BUY_TO_OPEN);
			// x.setOrderStrategyType(OrderStrategyType.TRIGGER);
			Order y = Util.makeOption(sellOrder.getLongOption(), longContracts, d, sellOrder.getLongStopLoss(),
					sellOrder.getPutOrCall(), stop, Instruction.SELL_TO_CLOSE);
			// y.setOrderStrategyType(OrderStrategyType.TRIGGER);
			Order z = Util.makeOptionWithClosingOrderForSellCallOrPutWithVerticalProtection(sellOrder.getShortOption(),
					sellOrder.getShortVertical(), numberOfContracts, d, sellOrder.getShortPremium(),
					sellOrder.getPutOrCall(), OrderType.MARKET, Instruction.SELL_TO_OPEN);
			// z.setOrderStrategyType(OrderStrategyType.TRIGGER);
			first.getChildOrderStrategies().add(x);
			Order xx = makeAdvancedSellOrder(sellOrder.advanceSellOrderForUpwardDirection(), numberOfContracts,
					numberOfCycles - 1);
			// Order xxx = makeAdvancedSellOrder(sellOrder.oppositeSellOrder(),
			// numberOfContracts, numberOfCycles);
			SellOrder opposite = sellOrder.oppositeSellOrder();
			Order xxx = Util.makeOptionWithClosingOrderForSellCallOrPutWithVerticalProtection(opposite.getShortOption(),
					opposite.getShortVertical(), numberOfContracts, d, opposite.getShortPremium(),
					opposite.getPutOrCall(), OrderType.MARKET, Instruction.SELL_TO_OPEN);
			x.getChildOrderStrategies().add(xx);
			x.getChildOrderStrategies().add(y);
			y.getChildOrderStrategies().add(z);
			y.getChildOrderStrategies().add(xxx);
			first = z;

		}
		return order;
	}

	private static Order makeAdvancedSellOrderOld(SellOrder sellOrder, int numberOfContracts, int numberOfCycles) {
		Duration d = Duration.DAY;
		Order order = Util.makeOptionWithClosingOrderForSellCallsOrPuts(sellOrder.getShortOption(), numberOfContracts,
				d, sellOrder.getShortPremium(), sellOrder.getPutOrCall(), sellOrder.getOpeningOrderType(),
				Instruction.SELL_TO_OPEN);
		Order first = order;
		// first.setOrderStrategyType(OrderStrategyType.TRIGGER);
		for (int i = 0; i < numberOfCycles; i++) {
			OrderType stop = OrderType.STOP_LIMIT;
			int longContracts = (i + 1) * numberOfContracts;
			//// Calls
			Order x = Util.makeOption(sellOrder.getLongOption(), longContracts, d, sellOrder.getLongPremium(),
					sellOrder.getPutOrCall(), stop, Instruction.BUY_TO_OPEN);
			// x.setOrderStrategyType(OrderStrategyType.TRIGGER);
			Order y = Util.makeOption(sellOrder.getLongOption(), longContracts, d, sellOrder.getLongStopLoss(),
					sellOrder.getPutOrCall(), stop, Instruction.SELL_TO_CLOSE);
			// y.setOrderStrategyType(OrderStrategyType.TRIGGER);
			Order z = Util.makeOptionWithClosingOrderForSellCallsOrPuts(sellOrder.getShortOption(), numberOfContracts,
					d, sellOrder.getShortPremium(), sellOrder.getPutOrCall(), OrderType.MARKET,
					Instruction.SELL_TO_OPEN);
			// z.setOrderStrategyType(OrderStrategyType.TRIGGER);
			first.getChildOrderStrategies().add(x);
			Order xx = makeAdvancedSellOrderOld(sellOrder.advanceSellOrderForUpwardDirection(), numberOfContracts,
					numberOfCycles - 1);
			// Order xxx = makeAdvancedSellOrder(sellOrder.oppositeSellOrder(),
			// numberOfContracts, numberOfCycles);
			SellOrder opposite = sellOrder.oppositeSellOrder();
			Order xxx = Util.makeOptionWithClosingOrderForSellCallsOrPuts(opposite.getShortOption(), numberOfContracts,
					d, opposite.getShortPremium(), opposite.getPutOrCall(), OrderType.MARKET, Instruction.SELL_TO_OPEN);
			x.getChildOrderStrategies().add(xx);
			x.getChildOrderStrategies().add(y);
			y.getChildOrderStrategies().add(z);
			y.getChildOrderStrategies().add(xxx);
			first = z;

		}
		return order;
	}

	private static void testSellOrderForQQQ(String accountId, OptionSellCallPut oscp, int numberOfContracts,
			int numberOfCycles) throws Exception {
		Order callOrder = Util.makeOption(oscp.getShortCallOption(), numberOfContracts, Duration.DAY, oscp.callPremium,
				OptionInstrument.PutCall.CALL, OrderType.LIMIT, Instruction.SELL_TO_OPEN);
		// callOrder.setOrderStrategyType(OrderStrategyType.TRIGGER);
		Order putOrder = Util.makeOption(oscp.getShortPutOption(), numberOfContracts, Duration.DAY, oscp.putPremium,
				OptionInstrument.PutCall.PUT, OrderType.LIMIT, Instruction.SELL_TO_OPEN);
		// putOrder.setOrderStrategyType(OrderStrategyType.TRIGGER);
		Order first = callOrder;
		first.setOrderStrategyType(OrderStrategyType.TRIGGER);
		Order second = putOrder;
		second.setOrderStrategyType(OrderStrategyType.TRIGGER);
		for (int i = 0; i < numberOfCycles; i++) {
			OrderType stop = OrderType.STOP;
			int longContracts = (i + 1) * numberOfContracts;
			//// Calls
			Order x = Util.makeOption(oscp.getLongCallOption(), longContracts, Duration.DAY,
					oscp.getLongCallOptionPrice(), OptionInstrument.PutCall.CALL, stop, Instruction.BUY_TO_OPEN);
			x.setOrderStrategyType(OrderStrategyType.TRIGGER);
			Order y = Util.makeOption(oscp.getLongCallOption(), longContracts, Duration.DAY,
					oscp.getLongCallStopLossPrice(), OptionInstrument.PutCall.CALL, stop, Instruction.SELL_TO_CLOSE);
			y.setOrderStrategyType(OrderStrategyType.TRIGGER);
			Order z = Util.makeOption(oscp.getShortCallOption(), numberOfContracts, Duration.DAY, oscp.callPremium,
					OptionInstrument.PutCall.CALL, OrderType.MARKET, Instruction.SELL_TO_OPEN);
			z.setOrderStrategyType(OrderStrategyType.TRIGGER);
			first.getChildOrderStrategies().add(x);
			x.getChildOrderStrategies().add(y);
			y.getChildOrderStrategies().add(z);
			first = z;

			//// Puts

			Order xx = Util.makeOption(oscp.getLongPutOption(), longContracts, Duration.DAY,
					oscp.getLongPutOptionPrice(), OptionInstrument.PutCall.PUT, stop, Instruction.BUY_TO_OPEN);
			xx.setOrderStrategyType(OrderStrategyType.TRIGGER);
			Order yy = Util.makeOption(oscp.getLongPutOption(), longContracts, Duration.DAY,
					oscp.getLongPutStopLossPrice(), OptionInstrument.PutCall.PUT, stop, Instruction.SELL_TO_CLOSE);
			yy.setOrderStrategyType(OrderStrategyType.TRIGGER);
			Order zz = Util.makeOption(oscp.getShortPutOption(), numberOfContracts, Duration.DAY, oscp.putPremium,
					OptionInstrument.PutCall.PUT, OrderType.MARKET, Instruction.SELL_TO_OPEN);
			zz.setOrderStrategyType(OrderStrategyType.TRIGGER);
			second.getChildOrderStrategies().add(xx);
			xx.getChildOrderStrategies().add(yy);
			yy.getChildOrderStrategies().add(zz);
			second = zz;
		}
		String acct = accountId;
		HttpTdaClient client = Util.getHttpTDAClient();
		client.placeOrder(acct, callOrder);
		client.placeOrder(acct, putOrder);

	}

	private static void testSellPutOrderForQQQ() throws Exception {
		OptionInstrument.PutCall put = OptionInstrument.PutCall.PUT;
		String acct = Util.getAccountId1();
		Order option = Util.makeOption("QQQ_090523P376", 1, Duration.DAY, 1d, put, OrderType.LIMIT,
				Instruction.SELL_TO_OPEN);
		option.setOrderStrategyType(OrderStrategyType.TRIGGER);

		Order first = Util.makeOption("QQQ_090523P385", 1, Duration.DAY, 9.0, put, OrderType.STOP_LIMIT,
				Instruction.BUY_TO_OPEN);
		first.setOrderStrategyType(OrderStrategyType.TRIGGER);
		option.getChildOrderStrategies().add(first);

		Order second = Util.makeOption("QQQ_090523P385", 1, Duration.DAY, 8.75, put, OrderType.STOP_LIMIT,
				Instruction.SELL_TO_CLOSE);
		second.setOrderStrategyType(OrderStrategyType.TRIGGER);
		first.getChildOrderStrategies().add(second);

		Order second_call = Util.makeOption("QQQ_090523P376", 1, Duration.DAY, 1d, put, OrderType.MARKET,
				Instruction.SELL_TO_OPEN);
		second.getChildOrderStrategies().add(second_call);

		Order third = Util.makeOption("QQQ_090523P385", 2, Duration.DAY, 9.0, put, OrderType.STOP_LIMIT,
				Instruction.BUY_TO_OPEN);
		third.setOrderStrategyType(OrderStrategyType.TRIGGER);
		second.getChildOrderStrategies().add(third);

		Order fourth = Util.makeOption("QQQ_090523P385", 2, Duration.DAY, 8.75, put, OrderType.STOP_LIMIT,
				Instruction.SELL_TO_CLOSE);
		fourth.setOrderStrategyType(OrderStrategyType.TRIGGER);
		third.getChildOrderStrategies().add(fourth);

		Order close_sell_calls = Util.makeOption("QQQ_090523P376", 2, Duration.DAY, 1d, put, OrderType.MARKET,
				Instruction.BUY_TO_CLOSE);
		fourth.getChildOrderStrategies().add(close_sell_calls);

		HttpTdaClient client = Util.getHttpTDAClient();
		client.placeOrder(acct, option);

	}

	private static void testSellCallOrderForQQQ() throws Exception {
		String acct = Util.getAccountId1();
		Order option = Util.makeOption("QQQ_090523C379", 1, Duration.DAY, 1d, OptionInstrument.PutCall.CALL,
				OrderType.LIMIT, Instruction.SELL_TO_OPEN);
		option.setOrderStrategyType(OrderStrategyType.TRIGGER);

		Order first = Util.makeOption("QQQ_090523C370", 1, Duration.DAY, 9.0, OptionInstrument.PutCall.CALL,
				OrderType.STOP_LIMIT, Instruction.BUY_TO_OPEN);
		first.setOrderStrategyType(OrderStrategyType.TRIGGER);
		option.getChildOrderStrategies().add(first);

		Order second = Util.makeOption("QQQ_090523C370", 1, Duration.DAY, 8.75, OptionInstrument.PutCall.CALL,
				OrderType.STOP_LIMIT, Instruction.SELL_TO_CLOSE);
		second.setOrderStrategyType(OrderStrategyType.TRIGGER);
		first.getChildOrderStrategies().add(second);

		Order second_call = Util.makeOption("QQQ_090523C379", 1, Duration.DAY, 1d, OptionInstrument.PutCall.CALL,
				OrderType.MARKET, Instruction.SELL_TO_OPEN);
		second.getChildOrderStrategies().add(second_call);

		Order third = Util.makeOption("QQQ_090523C370", 2, Duration.DAY, 9.0, OptionInstrument.PutCall.CALL,
				OrderType.STOP_LIMIT, Instruction.BUY_TO_OPEN);
		third.setOrderStrategyType(OrderStrategyType.TRIGGER);
		second.getChildOrderStrategies().add(third);

		Order fourth = Util.makeOption("QQQ_090523C370", 2, Duration.DAY, 8.75, OptionInstrument.PutCall.CALL,
				OrderType.STOP_LIMIT, Instruction.SELL_TO_CLOSE);
		fourth.setOrderStrategyType(OrderStrategyType.TRIGGER);
		third.getChildOrderStrategies().add(fourth);

		Order close_sell_calls = Util.makeOption("QQQ_090523C379", 2, Duration.DAY, 1d, OptionInstrument.PutCall.CALL,
				OrderType.MARKET, Instruction.BUY_TO_CLOSE);
		fourth.getChildOrderStrategies().add(close_sell_calls);

		HttpTdaClient client = Util.getHttpTDAClient();
		client.placeOrder(acct, option);

	}

	private static void testSellCallOrderChain() throws Exception {
		String acct = Util.getAccountId1();
		Order option = Util.makeSellOptionOrder("AFRM_090123C22", 1);
		option.setOrderStrategyType(OrderStrategyType.TRIGGER);
		// option.setPrice(new BigDecimal(0.25));
		// option.setOrderType(OrderType.LIMIT);
		Order first = Util.makeStockOrder("AFRM", new BigDecimal(21.90), 100, Instruction.BUY, OrderType.STOP_LIMIT);
		first.setOrderStrategyType(OrderStrategyType.TRIGGER);
		// first.setStatus(Status.AWAITING_PARENT_ORDER);
		Order second = Util.makeStockOrder("AFRM", new BigDecimal(21), 100, Instruction.SELL_SHORT,
				OrderType.STOP_LIMIT);
		second.setOrderStrategyType(OrderStrategyType.TRIGGER);
		// second.setStatus(Status.AWAITING_PARENT_ORDER);

		List<Object> cos = option.getChildOrderStrategies();

		cos.add(first);
		cos.add(second);
		// cos.add(first);
		// cos.add(second);

		Order close = Util.makeSellOptionOrder("AFRM_090123C22", 1);
		close.getOrderLegCollection().iterator().next().setInstruction(Instruction.BUY_TO_CLOSE);
		close.setOrderStrategyType(OrderStrategyType.TRIGGER);
		// cos.add(close);
		HttpTdaClient client = Util.getHttpTDAClient();
		client.placeOrder(acct, option);

	}

	private static void testOrderChain() throws Exception {
		String acct = Util.getAccountId4();
		Order first = Util.makeStockOrder("HOOD", new BigDecimal(13), 1, Instruction.BUY, OrderType.STOP_LIMIT);
		Order second = Util.makeStockOrder("HOOD", new BigDecimal(11), 1, Instruction.SELL, OrderType.STOP_LIMIT);
		second.setOrderStrategyType(OrderStrategyType.TRIGGER);
		Order third = Util.makeStockOrder("HOOD", new BigDecimal(13), 1, Instruction.BUY, OrderType.STOP_LIMIT);
		first.setOrderStrategyType(OrderStrategyType.TRIGGER);
		first.getChildOrderStrategies().add(second);
		second.getChildOrderStrategies().add(third);
		HttpTdaClient client = Util.getHttpTDAClient();
		client.placeOrder(acct, first);

	}

	private static void replaceOrder() {
		String accountId = Util.getAccountId4();
		OrderHandler oh = new OrderHandler();
		List<Order> orders = oh.getCurrentWorkingOrders(accountId);
		Order o = orders.get(0);
		BigDecimal bd = o.getPrice();
		OrderLegCollection olc = o.getOrderLegCollection().iterator().next();
		Order ord = Util.makeStockOrder(olc.getInstrument().getSymbol(), new BigDecimal(bd.doubleValue() - 0.10),
				o.getQuantity().intValue(), olc.getInstruction(), o.getOrderType());
		// ord.setOrderId(o.getOrderId());
		TdaClient client = Util.getHttpTDAClient();
		client.replaceOrder(accountId, ord, o.getOrderId() + "");
	}

}
