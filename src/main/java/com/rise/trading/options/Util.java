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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rise.trading.options.symbols.OptionSymbolWithAdjacents;
import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.model.account.ComplexOrderStrategyType;
import com.studerw.tda.model.account.Duration;
import com.studerw.tda.model.account.EquityInstrument;
import com.studerw.tda.model.account.Instrument;
import com.studerw.tda.model.account.Instrument.AssetType;
import com.studerw.tda.model.account.OptionInstrument;
import com.studerw.tda.model.account.Order;
import com.studerw.tda.model.account.OrderLegCollection;
import com.studerw.tda.model.account.OrderLegCollection.Instruction;
import com.studerw.tda.model.account.OrderStrategyType;
import com.studerw.tda.model.account.OrderType;
import com.studerw.tda.model.account.Position;
import com.studerw.tda.model.account.Session;
import com.studerw.tda.model.option.Option;
import com.studerw.tda.model.option.OptionChain;
import com.studerw.tda.model.option.OptionChainReq;
import com.studerw.tda.model.option.OptionChainReq.Range;
import com.studerw.tda.model.quote.EquityQuote;
import com.studerw.tda.model.quote.EtfQuote;
import com.studerw.tda.model.quote.Quote;

public class Util {

	// Tweak this when we are in different time zone, FOR EST, this number should be
	// 3, for CST, it should be 2
	public static int HOURS_TO_ADJUST_FOR_PST = 0;

	private static Properties props = fetchProperties();
	private static Accounts accounts = fetchAccounts();
	private static HttpTdaClient httpTDAClient = null;

	public static void main(String args[]) throws Exception {
		// playWithDates();
		String symbol = "SPY_021624P501";
		Option o = findRightNearestOption(symbol);
		System.out.println(Util.toJSON(o));
	}

	public static String getEODToken() {
		return props.getProperty("eod.api.token");
	}

	public static double getLatestTickerPrice(String symbol) {
		return getPrice(getHttpTDAClient().fetchQuote(symbol));
	}

	public static Map<String, Double> getLatestTickerPrices(Collection<String> symbols) {
		Map<String, Double> map = new HashMap<String, Double>();
		List<Quote> quotes = getHttpTDAClient().fetchQuotes(new ArrayList<String>(symbols));
		for (Quote q : quotes) {
			map.put(q.getSymbol(), getPrice(q));
		}
		return map;
	}

	public static double getPrice(Quote quote) {

		if (quote instanceof EtfQuote) {
			EtfQuote x = (EtfQuote) quote;
			return x.getBidPrice().doubleValue();
		}
		if (quote instanceof EquityQuote) {
			EquityQuote x = (EquityQuote) quote;
			return x.getBidPrice().doubleValue();
		}
		System.out.println(quote.getClass().getName() + toJSON(quote));
		throw new RuntimeException("CODE ME");

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
			if (httpTDAClient != null) {
				return httpTDAClient;
			}
			httpTDAClient = new HttpTdaClient(props);
			// System.out.println(Util.toJSON(httpTDAClient));
			return httpTDAClient;
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

	public static Order makeOrderForBuyingStockAtLimitPrice(String stockSymbol, double price, double quantity) {

		Order order = new Order();
		order.setOrderType(OrderType.LIMIT);
		// order.setSession(Session.NORMAL);
		order.setSession(Session.SEAMLESS);
		order.setDuration(Duration.GOOD_TILL_CANCEL);
		order.setOrderStrategyType(OrderStrategyType.SINGLE);
		order.setPrice(new BigDecimal(price));

		OrderLegCollection olc = new OrderLegCollection();

		olc.setInstruction(Instruction.BUY);
		olc.setQuantity(new BigDecimal(quantity));

		order.getOrderLegCollection().add(olc);

		Instrument instrument = new EquityInstrument();
		instrument.setSymbol(stockSymbol);
		olc.setInstrument(instrument);

		return order;

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

	public static Order makeOption(String optionSymbol, int quantity, Duration d, Double price,
			OptionInstrument.PutCall pc, OrderType type, Instruction i) {
		Order o = new Order();
		o.setSession(Session.NORMAL);
		o.setComplexOrderStrategyType(ComplexOrderStrategyType.NONE);
		o.setOrderStrategyType(OrderStrategyType.TRIGGER);
		o.setDuration(d);
		o.setOrderType(type);

		BigDecimal bd = null;
		if (price != null) {
			bd = new BigDecimal(price);
			bd = bd.setScale(2, RoundingMode.HALF_UP);
		}

		// System.out.println(bd);

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

	public static Order makeSmartOptionWhenTheSymbolDoesntExist(String optionSymbol, int quantity, Duration d,
			Double price, OptionInstrument.PutCall pc, OrderType type, Instruction i) {

		Option o = findRightNearestOption(optionSymbol);
		System.out.println("makeSmartOptionWhenTheSymbolDoesntExist -> " + Util.toJSON(o));
		Order oo = makeOption(o.getSymbol(), quantity, d, price, pc, type, i);

		return oo;
	}

	public static OptionChainReq makeOptionChainRequest(String symbol, LocalDateTime from, LocalDateTime to) {

		OptionChainReq req = OptionChainReq.Builder.optionChainReq().withSymbol(symbol)
				// .withDaysToExpiration(findDaysToExpiration())
				// .withFromDate(from).withToDate(to).withRange(Range.OTM)
				.withFromDate(from).withToDate(to).withRange(Range.ALL)

				.build();
		return req;
	}

	public static OptionChain getOptionChain(String symbol, LocalDateTime from, LocalDateTime to) {
		OptionChainReq req = makeOptionChainRequest(symbol, from, to);
		OptionChain chain = getHttpTDAClient().getOptionChain(req);
		return chain;
	}

	public static Option findRightNearestOption(String os) {
		OptionData od = OptionSymbolParser.parse(os);
		LocalDateTime x = od.getDateTime();
		OptionChainReq request = makeOptionChainRequest(od.stockTicker, x, x);
		// OptionChain chain = client.getOptionChain(stockTicker);
		OptionChain chain = getHttpTDAClient().getOptionChain(request);
		BigDecimal price = od.price;
		if (od.isCall()) {
			return getCallOption(chain, price);
		} else {
			return getPutOption(chain, price);
		}
	}

	public static OptionSymbolWithAdjacents findOptionSymbolsWithAdjacents(String os) {
		OptionData od = OptionSymbolParser.parse(os);
		LocalDateTime x = od.getDateTime();
		OptionChainReq request = makeOptionChainRequest(od.stockTicker, x, x);
		// OptionChain chain = client.getOptionChain(stockTicker);
		OptionChain chain = getHttpTDAClient().getOptionChain(request);
		BigDecimal price = od.price;
		Map map = null;
		if (od.isCall()) {
			map = chain.getCallExpDateMap();
		} else {
			map = chain.getPutExpDateMap();
		}
		Option high = findNearestHigherOption(price, map);
		Option low = findNearestLowerOption(price, map);
		if (od.isCall()) {
			return new OptionSymbolWithAdjacents(os, getSymbol(high), getSymbol(low));
		} else {
			return new OptionSymbolWithAdjacents(os, getSymbol(low), getSymbol(high));
		}

	}
	
	private static String getSymbol(Option o) {
		if( o == null)return null;
		return o.getSymbol();
	}

	public static boolean notMarketHours() {
		LocalDate date = LocalDate.now();
		if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
			return false;

		}
		if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
			return false;
		}
		LocalTime time = LocalTime.now();

		// System.out.println(time.getHour());
		// After 12 PM - pick the next day
		if (time.getHour() >= 14 + HOURS_TO_ADJUST_FOR_PST) {
			return false;
		}
		if (time.getHour() < 6 + HOURS_TO_ADJUST_FOR_PST) {
			return false;
		}
		if (time.getHour() == 6 + HOURS_TO_ADJUST_FOR_PST) {
			if (time.getMinute() >= 30) {
				return true;
			} else {
				return false;
			}
		}
		if (time.getHour() == 13 + HOURS_TO_ADJUST_FOR_PST) {
			if (time.getMinute() < 15 + HOURS_TO_ADJUST_FOR_PST) {
				return true;
			} else {
				return false;
			}
		}

		return true;
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

	public static String getTickerSymbol(Position p) {
		Instrument i = p.getInstrument();
		if (i instanceof OptionInstrument) {
			OptionInstrument oi = (OptionInstrument) i;
			return oi.getUnderlyingSymbol();
		}
		throw new RuntimeException("CODE ME");

	}

	public static List<String> tickersThatTradeAfterHoursForOptions = Arrays.asList("QQQ", "SPY");

	public static boolean isLastFewMinutesOfMarketHours(String ticker) {
		LocalDateTime ldt = LocalDateTime.now();
		int hour = ldt.getHour();
		int min = ldt.getMinute();

		if (tickersThatTradeAfterHoursForOptions.contains(ticker)) {
			if (hour == 13 + HOURS_TO_ADJUST_FOR_PST && min > 10) {
				return true;
			}
			return false;
		} else {
			if (hour == 12 + HOURS_TO_ADJUST_FOR_PST && min > 45) {
				return true;
			}
			return false;
		}

	}

	public static boolean isLastHourOfTrading() {
		LocalDateTime ldt = LocalDateTime.now();
		if (ldt.getHour() >= 12 + HOURS_TO_ADJUST_FOR_PST) {
			return true;
		}
		return false;
	}

	public static Integer findDaysToExpiration() {
		LocalDate date = LocalDate.now();
		if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
			return 2;

		}
		if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
			return 1;
		}
		LocalTime time = LocalTime.now();

		// System.out.println(time.getHour());
		// After 12 PM - pick the next day
		if (time.getHour() > 13 + HOURS_TO_ADJUST_FOR_PST) {
			return 1;
		}
		return 0;
	}

	public static Option getPutOption(OptionChain chain, BigDecimal bd) {
		Map<String, Map<BigDecimal, List<Option>>> optionsMapForDifferentExpiry = chain.getPutExpDateMap();
		Option o = findOption(bd, optionsMapForDifferentExpiry);
		if (o != null) {
			return o;
		}
		o = findNearestLowerOption(bd, optionsMapForDifferentExpiry);
		System.out.println(Util.toJSON(o));
		return o;
	}

	public static Option getCallOption(OptionChain chain, BigDecimal bd) {
		Map<String, Map<BigDecimal, List<Option>>> optionsMapForDifferentExpiry = chain.getCallExpDateMap();
		Option o = findOption(bd, optionsMapForDifferentExpiry);
		if (o != null) {
			return o;
		}
		o = findNearestHigherOption(bd, optionsMapForDifferentExpiry);
		// System.out.println(Util.toJSON(o));
		return o;
	}

	public static Option findOption(BigDecimal bd,
			Map<String, Map<BigDecimal, List<Option>>> optionsMapForDifferentExpiry) {
		if (optionsMapForDifferentExpiry.size() == 0) {
			return null;
		}
		Map<BigDecimal, List<Option>> optionsMap = optionsMapForDifferentExpiry.values().iterator().next();

		for (Map.Entry<BigDecimal, List<Option>> e : optionsMap.entrySet()) {
			BigDecimal price = e.getKey();
			Option o = e.getValue().iterator().next();
			if (price.intValue() == bd.intValue()) {
				return o;
			}

		}
		return null;
	}

	public static Option findNearestHigherOption(BigDecimal bd,
			Map<String, Map<BigDecimal, List<Option>>> optionsMapForDifferentExpiry) {
		OptionFinder of = (x, y) -> {
			return (x.doubleValue() > y.doubleValue());
		};
		Option o = findNearestOption(bd, optionsMapForDifferentExpiry, of, new TreeMap<Double, Option>());
		return o;
	}

	public static Option findNearestLowerOption(BigDecimal bd,
			Map<String, Map<BigDecimal, List<Option>>> optionsMapForDifferentExpiry) {
		OptionFinder of = (x, y) -> {
			return (x.doubleValue() < y.doubleValue());
		};
		Option o = findNearestOption(bd, optionsMapForDifferentExpiry, of,
				new TreeMap<Double, Option>(Collections.reverseOrder()));
		return o;

	}

	public static Option findNearestOption(BigDecimal bd,
			Map<String, Map<BigDecimal, List<Option>>> optionsMapForDifferentExpiry, OptionFinder finder,
			TreeMap<Double, Option> optionMap) {
		if (optionsMapForDifferentExpiry.size() == 0) {
			return null;
		}
		Map<BigDecimal, List<Option>> optionsMap = optionsMapForDifferentExpiry.values().iterator().next();
		// TreeMap<Double, Option> optionMap = new TreeMap<Double, Option>();
		for (Map.Entry<BigDecimal, List<Option>> e : optionsMap.entrySet()) {
			BigDecimal price = e.getKey();
			Option o = e.getValue().iterator().next();
			if (finder.match(price, bd)) {
				optionMap.put(price.doubleValue(), o);
			}
		}
		if (optionMap.size() > 0) {
			return optionMap.values().iterator().next();
		}
		return null;
	}

	interface OptionFinder {
		boolean match(BigDecimal optionPrice, BigDecimal price);
	}

}
