package com.rise.trading.options;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.studerw.tda.model.account.Duration;
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
import com.studerw.tda.model.option.OptionChainReq.ContractType;
import com.studerw.tda.model.option.OptionChainReq.Range;

public class OrderHandler extends BaseHandler {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public void cancelAllOpenOrders(String accountId) {

		List<Order> orders = getCurrentWorkingOrders(accountId);
		for (Order order : orders) {
			getClient().cancelOrder(accountId, order.getOrderId() + "");
		}
	}

	public OptionChain getOptionChain(String symbol, OptionChainReq.ContractType contractType, LocalDateTime toDate,
			Range range) {
		OptionChainReq ocr = OptionChainReq.Builder.optionChainReq().withSymbol(symbol).withContractType(contractType)
				.withRange(range).withToDate(toDate).withIncludeQuotes(true)
				// .withUnderlyingPrice(underlyingPrice)
				.build();
		OptionChain oc = getClient().getOptionChain(ocr);
		return oc;
	}

	public void placeClosingTradesOnShortOptions(String accountId) {
		GroupedPositions gp = getGroupedPositions(accountId);
		List<Order> orders = getCurrentWorkingOrders(accountId);
		for (String symbol : gp.getSymbols()) {
			GroupedPosition groupedPosition = gp.getGroupedPosition(symbol);
			placeOrModifyOrdersForOptionsIfExisting(groupedPosition, orders, accountId);
		}

	}

	public void placeProtectionCallTradesOnShortOptions(String accountId) {
		GroupedPositions gp = getGroupedPositions(accountId);
		List<Order> orders = getCurrentWorkingOrders(accountId);
		for (String symbol : gp.getSymbols()) {
			GroupedPosition groupedPosition = gp.getGroupedPosition(symbol);
			placeProtectionLongOptionForShortOptionIfNotExisting(groupedPosition, orders, accountId);
		}
	}

	public void workOnOptionsAndTheirCorrespondingClosingOrder(String accountId,
			ProcessExistingClosingOrderForOption existing, ProcessNoClosingOrderForOption nonExisting) {
		GroupedPositions gp = getGroupedPositions(accountId);
		List<Order> orders = getCurrentWorkingOrders(accountId);
		for (String symbol : gp.getSymbols()) {
			GroupedPosition groupedPosition = gp.getGroupedPosition(symbol);
			List<Position> options = groupedPosition.getOptions();
			if (options.size() == 0) {
				continue;
			}
			for (Position optionPosition : options) {
				OptionInstrument oic = (OptionInstrument) optionPosition.getInstrument();
				// System.out.println(oic.getSymbol());
				Order existingOrder = null;
				for (Order o : orders) {
					Instrument i = o.getOrderLegCollection().get(0).getInstrument();
					// System.out.println("\t"+i.getSymbol());
					if (oic.getSymbol().equals(i.getSymbol())) {
						existingOrder = o;
						break;
					}
				}
				if (existingOrder == null) {
					nonExisting.process(accountId, groupedPosition, optionPosition);
				} else {
					existing.process(accountId, groupedPosition, optionPosition, existingOrder);
				}
			}

		}
	}

	interface ProcessNoClosingOrderForOption {
		void process(String accountId, GroupedPosition gp, Position optionPosition);
	}

	interface ProcessExistingClosingOrderForOption {
		void process(String accountId, GroupedPosition gp, Position optionPosition, Order existingOrder);
	}

	private void placeOrModifyOrdersForOptionsIfExisting(GroupedPosition groupedPosition, List<Order> orders,
			String accountId) {
		List<Position> options = groupedPosition.getOptions();
		if (options.size() == 0) {
			return;
		}
		for (Position p : options) {
			OptionInstrument oic = (OptionInstrument) p.getInstrument();
			// System.out.println(oic.getSymbol());
			Order existingOrder = null;
			for (Order o : orders) {
				Instrument i = o.getOrderLegCollection().get(0).getInstrument();
				// System.out.println("\t"+i.getSymbol());
				if (oic.getSymbol().equals(i.getSymbol())) {
					existingOrder = o;
					break;
				}
			}
			if (existingOrder == null) {
				createAndPlaceClosingOrder(p, accountId);
			} else {
				optimizeExistingOrder(existingOrder, p, accountId);
			}

		}

	}

	private void placeProtectionLongOptionForShortOptionIfNotExisting(GroupedPosition groupedPosition,
			List<Order> orders, String accountId) {
		List<Position> options = groupedPosition.getOptions();
		if (options.size() == 0) {
			return;
		}
		for (Position p : options) {
			OptionInstrument oic = (OptionInstrument) p.getInstrument();

			// System.out.println(oic.getSymbol());
			Order existingOrder = null;
			for (Order o : orders) {
				Instrument i = o.getOrderLegCollection().get(0).getInstrument();
				// System.out.println("\t"+i.getSymbol());
				if (oic.getSymbol().equals(i.getSymbol())) {
					existingOrder = o;
					break;
				}
			}
			if (existingOrder == null) {
				// createAndPlaceClosingOrder(p, accountId);
			} else {
				// optimizeExistingOrder(existingOrder, p, accountId);
			}

		}

	}

	private void optimizeExistingOrder(Order existingOrder, Position p, String accountId) {
		System.out.println("Optimize Existing Order");
		// throw CODE_ME;
	}

	public void createAndPlaceClosingOrder(Position p, String accountId) {
		String json = toJSON(p);
		System.out.println(json);
		if (p.getLongQuantity().intValue() > 0) {
			return;
		}
		Order order = createClosingOrder(p);
		getClient().placeOrder(accountId, order);
	}

	private Order createClosingOrder(Position position) {
		double d = position.getAveragePrice().doubleValue();
		return createClosingOrder(position, rnd(d * 4), rnd(0.04d));
	}

	private Order createClosingOrderOld(Position position) {
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
		if (longQuantity > 0) {
			order.setPrice(new BigDecimal(rnd(position.getAveragePrice().doubleValue() * 1.5)));
		} else {
			order.setPrice(new BigDecimal(rnd(position.getAveragePrice().doubleValue() * 0.4)));
		}

		order.setOrderStrategyType(OrderStrategyType.SINGLE);
		// order.setComplexOrderStrategyType(ComplexOrderStrategyType.NONE);

		OrderLegCollection olc = new OrderLegCollection();

		if (longQuantity > 0) {
			System.out.println("LONG " + longQuantity);
			olc.setQuantity(new BigDecimal(longQuantity));
			olc.setInstruction(Instruction.SELL_TO_CLOSE);
		} else {
			System.out.println("SHORT " + shortQuantity);
			olc.setQuantity(new BigDecimal(shortQuantity));
			olc.setInstruction(Instruction.BUY_TO_CLOSE);
		}

		order.getOrderLegCollection().add(olc);

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

	public Order createOrderForCoveredCall(Option option, GroupedPosition gp) {
		if (option == null)
			return null;
		BigDecimal callQuantity = new BigDecimal(gp.getNumberOfPotentialCoveredCallContracts());
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
		olc.setQuantity(callQuantity);
		order.getOrderLegCollection().add(olc);

		OptionInstrument instrument = new OptionInstrument();

		instrument.setSymbol(option.getSymbol());
		instrument.setAssetType(AssetType.OPTION);
		// instrument.setPutCall(PutCall.CALL);
		// instrument.setUnderlyingSymbol(gp.symbol);
		instrument.setOptionDeliverables(null);
		olc.setInstrument(instrument);
		// LOGGER.debug(order.toString());
		return order;
	}

	public void placeLongCallsAndPutsForShortOptions(String accountId) {

		throw CODE_ME;
	}

	public void rollOptonsIfAnyDuringTheLastHourOfExpiration(String accountId) {
		// LocalDateTime now = LocalDateTime.now();

		NoOrderForRollingOptions no = new NoOrderForRollingOptions();
		ExistingOrderForRollingOptions eo = new ExistingOrderForRollingOptions();
		workOnOptionsAndTheirCorrespondingClosingOrder(accountId, eo, no);

	}

	class NoOrderForRollingOptions implements ProcessNoClosingOrderForOption {

		@Override
		public void process(String accountId, GroupedPosition gp, Position optionPosition) {
			System.out.println(optionPosition.getInstrument().getSymbol() + " has no order");

		}

	}

	public BigDecimal getNearestStrikePrice(BigDecimal price, Set<BigDecimal> prices) {
		BigDecimal ret = price;// 62.19
		for (BigDecimal bd : prices) {
			if (bd.compareTo(ret) > 0) {
				ret = bd;
			}
			if (ret != price) {
				return ret;
			}
		}
		return null;
	}

	class ExistingOrderForRollingOptions implements ProcessExistingClosingOrderForOption {

		@Override
		public void process(String accountId, GroupedPosition gp, Position optionPosition, Order existingOrder) {
			String optionSymbol = optionPosition.getInstrument().getSymbol();
			System.out.println(optionSymbol + " has a closing order");
			// Do a basic roll to next week by increasing the strike price to the next
			// nearest price from the current order
			String equity = gp.getSymbol();
			OptionChain chain = getOptionChain(equity, ContractType.CALL, LocalDateTime.now().plusDays(14), Range.ALL);
			Map<String, Map<BigDecimal, List<Option>>> callExpDataMap = chain.getCallExpDateMap();
			Iterator<Map<BigDecimal, List<Option>>> itr = callExpDataMap.values().iterator();
			itr.next();
			Map<BigDecimal, List<Option>> optionsForNextWeek = itr.next();
			Set<BigDecimal> keys = optionsForNextWeek.keySet();
			OptionInstrument oi = (OptionInstrument) optionPosition.getInstrument();
			// String json = toJSON(optionPosition);
			String json = toJSON(existingOrder);
			System.out.println(json);
			BigDecimal currentOptionPrice = extractFromOptionSymbol(optionSymbol);
			BigDecimal strikePrice = getNearestStrikePrice(currentOptionPrice, keys);
			System.out.println(strikePrice);
			List<Option> options = optionsForNextWeek.get(strikePrice);
			if (options != null) {
				Option o = options.get(0);
				System.out.println(toJSON(o));
				Order order = createOrderForCoveredCall(o, gp);
				modifyCurrentOrder(existingOrder);
				throw new RuntimeException("Finish code");
			}

		}

		private BigDecimal extractFromOptionSymbol(String optionSymbol) {
			int i = optionSymbol.indexOf('C');
			// BigDecimal price = null;
			if (i == -1) {
				i = optionSymbol.indexOf('P');
			}
			BigDecimal bd = new BigDecimal(optionSymbol.substring(i + 1));
			return bd;
		}

	}

	private RuntimeException CODE_ME = new RuntimeException("Code me..");

	public void modifyCurrentOrder(Order existingOrder) {
		existingOrder.setOrderType(OrderType.MARKET);

	}

}
