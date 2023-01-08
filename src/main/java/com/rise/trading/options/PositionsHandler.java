package com.rise.trading.options;

import java.util.List;

import com.studerw.tda.model.account.Position;
import com.studerw.tda.model.account.SecuritiesAccount;

public class PositionsHandler extends BaseHandler {

	public GroupedPositions getGroupedPositions(String accountId) {
		final SecuritiesAccount account = getClient().getAccount(accountId, true, true);
		// System.out.println(account.);
		List<Position> positions = account.getPositions();
		// System.out.println(positions);
		GroupedPositions gp = new GroupedPositions(positions);
		return gp;

	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
