package com.rise.trading.options;

import java.util.List;

import com.studerw.tda.model.account.Position;
import com.studerw.tda.model.account.SecuritiesAccount;

public class PositionsHandler extends BaseHandler {


	public static void main(String[] args) {
		PositionsHandler ph = new PositionsHandler();
		GroupedPositions gps = ph.getGroupedPositions(Util.getAccountId4());
		GroupedPosition gp = gps.getGroupedPosition("SPY");
		System.out.println(Util.toJSON(gp));

	}

}
