package com.rise.trading.options;

import java.util.List;

import com.studerw.tda.model.account.Order;

public class PositionsHandler extends BaseHandler {

	public static void main(String[] args) {
		PositionsHandler ph = new PositionsHandler();
		GroupedPositions gps = ph.getGroupedPositions(Util.getAccountId4());
		//System.out.println(Util.toJSON(gps));
		GroupedPosition gp = gps.getGroupedPosition("QQQ");

		// System.out.println(Util.toJSON(gp));

		// List<Order> o = ph.getCurrentWorkingOrders(Util.getAccountId4());
		List<Order> o = ph.getFilledOrders(Util.getAccountId4());
		System.out.println(Util.toJSON(o));
	}

}
