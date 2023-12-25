package com.rise.trading.options;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.model.account.Duration;
import com.studerw.tda.model.account.Instrument;
import com.studerw.tda.model.account.OptionInstrument;
import com.studerw.tda.model.account.Order;
import com.studerw.tda.model.account.OrderLegCollection;
import com.studerw.tda.model.account.OrderRequest;
import com.studerw.tda.model.account.OrderStrategyType;
import com.studerw.tda.model.account.OrderType;
import com.studerw.tda.model.account.Position;
import com.studerw.tda.model.account.SecuritiesAccount;
import com.studerw.tda.model.account.Session;
import com.studerw.tda.model.account.Status;
import com.studerw.tda.model.account.Instrument.AssetType;
import com.studerw.tda.model.account.OrderLegCollection.Instruction;

public class BaseHandler {

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

	protected List<Position> getOptionPositionsThatExpireOnThisDay(GroupedPosition gp, LocalDate day) {

		List<Position> matched = new ArrayList<Position>();
		if (gp == null)
			return matched;
		List<Position> position = gp.getOptions();
		for (Position p : position) {
			String symbol = p.getInstrument().getSymbol();
			LocalDate positionDayOfExpiry = parseDateForOptionSymbol(symbol);
			if (positionDayOfExpiry.isEqual(day)) {
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
		GroupedPositions gp = new GroupedPositions(positions);
		return gp;

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
	
	public List<Order> getFilledOrders(String accountId){
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
		//System.out.println(Util.toJSON(workingOrders));
		List<Order> ro = new ArrayList<Order>();
		//Instrument ip = p.getInstrument();
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
