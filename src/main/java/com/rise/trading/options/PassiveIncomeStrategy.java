package com.rise.trading.options;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.model.account.Duration;
import com.studerw.tda.model.account.EquityInstrument;
import com.studerw.tda.model.account.Instrument.AssetType;
import com.studerw.tda.model.account.OptionInstrument;
import com.studerw.tda.model.account.OptionInstrument.PutCall;
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

public class PassiveIncomeStrategy extends BaseHandler {
	public static void main(String args[]) {
		PassiveIncomeStrategy pis = new PassiveIncomeStrategy();
		pis.placeClosingTradesForOptionsOnDailyExpiringOptions(Util.getAccountId4(),"QQQ");
		pis.placeClosingTradesForEquityOnDailyExpiringOptions(Util.getAccountId4(),"QQQ");
	}
	
	public void closeLongEquitiesIfTheyAreInProfitAndPlaceAnotherOrderIfThereAreSellOptions(String accountId, String ticker, double gainDollars, double incrementAmountFromCurrentStockPrice) {
		GroupedPosition gp = getGroupedPosition(accountId,ticker);
		if( gp == null) {
			return;
		}
	}



	public void placeClosingTradesForOptionsOnDailyExpiringOptions(String accountId, String stock) {

		LocalDate day = getTheImmediateBusinessDay().toLocalDate();
		GroupedPositions gps = getGroupedPositions(accountId);
		List<Position> position = getOptionPositionsThatExpireOnThisDay(gps.getGroupedPosition(stock), day);
		placeClosingTradesForOptionsOnDailyExpiringOptons(accountId, position);
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
	
	private void placeClosingTradesForEquityOnDailyExpiringOptions(String accountId, List<Position> position, GroupedPositions gps) {
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
					if (leo.size() == 0) {
						BigDecimal d = new BigDecimal(
								rnd(strikePrice.doubleValue() - p.getAveragePrice().doubleValue() / 2));
						Order o = makeStockOrder(us, d, stockCount, Instruction.SELL_TO_CLOSE, OrderType.STOP_LIMIT);
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
					if (seo.size() == 0) {
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
		return LocalDateTime.now().plusDays(findDaysToExpiration());

	}

	public void placeDailyTradeForPassiveIncome(String accountId, String stockTicker, float callDistance,
			float putDistance, int numberOfContracts) {
		HttpTdaClient client = getClient();
		OptionChainReq request = makeOptionChainRequestForDailyTrade(stockTicker);
		// OptionChain chain = client.getOptionChain(stockTicker);
		OptionChain chain = client.getOptionChain(request);
		// System.out.println(Util.toJSON(chain));
		BigDecimal price = chain.getUnderlyingPrice();
		BigDecimal callPrice = findCallPrice(price, callDistance);
		Option callOption = getCallOption(chain, callPrice);
		BigDecimal putPrice = findPutPrice(price, putDistance);
		Option putOption = getPutOptionOption(chain, putPrice);
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

	private void placeShortStockOrderForPassiveIncome(String accountId, String stockTicker, BigDecimal stockPrice,
			int numberOfStocks) {
		HttpTdaClient client = getClient();
		int numberOfStocksForShort = shortStockOrderCanBePlaced(accountId, numberOfStocks, stockTicker);
		if (numberOfStocksForShort > 0) {

			Order putStockOrder = makePutStockOrder(stockTicker, new BigDecimal(stockPrice.doubleValue() + 0.5f),
					numberOfStocksForShort);
			client.placeOrder(accountId, putStockOrder);
		}
	}

	private void placeLongStockOrderForPassiveIncome(String accountId, String stockTicker, BigDecimal stockPrice,
			int numberOfStocks) {
		HttpTdaClient client = getClient();
		int numberOfStocksForLong = longStockOrderCanBePlaced(accountId, numberOfStocks, stockTicker);
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

	private Option getPutOptionOption(OptionChain chain, BigDecimal bd) {
		Map<String, Map<BigDecimal, List<Option>>> optionsMapForDifferentExpiry = chain.getPutExpDateMap();
		return findOption(bd, optionsMapForDifferentExpiry);
	}

	private Option getCallOption(OptionChain chain, BigDecimal bd) {
		Map<String, Map<BigDecimal, List<Option>>> optionsMapForDifferentExpiry = chain.getCallExpDateMap();
		return findOption(bd, optionsMapForDifferentExpiry);
	}

	private Option findOption(BigDecimal bd, Map<String, Map<BigDecimal, List<Option>>> optionsMapForDifferentExpiry) {
		Map<BigDecimal, List<Option>> optionsMap = optionsMapForDifferentExpiry.values().iterator().next();
		for (Map.Entry<BigDecimal, List<Option>> e : optionsMap.entrySet()) {
			BigDecimal price = e.getKey();
			if (price.intValue() == bd.intValue()) {
				return e.getValue().iterator().next();
			}
		}
		return null;
	}

	private BigDecimal findPutPrice(BigDecimal price, float putDistance) {
		double p = Math.ceil(price.floatValue() - putDistance);
		return new BigDecimal(p);
	}

	private BigDecimal findCallPrice(BigDecimal price, float callDistance) {
		double p = Math.floor(price.floatValue() + callDistance);
		return new BigDecimal(p);
	}

	private OptionChainReq makeOptionChainRequestForDailyTrade(String symbol) {
		LocalDateTime to = getTheImmediateBusinessDay();
		LocalDateTime from = to;
		OptionChainReq req = OptionChainReq.Builder.optionChainReq().withSymbol(symbol)
				// .withDaysToExpiration(findDaysToExpiration())
				.withFromDate(from).withToDate(to).withRange(Range.OTM)

				.build();
		return req;
	}

	private Integer findDaysToExpiration() {
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
		if (time.getHour() > 12) {
			return 1;
		}
		return 0;
	}

}
