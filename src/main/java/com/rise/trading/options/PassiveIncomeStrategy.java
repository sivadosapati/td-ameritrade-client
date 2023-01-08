package com.rise.trading.options;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.model.account.Duration;
import com.studerw.tda.model.account.EquityInstrument;
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

public class PassiveIncomeStrategy extends BaseHandler {

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

		int numberOfStocks = numberOfContracts * 100;
		client.placeOrder(accountId, callOrder);
		int numberOfStocksForLong = longStockOrderCanBePlaced(accountId, numberOfStocks, stockTicker);
		if (numberOfStocksForLong > 0) {
			Order longStockOrder = makeLongStockOrder(stockTicker, new BigDecimal(callPrice.doubleValue() - 0.5f),
					numberOfStocksForLong);
			client.placeOrder(accountId, longStockOrder);
		}
		client.placeOrder(accountId, putOrder);
		int numberOfStocksForShort = shortStockOrderCanBePlaced(accountId, numberOfStocks, stockTicker);
		if( numberOfStocksForShort > 0) {
			
			Order putStockOrder = makePutStockOrder(stockTicker, new BigDecimal(putPrice.doubleValue() + 0.5f),
					numberOfStocksForShort);
			client.placeOrder(accountId, putStockOrder);
		}
	
		
	}

	private int shortStockOrderCanBePlaced(String accountId, int numberOfStocks, String stockTicker) {
		PositionsHandler handler = new PositionsHandler();
		GroupedPositions gps = handler.getGroupedPositions(accountId);
		GroupedPosition gp = gps.getGroupedPosition(stockTicker);
		Position p = gp.getShortEquity();
		if( p == null) {
			return numberOfStocks;
		}
		double quantity = p.getShortQuantity().doubleValue();
		if( quantity > numberOfStocks) {
			return 0;
		}
		return (int)(numberOfStocks - quantity);

	}

	private int longStockOrderCanBePlaced(String accountId, int numberOfStocks, String stockTicker) {
		PositionsHandler handler = new PositionsHandler();
		GroupedPositions gps = handler.getGroupedPositions(accountId);
		GroupedPosition gp = gps.getGroupedPosition(stockTicker);
		Position p = gp.getEquity();
		if( p == null) {
			return numberOfStocks;
		}
		double quantity = p.getLongQuantity().doubleValue();
		if( quantity > numberOfStocks) {
			return 0;
		}
		return (int)(numberOfStocks - quantity);

	}
	


	private Order makePutStockOrder(String stockTicker, BigDecimal bigDecimal, int numberOfStocks) {
		return makeStockOrder(stockTicker, bigDecimal, numberOfStocks, Instruction.SELL_SHORT);
	}

	private Order makeStockOrder(String stockTicker, BigDecimal price, int numberOfStocks, Instruction instruction) {
		BigDecimal quantity = new BigDecimal(numberOfStocks);
		Order order = new Order();
		order.setOrderType(OrderType.STOP_LIMIT);
		order.setSession(Session.NORMAL);
		order.setDuration(Duration.DAY);
		order.setQuantity(quantity);
		order.setPrice(price);
		order.setStopPrice(price);
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

	private Order makeLongStockOrder(String stockTicker, BigDecimal callPrice, int numberOfStocks) {

		return makeStockOrder(stockTicker, callPrice, numberOfStocks, Instruction.BUY);
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
		OptionChainReq req = OptionChainReq.Builder.optionChainReq().withSymbol(symbol)
				// .withDaysToExpiration(findDaysToExpiration())
				.withFromDate(LocalDateTime.now()).withToDate(LocalDateTime.now().plusDays(findDaysToExpiration()))
				.withRange(Range.NTM)

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
		return 0;
	}

	public static void main(String args[]) {
		
		
	}
}
