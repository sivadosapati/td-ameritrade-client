package com.rise.trading.options;

import java.util.List;

import com.studerw.tda.model.account.Order;
import com.studerw.tda.model.account.Position;

public class PositionsHandler extends BaseHandler {

	public static void mainOld(String[] args) {
		PositionsHandler ph = new PositionsHandler();
		GroupedPositions gps = ph.getGroupedPositions(Util.getAccountId6());
		// System.out.println(Util.toJSON(gps));
		GroupedPosition gp = gps.getGroupedPosition("QQQ");

		// System.out.println(Util.toJSON(gp));

		// List<Order> o = ph.getCurrentWorkingOrders(Util.getAccountId4());
		List<Order> o = ph.getFilledOrders(Util.getAccountId4());
		System.out.println(Util.toJSON(o));
	}

	public static void main(String args[]) throws Exception {
		PositionsHandler ph = new PositionsHandler();
		GroupedPositions gps = ph.getGroupedPositionsWithCurrentStockPrice(Util.getAccountId6());
		System.out.println(Util.toJSON(gps));
		for (GroupedPosition gp : gps.getGroupedPositions()) {
			String x = gp.getSymbol();
			Position p = gp.getEquity();
			//System.out.println(x+"\n"+Util.toJSON(p));
		}
	}

}
