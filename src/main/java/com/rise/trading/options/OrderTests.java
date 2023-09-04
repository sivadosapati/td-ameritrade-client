package com.rise.trading.options;

import java.math.BigDecimal;
import java.util.List;

import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.client.TdaClient;
import com.studerw.tda.model.account.Duration;
import com.studerw.tda.model.account.OptionInstrument;
import com.studerw.tda.model.account.Order;
import com.studerw.tda.model.account.OrderLegCollection;
import com.studerw.tda.model.account.OrderLegCollection.Instruction;
import com.studerw.tda.model.account.OrderStrategyType;
import com.studerw.tda.model.account.OrderType;

public class OrderTests {

	public static void main(String[] args) throws Exception {
		// replaceOrder();
		// testOrderChain();
		// testSellCallOrderChain();
		// fetchOrders();
		testSellCallOrderForQQQ();
		testSellPutOrderForQQQ();

	}

	private static void fetchOrders() throws Exception {
		String acct = Util.getAccountId1();
		HttpTdaClient client = Util.getHttpTDAClient();
		List<Order> orders = new OrderHandler().getCurrentWorkingOrders(acct);
		System.out.println(Util.toJSON(orders));
	}
	
	
	static class OptionSellCallPut{
		double callStrike;
		double putStrike;
		double callLong;
		double putLong;
		double callPremium;
		double putPremium;
		String date;
		String stock;
		public OptionSellCallPut(String stock, String date, double callPremium, double putPremium, double callStrike, double putStrike, double callLong, double putLong) {
			this.stock = stock;
			this.date = date;
			this.callPremium = callPremium;
			this.putPremium = putPremium;
			this.callStrike = callStrike;
			this.putStrike = putStrike;
			this.callLong = callLong;
			this.putLong = putLong;
		}
		public String getShortCallOption() {
			return stock+"_"+date+"C"+callStrike;
		}
		public String getLongCallOption() {
			return stock+"_"+date+"C"+callLong;
		}
		public String getShortPutOption() {
			return stock+"_"+date+"P"+putStrike;
		}
		public String getLongPutOption() {
			return stock+"_"+date+"P"+putLong;
		}	
	}
	
	private static void testSellOrderForQQQ(OptionSellCallPut oscp, int numberOfContracts, int numberOfCycles ) throws Exception{
		Order callOrder = Util.makeOption(oscp.getShortCallOption(), numberOfContracts, Duration.DAY, oscp.callPremium, OptionInstrument.PutCall.CALL, OrderType.LIMIT, Instruction.SELL_TO_OPEN);
		callOrder.setOrderStrategyType(OrderStrategyType.TRIGGER);
		Order putOrder = Util.makeOption(oscp.getShortPutOption(), numberOfContracts, Duration.DAY, oscp.putPremium, OptionInstrument.PutCall.PUT, OrderType.LIMIT, Instruction.SELL_TO_OPEN);
		putOrder.setOrderStrategyType(OrderStrategyType.TRIGGER);
		for(int i =0;i<numberOfCycles;i++) {
			
		}
		String acct = Util.getAccountId1();
		HttpTdaClient client = Util.getHttpTDAClient();
		client.placeOrder(acct, callOrder);
		client.placeOrder(acct, putOrder);

	}

	private static void testSellPutOrderForQQQ() throws Exception {
		OptionInstrument.PutCall put = OptionInstrument.PutCall.PUT;
		String acct = Util.getAccountId1();
		Order option = Util.makeOption("QQQ_090523P376", 1, Duration.DAY, 1, put,
				OrderType.LIMIT, Instruction.SELL_TO_OPEN);
		option.setOrderStrategyType(OrderStrategyType.TRIGGER);

		Order first = Util.makeOption("QQQ_090523P385", 1, Duration.DAY, 9.0, put,
				OrderType.STOP_LIMIT, Instruction.BUY_TO_OPEN);
		first.setOrderStrategyType(OrderStrategyType.TRIGGER);
		option.getChildOrderStrategies().add(first);

		Order second = Util.makeOption("QQQ_090523P385", 1, Duration.DAY, 8.75, put,
				OrderType.STOP_LIMIT, Instruction.SELL_TO_CLOSE);
		second.setOrderStrategyType(OrderStrategyType.TRIGGER);
		first.getChildOrderStrategies().add(second);

		Order second_call = Util.makeOption("QQQ_090523P376", 1, Duration.DAY, 1, put,
				OrderType.MARKET, Instruction.SELL_TO_OPEN);
		second.getChildOrderStrategies().add(second_call);

		Order third = Util.makeOption("QQQ_090523P385", 2, Duration.DAY, 9.0, put,
				OrderType.STOP_LIMIT, Instruction.BUY_TO_OPEN);
		third.setOrderStrategyType(OrderStrategyType.TRIGGER);
		second.getChildOrderStrategies().add(third);

		Order fourth = Util.makeOption("QQQ_090523P385", 2, Duration.DAY, 8.75, put,
				OrderType.STOP_LIMIT, Instruction.SELL_TO_CLOSE);
		fourth.setOrderStrategyType(OrderStrategyType.TRIGGER);
		third.getChildOrderStrategies().add(fourth);

		Order close_sell_calls = Util.makeOption("QQQ_090523P376", 2, Duration.DAY, 1, put,
				OrderType.MARKET, Instruction.BUY_TO_CLOSE);
		fourth.getChildOrderStrategies().add(close_sell_calls);

		HttpTdaClient client = Util.getHttpTDAClient();
		client.placeOrder(acct, option);

	}

	private static void testSellCallOrderForQQQ() throws Exception {
		String acct = Util.getAccountId1();
		Order option = Util.makeOption("QQQ_090523C379", 1, Duration.DAY, 1, OptionInstrument.PutCall.CALL,
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

		Order second_call = Util.makeOption("QQQ_090523C379", 1, Duration.DAY, 1, OptionInstrument.PutCall.CALL,
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

		Order close_sell_calls = Util.makeOption("QQQ_090523C379", 2, Duration.DAY, 1, OptionInstrument.PutCall.CALL,
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
