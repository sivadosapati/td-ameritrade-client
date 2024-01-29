package com.rise.trading.options;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.model.account.ComplexOrderStrategyType;
import com.studerw.tda.model.account.Duration;
import com.studerw.tda.model.account.EquityInstrument;
import com.studerw.tda.model.account.Instrument.AssetType;
import com.studerw.tda.model.account.OptionInstrument;
import com.studerw.tda.model.account.Order;
import com.studerw.tda.model.account.OrderLegCollection;
import com.studerw.tda.model.account.OrderLegCollection.Instruction;
import com.studerw.tda.model.account.OrderStrategyType;
import com.studerw.tda.model.account.OrderType;
import com.studerw.tda.model.account.Session;

public class Util {

	private static Properties props = fetchProperties();
	private static Accounts accounts = fetchAccounts();

	public static void main(String args[]) throws Exception {
		playWithDates();
	}

	public static String getEODToken() {
		return props.getProperty("eod.api.token");
	}

	public static void pauseForSeconds(int seconds) {
		try {
			Thread.sleep(seconds * 1000);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void playWithDates() throws Exception {
		String ss = "6-11-2019";
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("d-M-yyyy");
		LocalDate x = LocalDate.parse(ss, dtf);
		System.out.println(x);

		LocalDate ld = LocalDate.now();
		// System.out.println(ld.);
		DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String s = ld.format(df);
		System.out.println(s);

		df = DateTimeFormatter.ofPattern("yyyy-MM-dd");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date d = sdf.parse("2019-08-02 13:35:00");
		System.out.println(d.getTime());

		d = new Date(946713600 * 1000);
		System.out.println(d);

	}

	public static List<String> readLinesFromURL(String url) {
		try {
			URL u = new URL(url);
			BufferedReader in = new BufferedReader(new InputStreamReader(u.openStream()));
			List<String> lines = new ArrayList<String>();
			String inputLine;
			while ((inputLine = in.readLine()) != null)
				lines.add(inputLine);
			in.close();
			return lines;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new ArrayList<String>();
	}

	public static Properties getAccessProperties() {
		return props;
	}

	public static Properties fetchProperties() {
		try {
			Properties props = new Properties();
			String path = System.getProperty("user.home");
			props.load(new FileInputStream(path + "/td-ameritrade/tda.properties"));
			return props;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static boolean areEqualDates(LocalDateTime a, LocalDateTime b) {
		if (a.getDayOfMonth() == b.getDayOfMonth()) {
			if (a.getMonth() == b.getMonth()) {
				if (a.getYear() == b.getYear()) {
					return true;
				}
			}
		}
		return false;
	}

	public static Accounts fetchAccounts() {
		try {
			String path = System.getProperty("user.home");
			String content = Files.readString(Paths.get(path + "/td-ameritrade/tda.account.json"));
			Accounts accounts = new ObjectMapper().readValue(content, Accounts.class);
			return accounts;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	public static HttpTdaClient getHttpTDAClient() {
		try {
			HttpTdaClient httpTdaClient = new HttpTdaClient(props);
			return httpTdaClient;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	public static String getAccountId1() {
		return props.getProperty("account1.account.id");
	}

	public static String getAccountId6() {
		return props.getProperty("account6.account.id");
	}

	public static String getAccountId7() {
		return props.getProperty("account7.account.id");
	}

	public static List<String> getSymbolsToBeSkipped(String accountId) {

		if (accountId.equals(getAccountId1())) {
			String prop = props.getProperty("account1.symbols.skip", "");
			return Arrays.asList(prop.split(","));
		}
		if (accountId.equals(getAccountId2())) {
			String prop = props.getProperty("account2.symbols.skip", "");
			return Arrays.asList(prop.split(","));
		}
		if (accountId.equals(getAccountId3())) {
			String prop = props.getProperty("account3.symbols.skip", "");
			return Arrays.asList(prop.split(","));
		}
		return new ArrayList<String>();

	}

	public static String toJSON(Object object) {
		try {
			return new ObjectMapper().writeValueAsString(object);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String getAccountId3() {
		return props.getProperty("account3.account.id");
	}

	public static String getAccountId2() {
		return props.getProperty("account2.account.id");
	}

	public static String getAccountId4() {
		return props.getProperty("account4.account.id");
	}

	public static String[] getAllAccounts() {
		Account[] accounts = getAccounts().getAccounts();
		String[] accountIds = new String[accounts.length];
		int counter = 0;
		for (Account a : accounts) {
			accountIds[counter++] = a.id;
		}
		return accountIds;
	}

	public static Accounts getAccounts() {
		return accounts;
	}

	public static Order makeSellOptionOrder(String optionSymbol, int quantity) {
		Order o = new Order();
		o.setSession(Session.NORMAL);
		o.setComplexOrderStrategyType(ComplexOrderStrategyType.NONE);
		o.setDuration(Duration.DAY);
		o.setOrderType(OrderType.MARKET);
		OrderLegCollection olc = new OrderLegCollection();
		olc.setInstruction(Instruction.SELL_TO_OPEN);
		olc.setQuantity(new BigDecimal(quantity));
		OptionInstrument oi = new OptionInstrument();
		olc.setInstrument(oi);
		oi.setAssetType(AssetType.OPTION);
		oi.setSymbol(optionSymbol);
		oi.setOptionDeliverables(null);
		oi.setPutCall(OptionInstrument.PutCall.CALL);

		o.getOrderLegCollection().add(olc);

		return o;
	}

	public static Order makeOption(String optionSymbol, int quantity, Duration d, double price,
			OptionInstrument.PutCall pc, OrderType type, Instruction i) {
		Order o = new Order();
		o.setSession(Session.NORMAL);
		o.setComplexOrderStrategyType(ComplexOrderStrategyType.NONE);
		o.setOrderStrategyType(OrderStrategyType.TRIGGER);
		o.setDuration(d);
		o.setOrderType(type);

		BigDecimal bd = new BigDecimal(price);
		bd = bd.setScale(2, RoundingMode.HALF_UP);
		//System.out.println(bd);

		if (type == OrderType.STOP) {
			o.setStopPrice(bd);
		}
		if (type == OrderType.STOP_LIMIT) {
			o.setStopPrice(bd);
			o.setPrice(bd);
		}
		if (type == OrderType.LIMIT) {
			o.setPrice(bd);
		}

		OrderLegCollection olc = new OrderLegCollection();
		olc.setInstruction(i);
		olc.setQuantity(new BigDecimal(quantity));
		OptionInstrument oi = new OptionInstrument();
		olc.setInstrument(oi);
		oi.setAssetType(AssetType.OPTION);
		oi.setSymbol(optionSymbol);
		oi.setOptionDeliverables(null);
		oi.setPutCall(pc);

		o.getOrderLegCollection().add(olc);

		return o;

	}

	public static Order makeOptionWithClosingOrderForSellCallsOrPuts(String optionSymbol, int quantity, Duration d,
			double doublePrice, OptionInstrument.PutCall pc, OrderType type, Instruction i) {
		Order o = makeOption(optionSymbol, quantity, d, doublePrice, pc, type, i);
		Order oo = makeOption(optionSymbol, quantity, d, 0.04d, pc, OrderType.LIMIT, Instruction.BUY_TO_CLOSE);
		o.getChildOrderStrategies().add(oo);
		return o;
	}

	public static Order makeOptionForPassiveIncome(String ticker, int quantity) {
		System.out.println("DO CODE ME SOON");
		return null;
	}

	public static Order makeOptionWithClosingOrderForSellCallOrPutWithVerticalProtectionOld(String optionSymbol,
			String protectionOptionSymbol, int quantity, Duration d, double doublePrice, double protectionPrice,
			OptionInstrument.PutCall pc, OrderType type, Instruction i) {
		Order o = makeOption(optionSymbol, quantity, d, doublePrice, pc, type, i);
		// o.setComplexOrderStrategyType(ComplexOrderStrategyType.VERTICAL);
		// o.setOrderType(OrderType.NET_CREDIT);
		i = reverseInstruction(i);
		Order vertical = makeOption(protectionOptionSymbol, quantity, d, protectionPrice, pc, type, i);
		o.getChildOrderStrategies().add(vertical);
		return o;
	}

	public static Order makeOptionWithClosingOrderForSellCallOrPutWithVerticalProtection(String optionSymbol,
			String protectionOptionSymbol, int quantity, Duration d, double doublePrice, OptionInstrument.PutCall pc,
			OrderType type, Instruction i) {
		Order o = makeOptionWithClosingOrderForSellCallsOrPuts(optionSymbol, quantity, d, doublePrice, pc, type, i);
		// o.setComplexOrderStrategyType(ComplexOrderStrategyType.VERTICAL);
		// o.setOrderType(OrderType.NET_CREDIT);
		i = reverseInstruction(i);
		Order vertical = makeOption(protectionOptionSymbol, quantity, d, doublePrice, pc, type, i);
		Order newOrder = new Order();
		newOrder.setSession(o.getSession());
		newOrder.setOrderType(OrderType.NET_CREDIT);
		newOrder.setPrice(o.getPrice());
		newOrder.setDuration(d);
		newOrder.setOrderStrategyType(o.getOrderStrategyType());
		newOrder.getOrderLegCollection().addAll(o.getOrderLegCollection());
		newOrder.getOrderLegCollection().addAll(vertical.getOrderLegCollection());
		newOrder.getChildOrderStrategies().addAll(o.getChildOrderStrategies());

		return newOrder;
		// return o;
	}

	private static Instruction reverseInstruction(Instruction i) {
		if (i == Instruction.SELL_TO_OPEN) {
			return Instruction.BUY_TO_OPEN;
		}
		if (i == Instruction.BUY_TO_OPEN) {
			return Instruction.SELL_TO_OPEN;
		}
		throw new RuntimeException(i + " needs to be coded");
	}

	public static Order makeStockOrder(String stockTicker, BigDecimal price, int numberOfStocks,
			Instruction instruction, OrderType orderType) {
		BigDecimal quantity = new BigDecimal(numberOfStocks);
		Order order = new Order();
		order.setOrderType(orderType);
		order.setSession(Session.NORMAL);
		order.setDuration(Duration.DAY);
		order.setQuantity(quantity);
		order.setPrice(price);
		if (orderType == OrderType.STOP_LIMIT) {
			order.setStopPrice(price);
		}
		order.setOrderStrategyType(OrderStrategyType.SINGLE);
		// order.setComplexOrderStrategyType(ComplexOrderStrategyType.NONE);

		OrderLegCollection olc = new OrderLegCollection();

		olc.setInstruction(instruction);
		olc.setQuantity(quantity);
		order.getOrderLegCollection().add(olc);

		EquityInstrument instrument = new EquityInstrument();
		instrument.setSymbol(stockTicker);
		instrument.setAssetType(AssetType.EQUITY);
		olc.setInstrument(instrument);
		// LOGGER.debug(order.toString());
		return order;

	}
}
